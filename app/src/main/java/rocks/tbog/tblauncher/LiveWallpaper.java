package rocks.tbog.tblauncher;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

import androidx.preference.PreferenceManager;

import java.util.Locale;

class LiveWallpaper {
    private final boolean wallpaperIsVisible = true;
    private TBLauncherActivity mTBLauncherActivity = null;
    private WallpaperManager mWallpaperManager;
    private Point mWindowSize;
    private android.os.IBinder mWindowToken;
    private View mContentView;
    private float mFirstTouchOffset;
    private float mFirstTouchPos;
    private float mLastTouchPos;
    private float mWallpaperOffset;
    private Anim mAnimation;
    private VelocityTracker mVelocityTracker;

    private boolean lwpTouch = true;
    private boolean lwpDrag = false;
    private boolean wpDragAnimate = true;
    private boolean wpReturnCenter = true;
    private boolean wpStickToSides = false;

    LiveWallpaper() {
//        TypedValue typedValue = new TypedValue();
//        mainActivity.getTheme().resolveAttribute(android.R.attr.windowShowWallpaper, typedValue, true);
//        TypedArray a = mainActivity.obtainStyledAttributes(typedValue.resourceId, new int[]{android.R.attr.windowShowWallpaper});
//        wallpaperIsVisible = a.getBoolean(0, true);
//        a.recycle();
    }

    public void onCreateActivity(TBLauncherActivity mainActivity) {
        mTBLauncherActivity = mainActivity;

        // load preferences
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity);
            lwpTouch = prefs.getBoolean("lwp-touch", true);
            lwpDrag = prefs.getBoolean("lwp-drag", false);
            wpDragAnimate = prefs.getBoolean("wp-drag-animate", false);
            wpReturnCenter = prefs.getBoolean("wp-animate-center", true);
            wpStickToSides = prefs.getBoolean("wp-animate-sides", false);
        }

        mWallpaperManager = (WallpaperManager) mainActivity.getSystemService(Context.WALLPAPER_SERVICE);
        assert mWallpaperManager != null;

        mContentView = mainActivity.findViewById(android.R.id.content);
        mWallpaperManager.setWallpaperOffsetSteps(.5f, 0.f);
        mWallpaperOffset = 0.5f; // this is the center
        mAnimation = new Anim();
        mVelocityTracker = null;
        mWindowSize = new Point(1, 1);
        mainActivity.findViewById(R.id.root_layout).setOnTouchListener(this::onTouch);
    }

    static void onClick(View view) {
        TBApplication.behaviour(view.getContext()).toggleSearchBar();
    }

    boolean onTouch(View view, MotionEvent event) {
        if (!wallpaperIsVisible) {
            return false;
        }

        int actionMasked = event.getActionMasked();
        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN:
                mFirstTouchPos = event.getRawX();
                mFirstTouchOffset = mWallpaperOffset;
                if (isPreferenceWPDragAnimate()) {
                    mContentView.clearAnimation();

                    mVelocityTracker = VelocityTracker.obtain();
                    mVelocityTracker.addMovement(event);

                    mLastTouchPos = event.getRawX();
                    mTBLauncherActivity.getWindowManager()
                            .getDefaultDisplay()
                            .getSize(mWindowSize);
                }
                //send touch event to the LWP
                if (isPreferenceLWPTouch())
                    sendTouchEvent(view, event);
                return true;
            case MotionEvent.ACTION_MOVE:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);

//                    float fTouchPos = event.getRawX();
//                    float fOffset = (mLastTouchPos - fTouchPos) * 1.01f / mWindowSize.x;
//                    fOffset += mWallpaperOffset;
//                    updateWallpaperOffset(fOffset);
//                    mLastTouchPos = fTouchPos;
                    mLastTouchPos = event.getRawX();
                    float fOffset = (mFirstTouchPos - mLastTouchPos) * 1.01f / mWindowSize.x;
                    updateWallpaperOffset(mFirstTouchOffset + fOffset);
                }

                //send move/drag event to the LWP
                if (isPreferenceLWPDrag())
                    sendTouchEvent(view, event);
                if (isPreferenceWPDragAnimate())
                    return true;
                else
                    break;
            case MotionEvent.ACTION_UP:
                // was this a click?
                if (mVelocityTracker == null) {
                    onClick(view);
                }
                //fallthrough
            case MotionEvent.ACTION_CANCEL:
                if (mVelocityTracker != null) {
                    mVelocityTracker.addMovement(event);

                    if (mAnimation.init())
                        mContentView.startAnimation(mAnimation);

                    // was this a click?
                    if (actionMasked == MotionEvent.ACTION_UP) {
                        float xMove = (mFirstTouchPos - mLastTouchPos) / mWindowSize.x;
                        float xVel = mVelocityTracker.getXVelocity() / mWindowSize.x;
                        float yVel = mVelocityTracker.getYVelocity() / mWindowSize.y;
                        Log.d("LWP", String.format(Locale.US, "Velocity=(%.3f, %.3f) Move=(%.3f, 0)", xVel, yVel, xMove));
                        if (Math.abs(xVel) < .01f
                                && Math.abs(yVel) < .01f
                                && Math.abs(xMove) < .01f)
                            onClick(view);
                    }

                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                if (isPreferenceWPDragAnimate())
                    return true;
                else
                    break;
        }

        // do not consume the event
        return false;
    }

    public Context getContext() {
        return mTBLauncherActivity;
    }

    public void onPrefChanged(SharedPreferences prefs, String key) {
        switch (key) {
            case "lwp-touch":
                lwpTouch = prefs.getBoolean("lwp-touch", true);
                break;
            case "lwp-drag":
                lwpDrag = prefs.getBoolean("lwp-drag", false);
                break;
            case "wp-drag-animate":
                wpDragAnimate = prefs.getBoolean("wp-drag-animate", false);
                break;
            case "wp-animate-center":
                wpReturnCenter = prefs.getBoolean("wp-animate-center", true);
                break;
            case "wp-animate-sides":
                wpStickToSides = prefs.getBoolean("wp-animate-sides", false);
                break;
        }
    }

    private boolean isPreferenceLWPTouch() {
        return lwpTouch;
    }

    private boolean isPreferenceLWPDrag() {
        return lwpDrag;
    }

    private boolean isPreferenceWPDragAnimate() {
        return wpDragAnimate;
    }

    private boolean isPreferenceWPReturnCenter() {
        return wpReturnCenter;
    }

    private boolean isPreferenceWPStickToSides() {
        return wpStickToSides;
    }

    private android.os.IBinder getWindowToken() {
        return mWindowToken != null ? mWindowToken : (mWindowToken = mContentView.getWindowToken());
    }

    private void updateWallpaperOffset(float offset) {
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            offset = Math.max(0.f, Math.min(1.f, offset));
            mWallpaperOffset = offset;
            mWallpaperManager.setWallpaperOffsets(iBinder, mWallpaperOffset, 0.f);
        }
    }

    private void sendTouchEvent(int x, int y, int index) {
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            String command = index == 0 ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP;
            mWallpaperManager.sendWallpaperCommand(iBinder, command, x, y, 0, null);
        }
    }

    private void sendTouchEvent(View view, MotionEvent event) {
        int pointerCount = event.getPointerCount();
        int[] viewOffset = {0, 0};
        // this will not account for a rotated view
        view.getLocationOnScreen(viewOffset);

        // get index of first finger
        int pointerIndex = event.findPointerIndex(0);
        if (pointerIndex >= 0 && pointerIndex < pointerCount) {
            sendTouchEvent((int) event.getX(pointerIndex) + viewOffset[0], (int) event.getY(pointerIndex) + viewOffset[1], pointerIndex);
        }

        // get index of second finger
        pointerIndex = event.findPointerIndex(1);
        if (pointerIndex >= 0 && pointerIndex < pointerCount) {
            sendTouchEvent((int) event.getX(pointerIndex) + viewOffset[0], (int) event.getY(pointerIndex) + viewOffset[1], pointerIndex);
        }
    }

    class Anim extends Animation {
        float mStartOffset;
        float mDeltaOffset;
        float mVelocity;

        Anim() {
            super();
            setDuration(1000);
        }

        boolean init() {
            mVelocityTracker.computeCurrentVelocity(1000 / 30);
            mVelocity = mVelocityTracker.getXVelocity();
            //Log.d("LWP", "mVelocity=" + String.format(Locale.US, "%.2f", mVelocity));

            mStartOffset = mWallpaperOffset;
            //Log.d("LWP", "mStartOffset=" + String.format(Locale.US, "%.2f", mStartOffset));

            boolean stickToSides = isPreferenceWPStickToSides();
            boolean stickToCenter = isPreferenceWPReturnCenter();
            float expectedPos = -Math.min(Math.max(mVelocity / mWindowSize.x, -.5f), .5f) + mStartOffset;
            //Log.d("LWP", "expectedPos=" + String.format(Locale.US, "%.2f", expectedPos));

            // if we stick only to the center
            float leftStickPercent = -1.f;
            float rightStickPercent = 2.f;

            if (stickToSides && stickToCenter) {
                // if we stick to the left, right and center
                leftStickPercent = .2f;
                rightStickPercent = .8f;
            } else if (stickToSides) {
                // if we stick only to the center
                leftStickPercent = .5f;
                rightStickPercent = .5f;
            }

            if (expectedPos <= leftStickPercent)
                mDeltaOffset = 0.f - mStartOffset;
            else if (expectedPos >= rightStickPercent)
                mDeltaOffset = 1.f - mStartOffset;
            else if (stickToCenter)
                mDeltaOffset = .5f - mStartOffset;
            else
                return false;
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float fOffset = mStartOffset + mDeltaOffset * interpolatedTime;
            float velocityInterpolator = (float) Math.sqrt(interpolatedTime) * 3.f;
            if (velocityInterpolator < 1.f)
                fOffset -= mVelocity / mWindowSize.x * velocityInterpolator;
            else
                fOffset -= mVelocity / mWindowSize.x * (1.f - 0.5f * (velocityInterpolator - 1.f));
            updateWallpaperOffset(fOffset);
        }
    }
}
