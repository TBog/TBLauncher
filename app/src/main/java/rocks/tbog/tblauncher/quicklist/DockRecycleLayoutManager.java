package rocks.tbog.tblauncher.quicklist;

import android.graphics.PointF;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.SparseArrayWrapper;

public class DockRecycleLayoutManager extends RecyclerView.LayoutManager implements RecyclerView.SmoothScroller.ScrollVectorProvider {
    private static final String TAG = "DRLM";
    private static final Boolean LOG_DEBUG = true;
    // Reusable array. This should only be used used transiently and should not be used to retain any state over time.
    private final SparseArrayWrapper<View> mViewCache = new SparseArrayWrapper<>();
    private RecyclerView mRecyclerView = null;
    private boolean mRefreshViews = false;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth = 0;
    private int mDecoratedChildHeight = 0;

    private int mColumnCount = 6;
    private int mRowCount = 1;
    private int mScrollToAdapterPosition = -1;
    private int mScrollOffsetHorizontal = 0;
    private boolean mRightToLeft = false;

    public DockRecycleLayoutManager(int columnCount, int rowCount) {
        super();
        setColumnCount(columnCount);
        setRowCount(rowCount);
    }

    public void setColumnCount(int count) {
        mColumnCount = count;
    }

    public void setRowCount(int count) {
        mRowCount = count;
    }

    public void setRightToLeft(boolean rightToLeft) {
        mRightToLeft = rightToLeft;
    }

    private static int adapterPosition(@NonNull View child) {
        return ((RecyclerView.LayoutParams) child.getLayoutParams()).getViewLayoutPosition();
    }

    private static boolean viewNeedsUpdate(View v) {
        RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) v.getLayoutParams();
        return p.viewNeedsUpdate();
    }

    private static String getDebugName(View v) {
        View name;
        name = v.findViewById(R.id.item_app_name);
        if (name instanceof TextView) {
            CharSequence text = ((TextView) name).getText();
            if (text != null && text.length() > 0)
                return text.toString();
        }
        name = v.findViewById(R.id.item_contact_name);
        if (name instanceof TextView) {
            CharSequence text = ((TextView) name).getText();
            if (text != null && text.length() > 0)
                return text.toString();
        }
        name = v.findViewById(android.R.id.text1);
        if (name instanceof TextView) {
            CharSequence text = ((TextView) name).getText();
            if (text != null && text.length() > 0)
                return text.toString();
        }
        return "";
    }

    private static String getDebugInfo(View v) {
        String info = "";
        if (v.getLayoutParams() instanceof RecyclerView.LayoutParams) {
            RecyclerView.LayoutParams p = (RecyclerView.LayoutParams) v.getLayoutParams();
            info += "lp" + p.getViewLayoutPosition();
            info += "ap" + p.getViewAdapterPosition();
            if (p.viewNeedsUpdate())
                info += "u";
            if (p.isItemChanged())
                info += "c";
            if (p.isItemRemoved())
                info += "r";
            if (p.isViewInvalid())
                info += "i";
        }
        return info;
    }

    private static void logDebug(String message) {
        if (LOG_DEBUG)
            Log.d(TAG, message);
    }

    @Override
    public void onAttachedToWindow(RecyclerView view) {
        super.onAttachedToWindow(view);
        mRecyclerView = view;
    }

    @Override
    public void onDetachedFromWindow(RecyclerView view, RecyclerView.Recycler recycler) {
        mRecyclerView = null;
        super.onDetachedFromWindow(view, recycler);
    }

    /*
     * You must return true from this method if you want your
     * LayoutManager to support anything beyond "simple" item
     * animations. Enabling this causes onLayoutChildren() to
     * be called twice on each animated change; once for a
     * pre-layout, and again for the real layout.
     */
    @Override
    public boolean supportsPredictiveItemAnimations() {
        return false;
    }

    /*
     * Called by RecyclerView when a view removal is triggered. This is called
     * before onLayoutChildren() in pre-layout if the views removed are not visible. We
     * use it in this case to inform pre-layout that a removal took place.
     *
     * This method is still called if the views removed were visible, but it will
     * happen AFTER pre-layout.
     */
    @Override
    public void onItemsRemoved(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        logDebug("onItemsRemoved start=" + positionStart + " count=" + itemCount);
        mRefreshViews = true;
    }

    @Override
    public void onItemsMoved(@NonNull RecyclerView recyclerView, int from, int to, int itemCount) {
        logDebug("onItemsMoved from=" + from + " to=" + to + " count=" + itemCount);
        mRefreshViews = true;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
    }

    /*
     * This method is your initial call from the framework. You will receive it when you
     * need to start laying out the initial set of views. This method will not be called
     * repeatedly, so don't rely on it to continually process changes during user
     * interaction.
     *
     * This method will be called when the data set in the adapter changes, so it can be
     * used to update a layout based on a new item count.
     *
     * If predictive animations are enabled, you will see this called twice. First, with
     * state.isPreLayout() returning true to lay out children in their initial conditions.
     * Then again to lay out children in their final locations.
     *
     * When scrolling, if a view has been added, this will be called after scroll*By
     */
    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        //We have nothing to show for an empty data set but clear any existing views
        if (getItemCount() == 0) {
            detachAndScrapAttachedViews(recycler);
            return;
        }
        if (getChildCount() == 0 && state.isPreLayout()) {
            //Nothing to do during prelayout when empty
            return;
        }

        // Always update the visible row/column counts
        updateSizing();

        logDebug("onLayoutChildren" +
            " childCount=" + getChildCount() +
            " itemCount=" + getItemCount() +
            (state.isPreLayout() ? " preLayout" : "") +
            (state.didStructureChange() ? " structureChanged" : "") +
            " stateItemCount=" + state.getItemCount());

        layoutChildren(recycler);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        //TODO: auto-scroll after resize
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state, int position) {
        LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext());
        scroller.setTargetPosition(position);
        startSmoothScroll(scroller);
    }

    @Override
    public void scrollToPosition(int position) {
        mScrollToAdapterPosition = position;
    }

    private void updateSizing() {
        final int width = getColumnWidth();
        final int height = getRowHeight();
        if (mDecoratedChildWidth != width || mDecoratedChildHeight != height) {
            if (mDecoratedChildWidth != width)
                logDebug("mDecoratedChildWidth changed from " + mDecoratedChildWidth + " to " + width);
            if (mDecoratedChildHeight != width)
                logDebug("mDecoratedChildHeight changed from " + mDecoratedChildHeight + " to " + height);
            mRefreshViews = true;
            mDecoratedChildWidth = width;
            mDecoratedChildHeight = height;
        }
    }

    /* Example for 15 items placed on 3 columns and 2 rows (3 pages)
     *
     * mRightToLeft == false
     *       page 0      page 1     page 2
     *       + - - - - - - - - - - - - - - - - +
     * row 0 |  0  1  2 |  6  7  8 | 12 13 14  |
     * row 1 |  3  4  5 |  9 10 11 | 15        |
     *       + - - - - - - - - - - - - - - - - +
     * column   0  1  2    3  4  5    6  7  8
     *
     *
     * mRightToLeft == true
     *       page 2      page 1     page 0
     *       + - - - - - - - - - - - - - - - - +
     * row 0 | 14 13 12 |  8  7  6 |  2  1  0  |
     * row 1 |       15 | 11 10  9 |  5  4  3  |
     *       + - - - - - - - - - - - - - - - - +
     * column   8  7  6    5  4  3    2  1  0
     */

    private int getAdapterIdx(int colIdx, int rowIdx) {
        int columnInPage = colIdx % mColumnCount;
        int page = colIdx / mColumnCount;
        return page * mColumnCount * mRowCount + rowIdx * mColumnCount + columnInPage;
    }

    private int getColumnIdx(int adapterPos) {
        int columnInPage = adapterPos % (mColumnCount * mRowCount) % mColumnCount;
        int page = getPageIdx(adapterPos);
        return page * mColumnCount + columnInPage;
    }

    private int getRowIdx(int adapterPos) {
        int group = adapterPos / mColumnCount;
        return group % mRowCount;
    }

    private int getPageIdx(int adapterPos) {
        return adapterPos / (mColumnCount * mRowCount);
    }

    private int getColumnWidth() {
        return getHorizontalSpace() / mColumnCount;
    }

    private int getRowHeight() {
        return getVerticalSpace() / mRowCount;
    }

    private int getColumnPosition(int columnIdx) {
        int lastPageIdx = getPageIdx(getItemCount() - 1);
        int maxColumns = (lastPageIdx + 1) * mColumnCount;
        if (columnIdx < 0 || columnIdx >= maxColumns) {
            Log.e(TAG, "getColumnPosition(" + columnIdx + ")" +
                " mColumnCount=" + mColumnCount +
                " mRowCount=" + mRowCount +
                " itemCount=" + getItemCount() +
                " lastPageIdx=" + lastPageIdx +
                " maxColumns=" + maxColumns);
        }
        final int columnOffset = columnIdx * mDecoratedChildWidth;
        if (mRightToLeft)
            return getWidth() - getPaddingRight() - columnOffset - mDecoratedChildWidth;
        return getPaddingLeft() + columnOffset;
    }

    private int getRowPosition(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mRowCount) {
            Log.e(TAG, "getRowPosition(" + rowIdx + "); mRowCount=" + mRowCount);
        }
        return getPaddingTop() + rowIdx * mDecoratedChildHeight;
    }

    private int getPagePosition(int pageIdx) {
        final int pageWidth = mColumnCount * mDecoratedChildWidth;
        final int pageOffset = pageIdx * pageWidth;
        if (mRightToLeft)
            return getWidth() - getPaddingRight() - pageOffset - pageWidth;
        return getPaddingLeft() + pageOffset;
    }

    private void layoutChildren(RecyclerView.Recycler recycler) {
        /*
         * Detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        cacheChildren();

        // if scrollToPosition is valid, force-scroll to that page
        if (mScrollToAdapterPosition >= 0 && mScrollToAdapterPosition < getItemCount()) {
            int pageIdx = getPageIdx(mScrollToAdapterPosition);
            int pagePosition = getPagePosition(pageIdx);
            // force-scroll the child views
            int dx = -pagePosition - mScrollOffsetHorizontal;
            logDebug("scrollToPosition " + mScrollToAdapterPosition +
                " pageIdx=" + pageIdx +
                " pagePos=" + pagePosition +
                " dx=" + dx);
            offsetChildrenHorizontal(dx);
            mScrollOffsetHorizontal += dx;
        }
        // turn off scrollToPosition
        mScrollToAdapterPosition = -1;

        // compute scroll position after we populate `mViewCache`
        final int scrollOffset = mScrollOffsetHorizontal;
        final int firstVisiblePos = findFirstVisibleAdapterPosition();

        logDebug("layoutChildren" +
            " scrollOffset=" + scrollOffset +
            " firstVisiblePos=" + firstVisiblePos +
            " padding=" + getPaddingLeft() + " " + getPaddingTop() + " " + getPaddingRight() + " " + getPaddingBottom() +
            " verticalSpace=" + getVerticalSpace() +
            " horizontalSpace=" + getHorizontalSpace());

        if (mRefreshViews) {
            logDebug("detachAndScrapAttachedViews" +
                " viewCache.size=" + mViewCache.size());
            // If we want to refresh all views, just scrap them and let the recycler rebind them
            mRefreshViews = false;
            mViewCache.clear();
            detachAndScrapAttachedViews(recycler);
        } else {
            logDebug("detachViews" +
                " viewCache.size=" + mViewCache.size());
            // Temporarily detach all views. We do this to easily move them.
            detachCachedChildren(recycler);
        }

        final int nextLeftPosDelta = mRightToLeft ? -mDecoratedChildWidth : mDecoratedChildWidth;

        int colIdx = getColumnIdx(firstVisiblePos);
        int rowIdx = getRowIdx(firstVisiblePos);

        int topPos = getRowPosition(rowIdx);
        int scrolledPosition = scrollOffset + getColumnPosition(colIdx);
        while (scrolledPosition < getWidth() && (scrolledPosition + mDecoratedChildWidth) > 0) {
            int adapterIdx = getAdapterIdx(colIdx, rowIdx);
            logDebug("col=" + colIdx + " row=" + rowIdx + " adapterIdx=" + adapterIdx + " scrolledPosition=" + scrolledPosition);
            View child = layoutAdapterPos(recycler, adapterIdx, scrolledPosition, topPos);
            if (child == null) {
                logDebug("null view in" +
                    " col=" + colIdx +
                    " row=" + rowIdx);
                if (rowIdx == 0)
                    break;
            }
            rowIdx += 1;
            if (rowIdx >= mRowCount) {
                rowIdx = 0;
                colIdx += 1;
                scrolledPosition += nextLeftPosDelta;
            }
            topPos = getRowPosition(rowIdx);
        }
        clearViewCache(recycler);
    }

    /**
     * Find adapter index of the first visible item on row 0.
     * It will be the left-most if (mRightToLeft == false) and the right-most otherwise.
     *
     * @return adapter index of the first visible view
     */
    private int findFirstVisibleAdapterPosition() {
        final int colIdx;
        if (mRightToLeft)
            colIdx = mScrollOffsetHorizontal / mDecoratedChildWidth;
        else
            colIdx = -mScrollOffsetHorizontal / mDecoratedChildWidth;
        return getAdapterIdx(colIdx, 0);
    }

    /**
     * Layout view from cache or recycler
     *
     * @param recycler   the recycler
     * @param adapterPos adapter index
     * @param leftPos    layout position X
     * @param topPos     layout position Y
     * @return child view
     */
    private View layoutAdapterPos(RecyclerView.Recycler recycler, int adapterPos, int leftPos, int topPos) {
        if (adapterPos < 0 || adapterPos >= getItemCount())
            return null;
        View child = mViewCache.get(adapterPos);
        if (child == null) {
            /*
             * The Recycler will give us either a newly constructed view or a recycled view it has on-hand.
             * In either case, the view will already be fully bound to the data by the adapter for us.
             */
            child = recycler.getViewForPosition(adapterPos);
            addView(child);
            /*
             * It is prudent to measure/layout each new view we receive from the Recycler.
             * We don't have to do this for views we are just re-arranging.
             */
            measureChildWithMargins(child, getHorizontalSpace() - mDecoratedChildWidth, getVerticalSpace() - mDecoratedChildHeight);
            final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int left = leftPos + lp.leftMargin;
            final int top = topPos + lp.topMargin;
            final int measuredWidth = child.getMeasuredWidth();
            final int measuredHeight = child.getMeasuredHeight();
            //TODO: if measured size is 0 (probably MeasureSpec.UNSPECIFIED) then fix it and measure again.

            layoutDecorated(child, left, top,
                left + measuredWidth,
                top + measuredHeight);

            logDebug("child #" + indexOfChild(child) + " pos=" + adapterPos +
                " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ")" +
                " left=" + leftPos +
                " top=" + topPos +
                " measured=" + measuredWidth + "x" + measuredHeight +
                " " + getDebugInfo(child) + " " + getDebugName(child));
        } else {
            attachView(child);
            logDebug("cache #" + indexOfChild(child) + " pos=" + adapterPos +
                " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ")" +
                " left=" + leftPos +
                " top=" + topPos +
                " " + getDebugInfo(child) + " " + getDebugName(child));
            mViewCache.remove(adapterPos);
        }
        return child;
    }

    /**
     * Cache all views by their existing position
     */
    private void cacheChildren() {
        mViewCache.clear();

        int childCount = getChildCount();
        mViewCache.ensureCapacity(childCount);

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and idx=" + i);
            int position = adapterPosition(child);
            mViewCache.put(position, child);
            logDebug("info #" + i + " pos=" + position + " " + getDebugInfo(child) + " " + getDebugName(child));
        }
    }

    private void detachCachedChildren(RecyclerView.Recycler recycler) {
        for (int i = 0; i < mViewCache.size(); i++) {
            View child = mViewCache.valueAt(i);
            // When an update is in order, scrap the view and let the recycler rebind it
            if (viewNeedsUpdate(child)) {
                detachAndScrapView(child, recycler);
                mViewCache.removeAt(i--);
            } else {
                detachView(child);
            }
        }
    }

    /**
     * We ask the Recycler to scrap and store any views that we did not re-attach.
     * These are views that are not currently necessary because they are no longer visible.
     */
    private void clearViewCache(RecyclerView.Recycler recycler) {
        for (int i = 0; i < mViewCache.size(); i++) {
            final View removingView = mViewCache.valueAt(i);
            logDebug("recycleView pos=" + mViewCache.keyAt(i) + " " + getDebugName(removingView));
            recycler.recycleView(removingView);
        }
        mViewCache.clear();
    }

    private int indexOfChild(View child) {
        for (int idx = getChildCount() - 1; idx >= 0; idx -= 1) {
            if (getChildAt(idx) == child)
                return idx;
        }
        return -1;
    }

    @Override
    public boolean canScrollHorizontally() {
        if (getItemCount() > (mColumnCount * mRowCount))
            return true;
        if (mScrollOffsetHorizontal != 0)
            return true;
        if (getChildCount() > 0) {
            // Allow scrolling if child views are outside visible range
            if (getDecoratedLeft(getLeftChildView()) < getPaddingLeft())
                return true;
            return getDecoratedRight(getRightChildView()) > (getPaddingLeft() + getHorizontalSpace());
        }
        return false;
    }

    @Override
    public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        final int amount;
        // compute amount of scroll without going beyond the bound
        if (dx < 0) { // finger is moving from left to right
            if (mRightToLeft) {
                int lastPageIdx = getPageIdx(getItemCount() - 1);
                int pageWidth = getHorizontalSpace();
                int maxScroll = lastPageIdx * pageWidth;
                logDebug("dx=" + dx +
                    " lastPageIdx=" + lastPageIdx +
                    " pageWidth=" + pageWidth +
                    " maxScroll=" + maxScroll +
                    " mScroll=" + mScrollOffsetHorizontal);
                amount = Math.max(dx, mScrollOffsetHorizontal - maxScroll);
            } else {
                logDebug("dx=" + dx +
                    " mScroll=" + mScrollOffsetHorizontal);
                amount = Math.max(dx, mScrollOffsetHorizontal);
            }
        } else if (dx > 0) { // finger is moving from right to left
            if (mRightToLeft) {
                logDebug("dx=" + dx +
                    " mScroll=" + mScrollOffsetHorizontal);
                amount = Math.min(dx, mScrollOffsetHorizontal);
            } else {
                int lastPageIdx = getPageIdx(getItemCount() - 1);
                int pageWidth = getHorizontalSpace();
                int maxScroll = lastPageIdx * -pageWidth;
                logDebug("dx=" + dx +
                    " lastPageIdx=" + lastPageIdx +
                    " pageWidth=" + pageWidth +
                    " maxScroll=" + maxScroll +
                    " mScroll=" + mScrollOffsetHorizontal);
                amount = Math.min(dx, mScrollOffsetHorizontal - maxScroll);
            }
        } else {
            amount = dx;
        }

        if (dx != amount)
            logDebug("dx=" + dx + " amount=" + amount);

        if (amount == 0 || (dx < 0 && amount > 0) || (dx > 0 && amount < 0))
            return 0;

        // scroll children
        offsetChildrenHorizontal(-amount);
        mScrollOffsetHorizontal -= amount;

        // check if we need to layout after the scroll
        if (checkVisibilityAfterScroll())
            layoutChildren(recycler);

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return amount;
    }

    /**
     * Check if child views became invisible after scroll or if we need to layout more views
     *
     * @return true if we need la re-layout
     */
    private boolean checkVisibilityAfterScroll() {
        View leftChild = getLeftChildView();
        View rightChild = getRightChildView();
        int left = getDecoratedLeft(leftChild);
        int right = getDecoratedRight(rightChild);
        // check if we should remove views
        if ((left + mDecoratedChildWidth) < 0 || (right - mDecoratedChildWidth) > getWidth())
            return true;
        // check if we should add views
        return left > 0 || right < getWidth();
    }

    /**
     * Return left-most child view from row 0 (may not be visible on screen)
     *
     * @return child view
     */
    @NonNull
    private View getLeftChildView() {
        int leftChildIdx = mRightToLeft ? (getChildCount() - 1) : 0;
        View child = getChildAt(leftChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + getChildCount() + " and leftChildIdx=" + leftChildIdx);
        while (true) {
            int idx = adapterPosition(child);
            int row = getRowIdx(idx);
            if (row == 0)
                break;
            leftChildIdx += mRightToLeft ? -1 : 1;
            if (leftChildIdx < 0 || leftChildIdx >= getChildCount())
                break;
            child = getChildAt(leftChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and leftChildIdx=" + leftChildIdx);
        }
        return child;
    }

    /**
     * Return right-most child view from row 0 (may not be visible on screen)
     *
     * @return child view
     */
    @NonNull
    private View getRightChildView() {
        int rightChildIdx = mRightToLeft ? 0 : (getChildCount() - 1);
        View child = getChildAt(rightChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + getChildCount() + " and rightChildIdx=" + rightChildIdx);
        while (true) {
            int idx = adapterPosition(child);
            int row = getRowIdx(idx);
            if (row == 0)
                break;
            rightChildIdx -= mRightToLeft ? -1 : 1;
            if (rightChildIdx < 0 || rightChildIdx >= getChildCount())
                break;
            child = getChildAt(rightChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and rightChildIdx=" + rightChildIdx);
        }
        return child;
    }

    /**
     * 0 for leftmost scroll and page count for rightmost
     *
     * @return page and scroll
     */
    public float getPageScroll() {
        if (getChildCount() == 0)
            return 0f;
        View view = getChildAt(0);
        if (view == null)
            return 0f;
        int col = getColumnIdx(adapterPosition(view));
        int pageWidth = getHorizontalSpace();
        float scroll = (getDecoratedLeft(view) - getColumnPosition(col)) / (float) pageWidth;
        return mRightToLeft ? scroll : -scroll;
    }

    public int getPageAdapterPosition(int page) {
        int col = page * mColumnCount;
        return getAdapterIdx(col, 0);
    }

    @Nullable
    @Override
    public PointF computeScrollVectorForPosition(int targetPosition) {
        if (getChildCount() == 0)
            return null;
        View view = getChildAt(0);
        if (view == null)
            return null;
        int pos = adapterPosition(view);
        return new PointF(targetPosition - pos, 0f);
    }
}
