package rocks.tbog.tblauncher.quicklist;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.SparseArrayWrapper;

public class DockRecycleLayoutManager extends RecyclerView.LayoutManager {
    private static final String TAG = "DRLM";
    private static final Boolean LOG_DEBUG = true;
    // Reusable array. This should only be used used transiently and should not be used to retain any state over time.
    private final SparseArrayWrapper<View> mViewCache = new SparseArrayWrapper<>();
    private RecyclerView mRecyclerView = null;
    private boolean mRefreshViews = false;
    /* First position visible at any point (adapter index) */
    private int mFirstVisiblePosition;

    private int mColumnCount = 6;
    private int mRowCount = 1;
    private boolean mRightToLeft = false;

    public DockRecycleLayoutManager() {
        super();
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
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
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

        if (getChildCount() == 0) {
            // First or empty layout
            mFirstVisiblePosition = 0;
        }

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

    /* Example of 3 pages each with 2 rows and 3 columns
     *
     * mRightToLeft == false
     *      + - - - - - - - - - - - - - - - - +
     *      |  0  1  2 |  6  7  8 | 12 13 14  |
     *      |  3  4  5 |  9 10 11 | 15 16 17  |
     *      + - - - - - - - - - - - - - - - - +
     *
     * mRightToLeft == true
     *      + - - - - - - - - - - - - - - - - +
     *      | 14 13 12 |  8  7  6 |  2  1  0  |
     *      | 17 16 15 | 11 10  9 |  5  4  3  |
     *      + - - - - - - - - - - - - - - - - +
     */

    private int getAdapterIdx(int colIdx, int rowIdx) {
        if (mRightToLeft)
            throw new IllegalStateException("implement getAdapterIdx when mRightToLeft==true");
        int columnInPage = colIdx % mColumnCount;
        int page = colIdx / mColumnCount;
        return page * mColumnCount * mRowCount + rowIdx * mColumnCount + columnInPage;
    }

    private int getColumnIdx(int adapterPos) {
        if (mRightToLeft)
            throw new IllegalStateException("implement getColumnIdx when mRightToLeft==true");
        int columnInPage = adapterPos % (mColumnCount * mRowCount) % mColumnCount;
        int page = getPageIdx(adapterPos);
        return page * mColumnCount + columnInPage;
    }

    private int getRowIdx(int adapterPos) {
        if (mRightToLeft)
            throw new IllegalStateException("implement getRowIdx when mRightToLeft==true");
        int group = adapterPos / mColumnCount;
        return group % mRowCount;
    }

    private int getPageIdx(int adapterPos) {
        if (mRightToLeft)
            throw new IllegalStateException("implement getPageIdx when mRightToLeft==true");
        return adapterPos / (mColumnCount * mRowCount);
    }

    private int getColumnWidth() {
        return getHorizontalSpace() / mColumnCount;
    }

    private int getRowHeight() {
        return getVerticalSpace() / mRowCount;
    }

    private int getColumnPosition(int columnIdx) {
        if (columnIdx < 0 || columnIdx >= mColumnCount) {
            Log.e(TAG, "getColumnPosition(" + columnIdx + "); mColumnCount=" + mColumnCount);
        }
        return getPaddingLeft() + columnIdx * getColumnWidth();
    }

    private int getRowPosition(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mRowCount) {
            Log.e(TAG, "getRowPosition(" + rowIdx + "); mRowCount=" + mRowCount);
        }
        return getPaddingTop() + rowIdx * getRowHeight();
    }

    /**
     * Compute horizontal scroll offset based on mViewCache
     *
     * @return difference between child left position and initial layout position
     */
    private int computeHorizontalScrollOffset() {
        if (mViewCache.size() > 0) {
            int adapterPos = mViewCache.keyAt(0);
            int colIdx = getColumnIdx(adapterPos);
            int colPosition = getColumnPosition(colIdx);
            View child = mViewCache.valueAt(0);
            return getDecoratedLeft(child) - colPosition;
        }
        return 0;
    }

    private void layoutChildren(RecyclerView.Recycler recycler) {
        /*
         * Detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        cacheChildren();

        // compute scroll position after we populate `mViewCache`
        final int scrollOffset = computeHorizontalScrollOffset();

        logDebug("layoutChildren" +
            " mFirstVisiblePosition=" + mFirstVisiblePosition +
            " scrollOffset=" + scrollOffset +
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

        final int nextLeftPosDelta = mRightToLeft ? -getColumnWidth() : getColumnWidth();

        int visibleAdapterPos = mFirstVisiblePosition;
        int colIdx = getColumnIdx(visibleAdapterPos);
        int rowIdx = getRowIdx(visibleAdapterPos);

        int topPos = getRowPosition(rowIdx);
        int scrolledPosition = scrollOffset + getColumnPosition(colIdx);
        while (scrolledPosition < getWidth()) {
            int adapterIdx = getAdapterIdx(colIdx, rowIdx);
            //logDebug("col=" + colIdx + " row=" + rowIdx + " adapterIdx=" + adapterIdx);
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
            measureChildWithMargins(child, 0, 0);

            layoutChildView(child, leftPos, topPos);

            logDebug("child #" + indexOfChild(child) + " pos=" + adapterPos +
                " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ")" +
                " " + getDebugInfo(child) + " " + getDebugName(child));
        } else {
            attachView(child);
            logDebug("cache #" + indexOfChild(child) + " pos=" + adapterPos +
                " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ")" +
                " top=" + topPos +
                " " + getDebugInfo(child) + " " + getDebugName(child));
            mViewCache.remove(adapterPos);
        }
        return child;
    }

    private void layoutChildView(View view, int left, int top) {
        layoutChildView(view, left, top, getColumnWidth(), getRowHeight());
    }

    private void layoutChildView(View view, int left, int top, int width, int height) {
        layoutDecorated(view, left, top,
            left + width,
            top + height);
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
        if (getChildCount() > 0) {
            //We do allow scrolling
            if (getDecoratedLeft(getLeftView()) < getPaddingLeft())
                return true;
            return getDecoratedRight(getRightView()) > (getPaddingLeft() + getHorizontalSpace());
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
        if (dx < 0) { // finger is moving from right to left
            View leftView = getLeftView();
            boolean leftBoundReached = adapterPosition(leftView) == leftAdapterItemIdx();
            final int leftBound = getPaddingLeft();
            final int childLeft = getDecoratedLeft(leftView);
            if (leftBoundReached && (childLeft - dx) > leftBound) {
                //If top bound reached, enforce limit
                int topOffset = leftBound - childLeft;

                amount = -Math.min(-dx, topOffset);
            } else {
                amount = dx;
            }
        } else if (dx > 0) { // finger is moving from left to right
            View rightView = getRightView();
            boolean rightBoundReached = adapterPosition(rightView) == rightAdapterItemIdx();
            final int rightBound = getWidth() - getPaddingRight();
            final int childRight = getDecoratedRight(rightView);
            if (rightBoundReached && (childRight - dx) < rightBound) {
                //If we've reached the last row, enforce limits
                int rightOffset = childRight - rightBound;

                amount = Math.min(dx, rightOffset);
            } else {
                amount = dx;
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

        // check if we need to layout after the scroll
        checkVisibilityAfterScroll(recycler, true);
        checkVisibilityAfterScroll(recycler, false);

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return amount;
    }

    private void checkVisibilityAfterScroll(RecyclerView.Recycler recycler, boolean checkLeft) {
        View child = checkLeft ? getLeftView() : getRightView();
        int adapterPosition = adapterPosition(child);
        int left = getDecoratedLeft(child);
        int newFirstVisible = mFirstVisiblePosition;
        while (needsVisibilityChange(adapterPosition, left, checkLeft)) {
            if (checkLeft) {
                adapterPosition = leftAdapterItemIdx(adapterPosition);
                newFirstVisible = leftAdapterItemIdx(newFirstVisible);
                left -= getColumnWidth();
            } else {
                left += getColumnWidth();
                adapterPosition = rightAdapterItemIdx(adapterPosition);
                newFirstVisible = rightAdapterItemIdx(newFirstVisible);
            }
        }
        changeFirstVisible(recycler, newFirstVisible);
    }

    private void changeFirstVisible(RecyclerView.Recycler recycler, int value) {
        int oldValue = mFirstVisiblePosition;
        int newValue = Math.max(Math.min(value, getItemCount() - 1), 0);
        if (oldValue != newValue) {
            logDebug("mFirstVisiblePosition changed from " + oldValue + " to " + newValue);
            mFirstVisiblePosition = newValue;
            layoutChildren(recycler);
        }
    }

    /**
     * @param adapterPosition needed to stop checking if adapter start or end reached
     * @param left            child decorated left
     * @param checkLeft       `bound` is the left position or the right
     * @return if we need to change `mFirstVisiblePosition`
     */
    private boolean needsVisibilityChange(int adapterPosition, int left, boolean checkLeft) {
        if (adapterPosition <= 0 || adapterPosition >= (getItemCount() - 1))
            return false;
        if (checkLeft)
            return left > 0;
        return (left + getColumnWidth()) < getWidth();
    }

    /**
     * Return left child view on screen (may not be visible)
     *
     * @return child view
     */
    @NonNull
    private View getLeftView() {
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
            child = getChildAt(leftChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and leftChildIdx=" + leftChildIdx);
        }
        return child;
    }

    /**
     * Return right child view on screen (may not be visible)
     *
     * @return child view
     */
    @NonNull
    private View getRightView() {
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
            child = getChildAt(rightChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and rightChildIdx=" + rightChildIdx);
        }
        return child;
    }

    /**
     * First (or last) item from the adapter that can be displayed at the left of the list
     *
     * @return index from adapter
     */
    private int leftAdapterItemIdx() {
        int idx = mRightToLeft ? (getItemCount() - 1) : 0;
        int row = getRowIdx(idx);
        if (row == 0)
            return idx;
        return getAdapterIdx(0, 0);
    }

    /**
     * First (or last) item from the adapter that can be displayed at the right of the list
     *
     * @return index from adapter
     */
    private int rightAdapterItemIdx() {
        int idx = mRightToLeft ? 0 : (getItemCount() - 1);
        int row = getRowIdx(idx);
        if (row == 0)
            return idx;
        int page = getPageIdx(idx);
        return getAdapterIdx((page + 1) * mColumnCount - 1, 0);
    }

    private int leftAdapterItemIdx(int idx) {
        int col = getColumnIdx(idx);
        int rightIdx = getAdapterIdx(col - 1, 0);
        if (rightIdx < 0) {
            return 0;
        } else if (rightIdx >= getItemCount()) {
            return getItemCount() - 1;
        }
        return rightIdx;
    }

    private int rightAdapterItemIdx(int idx) {
        int col = getColumnIdx(idx);
        int rightIdx = getAdapterIdx(col + 1, 0);
        if (rightIdx < 0) {
            return 0;
        } else if (rightIdx >= getItemCount()) {
            return getItemCount() - 1;
        }
        return rightIdx;
    }
}
