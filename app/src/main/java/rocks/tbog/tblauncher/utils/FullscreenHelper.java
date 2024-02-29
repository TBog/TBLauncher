package rocks.tbog.tblauncher.utils;

import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DELAY;
import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DURATION;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;

import rocks.tbog.tblauncher.LauncherState;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;

public class FullscreenHelper {
    private static final String TAG = FullscreenHelper.class.getSimpleName();

    @NonNull
    final TBLauncherActivity mTBLauncherActivity;
    @Nullable
    private View mDecorView = null;
    private final View mNotificationBackground;

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = mTBLauncherActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            SystemUiVisibility.clearFullscreen(mDecorView);
            //mTBLauncherActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    };

    private final Runnable mHidePart2Runnable = new Runnable() {
        @Override
        public void run() {
//            if (TBApplication.state().isKeyboardVisible()) {
//                // if keyboard is visible, the notification bar is also visible
//                return;
//            }

            // Delayed hide UI elements
            ActionBar actionBar = mTBLauncherActivity.getSupportActionBar();
            if (actionBar != null) {
                actionBar.hide();
            }
            SystemUiVisibility.setFullscreen(mDecorView);
            //mTBLauncherActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    };

    public FullscreenHelper(@NonNull TBLauncherActivity launcherActivity) {
        mTBLauncherActivity = launcherActivity;
        mNotificationBackground = launcherActivity.findViewById(R.id.notificationBackground);
    }

    @NonNull
    private View getDecorView() {
        if (mDecorView == null) {
            mDecorView = mTBLauncherActivity.getWindow().getDecorView();
        }
        return mDecorView;
    }

    /**
     * Hide status and notification bar
     *
     * @param startDelay milliseconds of delay
     */
    public void enableFullscreen(int startDelay) {
        View decorView = getDecorView();
        boolean animate = !SystemUiVisibility.isFullscreenSet(decorView) || TBApplication.state().isNotificationBarVisible();

        Log.i(TAG, "enableFullscreen delay=" + startDelay + " anim=" + animate);

        // Schedule a runnable to remove the status and navigation bar after a delay
        decorView.removeCallbacks(mShowPart2Runnable);
        decorView.postDelayed(mHidePart2Runnable, startDelay);

        // hide notification background
        final int statusHeight = UISizes.getStatusBarSize(mTBLauncherActivity);
        if (TBApplication.state().getNotificationBarVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN)
            mNotificationBackground.animate().cancel();
        if (animate) {
            mNotificationBackground.animate()
                .translationY(-statusHeight)
                .setStartDelay(startDelay)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.HIDDEN);
                    }
                })
                .start();
        } else {
            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.HIDDEN);
            mNotificationBackground.setTranslationY(-statusHeight);
        }
    }

    /**
     * Show status and notification bar
     */
    public void disableFullscreen() {
        View decorView = getDecorView();
        boolean animate = SystemUiVisibility.isFullscreenSet(decorView) || !TBApplication.state().isNotificationBarVisible();

        // Schedule a runnable to display UI elements after a delay
        decorView.removeCallbacks(mHidePart2Runnable);
        decorView.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

        // show notification background
        final int statusHeight = UISizes.getStatusBarSize(mTBLauncherActivity);
        if (!TBApplication.state().isNotificationBarVisible())
            mNotificationBackground.setTranslationY(-statusHeight);
        if (TBApplication.state().getNotificationBarVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE)
            mNotificationBackground.animate().cancel();
        if (animate) {
            mNotificationBackground.animate()
                .translationY(0f)
                .setStartDelay(0)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.VISIBLE);
                    }
                })
                .start();
        } else {
            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.VISIBLE);
            mNotificationBackground.setTranslationY(0f);
        }
    }
}
