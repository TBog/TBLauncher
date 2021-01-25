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
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UIColors;

public class QuickList {
    private TBLauncherActivity mTBLauncherActivity;
    private boolean mIsEnabled = true;
    private boolean mAlwaysVisible = true;
    private boolean mListDirty = true;
    private LinearLayout mQuickList;

    // bAdapterEmpty is true when no search results are displayed
    private boolean bAdapterEmpty = true;

    // is any filter activated?
    private boolean bFilterOn = false;
    // is any action activated?
    private boolean bActionOn = false;

    // last filter scheme, used for better toggle behaviour
    private String mLastSelection = null;

    public Context getContext() {
        return mTBLauncherActivity;
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;

        mQuickList = mTBLauncherActivity.findViewById(R.id.quickList);
        mListDirty = true;
    }

    public void onFavoritesChanged() {
        if (mQuickList == null)
            return;
        mListDirty = true;
        if (TBApplication.state().isQuickListVisible())
            populateList();
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
        mQuickList.removeAllViews();
        if (!isQuickListEnabled()) {
            mQuickList.setVisibility(View.GONE);
            return;
        }
        QuickListProvider provider = TBApplication.dataHandler(getContext()).getQuickListProvider();
        List<EntryItem> list = provider != null ? provider.getPojos() : null;
        if (list == null)
            return;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mQuickList.getContext());
        int drawFlags = getDrawFlags(prefs);
        for (EntryItem entry : list) {
            View view = LayoutInflater.from(getContext()).inflate(entry.getResultLayout(drawFlags), mQuickList, false);
            entry.displayResult(view, drawFlags);
            mQuickList.addView(view);

            view.setOnClickListener(entry::doLaunch);
            view.setOnLongClickListener(v -> {
                ListPopup menu = entry.getPopupMenu(v, EntryItem.FLAG_POPUP_MENU_QUICK_LIST);

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
            Drawable selector = TBApplication.ui(getContext()).getSelectorDrawable(view, color, true);
            view.setBackground(selector);
        }
        //mQuickList.setVisibility(mQuickList.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        mQuickList.requestLayout();
    }

    public void toggleProvider(View v, IProvider<?> provider, @Nullable java.util.Comparator<? super EntryItem> comparator) {
        Context ctx = v.getContext();
        TBApplication app = TBApplication.getApplication(ctx);
        Object tag_actionId = v.getTag(R.id.tag_actionId);
        String actionId = tag_actionId instanceof String ? (String) tag_actionId : "";

        // toggle off any filter
        if (bFilterOn) {
            animToggleOff();
            bFilterOn = false;
            app.behaviour().filterResults(null);
        }

        // show action provider content
        {
            // if the last action is not the current action, toggle on this action
            if (!bActionOn || !actionId.equals(mLastSelection)) {
                List<? extends EntryItem> list;
                list = provider != null ? provider.getPojos() : null;
                if (list != null) {
                    // copy list in order to change it
                    list = new ArrayList<>(list);

                    // remove actions and filters from the result list
                    for (Iterator<? extends EntryItem> iterator = list.iterator(); iterator.hasNext(); ) {
                        EntryItem entry = iterator.next();
                        if (entry instanceof ActionEntry)
                            iterator.remove();
                        if (entry instanceof FilterEntry)
                            iterator.remove();
                    }

                    // sort if we have a comparator
                    if (comparator != null) {
                        //TODO: do we need this on another thread?
                        Collections.sort(list, comparator);
                    }

                    // show result list
                    app.behaviour().clearSearch();
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

        // if there is no search we need to filter, just show all matching entries
        if (bAdapterEmpty) {
            if (bFilterOn && provider != null && filterName.equals(mLastSelection)) {
                app.behaviour().clearAdapter();
                bFilterOn = false;
            } else {
                List<? extends EntryItem> list;
                list = provider != null ? provider.getPojos() : null;
                if (list != null) {
                    app.behaviour().updateAdapter(list, false);
                    mLastSelection = filterName;
                    bFilterOn = true;
                } else {
                    bFilterOn = false;
                }
            }
            // updateAdapter will change `bAdapterEmpty` and we change it back because we want
            // bAdapterEmpty to represent a search we need to filter
            bAdapterEmpty = true;
        } else if (bFilterOn && (provider == null || filterName.equals(mLastSelection))) {
            animToggleOff();
            bFilterOn = false;
            app.behaviour().filterResults(null);
        } else if (provider != null) {
            animToggleOff();
            bFilterOn = true;
            mLastSelection = filterName;
            app.behaviour().filterResults(filterName);
        }

        // show what is currently toggled
        if (bFilterOn) {
            animToggleOn(v);
        }
    }

    public void toggleFilter(View v, @Nullable Provider<? extends EntryItem> provider) {
        String filterName = provider != null ? provider.getScheme() : "";
        toggleFilter(v, provider, filterName);
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
            if (mLastSelection == null || mLastSelection == view.getTag(R.id.tag_filterName)) {
                view.setSelected(false);
                view.setHovered(false);
            }
        }
    }

    private boolean isQuickListEnabled() {
        return mIsEnabled;
    }

    public void showQuickList() {
        if (mAlwaysVisible)
            show();
    }

    private void show() {
        if (mListDirty)
            populateList();
        if (isQuickListEnabled()) {
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

    public void onResume(SharedPreferences pref) {
        mIsEnabled = pref.getBoolean("quick-list-enabled", true);
        mAlwaysVisible = pref.getBoolean("quick-list-always-visible", true);
        applyUiPref(pref, mQuickList);
    }

    public void adapterCleared() {
        animToggleOff();
        bFilterOn = false;
        bActionOn = false;
        bAdapterEmpty = true;
        if (!mAlwaysVisible)
            hideQuickList(true);
    }

    public void adapterUpdated() {
        show();
        animToggleOff();
        bFilterOn = false;
        bActionOn = false;
        bAdapterEmpty = false;
    }

    public static void applyUiPref(SharedPreferences pref, LinearLayout quickList) {
        Resources resources = quickList.getResources();
        // size
        int percent = pref.getInt("quick-list-size", 0);

        if (!(quickList.getLayoutParams() instanceof LinearLayout.LayoutParams))
            throw new IllegalStateException("mSearchBarContainer has the wrong layout params");

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) quickList.getLayoutParams();
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
}
