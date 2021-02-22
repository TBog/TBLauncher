package rocks.tbog.tblauncher;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowMetrics;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import androidx.preference.PreferenceManager;

import java.util.Locale;

import rocks.tbog.tblauncher.ui.ListPopup;

public class LiveWallpaper {
    private static final int longPressTimeout = ViewConfiguration.getLongPressTimeout();
    private static final float minMovement = .025f;
    private static final float minVelocity = .01f;
    private static final String TAG = "LWP";
    private TBLauncherActivity mTBLauncherActivity = null;
    private WallpaperManager mWallpaperManager;
    private final Point mWindowSize = new Point(1, 1);
    private View mContentView;
    private final PointF mFirstTouchOffset = new PointF();
    private final PointF mFirstTouchPos = new PointF();
    private final PointF mLastTouchPos = new PointF();
    private final PointF mWallpaperOffset = new PointF(.5f, .5f);
    private Anim mAnimation;
    private VelocityTracker mVelocityTracker;

    public static final int SCREEN_COUNT_HORIZONTAL = Integer.parseInt("3");
    public static final int SCREEN_COUNT_VERTICAL = Integer.parseInt("1"); // not tested with values != 1

    private boolean lwpTouch = true;
    private boolean lwpDrag = false;
    private boolean wpDragAnimate = true;
    private boolean wpReturnCenter = true;
    private boolean wpStickToSides = false;

    private final Runnable mLongClickRunnable = () -> {
        if (!TBApplication.state().isWidgetScreenVisible())
            return;
        View view = mTBLauncherActivity.findViewById(R.id.root_layout);
        onLongClick(view);
    };

    LiveWallpaper() {
//        TypedValue typedValue = new TypedValue();
//        mainActivity.getTheme().resolveAttribute(android.R.attr.windowShowWallpaper, typedValue, true);
//        TypedArray a = mainActivity.obtainStyledAttributes(typedValue.resourceId, new int[]{android.R.attr.windowShowWallpaper});
//        wallpaperIsVisible = a.getBoolean(0, true);
//        a.recycle();
    }

    public PointF getWallpaperOffset() {
        return mWallpaperOffset;
    }

    public void scroll(MotionEvent e1, MotionEvent e2) {
        if (!isPreferenceWPDragAnimate())
            return;
        cacheWindowSize();
        mFirstTouchPos.set(e1.getRawX(), e1.getRawY());
        mLastTouchPos.set(e2.getRawX(), e2.getRawY());
        float xMove = (mFirstTouchPos.x - mLastTouchPos.x) / mWindowSize.x;
        float yMove = (mFirstTouchPos.y - mLastTouchPos.y) / mWindowSize.y;
        float offsetX = mFirstTouchOffset.x + xMove * 1.01f;
        float offsetY = mFirstTouchOffset.y + yMove * 1.01f;
        updateWallpaperOffset(offsetX, offsetY);
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

        // set mContentView before we call updateWallpaperOffset
        mContentView = mainActivity.findViewById(android.R.id.content);
        {
            float xStep = (SCREEN_COUNT_HORIZONTAL > 1) ? (1.f / (SCREEN_COUNT_HORIZONTAL - 1)) : 0.f;
            float yStep = (SCREEN_COUNT_VERTICAL > 1) ? (1.f / (SCREEN_COUNT_VERTICAL - 1)) : 0.f;
            mWallpaperManager.setWallpaperOffsetSteps(xStep, yStep);

            int centerScreenX = SCREEN_COUNT_HORIZONTAL / 2;
            int centerScreenY = SCREEN_COUNT_VERTICAL / 2;
            updateWallpaperOffset(centerScreenX * xStep, centerScreenY * yStep);
        }
        mAnimation = new Anim();
        mVelocityTracker = null;
        View root = mainActivity.findViewById(R.id.root_layout);
        root.setOnTouchListener(this::onRootTouch);
    }

    private static boolean onClick(View view) {
        return TBApplication.behaviour(view.getContext()).onClick();
    }

    private boolean onFling(View view, float xMove, float yMove, float xVel, float yVel) {
        int posX = (int) mFirstTouchPos.x;

        Behaviour behaviour = TBApplication.behaviour(view.getContext());
        // fling direction is greater on the Y axis
        if (Math.abs(yVel) > Math.abs(xVel)) {
            // fling direction downward
            if (yVel > 0.f) {
                if (posX < (mWindowSize.x / 2))
                    return behaviour.onFlingDownLeft();
                else
                    return behaviour.onFlingDownRight();
            } else
                return behaviour.onFlingUp();
        } else {
            if (xVel < 0.f) {
                return behaviour.onFlingLeft();
            } else {
                return behaviour.onFlingRight();
            }
        }
    }

    private void onLongClick(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        Context ctx = view.getContext();
        ListPopup menu = TBApplication.widgetManager(ctx).getConfigPopup(mTBLauncherActivity);
        TBApplication.behaviour(ctx).registerPopup(menu);
        int x = (int) (mLastTouchPos.x + .5f);
        int y = (int) (mLastTouchPos.y + .5f);
        menu.showAtLocation(view, Gravity.START | Gravity.TOP, x, y);
    }

    private void cacheWindowSize() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = mTBLauncherActivity.getWindowManager().getCurrentWindowMetrics();
            //Insets insets = windowMetrics.getWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
            Rect windowBound = windowMetrics.getBounds();
            int width = windowBound.width();// - insets.left - insets.right;
            int height = windowBound.height();// - insets.top - insets.bottom;
            mWindowSize.set(width, height);
        } else {
            mTBLauncherActivity.getWindowManager()
                    .getDefaultDisplay()
                    .getSize(mWindowSize);
        }
    }

    boolean onRootTouch(View view, MotionEvent event) {
        if (!view.isAttachedToWindow()) {
            return false;
        }

        Log.d(TAG, "onRootTouch\r\n" + event);
        int actionMasked = event.getActionMasked();
        boolean eventConsumed = false;

        switch (actionMasked) {
            case MotionEvent.ACTION_DOWN: {
                view.postDelayed(mLongClickRunnable, longPressTimeout);
                mFirstTouchPos.set(event.getRawX(), event.getRawY());
                mLastTouchPos.set(mFirstTouchPos);
                mFirstTouchOffset.set(mWallpaperOffset);
                cacheWindowSize();
                if (isPreferenceWPDragAnimate()) {
                    mContentView.clearAnimation();
                }
                if (mVelocityTracker != null)
                    mVelocityTracker.recycle();
                mVelocityTracker = VelocityTracker.obtain();
                mVelocityTracker.addMovement(event);
                //send touch event to the LWP
                if (isPreferenceLWPTouch())
                    sendTouchEvent(view, event);
                eventConsumed = true;
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                mLastTouchPos.set(event.getRawX(), event.getRawY());
                float xMove = (mFirstTouchPos.x - mLastTouchPos.x) / mWindowSize.x;
                float yMove = (mFirstTouchPos.y - mLastTouchPos.y) / mWindowSize.y;
                if (Math.abs(xMove) > .01f || Math.abs(yMove) > .01f)
                    view.removeCallbacks(mLongClickRunnable);
                if (mVelocityTracker != null)
                    mVelocityTracker.addMovement(event);

                if (isPreferenceWPDragAnimate()) {
                    float offsetX = mFirstTouchOffset.x + xMove * 1.01f;
                    float offsetY = mFirstTouchOffset.y + yMove * 1.01f;
                    updateWallpaperOffset(offsetX, offsetY);
                }

                //send move/drag event to the LWP
                if (isPreferenceLWPDrag())
                    sendTouchEvent(view, event);
                if (isPreferenceWPDragAnimate())
                    eventConsumed = true;
                break;
            }
            case MotionEvent.ACTION_UP: {
                // was this a click?
                float xMove = (mFirstTouchPos.x - mLastTouchPos.x) / mWindowSize.x;
                float yMove = (mFirstTouchPos.y - mLastTouchPos.y) / mWindowSize.y;
                if (mVelocityTracker == null) {
                    Log.d(TAG, String.format(Locale.US, "Move=(%.3f, %.3f)", xMove, yMove));
                    if (Math.abs(xMove) < minMovement && Math.abs(yMove) < minMovement) {
                        if (event.getEventTime() - event.getDownTime() < longPressTimeout) {
                            eventConsumed = onClick(view);
                        }
                    }
                } else {
                    mVelocityTracker.addMovement(event);
                    mVelocityTracker.computeCurrentVelocity(1000 / 30);
                    float xVel = mVelocityTracker.getXVelocity() / mWindowSize.x;
                    float yVel = mVelocityTracker.getYVelocity() / mWindowSize.y;
                    Log.d(TAG, String.format(Locale.US, "Velocity=(%.3f, %.3f) Move=(%.3f, %.3f)", xVel, yVel, xMove, yMove));
                    // if no movement detected
                    if (Math.abs(xVel) < minVelocity
                            && Math.abs(yVel) < minVelocity
                            && Math.abs(xMove) < minMovement
                            && Math.abs(yMove) < minMovement) {
                        if (event.getEventTime() - event.getDownTime() < longPressTimeout)
                            onClick(view);
                        // snap position if needed
                        if (isPreferenceWPDragAnimate() && mAnimation.init())
                            mContentView.startAnimation(mAnimation);
                        eventConsumed = true;
                    } else {
                        eventConsumed = onFling(view, xMove, yMove, xVel, yVel);
                    }
                }
                //fallthrough
            }
            case MotionEvent.ACTION_CANCEL:
                view.removeCallbacks(mLongClickRunnable);
                if (isPreferenceWPDragAnimate()) {
                    if (mVelocityTracker != null) {
                        mVelocityTracker.addMovement(event);

                        mVelocityTracker.computeCurrentVelocity(1000 / 30);
                        if (mAnimation.init())
                            mContentView.startAnimation(mAnimation);

                        mVelocityTracker.recycle();
                        mVelocityTracker = null;
                    } else {
                        if (mAnimation.init())
                            mContentView.startAnimation(mAnimation);
                    }
                    eventConsumed = true;
                }
                break;
        }

        Log.d(TAG, "onRootTouch event " + (eventConsumed ? "" : "NOT") + "consumed");
        return eventConsumed;
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

    public boolean isPreferenceWPDragAnimate() {
        return wpDragAnimate;
    }

    private boolean isPreferenceWPReturnCenter() {
        return wpReturnCenter;
    }

    private boolean isPreferenceWPStickToSides() {
        return wpStickToSides;
    }

    private android.os.IBinder getWindowToken() {
        return mContentView != null && mContentView.isAttachedToWindow() ? mContentView.getWindowToken() : null;
    }

    private void updateWallpaperOffset(float offsetX, float offsetY) {
        offsetX = Math.max(0.f, Math.min(1.f, offsetX));
        offsetY = Math.max(0.f, Math.min(1.f, offsetY));
        mWallpaperOffset.set(offsetX, offsetY);
        TBApplication.widgetManager(mTBLauncherActivity).scroll(offsetX, offsetY);
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            mWallpaperManager.setWallpaperOffsets(iBinder, offsetX, offsetY);
        }
    }

    private void sendTouchEvent(int x, int y, int index) {
        android.os.IBinder iBinder = getWindowToken();
        if (iBinder != null) {
            String command = index == 0 ? WallpaperManager.COMMAND_TAP : WallpaperManager.COMMAND_SECONDARY_TAP;
            try {
                mWallpaperManager.sendWallpaperCommand(iBinder, command, x, y, 0, null);
            } catch (Exception e) {
                Log.e(TAG, "sendTouchEvent (" + x + "," + y + ") idx=" + index, e);
            }
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
        final PointF mStartOffset = new PointF();
        final PointF mDeltaOffset = new PointF();
        final PointF mVelocity = new PointF();

        Anim() {
            super();
            setDuration(500);
            setInterpolator(new DecelerateInterpolator());
        }

        boolean init() {
            if (mVelocityTracker == null) {
                mVelocity.set(0.f, 0.f);
            } else {
                mVelocity.set(mVelocityTracker.getXVelocity(), mVelocityTracker.getYVelocity());
            }
            //Log.d(TAG, "mVelocity=" + String.format(Locale.US, "%.2f", mVelocity));

            mStartOffset.set(mWallpaperOffset);
            //Log.d(TAG, "mStartOffset=" + String.format(Locale.US, "%.2f", mStartOffset));

            boolean stickToSides = isPreferenceWPStickToSides();
            boolean stickToCenter = isPreferenceWPReturnCenter();
            float expectedPosX = -Math.min(Math.max(mVelocity.x / mWindowSize.x, -.5f), .5f) + mStartOffset.x;
            //float expectedPosY = -Math.min(Math.max(mVelocity.y / mWindowSize.y, -.5f), .5f) + mStartOffset.y;
            //Log.d(TAG, "expectedPos=" + String.format(Locale.US, "%.2f %.2f", expectedPosX, expectedPosY));

            // stick to center
            mDeltaOffset.y = .5f - mStartOffset.y;

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

            if (expectedPosX <= leftStickPercent)
                mDeltaOffset.x = 0.f - mStartOffset.x;
            else if (expectedPosX >= rightStickPercent)
                mDeltaOffset.x = 1.f - mStartOffset.x;
            else if (stickToCenter)
                mDeltaOffset.x = .5f - mStartOffset.x;
            else
                return false;
            return true;
        }

        @Override
        protected void applyTransformation(float interpolatedTime, Transformation t) {
            float offsetX = mStartOffset.x + mDeltaOffset.x * interpolatedTime;
            float offsetY = mStartOffset.y + mDeltaOffset.y * interpolatedTime;
            float velocityInterpolator = (float) Math.sqrt(interpolatedTime) * 3.f;
            if (velocityInterpolator < 1.f) {
                offsetX -= mVelocity.x / mWindowSize.x * velocityInterpolator;
                offsetY -= mVelocity.y / mWindowSize.y * velocityInterpolator;
            } else {
                offsetX -= mVelocity.x / mWindowSize.x * (1.f - 0.5f * (velocityInterpolator - 1.f));
                offsetY -= mVelocity.y / mWindowSize.y * (1.f - 0.5f * (velocityInterpolator - 1.f));
            }
            updateWallpaperOffset(offsetX, offsetY);
        }
    }
}
