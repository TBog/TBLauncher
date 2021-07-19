package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class RecycleListLayoutManager extends LinearLayoutManager {
    private static final String TAG = "RLLM";
    private int mLastScrollPos = -1;

    public RecycleListLayoutManager(Context context) {
        this(context, LinearLayoutManager.VERTICAL, false);
        setStackFromEnd(true);
    }

    public RecycleListLayoutManager(Context context, int orientation, boolean reverseLayout) {
        super(context, orientation, reverseLayout);
    }

    public RecycleListLayoutManager(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }



    @Override
    public boolean isAutoMeasureEnabled() {
        return false;
    }

    public void onBeforeLayout() {
        if (mLastScrollPos != -1) {
            Log.d(TAG, "onBeforeLayout: mLastScrollPos=" + mLastScrollPos);
            scrollToPositionWithOffset(mLastScrollPos, 0);

        }
    }

//    @Override
//    public void onLayoutCompleted(RecyclerView.State state) {
//        super.onLayoutCompleted(state);
//        if (mLastScrollPos != -1) {
//            Log.d(TAG, "onLayoutCompleted: mLastScrollPos=" + mLastScrollPos);
//            scrollToPositionWithOffset(mLastScrollPos, 0);
//        }
//    }

    @Override
    public void scrollToPosition(int position) {
        Log.d(TAG, "scrollToPosition: pos=" + position + " mLastScrollPos=" + mLastScrollPos);
        mLastScrollPos = position;
        super.scrollToPosition(position);
    }

    public void onItemAnimationsFinished() {
        if (mLastScrollPos != -1)
            scrollToPositionWithOffset(mLastScrollPos, 0);
    }

    public void resetLastScrollPosition(int newPos) {
        Log.d(TAG, "scrollToPosition: pos=" + newPos + " mLastScrollPos=" + mLastScrollPos);
        mLastScrollPos = newPos;
    }

    public void resetLastScrollPosition() {
        resetLastScrollPosition(-1);
    }

    public int scrollToLastScrollPosition() {
        if (mLastScrollPos != -1) {
            Log.d(TAG, "scrollToLastPosition: mLastScrollPos=" + mLastScrollPos);
            scrollToPositionWithOffset(mLastScrollPos, 0);
        }
        return mLastScrollPos;
    }

//    public void scrollToLastPosition() {
//        int itemCount = getItemCount();
//        if (itemCount > 0) {
//            int position = itemCount - 1;
//            Log.d(TAG, "scrollToLastPosition: pos=" + position + " mLastScrollPos=" + mLastScrollPos);
//            mLastScrollPos = position;
//        } else {
//            Log.d(TAG, "scrollToLastPosition: itemCount=" + itemCount + " mLastScrollPos=" + mLastScrollPos);
//            mLastScrollPos = -1;
//        }
//    }
}
