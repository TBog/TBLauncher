package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

public class WidgetView extends AppWidgetHostView {
    private static final String TAG = "WdgView";
    private final GestureDetectorCompat gestureDetector;
    private boolean mIntercepted = false;
    private boolean mJustIntercepted = false;
    private boolean mSendCancel = false;
    private boolean mLongClickCalled = false;
    private OnClickListener mOnClickListener = null;
    private OnClickListener mOnDoubleClickListener = null;
    private OnLongClickListener mOnLongClickListener = null;

    public WidgetView(Context context) {
        super(context);
        //TODO: make WidgetView implement OnGestureListener and get rid of onGestureListener
        GestureDetector.SimpleOnGestureListener onGestureListener = new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                // if we have a double tap listener, wait for onSingleTapConfirmed
                if (mOnDoubleClickListener != null)
                    return true;
                if (mOnClickListener != null) {
                    mOnClickListener.onClick(WidgetView.this);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                // if we have both a double tap and click, handle click here
                if (mOnClickListener != null && mOnDoubleClickListener != null) {
                    mOnClickListener.onClick(WidgetView.this);
                    return true;
                }
                return false;
            }

            @Override
            public void onLongPress(MotionEvent e) {
                if (mOnLongClickListener != null) {
                    mLongClickCalled = true;
                    mOnLongClickListener.onLongClick(WidgetView.this);
                }
            }

            @Override
            public boolean onDoubleTapEvent(MotionEvent e) {
                //Log.d(TAG, "onDoubleTapEvent " + e);
                if (mOnDoubleClickListener != null) {
                    final int act = e.getActionMasked();
                    if (act == MotionEvent.ACTION_UP)
                        mOnDoubleClickListener.onClick(WidgetView.this);
                    return true;
                }
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                //Log.d(TAG, "onScroll mSendCancel = true");
                //mSendCancel = true;
                //TBApplication.liveWallpaper(context).scroll(e1, e2);
                //return true;

                return false;
            }

            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
//                Log.d(TAG, "onFling mSendCancel = true" +
//                        String.format("\r\nvelocity( %.2f, %.2f )", velocityX, velocityY) +
//                        String.format("\r\ndown pos ( %.2f, %.2f )", e1.getX(), e1.getY()) +
//                        String.format("\r\n up  pos ( %.2f, %.2f )", e2.getX(), e2.getY())
//                        );
//                mSendCancel = true;
//                return true;

                return false;
            }
        };
        gestureDetector = new GestureDetectorCompat(context, onGestureListener);
    }

    @Override
    public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        gestureDetector.setIsLongpressEnabled(listener != null);
        mOnLongClickListener = listener;
    }

    @Override
    public void setOnClickListener(@Nullable OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setOnDoubleClickListener(@Nullable OnClickListener listener) {
        mOnDoubleClickListener = listener;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        Log.d(TAG, "onInterceptTouchEvent\r\n" + event + "\r\nmIntercepted = " + mIntercepted);
        if (event.getPointerCount() != 1)
            return false;
        final int act = event.getActionMasked();
        switch (act) {
            case MotionEvent.ACTION_DOWN:
                // Throw away all previous state when starting a new touch gesture.
                // The framework may have dropped the up or cancel event for the previous gesture
                // due to an app switch, ANR, or some other state change.
                mIntercepted = false;
                mJustIntercepted = false;
                mSendCancel = false;
                mLongClickCalled = false;
                break;
            case MotionEvent.ACTION_UP:
                if (mLongClickCalled) {
                    mLongClickCalled = false;
                    return mJustIntercepted = true;
                }
                break;
        }

        if (mIntercepted)
            return true;
        if (gestureDetector.onTouchEvent(event)) {
            Log.d(TAG, "mJustIntercepted = " + true);
            return mJustIntercepted = true;
        }

        Log.d(TAG, "super.onInterceptTouchEvent");
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent\t\n" + event +
                "\r\nmJustIntercepted " + mJustIntercepted +
                " mIntercepted " + mIntercepted);
        // first call after the intercept can be ignored
        if (mJustIntercepted) {
            mJustIntercepted = false;
            mIntercepted = true;
            Log.d(TAG, "mJustIntercepted = " + false);
            return true;
        }
        if (event.getPointerCount() != 1)
            return false;
        if (gestureDetector.onTouchEvent(event))
            return true;

        // if we intercepted this gesture, handle all touch events
        boolean handled = mIntercepted;

        if (mSendCancel) {
            handled = true;
            event.setAction(MotionEvent.ACTION_CANCEL);
        }

        Log.d(TAG, "super.onTouchEvent");
        if (super.onTouchEvent(event)) {
            Log.d(TAG, "mIntercepted = " + false);
            mIntercepted = false;
            handled = true;
        } else {
            // if no child view handled this event, send cancel to gestureDetector
            MotionEvent cancel = MotionEvent.obtainNoHistory(event);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            Log.d(TAG, "gestureDetector CANCEL");
            gestureDetector.onTouchEvent(cancel);
        }
        return handled;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // deny this request
        //super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }
}
