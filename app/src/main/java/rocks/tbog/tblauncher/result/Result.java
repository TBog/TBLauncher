package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import androidx.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;
import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UIColors;

public abstract class Result {
    /**
     * Current information pojo
     */
    @NonNull
    final EntryItem pojo;

    Result(@NonNull EntryItem pojo) {
        this.pojo = pojo;
    }

    public static Result fromPojo(ISearchActivity parent, EntryItem pojo) {
        //TODO: enable this
        if (pojo instanceof AppEntry)
            return new AppResult((AppEntry) pojo);
//        else if (pojo instanceof ContactEntry)
//            return new ContactsResult(parent, (ContactEntry) pojo);
//        else if (pojo instanceof ShortcutEntry)
//            return new ShortcutsResult((ShortcutEntry) pojo);

//        else if (pojo instanceof SearchPojo)
//            return new SearchResult((SearchPojo) pojo);
//        else if (pojo instanceof SettingPojo)
//            return new SettingsResult((SettingPojo) pojo);
//        else if (pojo instanceof PhonePojo)
//            return new PhoneResult((PhonePojo) pojo);
//        else if (pojo instanceof TagDummyPojo)
//            return new TagDummyResult((TagDummyPojo)pojo);


        throw new RuntimeException("Unable to create a result from POJO");
    }

    public String getPojoId() {
        return pojo.id;
    }

    @Override
    public String toString() {
        return pojo.getName();
    }

    /**
     * How to display this record ?
     *
     * @param context     android context
     * @param convertView a view to be recycled
     * @param parent      view that provides a set of LayoutParams values
     * @param fuzzyScore  information for highlighting search result
     * @return a view to display as item
     */
    @NonNull
    public abstract View display(Context context, View convertView, @NonNull ViewGroup parent, FuzzyScore fuzzyScore);

    @NonNull
    public View inflateFavorite(@NonNull Context context, @NonNull ViewGroup parent) {
        View favoriteView = LayoutInflater.from(context).inflate(R.layout.favorite_item, parent, false);
        Drawable drawable = getDrawable(context);
        ImageView favoriteImage = favoriteView.findViewById(R.id.favorite);
        if (drawable == null)
            favoriteImage.setImageResource(R.drawable.launcher_white);
        else
            favoriteImage.setImageDrawable(drawable);
        favoriteView.setContentDescription(pojo.getName());
        return favoriteView;
    }

    public void displayHighlighted(String text, List<Pair<Integer, Integer>> positions, TextView view, Context context) {
        SpannableString enriched = new SpannableString(text);
        int primaryColor = UIColors.getPrimaryColor(context);

        for (Pair<Integer, Integer> position : positions) {
            enriched.setSpan(
                    new ForegroundColorSpan(primaryColor),
                    position.first,
                    position.second,
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
        }
        view.setText(enriched);
    }

    public boolean displayHighlighted(StringNormalizer.Result normalized, String text, FuzzyScore fuzzyScore,
                                      TextView view, Context context) {
        FuzzyScore.MatchInfo matchInfo = fuzzyScore.match(normalized.codePoints);

        if (!matchInfo.match) {
            view.setText(text);
            return false;
        }

        SpannableString enriched = new SpannableString(text);
        int primaryColor = UIColors.getPrimaryColor(context);

        for (Pair<Integer, Integer> position : matchInfo.getMatchedSequences()) {
            enriched.setSpan(
                    new ForegroundColorSpan(primaryColor),
                    normalized.mapPosition(position.first),
                    normalized.mapPosition(position.second),
                    Spannable.SPAN_INCLUSIVE_INCLUSIVE
            );
        }
        view.setText(enriched);
        return true;
    }

    public String getSection() {
        try {
            // get the normalized first letter of the pojo
            // Ensure accented characters are never displayed. (É => E)
            // convert to uppercase otherwise lowercase a -z will be sorted after upper A-Z
            int codePoint = Character.toUpperCase(pojo.normalizedName.codePoints[0]);
            return Character.toString(Character.toChars(codePoint)[0]);
        } catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            // Normalized name is empty.
            return "-";
        }
    }

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    public ListPopup getPopupMenu(final Context context, final ResultAdapter parent, final View parentView) {
        ArrayAdapter<ListPopup.Item> adapter = new ArrayAdapter<>(context, R.layout.popup_list_item);
        ListPopup menu = buildPopupMenu(context, adapter, parent, parentView);

        menu.setOnItemClickListener(new ListPopup.OnItemClickListener() {
            @Override
            public void onItemClick(ListAdapter adapter, View view, int position) {
                @StringRes int stringId = ((ListPopup.Item) adapter.getItem(position)).stringId;
                popupMenuClickHandler(view.getContext(), parent, stringId, parentView);
            }
        });

        return menu;
    }

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    ListPopup buildPopupMenu(Context context, ArrayAdapter<ListPopup.Item> adapter, final ResultAdapter parent, View parentView) {
        adapter.add(new ListPopup.Item(context, R.string.menu_remove));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_add));
        adapter.add(new ListPopup.Item(context, R.string.menu_favorites_remove));
        return inflatePopupMenu(adapter, context);
    }

    ListPopup inflatePopupMenu(ArrayAdapter<ListPopup.Item> adapter, Context context) {
        ListPopup menu = new ListPopup(context);
        menu.setAdapter(adapter);

        // If app already pinned, do not display the "add to favorite" option
        // otherwise don't show the "remove favorite button"
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");
        if (favApps.contains(this.pojo.id + ";")) {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                ListPopup.Item item = adapter.getItem(i);
                assert item != null;
                if (item.stringId == R.string.menu_favorites_add)
                    adapter.remove(item);
            }
        } else {
            for (int i = 0; i < adapter.getCount(); i += 1) {
                ListPopup.Item item = adapter.getItem(i);
                assert item != null;
                if (item.stringId == R.string.menu_favorites_remove)
                    adapter.remove(item);
            }
        }

        if (BuildConfig.DEBUG) {
            adapter.add(new ListPopup.Item("Relevance: " + pojo.getRelevance()));
        }

        return menu;
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    boolean popupMenuClickHandler(Context context, ResultAdapter parent, @StringRes int stringId, View parentView) {
        switch (stringId) {
            case R.string.menu_remove:
                removeFromResultsAndHistory(context, parent);
                return true;
            case R.string.menu_favorites_add:
                launchAddToFavorites(context, pojo);
                break;
            case R.string.menu_favorites_remove:
                launchRemoveFromFavorites(context, pojo);
                break;
        }

//        FullscreenActivity mainActivity = (FullscreenActivity) context;
//        // Update favorite bar
//        mainActivity.onFavoriteChange();
//        mainActivity.launchOccurred();
//        // Update Search to reflect favorite add, if the "exclude favorites" option is active
//        if (mainActivity.prefs.getBoolean("exclude-favorites", false) && mainActivity.isViewingSearchResults()) {
//            mainActivity.updateSearchRecords(true);
//        }

        return false;
    }

    private void launchAddToFavorites(Context context, EntryItem pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_added);
        TBApplication.getApplication(context).getDataHandler().addToFavorites(pojo.getHistoryId());
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    private void launchRemoveFromFavorites(Context context, EntryItem pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_removed);
        TBApplication.getApplication(context).getDataHandler().removeFromFavorites(pojo.getHistoryId());
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    /**
     * Remove the current result from the list
     *
     * @param context android context
     * @param parent  adapter on which to remove the item
     */
    private void removeFromResultsAndHistory(Context context, ResultAdapter parent) {
        removeFromHistory(context);
        Toast.makeText(context, R.string.removed_item, Toast.LENGTH_SHORT).show();
        parent.removeResult(context, this);
    }

    public final void launch(Context context, View v) {
        Log.i("log", "Launching " + pojo.id);

        recordLaunch(context);

        // Launch
        doLaunch(context, v);
    }

    /**
     * How to launch this record ? Most probably, will fire an intent. This
     * function must call recordLaunch()
     *
     * @param context android context
     */
    protected abstract void doLaunch(Context context, View v);

    /**
     * How to launch this record "quickly" ? Most probably, same as doLaunch().
     * Override to define another behavior.
     *
     * @param context android context
     */
    public void fastLaunch(Context context, View v) {
        this.launch(context, v);
    }

    /**
     * Return the icon for this Result, or null if non existing.
     *
     * @param context android context
     */
    public Drawable getDrawable(Context context) {
        return null;
    }

    boolean isDrawableCached() {
        return false;
    }

    void setDrawableCache(Drawable drawable) {
    }

    void setAsyncDrawable(ImageView view) {
        // the ImageView tag will store the async task if it's running
        if (view.getTag() instanceof AsyncSetImage) {
            AsyncSetImage asyncSetImage = (AsyncSetImage) view.getTag();
            if (this.equals(asyncSetImage.appResultWeakReference.get())) {
                // we are already loading the icon for this
                return;
            } else {
                asyncSetImage.cancel(true);
                view.setTag(null);
            }
        }
        // the ImageView will store the Result after the AsyncTask finished
        else if (this.equals(view.getTag())) {
            ((Result) view.getTag()).setDrawableCache(view.getDrawable());
            return;
        }
        if (isDrawableCached()) {
            view.setImageDrawable(getDrawable(view.getContext()));
            view.setTag(this);
        } else {
            view.setTag(createAsyncSetImage(view).execute());
        }
    }

    private AsyncSetImage createAsyncSetImage(ImageView imageView) {
        return new AsyncSetImage(imageView, this);
    }

    /**
     * Helper function to get a view
     *
     * @param context android context
     * @param id      id to inflate
     * @param parent  view that provides a set of LayoutParams values
     * @return the view specified by the id
     */
    View inflateFromId(Context context, @LayoutRes int id, @NonNull ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(context);
        return inflater.inflate(id, parent, false);
    }

    /**
     * Put this item in application history
     *
     * @param context android context
     */
    void recordLaunch(Context context) {
        // Save in history
        TBApplication.getApplication(context).getDataHandler().addToHistory(pojo.getHistoryId());
    }

    void removeFromHistory(Context context) {
        DBHelper.removeFromHistory(context, pojo.id);
    }

    /*
     * Get fill color from theme
     *
     */
    int getThemeFillColor(Context context) {
        int[] attrs = new int[]{R.attr.resultColor /* index 0 */};
        TypedArray ta = context.obtainStyledAttributes(attrs);
        int color = ta.getColor(0, Color.WHITE);
        ta.recycle();
        return color;
    }

    public long getUniqueId() {
        // we can consider hashCode unique enough in this context
        return this.pojo.id.hashCode();
    }

    static class AsyncSetImage extends AsyncTask<Void, Void, Drawable> {
        final WeakReference<ImageView> imageViewWeakReference;
        final WeakReference<Result> appResultWeakReference;

        AsyncSetImage(ImageView image, Result result) {
            super();
            image.setTag(this);
            image.setImageResource(android.R.color.transparent);
            this.imageViewWeakReference = new WeakReference<>(image);
            this.appResultWeakReference = new WeakReference<>(result);
        }

        @Override
        protected Drawable doInBackground(Void... voids) {
            ImageView image = imageViewWeakReference.get();
            if (isCancelled() || image == null || image.getTag() != this) {
                imageViewWeakReference.clear();
                return null;
            }
            Result result = appResultWeakReference.get();
            if (result == null)
                return null;
            return result.getDrawable(image.getContext());
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            ImageView image = imageViewWeakReference.get();
            if (isCancelled() || image == null || drawable == null) {
                imageViewWeakReference.clear();
                return;
            }
            image.setImageDrawable(drawable);
            image.setTag(appResultWeakReference.get());
        }
    }
}
