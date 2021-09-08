package rocks.tbog.tblauncher.quicklist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.LauncherState;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.PlaceholderEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UIColors;

public class QuickList {
    private TBLauncherActivity mTBLauncherActivity;
    private boolean mOnlyForResults = false;
    private boolean mListDirty = true;
    private LinearLayout mQuickList;
    private final ArrayList<EntryItem> mQuickListItems = new ArrayList<>(0);
    private SharedPreferences mSharedPreferences = null;

    // bAdapterEmpty is true when no search results are displayed
    private boolean bAdapterEmpty = true;

    // is any filter activated?
    private boolean bFilterOn = false;
    // is any action activated?
    private boolean bActionOn = false;

    // last filter scheme, used for better toggle behaviour
    private String mLastSelection = null;
    private String mLastAction = null;

    private final Runnable runCleanList = new Runnable() {
        @Override
        public void run() {
            if (mListDirty && TBApplication.state().isQuickListVisible()) {
                QuickList.this.populateList();
                TBApplication.dataHandler(mQuickList.getContext()).runAfterLoadOver(() -> {
                    if (mListDirty)
                        mQuickList.postDelayed(this, 500);
                });
            }
        }
    };

    public Context getContext() {
        return mTBLauncherActivity;
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mTBLauncherActivity);
        mQuickList = mTBLauncherActivity.findViewById(R.id.quickList);
        mListDirty = true;
    }

    public void onFavoritesChanged() {
        mListDirty = true;
        if (mQuickList == null)
            return;
        mQuickList.removeCallbacks(runCleanList);
        mQuickList.postDelayed(runCleanList, 100);
    }

    public static int getDrawFlags(SharedPreferences prefs) {
        int drawFlags = EntryItem.FLAG_DRAW_QUICK_LIST | EntryItem.FLAG_DRAW_NO_CACHE;
        if (prefs.getBoolean("quick-list-text-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_NAME;
        if (prefs.getBoolean("quick-list-icons-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON;
        if (prefs.getBoolean("quick-list-show-badge", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON_BADGE;
        if (UIColors.isColorLight(UIColors.getColor(prefs, "quick-list-color"))) {
            drawFlags |= EntryItem.FLAG_DRAW_WHITE_BG;
            //drawFlags |= EntryItem.FLAG_DRAW_NO_CACHE; // no need, we already have it
        }
        return drawFlags;
    }

    private void populateList() {
        mListDirty = false;
        // keep a list of the old views so we can reuse them
        final View[] oldList = new View[mQuickList.getChildCount()];
        for (int nChild = 0; nChild < oldList.length; nChild += 1)
            oldList[nChild] = mQuickList.getChildAt(nChild);
        //Log.d("QL", "oldList.length=" + oldList.length + " getChildCount=" + mQuickList.getChildCount());

        // clean the QuickList
        mQuickList.removeAllViews();
        if (!isQuickListEnabled()) {
            mQuickListItems.clear();
            mQuickList.setVisibility(View.GONE);
            return;
        }

        // get list of entries we must show
        final List<EntryItem> list;
        {
            QuickListProvider provider = TBApplication.dataHandler(getContext()).getQuickListProvider();
            if (provider != null && provider.isLoaded()) {
                List<EntryItem> pojos = provider.getPojos();
                list = pojos != null ? pojos : Collections.emptyList();
            } else {
                list = Collections.emptyList();
                mListDirty = true;
            }
        }

        // get old item ids
        final List<String> oldItems;
        // if the two lists have the same length, assume we can reuse the views
        if (mQuickListItems.size() == oldList.length) {
            oldItems = new ArrayList<>(mQuickListItems.size());
            for (EntryItem entry : mQuickListItems) {
                if (entry instanceof PlaceholderEntry)
                    oldItems.add("placeholder:" + entry.id);
                else
                    oldItems.add(entry.id);
                //Log.i("QL", "oldItem[" + (oldItems.size() - 1) + "]=" + oldItems.get(oldItems.size() - 1));
            }
        } else {
            oldItems = Collections.emptyList();
        }

        final SharedPreferences prefs = mSharedPreferences;
        int drawFlags = getDrawFlags(prefs);
        LayoutInflater inflater = LayoutInflater.from(getContext());

        // clear old items before we add the new ones
        mQuickListItems.clear();
        for (EntryItem entry : list) {
            if (entry instanceof PlaceholderEntry)
                mListDirty = true;
            View view;
            int oldPos = oldItems.indexOf(entry.id);
            //Log.d("QL", "oldPos=" + oldPos + " for " + entry.id);
            if (oldPos > -1 && oldPos < oldList.length) {
                //Log.i("QL", "[" + oldPos + "] reuse view for " + entry.id);
                view = oldList[oldPos];
            } else {
                //Log.i("QL", "inflate view for " + entry.id);
                view = inflater.inflate(entry.getResultLayout(drawFlags), mQuickList, false);
            }
            entry.displayResult(view, drawFlags);
            mQuickList.addView(view);
            mQuickListItems.add(entry);

//            view.setScaleX(0f);
//            view.animate()
//                    .setInterpolator(new DecelerateInterpolator())
//                    .scaleX(1f)
//                    .setDuration(AnimatedListView.SCALE_DURATION)
//                    .start();

            view.setOnClickListener(v -> {
                if (entry instanceof StaticEntry) {
                    entry.doLaunch(v, EntryItem.LAUNCHED_FROM_QUICK_LIST);
                } else {
                    ResultHelper.launch(v, entry);
                }
            });
            view.setOnLongClickListener(v -> {
                ListPopup menu = entry.getPopupMenu(v, EntryItem.LAUNCHED_FROM_QUICK_LIST);

                // show menu only if it contains elements
                if (!menu.getAdapter().isEmpty()) {
                    TBApplication.behaviour(v.getContext()).registerPopup(menu);
                    menu.show(v);
                    return true;
                }

                return false;
            });
            final int color;
            if (entry instanceof FilterEntry)
                color = UIColors.getQuickListToggleColor(prefs);
            else
                color = UIColors.getQuickListRipple(prefs);
            Drawable selector = CustomizeUI.getSelectorDrawable(view, color, true);
            view.setBackground(selector);
        }
        //mQuickList.setVisibility(mQuickList.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        mQuickList.requestLayout();
    }

    public void toggleSearch(@NonNull View v, @NonNull String query, @NonNull Class<? extends Searcher> searcherClass) {
        Context ctx = v.getContext();
        TBApplication app = TBApplication.getApplication(ctx);
        final String actionId;
        {
            Object tag_actionId = v.getTag(R.id.tag_actionId);
            actionId = tag_actionId instanceof String ? (String) tag_actionId : "";
        }

        // toggle off any filter
        if (bFilterOn) {
            animToggleOff();
            bFilterOn = false;
            app.behaviour().filterResults(null);
        }

        // show search content
        {
            // if the last action is not the current action, toggle on this action
            if (!bActionOn || !isLastSelection(actionId)) {
                app.behaviour().runSearcher(query, searcherClass);

                // update toggle information
                mLastSelection = actionId;
                bActionOn = true;
            } else {
                // to toggle off the action, set bActionOn to false
                app.behaviour().clearSearch();
            }
        }
    }

    public void toggleProvider(View v, IProvider<?> provider, @Nullable java.util.Comparator<? super EntryItem> comparator) {
        Context ctx = v.getContext();
        TBApplication app = TBApplication.getApplication(ctx);
        final String actionId;
        {
            Object tag_actionId = v.getTag(R.id.tag_actionId);
            actionId = tag_actionId instanceof String ? (String) tag_actionId : "";
        }

        // toggle off any filter
        if (bFilterOn) {
            animToggleOff();
            bFilterOn = false;
            app.behaviour().filterResults(null);
        }

        // show action provider content
        {
            // if the last action is not the current action, toggle on this action
            if (!bActionOn || !isLastSelection(actionId)) {
                List<? extends EntryItem> list;
                list = provider != null ? provider.getPojos() : null;
                if (list != null) {
                    // copy list in order to change it
                    list = new ArrayList<>(list);

                    // remove actions and filters from the result list
                    for (Iterator<? extends EntryItem> iterator = list.iterator(); iterator.hasNext(); ) {
                        EntryItem entry = iterator.next();
                        if (entry instanceof FilterEntry)
                            iterator.remove();
                    }

                    // sort if we have a comparator
                    if (comparator != null) {
                        //TODO: do we need this on another thread?
                        Collections.sort(list, comparator);
                    }

                    // show result list
                    app.behaviour().clearSearchText();
                    app.behaviour().updateAdapter(list, false);

                    // update toggle information
                    mLastSelection = actionId;
                    bActionOn = true;
                }
            } else {
                // to toggle off the action, set bActionOn to false
                app.behaviour().clearSearch();
            }
        }
    }

    public void toggleFilter(View v, IProvider<?> provider, @NonNull String filterName) {
        Context ctx = v.getContext();
        TBApplication app = TBApplication.getApplication(ctx);
        final String actionId;
        {
            Object tag_actionId = v.getTag(R.id.tag_actionId);
            actionId = tag_actionId instanceof String ? (String) tag_actionId : "";
        }

        // if there is no search we need to filter, just show all matching entries
        if (bAdapterEmpty) {
            if (bFilterOn && provider != null && isLastSelection(actionId)) {
                app.behaviour().clearAdapter();
                bFilterOn = false;
            } else {
                List<? extends EntryItem> list;
                list = provider != null ? provider.getPojos() : null;
                if (list != null) {
                    app.behaviour().updateAdapter(list, false);
                    mLastSelection = actionId;
                    bFilterOn = true;
                } else {
                    bFilterOn = false;
                }
            }
            // updateAdapter will change `bAdapterEmpty` and we change it back because we want
            // bAdapterEmpty to represent a search we need to filter
            bAdapterEmpty = true;
        } else if (bFilterOn && (provider == null || isLastSelection(actionId))) {
            animToggleOff();
            if (mLastAction != null) {
                bActionOn = true;
                mLastSelection = mLastAction;
                mLastAction = null;
            }
            bFilterOn = false;
            app.behaviour().filterResults(null);
        } else if (provider != null) {
            animToggleOff();
            if (bActionOn)
                mLastAction = mLastSelection;
            bFilterOn = true;
            mLastSelection = actionId;
            app.behaviour().filterResults(filterName);
        }

        // show what is currently toggled
        if (bFilterOn) {
            animToggleOn(v);
        }
    }

    public void toggleFilter(View v, @Nullable Provider<? extends EntryItem> provider) {
        Object tag_filterText = v.getTag(R.id.tag_filterText);
        String filterText = (tag_filterText instanceof String) ? (String) tag_filterText : "";
        toggleFilter(v, provider, filterText);
    }

    private void animToggleOn(View v) {
        v.setSelected(true);
        v.setHovered(true);
    }

    private void animToggleOff() {
        if (!bFilterOn)
            return;
        int n = mQuickList.getChildCount();
        for (int i = 0; i < n; i += 1) {
            View view = mQuickList.getChildAt(i);
            if (mLastSelection == null || mLastSelection == view.getTag(R.id.tag_actionId)) {
                view.setSelected(false);
                view.setHovered(false);
            }
        }
    }

    private boolean isQuickListEnabled() {
        return mSharedPreferences.getBoolean("quick-list-enabled", true);
    }

    public void showQuickList() {
        if (!mOnlyForResults)
            show();
    }

    private void show() {
        mQuickList.removeCallbacks(runCleanList);
        runCleanList.run();

        if (isQuickListEnabled()) {
            final SharedPreferences pref = mSharedPreferences;
            if (pref.getBoolean("quick-list-animation", true)) {
                mQuickList.animate()
                        .scaleY(1f)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.VISIBLE);
                            }
                        })
                        .start();
            } else {
                mQuickList.setScaleY(1f);
                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.VISIBLE);
            }
            mQuickList.setVisibility(View.VISIBLE);
        }
    }

    public void hideQuickList(boolean animate) {
        if (isQuickListEnabled()) {
            animToggleOff();
            if (animate) {
                mQuickList.animate()
                        .scaleY(0f)
                        .setInterpolator(new DecelerateInterpolator())
                        .setListener(new AnimatorListenerAdapter() {
                            @Override
                            public void onAnimationStart(Animator animation) {
                                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN);
                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.HIDDEN);
                                mQuickList.setVisibility(View.GONE);
                            }
                        })
                        .start();
                mQuickList.setVisibility(View.VISIBLE);
            } else {
                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.HIDDEN);
                mQuickList.setScaleY(0f);
                mQuickList.setVisibility(View.GONE);
            }
        } else {
            TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.HIDDEN);
            mQuickList.setVisibility(View.GONE);
        }
    }

    public void onResume() {
        final SharedPreferences pref = mSharedPreferences;
        mOnlyForResults = pref.getBoolean("quick-list-only-for-results", false);
        applyUiPref(pref, mQuickList);
    }

    public void adapterCleared() {
        animToggleOff();
        bFilterOn = false;
        bActionOn = false;
        bAdapterEmpty = true;
        if (mOnlyForResults)
            hideQuickList(true);
        mLastSelection = null;
    }

    public void adapterUpdated() {
        show();
        animToggleOff();
        bFilterOn = false;
        bActionOn = mLastSelection != null
                && (mLastSelection.startsWith(ActionEntry.SCHEME)
                || mLastSelection.startsWith(TagEntry.SCHEME));
        bAdapterEmpty = false;
    }

    public static void applyUiPref(SharedPreferences pref, LinearLayout quickList) {
        Resources resources = quickList.getResources();
        // size
        int percent = pref.getInt("quick-list-size", 0);

        if (!(quickList.getLayoutParams() instanceof ViewGroup.MarginLayoutParams))
            throw new IllegalStateException("mSearchBarContainer has the wrong layout params");

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) quickList.getLayoutParams();
        // set layout height
        {
            int smallSize = resources.getDimensionPixelSize(R.dimen.bar_height);
            int largeSize = resources.getDimensionPixelSize(R.dimen.large_quick_list_bar_height);
            params.height = smallSize + (largeSize - smallSize) * percent / 100;
            quickList.setLayoutParams(params);
        }

        int color = getBackgroundColor(pref);

        // rounded drawable
        PaintDrawable drawable = new PaintDrawable();
        drawable.getPaint().setColor(color);
        drawable.setCornerRadius(resources.getDimension(R.dimen.bar_corner_radius));
        quickList.setBackground(drawable);
        int margin = (int) (params.height * .25f);
        params.setMargins(margin, 0, margin, margin);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // clip list content to rounded corners
            quickList.setClipToOutline(true);
        }

    }

    public static int getBackgroundColor(SharedPreferences pref) {
        int color = UIColors.getColor(pref, "quick-list-color");
        int alpha = UIColors.getAlpha(pref, "quick-list-alpha");
        return UIColors.setAlpha(color, alpha);
    }

    // ugly: check from where the entry was launched
    public boolean isViewInList(View view) {
        return mQuickList.indexOfChild(view) != -1;
    }

    public boolean isLastSelection(@NonNull String entryId) {
        return entryId.equals(mLastSelection);
    }
}
