package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.result.CustomRecycleLayoutManager;

public class RecyclerList extends RecyclerView {
    private static final String TAG = "list";

    public RecyclerList(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

//    /**
//     * Prevent this ListView from receiving any new touch events
//     * <p>
//     * Use {@link #unblockTouchEvents()} to end the blockage.
//     */
//    public void blockTouchEvents() {
//        this.touchEventsBlocked = true;
//        if (getLayoutManager() instanceof CustomRecycleLayoutManager) {
//            CustomRecycleLayoutManager customRecycleLayoutManager = (CustomRecycleLayoutManager) getLayoutManager();
//            customRecycleLayoutManager.setAutoScrollBottom(false);
//        }
//    }
//
//    /**
//     * Stop preventing this ListView from receiving touch events
//     */
//    public void unblockTouchEvents() {
//        this.touchEventsBlocked = false;
//        if (getLayoutManager() instanceof CustomRecycleLayoutManager) {
//            CustomRecycleLayoutManager customRecycleLayoutManager = (CustomRecycleLayoutManager) getLayoutManager();
//            customRecycleLayoutManager.setAutoScrollBottom(true);
//        }
//    }
//
//    public boolean touchEventsBlocked() {
//        return touchEventsBlocked;
//    }

    public void scrollToLastPosition() {
        final int resultCount = getAdapter() != null ? getAdapter().getItemCount() : 0;
        if (resultCount > 0) {
            Log.d(TAG, "scrollToPosition( " + (resultCount - 1) + " )");
            scrollToPosition(resultCount - 1);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
//        boolean handled = false;
//        int action = ev.getActionMasked();
//        if (!touchEventsBlocked && action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE)
//            lastTouchPos.set(ev.getRawX(), ev.getRawY());
//        MotionEvent e = MotionEvent.obtain(ev);
//        e.offsetLocation(0f, 0f);
//        if (touchEventsBlocked) {
//            e.setAction(MotionEvent.ACTION_CANCEL);
//            handled = super.onTouchEvent(ev);
//        }
//        e.recycle();
//        return handled;
        return super.onTouchEvent(ev);
    }

    public void setTranscriptMode(int mode) {
        // no need, this was useful for the ListView
    }

    public void prepareChangeAnim() {
        ItemAnimator itemAnimator = getItemAnimator();
        if (itemAnimator != null)
            itemAnimator.endAnimations();
    }

    public void animateChange() {
        // no need, the RecyclerView takes care of it
    }
}
