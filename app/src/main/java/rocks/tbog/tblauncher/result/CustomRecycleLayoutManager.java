package rocks.tbog.tblauncher.result;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearSmoothScroller;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.SparseArrayWrapper;

public class CustomRecycleLayoutManager extends RecyclerView.LayoutManager implements ReversibleAdapterRecyclerLayoutManager {

    private static final String TAG = "CRLM";
    private static final Boolean LOG_DEBUG = false;

    private RecyclerView mRecyclerView = null;

    /* First position visible at any point (adapter index) */
    private int mFirstVisiblePosition;
    /* Number of items to show on a row */
    private int mColCount = 1;
    /* Number of visible rows. It is computed when the size changes but may also increase when layout occurs */
    private int mVisibleRows;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth = 0;

    private final ArrayList<RowInfo> mRowInfo = new ArrayList<>(0);

    /* Used for starting the layout from the bottom. If true the first item is places at the bottom */
    private boolean mBottomToTop;
    /* Used for starting the layout from the right. If true the first item of each row is places at the right */
    private boolean mRightToLeft;
    /* Used for reversing adapter order */
    private boolean mReverseAdapter;
    /* Compute column count based on available space */
    private boolean mAutoColumn = false;
    /* When list height grows, keep bottom view at the bottom */
    private boolean mAutoScrollBottom = true;
    /* Keep track of the amount of over-scroll */
    private int mOverScrollVertical = 0;
    private OverScrollListener mOverScrollListener = null;

    private boolean mRefreshViews = false;

    // Reusable array. This should only be used used transiently and should not be used to retain any state over time.
    private final SparseArrayWrapper<View> mViewCache = new SparseArrayWrapper<>();

    public interface OverScrollListener {
        void onOverScroll(RecyclerView recyclerView, int amount);
    }

    private static class RowInfo {
        /* row alignment offset. When layout is bottom to top the first row has pos==-height */
        int pos = 0;
        /* height of this row */
        int height = 0;
    }

    public CustomRecycleLayoutManager() {
        this(false, false, false);
    }

    public CustomRecycleLayoutManager(boolean firstAtBottom, boolean rightToLeft, boolean reverseAdapter) {
        super();
        mBottomToTop = firstAtBottom;
        mRightToLeft = rightToLeft;
        mReverseAdapter = reverseAdapter;
    }

    public void setBottomToTop(boolean bottomToTop) {
        assertNotInLayoutOrScroll(null);
        if (mBottomToTop == bottomToTop) {
            return;
        }
        mBottomToTop = bottomToTop;
        requestLayout();
    }

    public boolean isBottomToTop() {
        return mBottomToTop;
    }

    public void setRightToLeft(boolean rightToLeft) {
        assertNotInLayoutOrScroll(null);
        if (mRightToLeft == rightToLeft) {
            return;
        }
        mRightToLeft = rightToLeft;
        requestLayout();
    }

    public boolean isRightToLeft() {
        return mRightToLeft;
    }

    @Override
    public void setReverseAdapter(boolean reverseAdapter) {
        assertNotInLayoutOrScroll(null);
        if (mReverseAdapter == reverseAdapter) {
            return;
        }
        mReverseAdapter = reverseAdapter;
        requestLayout();
    }

    @Override
    public boolean isReverseAdapter() {
        return mReverseAdapter;
    }

    /**
     * Set column parameters
     *
     * @param columnCount number of columns
     * @param autoFill    if true the column count will be recomputed to fit the width
     */
    public void setColumns(int columnCount, boolean autoFill) {
        assertNotInLayoutOrScroll(null);
        mColCount = columnCount <= 0 ? 1 : columnCount;
        mAutoColumn = autoFill;

        // make sure we are using recycled views
        mRefreshViews = true;

        // reset row info
        mRowInfo.clear();

        requestLayout();
    }

    public int getColumnCount() {
        return mColCount;
    }

    public void setAutoScrollBottom(boolean autoScrollBottom) {
        mAutoScrollBottom = autoScrollBottom;
    }

    public void setOverScrollListener(@Nullable OverScrollListener listener) {
        mOverScrollListener = listener;
    }


    @Override
    public int computeVerticalScrollExtent(@NonNull RecyclerView.State state) {
        return getVerticalSpace();
    }

    @Override
    public int computeVerticalScrollOffset(@NonNull RecyclerView.State state) {
        if (getChildCount() <= 1 || mRowInfo.isEmpty())
            return 0;

        int offset;
        View child = getChildAt(0);
        if (child != null) {
            int rowPosition = getRowPosition(getRowIdx(adapterPosition(child)));
            rowPosition -= getPaddingTop();

            int childTop = getDecoratedTop(child);
            childTop -= getPaddingTop();

            offset = rowPosition - childTop;

            if (mBottomToTop) {
                // align to bottom
                offset = -mRowInfo.get(mRowInfo.size() - 1).pos + offset - getVerticalSpace();
            }
        } else {
            final View view = findBottomVisibleItemView();
            final int topRow = getRowIdx(topAdapterItemIdx());
            final int bottomRow = getRowIdx(adapterPosition(view));

            final int topRowPos = mRowInfo.get(topRow).pos;
            final int bottomRowPos = mRowInfo.get(bottomRow).pos;

            int topRowsHeight = bottomRowPos - topRowPos;
            topRowsHeight += getRowHeight(bottomRow);

            offset = getPaddingTop() - getDecoratedBottom(view) + topRowsHeight;
        }

        return offset;
    }

    @Override
    public int computeVerticalScrollRange(@NonNull RecyclerView.State state) {
        if (!mRowInfo.isEmpty()) {
            RowInfo rowInfo = mRowInfo.get(mRowInfo.size() - 1);
            return mBottomToTop ? -rowInfo.pos : (rowInfo.pos + rowInfo.height);
        }
        return 0;
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
        mRowInfo.clear();
    }

    /**
     * Called in response to a call to {@link RecyclerView.Adapter#notifyDataSetChanged()} or
     * {@link RecyclerView#swapAdapter(RecyclerView.Adapter, boolean)} ()} and signals that the the entire
     * data set has changed.
     *
     * @param recyclerView not used
     */
    @Override
    public void onItemsChanged(@NonNull RecyclerView recyclerView) {
        mRefreshViews = true;
        if (mFirstVisiblePosition > getItemCount()) {
            int oldValue = mFirstVisiblePosition;
            int newValue = mBottomToTop ? bottomAdapterItemIdx() : topAdapterItemIdx();
            logDebug("onItemsChanged mFirstVisiblePosition changed from " + oldValue + " to " + newValue);
            mFirstVisiblePosition = newValue;
        } else {
            logDebug("onItemsChanged");
        }
        if (mRowInfo.size() != computeRowCount(getItemCount()))
            mRowInfo.clear();
    }

    @Override
    public void onItemsUpdated(@NonNull RecyclerView recyclerView, int positionStart, int itemCount) {
        logDebug("onItemsUpdated start=" + positionStart + " count=" + itemCount);
        mRefreshViews = true;
    }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        logDebug("onAdapterChanged");
        mRefreshViews = true;
        mRowInfo.clear();
    }

    @Override
    public void scrollToPosition(int adapterPos) {
        if (adapterPos < 0 || adapterPos >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + adapterPos + ", item count " + getItemCount());
            return;
        }
        int oldValue = mFirstVisiblePosition;
        int newValue = adapterPos / mColCount * mColCount;
        if (oldValue != newValue) {
            logDebug("scrollToPosition mFirstVisiblePosition changed from " + oldValue + " to " + newValue);
            mFirstVisiblePosition = newValue;
        }
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
            mFirstVisiblePosition = mBottomToTop ? bottomAdapterItemIdx() : topAdapterItemIdx();
        }

        if (getChildCount() == 0 || mRowInfo.isEmpty()) {
            final int decoratedChildHeight;
            if (getChildCount() == 0) {
                // Scrap measure one child
                View scrap = recycler.getViewForPosition(mFirstVisiblePosition);
                addView(scrap);
                measureChildWithMargins(scrap, 0, 0);
                mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
                decoratedChildHeight = getDecoratedMeasuredHeight(scrap);

                detachAndScrapView(scrap, recycler);
            } else {
                View child = getChildAt(0);
                if (child == null)
                    throw new IllegalStateException("null child at idx 0 when childCount=" + getChildCount());

                mDecoratedChildWidth = getDecoratedMeasuredWidth(child);
                decoratedChildHeight = getDecoratedMeasuredHeight(child);
            }

            updateRowInfo(decoratedChildHeight);
        }

        // Always update the visible row/column counts
        updateSizing();

        logDebug("onLayoutChildren" +
                 " childCount=" + getChildCount() +
                 " itemCount=" + getItemCount() +
                 " columns=" + mColCount +
                 " mDecoratedChildWidth=" + mDecoratedChildWidth +
                 (state.isPreLayout() ? " preLayout" : "") +
                 (state.didStructureChange() ? " structureChanged" : "") +
                 " stateItemCount=" + state.getItemCount());

        layoutChildren(recycler);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);

        if (getChildCount() > 0) {
            // check if this was a resize that requires a forced scroll
            if (mBottomToTop && mAutoScrollBottom) {
                View bottomView = getBottomView();
                int childHeight = getDecoratedMeasuredHeight(bottomView);
                int bottomDelta = getPaddingTop() + getVerticalSpace() - getDecoratedBottom(bottomView);
                if (bottomDelta > 0) {
                    // the last view is too high
                    offsetChildrenVertical(bottomDelta);
                    logDebug("(1) auto-scroll (" + getDebugName(bottomView) + ") bottom amount=" + bottomDelta);
                } else if (bottomDelta > (-childHeight * 3 / 5) && bottomAdapterItemIdx() == mFirstVisiblePosition) {
                    // the first visible item (at the bottom) is hidden by a small amount, scroll it into view smoothly
                    //TODO: detect if this occurred because of a user scroll so we can skip this if so
                    logDebug("(2) smooth auto-scroll (" + getDebugName(bottomView) + ") bottom amount=" + bottomDelta);

                    LinearSmoothScroller smoothScroller = new LinearSmoothScroller(bottomView.getContext());
                    smoothScroller.setTargetPosition(mFirstVisiblePosition);
                    startSmoothScroll(smoothScroller);
                } else if (bottomAdapterItemIdx() == mFirstVisiblePosition) {
                    // list got resized and the first visible item (at the bottom) is now hidden
                    offsetChildrenVertical(bottomDelta);
                    logDebug("(3) auto-scroll (" + getDebugName(bottomView) + ") bottom amount=" + bottomDelta);
                } else {
                    logDebug("(4) no auto-scroll (" + getDebugName(bottomView) + ") bottom amount=" + bottomDelta);
                }
            }
        }
    }

    /**
     * Compute scroll offset based on mViewCache
     *
     * @return difference between child top position and initial layout position
     */
    private int computeScrollOffset() {
        if (mViewCache.size() > 0) {
            int adapterPos = mViewCache.keyAt(0);
            int rowIdx = getRowIdx(adapterPos);
            int rowPosition = getRowPosition(rowIdx);
            View child = mViewCache.valueAt(0);
            return getDecoratedTop(child) - rowPosition;
        }
        return 0;
    }

    private int computeRowCount(int itemCount) {
        return itemCount / mColCount + (itemCount % mColCount == 0 ? 0 : 1);
    }

    private class LayoutRowHelper {
        public final RecyclerView.Recycler recycler;
        @NonNull
        public final View[] viewRow;
        public int mDecoratedHeight;
        public int mDecoratedTop;
        public int mDecoratedBottom;
        public int nextLeftPosDelta;

        private LayoutRowHelper(RecyclerView.Recycler recycler, int columnCount) {
            this.recycler = recycler;
            viewRow = new View[columnCount];
        }

        public void setPositionIncrement(int nextLeft) {
            nextLeftPosDelta = nextLeft;
        }

        public void layoutRow(int rowIdx, int posX, int posY) {
            int columnCount = viewRow.length;
            mDecoratedHeight = 0;
            mDecoratedTop = Integer.MAX_VALUE;
            mDecoratedBottom = Integer.MIN_VALUE;

            for (int colIdx = 0; colIdx < columnCount; colIdx += 1) {
                final int leftPos = posX + nextLeftPosDelta * colIdx;
                final int adapterPos = adapterPosition(colIdx, rowIdx);

                View child = layoutView(adapterPos, leftPos, posY);

                // child may be null if row is incomplete
                viewRow[colIdx] = child;
                if (child == null)
                    continue;

                mDecoratedTop = Math.min(mDecoratedTop, CustomRecycleLayoutManager.this.getDecoratedTop(child));
                mDecoratedBottom = Math.max(mDecoratedBottom, CustomRecycleLayoutManager.this.getDecoratedBottom(child));

                // update row height
                int measuredHeight = getDecoratedMeasuredHeight(child);
                mDecoratedHeight = Math.max(mDecoratedHeight, measuredHeight);
            }

            int heightDelta = posY - getDecoratedTop();
            if (heightDelta != 0) {
                offsetVertical(heightDelta);
            }

            // make all views in this row the same height
            for (View child : viewRow) {
                if (child != null) {
                    // set bottom; we expect the top to not change
                    child.setBottom(mDecoratedBottom - getBottomDecorationHeight(child));
                }
            }
        }

        @Nullable
        private View layoutView(int adapterPos, int leftPos, int topPos) {
            if (adapterPos < 0 || adapterPos >= getItemCount())
                return null;
            return layoutAdapterPos(recycler, adapterPos, leftPos, topPos);
        }

        public void offsetVertical(int offsetY) {
            for (View child : viewRow) {
                if (child == null)
                    continue;
                child.offsetTopAndBottom(offsetY);

                int childIdx = indexOfChild(child);
                int position = adapterPosition(child);
                logDebug("move #" + childIdx + " pos=" + position +
                         " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ")" +
                         " " + getDebugInfo(child) + " " + getDebugName(child));
            }
            mDecoratedTop += offsetY;
            mDecoratedBottom += offsetY;
        }

        public void initRow() {
            Arrays.fill(viewRow, null);
        }

        public int getDecoratedHeight() {
            return mDecoratedHeight;
        }

        public int getDecoratedTop() {
            return mDecoratedTop;
        }

        public int getDecoratedBottom() {
            return mDecoratedBottom;
        }
    }

    private int getRowPosition(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mRowInfo.size()) {
            Log.e(TAG, "getRowPosition(" + rowIdx + "); rowInfo.size=" + mRowInfo.size());
            return 0;
        }

        int pos = getPaddingTop();
        if (mBottomToTop)
            pos += getVerticalSpace();
        pos += mRowInfo.get(rowIdx).pos;
        return pos;
    }

    private int getRowHeight(int rowIdx) {
        if (rowIdx < 0 || rowIdx >= mRowInfo.size()) {
            Log.e(TAG, "getRowHeight(" + rowIdx + "); rowInfo.size=" + mRowInfo.size());
            return 0;
        }
        return mRowInfo.get(rowIdx).height;
    }

    private void layoutChildren(RecyclerView.Recycler recycler) {
        /*
         * Detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        cacheChildren();

        // compute scroll position after we populate `mViewCache`
        final int scrollOffset = computeScrollOffset();

        logDebug("layoutChildren" +
                 " mFirstVisiblePosition=" + mFirstVisiblePosition +
                 " scrollOffset=" + scrollOffset +
                 " paddingTop=" + getPaddingTop() +
                 " paddingBottom=" + getPaddingBottom() +
                 " verticalSpace=" + getVerticalSpace());

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
            // Temporarily detach all views. We do this to easily reorder them.
            detachCachedChildren(recycler);
        }

        final int posX = getPaddingLeft() + (mRightToLeft ? (getHorizontalSpace() - mDecoratedChildWidth) : 0);
        final int nextLeftPosDelta = mRightToLeft ? -mDecoratedChildWidth : mDecoratedChildWidth;
        LayoutRowHelper rowHelper = new LayoutRowHelper(recycler, mColCount);
        rowHelper.setPositionIncrement(nextLeftPosDelta);

        //TODO: start laying out children from the ones we have in cache;
        // this way there is less chance of moving already cached children

        //TODO: if (mBottomToTop==true) and we scroll up (thumb goes up) the rows should
        // update in reverse (starting from current row to 0)

        // layout rows
        int visibleRowIdx = 0;
        while (visibleRowIdx < mVisibleRows) {
            final int adapterPos = adapterPosition(visibleRowIdx * mColCount);
            if (adapterPos < 0 || adapterPos >= getItemCount()) {
                Log.w(TAG, "! visibleRowIdx=" + visibleRowIdx + " missing adapter pos=" + adapterPos + " itemCount=" + getItemCount() + " mVisibleRows=" + mVisibleRows);
                visibleRowIdx += 1;
                continue;
            }
            rowHelper.initRow();
            final int rowIdx = getRowIdx(adapterPos);
            final int rowHeight = getRowHeight(rowIdx);
            final int rowPosition = getRowPosition(rowIdx);
            final int scrolledRowPosition = scrollOffset + rowPosition;

            logDebug("row #" + rowIdx +
                     " pos=" + adapterPos +
                     " before layout" +
                     " rowHeight=" + rowHeight +
                     " rowPosition=" + rowPosition +
                     " scrolledPos=" + scrolledRowPosition);

            rowHelper.layoutRow(rowIdx, posX, scrolledRowPosition);

            if (rowHeight != rowHelper.getDecoratedHeight()) {
                updateRowHeight(rowIdx, rowHelper.getDecoratedHeight());
                if (mBottomToTop) // same as (rowPosition != getRowPosition(rowIdx))
                    rowHelper.offsetVertical(rowHeight - rowHelper.getDecoratedHeight());
            }

            visibleRowIdx += 1;

            // if this is the last visible row, check if we could show more
            if (visibleRowIdx == mVisibleRows) {
                boolean needsMoreRows = mBottomToTop ? (rowHelper.getDecoratedTop() > 0) : (rowHelper.getDecoratedBottom() < getHeight());
                needsMoreRows = needsMoreRows && (visibleRowIdx * mColCount < getItemCount());
                if (needsMoreRows) {
                    // force-show more views
                    mVisibleRows += 1;
                }
            }
        }

        clearViewCache(recycler);
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
     * Layout view from cache or recycler
     *
     * @param recycler   the recycler
     * @param adapterPos adapter index
     * @param leftPos    layout position X
     * @param topPos     layout position Y
     * @return child view
     */
    private View layoutAdapterPos(RecyclerView.Recycler recycler, int adapterPos, int leftPos, int topPos) {
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
            measureChildWithMargins(child, (mColCount - 1) * mDecoratedChildWidth, 0);

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
        layoutChildView(view, left, top, mDecoratedChildWidth, getDecoratedMeasuredHeight(view));
    }

    private void layoutChildView(View view, int left, int top, int width, int height) {
        layoutDecorated(view, left, top,
        left + width,
        top + height);
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

    /*
     * Rather than continuously checking how many views we can fit
     * based on scroll offsets, we simplify the math by computing the
     * visible grid as what will initially fit on screen, plus one.
     */
    private void updateSizing() {
        int visibleWidth = getHorizontalSpace();
        int visibleCols;
        if (mAutoColumn)
            visibleCols = visibleWidth / mDecoratedChildWidth;
        else
            visibleCols = mColCount;
        if (visibleCols <= 0)
            visibleCols = 1;

        int averageChildHeight;
        if (mBottomToTop) {
            averageChildHeight = -mRowInfo.get(mRowInfo.size() - 1).pos;
        } else {
            RowInfo rowInfo = mRowInfo.get(mRowInfo.size() - 1);
            averageChildHeight = rowInfo.pos + rowInfo.height;
        }
        averageChildHeight /= mRowInfo.size();

        // we use getHeight instead of `getVerticalSpace` because we can scroll vertically
        int visibleHeight = getHeight();
        int visibleRows = (visibleHeight / averageChildHeight) + 1;
        if (visibleHeight % averageChildHeight > 0) {
            visibleRows++;
        }

        if (mColCount != visibleCols) {
            mColCount = visibleCols;
            int rowHeight = getRowHeight(0);
            updateRowInfo(rowHeight);
        }
        mVisibleRows = computeRowCount(Math.min(visibleRows * visibleCols, getItemCount()));
        mDecoratedChildWidth = visibleWidth / visibleCols;
    }

    /**
     * Initialize all rows with valid information. This should be called every time the number of
     * rows or columns changes.
     *
     * @param expectedRowHeight initial height. Row information will be updated during the layout
     *                          phase. See `updateRowHeight`
     */
    private void updateRowInfo(int expectedRowHeight) {
        final int rowCount = computeRowCount(getItemCount());
        mRowInfo.clear();
        mRowInfo.ensureCapacity(rowCount);
        logDebug("updateRowInfo" +
                 " height=" + expectedRowHeight +
                 " rowCount=" + rowCount);

        for (int rowIdx = 0; rowIdx < rowCount; rowIdx++) {
            RowInfo rowInfo = new RowInfo();
            rowInfo.height = expectedRowHeight;
            if (mBottomToTop)
                rowInfo.pos = -expectedRowHeight * (rowIdx + 1);
            else
                rowInfo.pos = expectedRowHeight * rowIdx;
            mRowInfo.add(rowInfo);
        }
    }

    /**
     * This is called during layout when a row height needs adjusting. All rows after the current
     * one will have their position updated. In bottom to top layout the current row position will
     * also be updated.
     *
     * @param rowIdx    row that needs to be adjusted
     * @param newHeight new height of the row
     */
    private void updateRowHeight(int rowIdx, int newHeight) {
        if (rowIdx < 0 || rowIdx >= mRowInfo.size()) {
            Log.e(TAG, "updateRowHeight(" + rowIdx + ")" +
                       " rowInfo.size=" + mRowInfo.size());
            return;
        }
        RowInfo rowInfo = mRowInfo.get(rowIdx);

        final int oldPos = rowInfo.pos;
        final int oldHeight = rowInfo.height;

        if (mBottomToTop)
            rowInfo.pos -= newHeight - rowInfo.height;
        rowInfo.height = newHeight;

        logDebug("updateRowHeight" +
                 " row #" + rowIdx +
                 " posY changed from " + oldPos + " to " + rowInfo.pos +
                 " height changed from " + oldHeight + " to " + rowInfo.height);

        for (int row = rowIdx + 1, rowCount = mRowInfo.size(); row < rowCount; row += 1) {
            RowInfo rowIt = mRowInfo.get(row);
            if (mBottomToTop)
                rowIt.pos = rowInfo.pos - rowIt.height;
            else
                rowIt.pos = rowInfo.pos + rowInfo.height;

            rowInfo = rowIt;
        }
    }

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the vertical direction.
     */
    @Override
    public boolean canScrollVertically() {
        if (getChildCount() > 0) {
            //We do allow scrolling
            if (getDecoratedTop(getTopView()) < getPaddingTop())
                return true;
            return getDecoratedBottom(getBottomView()) > (getPaddingTop() + getVerticalSpace());
        }
        return false;
    }

    private int indexOfChild(View child) {
        for (int idx = getChildCount() - 1; idx >= 0; idx -= 1) {
            if (getChildAt(idx) == child)
                return idx;
        }
        return -1;
    }

    private int adapterPosition(@NonNull View child) {
        return ((RecyclerView.LayoutParams) child.getLayoutParams()).getViewLayoutPosition();
    }

    private int adapterPosition(int childIdx) {
        int idx = childIdx;
        if (mReverseAdapter)
            idx = -idx;
        return mFirstVisiblePosition + idx;
    }

    private int adapterPosition(int colIdx, int rowIdx) {
        int idx = rowIdx * mColCount + colIdx;
        if (mReverseAdapter)
            idx = getItemCount() - 1 - idx;
        return idx;
    }

    private int getRowIdx(int adapterPos) {
        if (mReverseAdapter)
            return (getItemCount() - 1 - adapterPos) / mColCount;
        return adapterPos / mColCount;
    }

    /**
     * Return top child view on screen (may not be visible)
     *
     * @return child view
     */
    @NonNull
    private View getTopView() {
        final int topChildIdx = mBottomToTop ? (getChildCount() - 1) : 0;
        View child = getChildAt(topChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + getChildCount() + " and topChildIdx=" + topChildIdx);
        return child;
    }

    /**
     * Return bottom child view on screen (may not be visible)
     *
     * @return child view
     */
    @NonNull
    private View getBottomView() {
        final int bottomChildIdx = mBottomToTop ? 0 : (getChildCount() - 1);
        View child = getChildAt(bottomChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + getChildCount() + " and bottomChildIdx=" + bottomChildIdx);
        return child;
    }

    @NonNull
    private View findBottomVisibleItemView() {
        final int childCount = getChildCount();
        int botChildIdx = mBottomToTop ? 0 : (childCount - 1);
        View child = getChildAt(botChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        while (getDecoratedTop(child) > getHeight()) {
            botChildIdx += mBottomToTop ? 1 : -1;
            if (botChildIdx < 0 || botChildIdx >= childCount)
                return child;
            child = getChildAt(botChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        }
        return child;
    }

    public int findLastVisibleItemPosition() {
        final int childCount = getChildCount();
        int botChildIdx = mBottomToTop ? 0 : (childCount - 1);
        if (botChildIdx < 0 || botChildIdx >= childCount)
            return -1;
        View child = getChildAt(botChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        while (getDecoratedTop(child) > getHeight()) {
            botChildIdx += mBottomToTop ? 1 : -1;
            if (botChildIdx < 0 || botChildIdx >= childCount)
                return adapterPosition(child);
            child = getChildAt(botChildIdx);
            if (child == null)
                throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        }
        return adapterPosition(child);
    }

    /**
     * First (or last) item from the adapter that can be displayed at the top of the list
     *
     * @return index from adapter
     */
    private int topAdapterItemIdx() {
        return (mBottomToTop ^ mReverseAdapter) ? (getItemCount() - 1) : 0;
    }

    /**
     * First (or last) item from the adapter that can be displayed at the bottom of the list
     *
     * @return index from adapter
     */
    private int bottomAdapterItemIdx() {
        return (mBottomToTop ^ mReverseAdapter) ? 0 : (getItemCount() - 1);
    }

    private int aboveAdapterItemIdx(int idx) {
        return idx - belowAdapterItemIdx(0);
    }

    private int belowAdapterItemIdx(int idx) {
        return idx + ((mBottomToTop ^ mReverseAdapter) ? -mColCount : mColCount);
    }

    /*
     * This method describes how far RecyclerView thinks the contents should scroll vertically.
     * You are responsible for verifying edge boundaries, and determining if this scroll
     * event somehow requires that new views be added or old views get recycled.
     */
    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) {
            return 0;
        }

        final int amount;
        // compute amount of scroll without going beyond the bound
        if (dy < 0) { // finger is moving downward
            View topView = getTopView();
            boolean topBoundReached = adapterPosition(topView) == topAdapterItemIdx();
            final int topBound = getPaddingTop();
            final int childTop = getDecoratedTop(topView);
            if (topBoundReached && (childTop - dy) > topBound) {
                //If top bound reached, enforce limit
                int topOffset = topBound - childTop;

                amount = -Math.min(-dy, topOffset);
            } else {
                amount = dy;
            }
        } else if (dy > 0) { // finger is moving upward
            View bottomView = getBottomView();
            boolean bottomBoundReached = adapterPosition(bottomView) == bottomAdapterItemIdx();
            final int bottomBound = getVerticalSpace() + getPaddingTop();
            final int childBottom = getDecoratedBottom(bottomView);
            if (bottomBoundReached && (childBottom - dy) < bottomBound) {
                //If we've reached the last row, enforce limits
                int bottomOffset = childBottom - bottomBound;

                amount = Math.min(dy, bottomOffset);
            } else {
                amount = dy;
            }
        } else {
            amount = dy;
        }

        if (dy != amount) {
            logDebug("dy=" + dy + " amount=" + amount + " overScroll=" + mOverScrollVertical);
            overScrollVertical(dy - amount);
        } else {
            resetOverScrollVertical();
        }

        if (amount == 0 || (dy < 0 && amount > 0) || (dy > 0 && amount < 0))
            return 0;

        // scroll children
        offsetChildrenVertical(-amount);

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

    private void overScrollVertical(int amount) {
        // sum up amount until we can trigger the over scroll event
        mOverScrollVertical += amount;
        if (mOverScrollListener != null && mRecyclerView != null) {
            if (mOverScrollVertical > getDecoratedMeasuredHeight(getBottomView())) {
                mOverScrollListener.onOverScroll(mRecyclerView, mOverScrollVertical);
            }
        }
    }

    private void resetOverScrollVertical() {
        // reset amount
        mOverScrollVertical = 0;
    }

    private void checkVisibilityAfterScroll(RecyclerView.Recycler recycler, boolean checkTop) {
        View child = checkTop ? getTopView() : getBottomView();
        int adapterPosition = adapterPosition(child);
        int top = getDecoratedTop(child);
        int newFirstVisible = mFirstVisiblePosition;
        while (needsVisibilityChange(adapterPosition, top, checkTop)) {
            if (checkTop) {
                adapterPosition = aboveAdapterItemIdx(adapterPosition);
                newFirstVisible = aboveAdapterItemIdx(newFirstVisible);
                top -= getRowHeight(getRowIdx(adapterPosition));
            } else {
                top += getRowHeight(getRowIdx(adapterPosition));
                adapterPosition = belowAdapterItemIdx(adapterPosition);
                newFirstVisible = belowAdapterItemIdx(newFirstVisible);
            }
        }
        changeFirstVisible(recycler, newFirstVisible);
    }

    /**
     * @param adapterPosition needed to stop checking if adapter start or end reached
     * @param top             child decorated top
     * @param checkTop        `bound` is the top position or the bottom
     * @return if we need to change `mFirstVisiblePosition`
     */
    private boolean needsVisibilityChange(int adapterPosition, int top, boolean checkTop) {
        if (adapterPosition <= 0 || adapterPosition >= (getItemCount() - 1))
            return false;
        if (checkTop)
            return top > 0;
        int rowHeight = getRowHeight(getRowIdx(adapterPosition));
        return (top + rowHeight) < getHeight();
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
}
