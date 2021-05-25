package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ResultListLayoutManager extends LinearLayoutManager {
    private static final String TAG = "RLLM";
    int lastScrollPos = -1;

    public ResultListLayoutManager(Context context) {
        this(context, LinearLayoutManager.VERTICAL, false);
        setStackFromEnd(true);
    }

    public ResultListLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public ResultListLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        scrollToPositionWithOffset(lastScrollPos, 0);
    }

    @Override
    public void scrollToPosition(int position) {
        Log.d(TAG, "scrollToPosition: pos=" + position + " lastScrollPos=" + lastScrollPos);
        lastScrollPos = position;
        super.scrollToPosition(position);
    }

    public void onItemAnimationsFinished() {
        scrollToPositionWithOffset(lastScrollPos, 0);
    }
}
