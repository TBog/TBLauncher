package rocks.tbog.tblauncher.result;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ResultItemDecoration extends RecyclerView.ItemDecoration {
    private final int mHorizontal;
    private final int mVertical;
    private final boolean mOnlyForGrid;

    public ResultItemDecoration(int horizontal, int vertical, boolean onlyForGrid) {
        mHorizontal = horizontal;
        mVertical = vertical;
        mOnlyForGrid = onlyForGrid;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int columns = 1;
        boolean bottom2top = false;
        boolean right2left = false;
        boolean reverseAdapter = false;
        RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
        if (layoutManager instanceof CustomRecycleLayoutManager) {
            columns = ((CustomRecycleLayoutManager) layoutManager).getColumnCount();
            bottom2top = ((CustomRecycleLayoutManager) layoutManager).isBottomToTop();
            reverseAdapter = ((CustomRecycleLayoutManager) layoutManager).isReverseAdapter();
            right2left = ((CustomRecycleLayoutManager) layoutManager).isRightToLeft();
        }
        if (mOnlyForGrid && columns <= 1)
            return;

        int adapterPos = parent.getChildAdapterPosition(view);
        if (reverseAdapter) {
            int itemCount = parent.getAdapter() != null ? parent.getAdapter().getItemCount() : 0;
            adapterPos = itemCount - 1 - adapterPos;
        }

        int leftColumn = right2left ? (columns - 1) : 0;

        // add left margin only to the leftmost column
        if (adapterPos % columns == leftColumn)
            outRect.left += mHorizontal;

        // add top margin only to the topmost row
        if (bottom2top) {
            int itemCount = parent.getAdapter() != null ? parent.getAdapter().getItemCount() : 0;
            int rowCount = itemCount / columns + (itemCount % columns == 0 ? 0 : 1);
            int row = adapterPos / columns;
            if (row == (rowCount - 1))
                outRect.top += mVertical;
        } else {
            if (adapterPos < columns)
                outRect.top += mVertical;
        }
        outRect.right += mHorizontal;
        outRect.bottom += mVertical;
    }
}
