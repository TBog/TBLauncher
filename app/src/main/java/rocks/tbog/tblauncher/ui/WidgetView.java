package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import androidx.annotation.Nullable;
import androidx.core.view.GestureDetectorCompat;

import rocks.tbog.tblauncher.WidgetManager;

public class WidgetView extends AppWidgetHostView {
    private static final String TAG = "WdgView";
    private final GestureDetectorCompat gestureDetector;
    private boolean mIntercepted = false;
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
        Log.d(TAG, "onInterceptTouchEvent\r\n" + event);
        if (event.getPointerCount() != 1)
            return false;
        final int act = event.getActionMasked();
        switch (act) {
            case MotionEvent.ACTION_DOWN:
                // Throw away all previous state when starting a new touch gesture.
                // The framework may have dropped the up or cancel event for the previous gesture
                // due to an app switch, ANR, or some other state change.
                mIntercepted = false;
                mLongClickCalled = false;
                break;
            case MotionEvent.ACTION_UP:
                if (mLongClickCalled) {
                    mLongClickCalled = false;
                    return mIntercepted = true;
                }
                break;
        }

        if (gestureDetector.onTouchEvent(event)) {
            Log.d(TAG, "mIntercepted " + true);
            return mIntercepted = true;
        }

        Log.d(TAG, "super.onInterceptTouchEvent");
        return super.onInterceptTouchEvent(event);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent\t\n" + event + "\r\nmIntercepted " + mIntercepted);
        // first call after the intercept can be ignored
        if (mIntercepted) {
            mIntercepted = false;
            Log.d(TAG, "mIntercepted " + false);
            return true;
        }
        if (event.getPointerCount() != 1)
            return false;
        if (gestureDetector.onTouchEvent(event))
            return true;

        Log.d(TAG, "super.onTouchEvent");
        if (super.onTouchEvent(event))
            return true;
        // if no child view handled this event, send cancel to gestureDetector
        MotionEvent cancel = MotionEvent.obtainNoHistory(event);
        cancel.setAction(MotionEvent.ACTION_CANCEL);
        Log.d(TAG, "gestureDetector CANCEL");
        gestureDetector.onTouchEvent(cancel);
        return false;
    }

    @Override
    public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
        // deny this request
        //super.requestDisallowInterceptTouchEvent(disallowIntercept);
    }
}
