package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.Comparator;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public abstract class EntryItem {

    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    @NonNull
    public final String id;
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Name for this Entry, e.g. app name
    @NonNull
    private
    String name = "";
    // How relevant is this record? The higher, the most probable it will be displayed
    private int relevance = 0;

    public EntryItem(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        if (name != null) {
            // Set the actual user-friendly name
            this.name = name;
            this.normalizedName = StringNormalizer.normalizeWithResult(this.name, false);
        } else {
            this.name = "null";
            this.normalizedName = null;
        }
    }

    public void setName(String name, boolean generateNormalization) {
        if (generateNormalization) {
            setName(name);
        } else {
            this.name = name;
            this.normalizedName = null;
        }
    }

    public int getRelevance() {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    /**
     * ID to use in the history
     * (may be different from the one used in the adapter for display)
     */
    public String getHistoryId() {
        return this.id;
    }

    @LayoutRes
    public abstract int getResultLayout();

    public abstract void displayResult(Context context, View view, FuzzyScore score);

    public static class RelevanceComparator implements java.util.Comparator<EntryItem> {
        @Override
        public int compare(EntryItem lhs, EntryItem rhs) {
            if (lhs.getRelevance() == rhs.getRelevance()) {
                if (lhs.normalizedName != null && rhs.normalizedName != null)
                    return lhs.normalizedName.compareTo(rhs.normalizedName);
                else
                    return lhs.name.compareTo(rhs.name);
            }
            return lhs.getRelevance() - rhs.getRelevance();
        }

    }
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

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

    ListPopup inflatePopupMenu(@NonNull ArrayAdapter<ListPopup.Item> adapter, @NonNull Context context) {
        ListPopup menu = new ListPopup(context);
        menu.setAdapter(adapter);

        // If app already pinned, do not display the "add to favorite" option
        // otherwise don't show the "remove favorite button"
        String favApps = PreferenceManager.getDefaultSharedPreferences(context).
                getString("favorite-apps-list", "");
        if (favApps.contains(id + ";")) {
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
            adapter.add(new ListPopup.Item("Relevance: " + getRelevance()));
        }

        return menu;
    }

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    @NonNull
    public ListPopup getPopupMenu(final Context context, final ResultAdapter resultAdapter, final View parentView) {
        ArrayAdapter<ListPopup.Item> menuAdapter = new ArrayAdapter<>(context, R.layout.popup_list_item);
        ListPopup menu = buildPopupMenu(context, menuAdapter, resultAdapter, parentView);

        menu.setOnItemClickListener((adapter, view, position) -> {
            @StringRes int stringId = ((ListPopup.Item) adapter.getItem(position)).stringId;
            popupMenuClickHandler(view.getContext(), resultAdapter, stringId);
        });

        return menu;
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    @CallSuper
    boolean popupMenuClickHandler(@NonNull Context context, @NonNull ResultAdapter resultAdapter, @StringRes int stringId) {
        switch (stringId) {
            case R.string.menu_remove:
                ResultHelper.removeFromResultsAndHistory(this, context, resultAdapter);
                return true;
            case R.string.menu_favorites_add:
                ResultHelper.launchAddToFavorites(context, this);
                break;
            case R.string.menu_favorites_remove:
                ResultHelper.launchRemoveFromFavorites(context, this);
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

    public void doLaunch(@NonNull View view) {
        throw new RuntimeException("No launch action defined for " + getClass().getSimpleName());
    }
}
