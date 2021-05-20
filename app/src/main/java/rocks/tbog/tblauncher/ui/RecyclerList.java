package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

public class RecyclerList extends RecyclerView {
    private boolean touchEventsBlocked = false;
    AdapterView.OnItemClickListener mOnItemClickListener = null;
    AdapterView.OnItemLongClickListener mOnItemLongClickListener = null;

    public RecyclerList(@NonNull @NotNull Context context) {
        super(context);
    }

    public RecyclerList(@NonNull @NotNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerList(@NonNull @NotNull Context context, @Nullable @org.jetbrains.annotations.Nullable AttributeSet attrs, int defStyleAttr) {
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

    public void prepareChangeAnim() {}
    public void animateChange() {}
    public void refreshViews() {}

    public void setOnItemClickListener() {

    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked.
     *
     * @param listener The callback that will be invoked.
     */
    public void setOnItemClickListener(@Nullable AdapterView.OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    /**
     * Register a callback to be invoked when an item in this AdapterView has
     * been clicked and held
     *
     * @param listener The callback that will run
     */
    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
        if (!isLongClickable()) {
            setLongClickable(true);
        }
        mOnItemLongClickListener = listener;
    }

}
