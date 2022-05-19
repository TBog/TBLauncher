package rocks.tbog.tblauncher.quicklist;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.LauncherState;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.ui.ViewStubPreview;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;

/**
 * Dock
 */
public class QuickList {
    private static final String TAG = "Dock";
    private static final int RETRY_COUNT = 3;

    private TBLauncherActivity mTBLauncherActivity;
    private boolean mQuickListEnabled = false;
    private boolean mOnlyForResults = false;
    private boolean mListDirty = true;
    private int mRetryCountdown;
    private RecyclerList mQuickList;
    private RecycleAdapter mAdapter;
    //    private final ArrayList<EntryItem> mQuickListItems = new ArrayList<>(0);
    private SharedPreferences mSharedPreferences = null;
    private final Runnable runCleanList = new Runnable() {
        @Override
        public void run() {
            if (mListDirty && TBApplication.state().isQuickListVisible()) {
                QuickList.this.populateList();
                DataHandler dataHandler = TBApplication.dataHandler(mQuickList.getContext());
                if (mListDirty && dataHandler.fullLoadOverSent()) {
                    if (--mRetryCountdown <= 0) {
                        Log.w(TAG, "Can't load all entries");
                        return;
                    }
                }
                dataHandler.runAfterLoadOver(() -> {
                    if (mListDirty)
                        mQuickList.postDelayed(this, 500);
                });
            }
        }
    };
    // bAdapterEmpty is true when no search results are displayed
    private boolean bAdapterEmpty = true;
    // is any filter activated?
    private boolean bFilterOn = false;
    // is any action activated?
    private boolean bActionOn = false;
    // last filter scheme, used for better toggle behaviour
    private String mLastSelection = null;
    private String mLastAction = null;

    private static int cornerRadius(@NonNull Context ctx, @NonNull SharedPreferences pref) {
        Resources resources = ctx.getResources();
        final int defaultCorner = resources.getInteger(R.integer.default_corner_radius);
        final int cornerRadius = pref.getInt("quick-list-radius", defaultCorner);
        return UISizes.dp2px(ctx, cornerRadius);
    }

    public static void applyUiPref(@NonNull SharedPreferences pref, View quickList) {
        Context ctx = quickList.getContext();
        Resources resources = quickList.getResources();
        // size
        int barHeight = pref.getInt("quick-list-height", 0);
        if (barHeight <= 1)
            barHeight = resources.getInteger(R.integer.default_dock_height);
        barHeight = UISizes.dp2px(ctx, barHeight);

        if (quickList.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) quickList.getLayoutParams();
            // set layout height
            params.height = barHeight;
            int hMargin = UISizes.getQuickListMarginHorizontal(ctx);
            int vMargin = UISizes.getQuickListMarginVertical(ctx);
            params.setMargins(hMargin, vMargin, hMargin, vMargin);
            quickList.setLayoutParams(params);
        } else {
            throw new IllegalStateException("quickList has the wrong layout params");
        }

        final int cornerRadius = cornerRadius(ctx, pref);
        final int color = getBackgroundColor(pref);

        // rounded drawable
        if (cornerRadius > 0) {
            final PaintDrawable drawable;
            {
                Drawable background = quickList.getBackground();
                if (background instanceof PaintDrawable)
                    drawable = (PaintDrawable) background;
                else
                    drawable = new PaintDrawable();
            }
            drawable.getPaint().setColor(color);
            drawable.setCornerRadius(cornerRadius);
            quickList.setBackground(drawable);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // clip list content to rounded corners
                quickList.setClipToOutline(true);
            }
        } else {
            quickList.setBackgroundColor(color);
        }
    }

    public static int getBackgroundColor(SharedPreferences pref) {
        return UIColors.getColor(pref, "quick-list-argb");
    }

    public Context getContext() {
        return mTBLauncherActivity;
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mTBLauncherActivity);

        inflateQuickListView();
        mAdapter = new RecycleAdapter(getContext(), new ArrayList<>());
        //mAdapter.setGridLayout();

        mQuickList.setHasFixedSize(true);
        mQuickList.setAdapter(mAdapter);

        mQuickList.setLayoutManager(new DockRecycleLayoutManager(8, 1));
        mQuickList.addOnScrollListener(new PagedScrollListener());

        mRetryCountdown = RETRY_COUNT;
        mListDirty = true;
        runCleanList.run();
    }

    public void reload() {
        mRetryCountdown = RETRY_COUNT;
        mListDirty = true;
        if (mQuickList == null)
            return;
        mQuickList.removeCallbacks(runCleanList);
        mQuickList.postDelayed(runCleanList, 100);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T inflateViewStub(@IdRes int id) {
        View stub = mTBLauncherActivity.findViewById(id);
        return (T) ViewStubPreview.inflateStub(stub);
    }

    private void inflateQuickListView() {
        QuickListPosition position = PrefCache.getDockPosition(mSharedPreferences);
        if (position == QuickListPosition.POSITION_UNDER_SEARCH_BAR && !PrefCache.searchBarAtBottom(mSharedPreferences))
            position = QuickListPosition.POSITION_ABOVE_RESULTS;

        if (position == QuickListPosition.POSITION_ABOVE_RESULTS)
            mQuickList = inflateViewStub(R.id.dockAboveResults);
        else if (position == QuickListPosition.POSITION_UNDER_RESULTS)
            mQuickList = inflateViewStub(R.id.dockUnderResults);
        else if (position == QuickListPosition.POSITION_UNDER_SEARCH_BAR)
            mQuickList = inflateViewStub(R.id.dockAtBottom);
        else
            mQuickList = inflateViewStub(R.id.quickList);
    }

    private void populateList() {
        if (mAdapter != null) {
            mListDirty = false;
            final List<EntryItem> list;
            if (isQuickListEnabled()) {
                QuickListProvider provider = TBApplication.dataHandler(getContext()).getQuickListProvider();
                if (provider != null && provider.isLoaded()) {
                    List<EntryItem> pojos = provider.getPojos();
                    list = pojos != null ? pojos : Collections.emptyList();
                } else {
                    list = Collections.emptyList();
                    mListDirty = true;
                }
            } else {
                list = Collections.emptyList();
                mQuickList.setVisibility(View.GONE);
            }
//            for (EntryItem entry : list) {
//                if (entry instanceof PlaceholderEntry)
//                    oldItems.add("placeholder:" + entry.id);
//                else
//                    oldItems.add(entry.id);
//            }
            mAdapter.updateResults(list);
        }
/*
        // keep a list of the old views so we can reuse them
        final View[] oldList = new View[mQuickList.getChildCount()];
        for (int nChild = 0; nChild < oldList.length; nChild += 1)
            oldList[nChild] = mQuickList.getChildAt(nChild);
        //Log.d("QL", "oldList.length=" + oldList.length + " getChildCount=" + mQuickList.getChildCount());

        // clean the QuickList
        if (!isQuickListEnabled()) {
            mQuickList.removeAllViews();
            mQuickListItems.clear();
            mQuickList.setVisibility(View.GONE);
            return;
        }

        mListDirty = false;

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
        int listSize = list.size();
        for (int idx = 0; idx < listSize; idx++) {
            EntryItem entry = list.get(idx);
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

            int childIdx = mQuickList.indexOfChild(view);
            if (childIdx != idx) {
                if (childIdx != -1)
                    mQuickList.removeViewAt(childIdx);
                mQuickList.addView(view, idx);
            }
            mQuickListItems.add(entry);

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
                    TBApplication.getApplication(v.getContext()).registerPopup(menu);
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
        mQuickList.removeViews(listSize, mQuickList.getChildCount() - listSize);
        //mQuickList.setVisibility(mQuickList.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        mQuickList.requestLayout();
 */
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
        Behaviour behaviour = TBApplication.behaviour(ctx);
        final String actionId;
        {
            Object tag_actionId = v.getTag(R.id.tag_actionId);
            actionId = tag_actionId instanceof String ? (String) tag_actionId : "";
        }

        // toggle off any filter
        if (bFilterOn) {
            animToggleOff();
            bFilterOn = false;
            behaviour.filterResults(null);
        }

        // if the last action is not the current action, toggle on this action
        if (!bActionOn || !isLastSelection(actionId)) {
            behaviour.clearSearchText();
            // show provider content or toggle off if nothing to show
            if (behaviour.showProviderEntries(provider, comparator)) {
                // update toggle information
                mLastSelection = actionId;
                bActionOn = true;
            } else {
                // to toggle off the action, set bActionOn to false
                behaviour.clearSearch();
            }
        } else {
            // to toggle off the action, set bActionOn to false
            behaviour.clearSearch();
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
                if (app.behaviour().showProviderEntries(provider)) {
                    mLastSelection = actionId;
                    bFilterOn = true;
                } else {
                    bFilterOn = false;
                }
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
        if (mQuickListEnabled) {
            if (TBApplication.state().getDesktop() == LauncherState.Desktop.SEARCH)
                return PrefCache.modeSearchQuickListVisible(mSharedPreferences);
            if (TBApplication.state().getDesktop() == LauncherState.Desktop.EMPTY)
                return PrefCache.modeEmptyQuickListVisible(mSharedPreferences);
            if (TBApplication.state().getDesktop() == LauncherState.Desktop.WIDGET)
                return PrefCache.modeWidgetQuickListVisible(mSharedPreferences);
        }
        return mQuickListEnabled;
    }

    private boolean isOnlyForResults() {
        if (TBApplication.state().getDesktop() == LauncherState.Desktop.SEARCH)
            return mOnlyForResults;
        return false;
    }

    public void updateVisibility() {
        if (isQuickListEnabled()) {
            if (isOnlyForResults()) {
                if (TBApplication.state().isResultListVisible()) {
                    show();
                    return;
                }
            } else {
                show();
                return;
            }
        }
        hideQuickList(false);
    }

    private void show() {
        mQuickList.removeCallbacks(runCleanList);

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
                mQuickList.animate().cancel();
                mQuickList.setScaleY(1f);
                TBApplication.state().setQuickList(LauncherState.AnimatedVisibility.VISIBLE);
            }
            mQuickList.setVisibility(View.VISIBLE);
        }
        // after state set, make sure the list is not dirty
        runCleanList.run();
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

    public void onStart() {
        final SharedPreferences pref = mSharedPreferences;
        mQuickListEnabled = pref.getBoolean("quick-list-enabled", true);
        mOnlyForResults = pref.getBoolean("quick-list-only-for-results", false);

        applyUiPref(pref, mQuickList);
    }

    public void adapterCleared() {
        animToggleOff();
        bFilterOn = false;
        bActionOn = false;
        bAdapterEmpty = true;
        if (isOnlyForResults())
            hideQuickList(true);
        mLastSelection = null;
    }

    public void adapterUpdated() {
        if (isOnlyForResults())
            show();
        animToggleOff();
        bFilterOn = false;
        bActionOn = mLastSelection != null
            && (mLastSelection.startsWith(ActionEntry.SCHEME)
            || mLastSelection.startsWith(TagEntry.SCHEME));
        bAdapterEmpty = false;
    }

    // ugly: check from where the entry was launched
    public boolean isViewInList(View view) {
        return mQuickList.indexOfChild(view) != -1;
    }

    public boolean isLastSelection(@NonNull String entryId) {
        return entryId.equals(mLastSelection);
    }

    public enum QuickListPosition {
        POSITION_ABOVE_RESULTS,
        POSITION_UNDER_RESULTS,
        POSITION_UNDER_SEARCH_BAR,
    }
}
