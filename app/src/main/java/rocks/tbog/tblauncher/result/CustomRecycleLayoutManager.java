package rocks.tbog.tblauncher.result;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;

public class CustomRecycleLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "CRLM";
    private static final Boolean LOG_DEBUG = BuildConfig.DEBUG;

    /* First position visible at any point (adapter index) */
    private int mFirstVisiblePosition;

    private int mVisibleCount;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;

    /* Used for starting the layout from the bottom */
    private boolean mFirstAtBottom;
    /* Used for reversing adapter order */
    private boolean mReverseAdapter;
    /* When list height grows, keep bottom view at the bottom */
    private boolean mAutoScrollBottom = true;

    private boolean mRefreshViews = false;

    // Reusable array. This should only be used used transiently and should not be used to retain any state over time.
    private SparseArray<View> mViewCache = null;

    public CustomRecycleLayoutManager() {
        this(true, true);
    }

    public CustomRecycleLayoutManager(boolean firstAtBottom, boolean reverseAdapter) {
        super();
        mFirstAtBottom = firstAtBottom;
        mReverseAdapter = reverseAdapter;
    }

    public void setFirstAtBottom(boolean firstAtBottom) {
        assertNotInLayoutOrScroll(null);
        if (mFirstAtBottom == firstAtBottom) {
            return;
        }
        mFirstAtBottom = firstAtBottom;
        requestLayout();
    }

    public void setReverseAdapter(boolean reverseAdapter) {
        assertNotInLayoutOrScroll(null);
        if (mReverseAdapter == reverseAdapter) {
            return;
        }
        mReverseAdapter = reverseAdapter;
        requestLayout();
    }

    public void setAutoScrollBottom(boolean autoScrollBottom) {
        mAutoScrollBottom = autoScrollBottom;
    }

    @Override
    public int computeVerticalScrollExtent(@NonNull RecyclerView.State state) {
        return getVerticalSpace();
    }

    @Override
    public int computeVerticalScrollOffset(@NonNull RecyclerView.State state) {
        if (getChildCount() <= 1)
            return 0;
        View view = findBottomVisibleItemView();
        int aboveCount = Math.abs(topAdapterItemIdx() - adapterPosition(view));
        return getPaddingTop() - view.getBottom() + (aboveCount + 1) * mDecoratedChildHeight;
    }

    @Override
    public int computeVerticalScrollRange(@NonNull RecyclerView.State state) {
        return mDecoratedChildHeight * state.getItemCount();
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
            int newValue = mFirstAtBottom ? bottomAdapterItemIdx() : topAdapterItemIdx();
            logDebug("onItemsChanged mFirstVisiblePosition changed from " + oldValue + " to " + newValue);
            mFirstVisiblePosition = newValue;
        } else {
            logDebug("onItemsChanged");
        }
    }

    @Override
    public void onAdapterChanged(@Nullable RecyclerView.Adapter oldAdapter, @Nullable RecyclerView.Adapter newAdapter) {
        logDebug("onAdapterChanged");
        mRefreshViews = true;
    }

    @Override
    public void scrollToPosition(int position) {
        if (position < 0 || position >= getItemCount()) {
            Log.e(TAG, "Cannot scroll to " + position + ", item count " + getItemCount());
            return;
        }
        logDebug("scrollToPosition mFirstVisiblePosition changed from " + mFirstVisiblePosition + " to " + position);
        mFirstVisiblePosition = position;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
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

        if (getChildCount() == 0) { //First or empty layout
            mFirstVisiblePosition = mFirstAtBottom ? bottomAdapterItemIdx() : topAdapterItemIdx();

            //Scrap measure one child
            View scrap = recycler.getViewForPosition(mFirstVisiblePosition);
            addView(scrap);
            measureChildWithMargins(scrap, 0, 0);

            /*
             * We make some assumptions in this code based on every child
             * view being the same size (i.e. a uniform grid). This allows
             * us to compute the following values up front because they
             * won't change.
             */
            mDecoratedChildWidth = getDecoratedMeasuredWidth(scrap);
            mDecoratedChildHeight = getDecoratedMeasuredHeight(scrap);

            detachAndScrapView(scrap, recycler);
        }

        //Always update the visible row/column counts
        updateSizing();

        logDebug("onLayoutChildren" +
                " childCount=" + getChildCount() +
                " itemCount=" + getItemCount() +
                " mVisibleCount=" + mVisibleCount +
                " mDecoratedChildWidth=" + mDecoratedChildWidth +
                " mDecoratedChildHeight=" + mDecoratedChildHeight +
                (state.isPreLayout() ? " preLayout" : "") +
                (state.didStructureChange() ? " structureChanged" : "") +
                " stateItemCount=" + state.getItemCount());

        layoutChildren(recycler);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        //mPendingSavedState = null; // we don't need this anymore

        if (getChildCount() > 0) {
            // check if this was a resize that requires a forced scroll
            if (mFirstAtBottom && mAutoScrollBottom) {
                int bottomDelta = getPaddingTop() + getVerticalSpace() - getBottomView().getBottom();
                if (bottomDelta > 0) {
                    // the last view is too high
                    offsetChildrenVertical(bottomDelta);
                    logDebug("(1) auto-scroll bottom amount=" + bottomDelta);
                } else if (bottomDelta < 0 && bottomAdapterItemIdx() == mFirstVisiblePosition) {
                    // the first visible item (at the bottom) is hidden, scroll it into view
                    offsetChildrenVertical(bottomDelta);
                    logDebug("(2) auto-scroll bottom amount=" + bottomDelta);
                }
            }
        }
    }

    private void layoutChildren(RecyclerView.Recycler recycler) {
        // find start offset
        int posX = getPaddingLeft();
        int posY = getPaddingTop() + (mFirstAtBottom ? (getVerticalSpace() - mDecoratedChildHeight) : 0);

        layoutChildren(recycler, posX, posY);
    }

    /**
     * Compute y position for first visible child based on mViewCache
     *
     * @return top value for first visible
     */
    private int smartGetFirstVisibleTop() {
        if (mViewCache.size() > 0) {
            int missing = 0;
            for (int i = 0; i < mVisibleCount; i += 1) {
                int adapterPos = adapterPosition(i);
                View child = mViewCache.get(adapterPos);
                if (child == null) {
                    missing += 1;
                } else {
                    return child.getTop() - (mFirstAtBottom ? -mDecoratedChildHeight : mDecoratedChildHeight) * missing;
                }
            }
        }
        return getPaddingTop() + (mFirstAtBottom ? (getVerticalSpace() - mDecoratedChildHeight) : 0);
    }

    private void layoutChildren(RecyclerView.Recycler recycler, final int posX, final int posY) {
        logDebug("layoutChildren mFirstVisiblePosition=" + mFirstVisiblePosition + " startX=" + posX + " startY=" + posY);
        View child;
        /*
         * Detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        if (mViewCache == null)
            mViewCache = new SparseArray<>(Math.max(mVisibleCount, getChildCount()));
        else
            mViewCache.clear();

        cacheChildren();

        // compute start position after we populate `mViewCache`
        final int firstTopPos = smartGetFirstVisibleTop();

        if (mRefreshViews) {
            logDebug("detachAndScrapAttachedViews");
            // If we want to refresh all views, just scrap them and let the recycler rebind them
            mRefreshViews = false;
            mViewCache.clear();
            detachAndScrapAttachedViews(recycler);
        } else {
            logDebug("detachViews");
            // Temporarily detach all views. We do this to easily reorder them.
            for (int i = 0; i < mViewCache.size(); i++) {
                child = mViewCache.valueAt(i);
                // When an update is in order, scrap the view and let the recycler rebind it
                if (viewNeedsUpdate(child)) {
                    detachAndScrapView(child, recycler);
                    mViewCache.removeAt(i--);
                } else {
                    detachView(child);
                }
            }
        }

        final int nextTopPosDelta = mFirstAtBottom ? -mDecoratedChildHeight : mDecoratedChildHeight;
        int childIdx = 0;

        // layout
        while (childIdx < mVisibleCount) {
            int adapterPos = adapterPosition(childIdx);
            if (adapterPos < 0 || adapterPos >= getItemCount()) {
                Log.w(TAG, "! child #" + childIdx + " missing adapter pos=" + adapterPos + " itemCount=" + getItemCount() + " mVisibleCount=" + mVisibleCount);
                childIdx += 1;
                continue;
            }

            final int topPos = firstTopPos + childIdx * nextTopPosDelta;

            //Layout this position
            child = layoutAdapterPos(recycler, adapterPos, posX, topPos);

            childIdx += 1;
            if (childIdx == mVisibleCount) {
                boolean needsMoreChildren = mFirstAtBottom ? (child.getTop() > 0) : (child.getBottom() < getHeight());
                if (needsMoreChildren) {
                    // force-show more views
                    mVisibleCount += 1;
                }
            }
        }

        clearViewCache(recycler);
    }

    /**
     * Cache all views by their existing position
     */
    private void cacheChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child == null)
                throw new IllegalStateException("null child when count=" + getChildCount() + " and idx=" + i);
            int position = adapterPosition(child);
            mViewCache.put(position, child);
            logDebug("info #" + i + " pos=" + position + " " + getDebugInfo(child) + " " + getDebugName(child));
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
            measureChildWithMargins(child, 0, 0);
            layoutChildView(child, leftPos, topPos);
            logDebug("child #" + indexOfChild(child) + " pos=" + adapterPos + " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ") " + getDebugInfo(child) + " " + getDebugName(child));
        } else {
            attachView(child);
            logDebug("cache #" + indexOfChild(child) + " pos=" + adapterPos + " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ") top=" + topPos + " " + getDebugInfo(child) + " " + getDebugName(child));
            mViewCache.remove(adapterPos);
        }
        return child;
    }

    private void layoutChildView(View view, int left, int top) {
        layoutDecorated(view, left, top,
                left + mDecoratedChildWidth,
                top + mDecoratedChildHeight);
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
        int visibleSpace = getHeight();
        mVisibleCount = (visibleSpace / mDecoratedChildHeight) + 1;
        if (visibleSpace % mDecoratedChildWidth > 0) {
            mVisibleCount++;
        }

        //Allow minimum value for small data sets
        if (mVisibleCount > getItemCount()) {
            mVisibleCount = getItemCount();
        }
    }

    /*
     * Use this method to tell the RecyclerView if scrolling is even possible
     * in the vertical direction.
     */
    @Override
    public boolean canScrollVertically() {
        //We do allow scrolling
        return mDecoratedChildHeight * mVisibleCount > getVerticalSpace();
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

    /**
     * Return top child view on screen (may not be visible)
     *
     * @return child view
     */
    @NonNull
    private View getTopView() {
        final int topChildIdx = mFirstAtBottom ? (getChildCount() - 1) : 0;
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
        final int bottomChildIdx = mFirstAtBottom ? 0 : (getChildCount() - 1);
        View child = getChildAt(bottomChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + getChildCount() + " and bottomChildIdx=" + bottomChildIdx);
        return child;
    }

    @NonNull
    private View findBottomVisibleItemView() {
        final int childCount = getChildCount();
        int botChildIdx = mFirstAtBottom ? 0 : (childCount - 1);
        View child = getChildAt(botChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        while (child.getTop() > getHeight()) {
            botChildIdx += mFirstAtBottom ? 1 : -1;
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
        int botChildIdx = mFirstAtBottom ? 0 : (childCount - 1);
        if (botChildIdx < 0 || botChildIdx >= childCount)
            return -1;
        View child = getChildAt(botChildIdx);
        if (child == null)
            throw new IllegalStateException("null child when count=" + childCount + " and bottomChildIdx=" + botChildIdx);
        while (child.getTop() > getHeight()) {
            botChildIdx += mFirstAtBottom ? 1 : -1;
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
        return (mFirstAtBottom ^ mReverseAdapter) ? (getItemCount() - 1) : 0;
    }

    /**
     * First (or last) item from the adapter that can be displayed at the bottom of the list
     *
     * @return index from adapter
     */
    private int bottomAdapterItemIdx() {
        return (mFirstAtBottom ^ mReverseAdapter) ? 0 : (getItemCount() - 1);
    }

    private int aboveAdapterItemIdx(int idx) {
        return idx - belowAdapterItemIdx(0);
    }

    private int belowAdapterItemIdx(int idx) {
        return idx + ((mFirstAtBottom ^ mReverseAdapter) ? -1 : 1);
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

        if (dy != amount)
            logDebug("dy=" + dy + " amount=" + amount);

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

    private void checkVisibilityAfterScroll(RecyclerView.Recycler recycler, boolean checkTop) {
        View child = checkTop ? getTopView() : getBottomView();
        int adapterPosition = adapterPosition(child);
        int top = getDecoratedTop(child);
        int newFirstVisible = mFirstVisiblePosition;
        while (needsVisibilityChange(adapterPosition, top, checkTop)) {
            if (checkTop) {
                adapterPosition = aboveAdapterItemIdx(adapterPosition);
                newFirstVisible = aboveAdapterItemIdx(newFirstVisible);
                top -= mDecoratedChildHeight;
            } else {
                adapterPosition = belowAdapterItemIdx(adapterPosition);
                newFirstVisible = belowAdapterItemIdx(newFirstVisible);
                top += mDecoratedChildHeight;
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
        return checkTop ? (top > 0) : ((top + mDecoratedChildHeight) < getHeight());
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
            if (p.viewNeedsUpdate()) info += "u";
            if (p.isItemChanged()) info += "c";
            if (p.isItemRemoved()) info += "r";
            if (p.isViewInvalid()) info += "i";
        }
        return info;
    }

    private static void logDebug(String message) {
        if (LOG_DEBUG)
            Log.d(TAG, message);
    }
}
