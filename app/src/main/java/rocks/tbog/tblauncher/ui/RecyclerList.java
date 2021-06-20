package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.result.RecycleListLayoutManager;

public class RecyclerList extends RecyclerView {
    private boolean touchEventsBlocked = false;

    public RecyclerList(@NonNull Context context) {
        this(context, null);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Prevent this ListView from receiving any new touch events
     * <p>
     * Use {@link #unblockTouchEvents()} to end the blockage.
     */
    public void blockTouchEvents() {
        this.touchEventsBlocked = true;
    }

    /**
     * Stop preventing this ListView from receiving touch events
     */
    public void unblockTouchEvents() {
        this.touchEventsBlocked = false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return this.touchEventsBlocked || super.onTouchEvent(ev);
    }

    public void setTranscriptMode(int mode) {
        //super.setTranscriptMode(mode);
    }

    public void prepareChangeAnim() {
        ItemAnimator itemAnimator = getItemAnimator();
        if (itemAnimator != null)
            itemAnimator.endAnimations();
    }

    public void animateChange() {
        ItemAnimator itemAnimator = getItemAnimator();
        if (itemAnimator != null)
            itemAnimator.isRunning(() -> {
                LayoutManager layoutManager = getLayoutManager();
                if (layoutManager instanceof RecycleListLayoutManager) {
                    ((RecycleListLayoutManager) layoutManager).onItemAnimationsFinished();
                }
            });
    }

    public void refreshViews() {
        //TODO: implement this
    }
}
