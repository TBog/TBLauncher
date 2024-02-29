package rocks.tbog.tblauncher.utils;

import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DELAY;
import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DURATION;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Random;
import java.util.Set;

import rocks.tbog.tblauncher.LauncherState;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;


public class DesktopHelper {
    private static final String TAG = DesktopHelper.class.getSimpleName();

    @NonNull
    final TBLauncherActivity mTBLauncherActivity;
    private final SharedPreferences mPref;
    private final View mWidgetContainer;
    private final View mSearchBarContainer;
    private final EditText mSearchEditText;

    public DesktopHelper(@NonNull TBLauncherActivity launcherActivity) {
        mTBLauncherActivity = launcherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(launcherActivity);
        mWidgetContainer = mTBLauncherActivity.findViewById(R.id.widgetContainer);
        mSearchBarContainer = mTBLauncherActivity.findViewById(R.id.searchBarContainer);
        mSearchEditText = mSearchBarContainer.findViewById(R.id.launcherSearch);
    }

    private Context getContext() {
        return mTBLauncherActivity;
    }

    public void switchToDesktop(@NonNull LauncherState.Desktop mode) {
        // get current mode
        @Nullable
        LauncherState.Desktop currentMode = TBApplication.state().getDesktop();
        Log.d(TAG, "desktop changed " + currentMode + " -> " + mode);
        if (mode.equals(currentMode)) {
            // no change, maybe refresh?
            if (TBApplication.state().isResultListVisible() && mTBLauncherActivity.getResultAdapter().getItemCount() == 0)
                showDesktop(mode);
            return;
        }

        // hide current mode
        if (currentMode != null) {
            switch (currentMode) {
                case SEARCH:
                    TBApplication.resetTask(mTBLauncherActivity);
                    mTBLauncherActivity.hideSearchBar(true);
                    break;
                case WIDGET:
                    hideWidgets();
                    break;
                case EMPTY:
                default:
                    break;
            }
        }

        // show next mode
        showDesktop(mode);
    }

    private void showWidgets(boolean animate) {
        if (TBApplication.state().getWidgetScreenVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE)
            mWidgetContainer.animate().cancel();
        if (mWidgetContainer.getVisibility() == View.VISIBLE) {
            mWidgetContainer.setAlpha(1f);
            mTBLauncherActivity.hideResultList(false);
            return;
        }
        mWidgetContainer.setVisibility(View.VISIBLE);
        if (animate) {
            mWidgetContainer.setAlpha(0f);
            mWidgetContainer.animate()
                .setStartDelay(UI_ANIMATION_DURATION)
                .alpha(1f)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.VISIBLE);
                    }
                })
                .start();
        } else {
            TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.VISIBLE);
            mWidgetContainer.setAlpha(1f);
        }
        mTBLauncherActivity.hideResultList(true);
    }

    private void hideWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.HIDDEN);
        mWidgetContainer.setVisibility(View.GONE);
    }

    private void showSearchBar() {
        mSearchEditText.setEnabled(true);
        setSearchHint();
        UITheme.applySearchBarTextShadow(mSearchEditText);

        mSearchBarContainer.animate().cancel();
        mSearchBarContainer.setVisibility(View.VISIBLE);
        mSearchBarContainer.animate()
            .setStartDelay(0)
            .alpha(1f)
            .translationY(0f)
            .setDuration(UI_ANIMATION_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    LauncherState state = TBApplication.state();
                    state.setSearchBar(LauncherState.AnimatedVisibility.VISIBLE);
                    if (PrefCache.linkKeyboardAndSearchBar(mPref))
                        mTBLauncherActivity.showKeyboard();
                    else {
                        // sync keyboard state
                        state.syncKeyboardVisibility(mSearchEditText);
                    }
                }
            })
            .start();

        mSearchEditText.requestFocus();
    }

    public void showDesktop(LauncherState.Desktop mode) {
        if (TBApplication.activityInvalid(mTBLauncherActivity)) {
            Log.e(TAG, "[activityInvalid] showDesktop " + mode);
            return;
        }
        TBApplication.state().setDesktop(mode);
        switch (mode) {
            case SEARCH:
                // show the SearchBar
                showSearchBar();

                // hide/show result list
                final String openResult = PrefCache.modeSearchOpenResult(mPref);
                if ("none".equals(openResult)) {
                    // hide result
                    mTBLauncherActivity.hideResultList(false);
                } else {
                    // try to execute the action
                    mTBLauncherActivity.mActionHelper.executeResultActionAsync(openResult, "dm-search-open-result");
                }

                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();

                // enable/disable fullscreen (status and navigation bar)
                if (TBApplication.state().isKeyboardHidden()
                    && PrefCache.modeSearchFullscreen(mPref))
                    mTBLauncherActivity.enableFullscreen(UI_ANIMATION_DELAY);
                else
                    mTBLauncherActivity.disableFullscreen();
                break;
            case WIDGET:
                // show widgets
                showWidgets(true);
                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();
                // enable/disable fullscreen (status and navigation bar)
                if (PrefCache.modeWidgetFullscreen(mPref))
                    mTBLauncherActivity.enableFullscreen(UI_ANIMATION_DELAY);
                else
                    mTBLauncherActivity.disableFullscreen();
                break;
            case EMPTY:
            default:
                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();
                // enable/disable fullscreen (status and navigation bar)
                if (PrefCache.modeEmptyFullscreen(mPref))
                    mTBLauncherActivity.enableFullscreen(UI_ANIMATION_DELAY);
                else
                    mTBLauncherActivity.disableFullscreen();
                break;
        }
    }

    private void setSearchHint() {
        Set<String> selectedHints = mPref.getStringSet("selected-search-hints", null);
        if (selectedHints != null && !selectedHints.isEmpty()) {
            int random = new Random().nextInt(selectedHints.size());
            for (String selectedHint : selectedHints) {
                if (--random < 0) {
                    mSearchEditText.setHint(selectedHint);
                    break;
                }
            }
        }
    }
}
