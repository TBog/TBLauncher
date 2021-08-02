package rocks.tbog.tblauncher.result;

import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import rocks.tbog.tblauncher.R;

public class CustomRecycleLayoutManager extends RecyclerView.LayoutManager {

    private static final String TAG = "CRLM";

    /* First position visible at any point (adapter index) */
    private int mFirstVisiblePosition;
    /* Scroll offset in px (modulo mDecoratedChildHeight)*/
    private int mVerticalScrollPositionOffset;

    private int mVisibleCount;
    /* Consistent size applied to all child views */
    private int mDecoratedChildWidth;
    private int mDecoratedChildHeight;

    /* Used for starting the layout from the bottom / right */
    private boolean mFirstAtBottom = true;
    /* Used for reversing adapter order */
    private boolean mReverseAdapter = true;

    // Reusable int array to be passed to method calls that mutate it in order to "return" two ints.
    // This should only be used used transiently and should not be used to retain any state over
    // time.
    private final int[] mReusableIntPair = new int[2];

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
//        mFirstChangedPosition = positionStart;
//        mChangedPositionCount = itemCount;
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int getHorizontalSpace() {
        return getWidth() - getPaddingRight() - getPaddingLeft();
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
            mVerticalScrollPositionOffset = 0;

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
        updateWindowSizing();

        //Clear all attached views into the recycle bin
        detachAndScrapAttachedViews(recycler);

        layoutChildren(recycler);
    }

    @Override
    public void onLayoutCompleted(RecyclerView.State state) {
        super.onLayoutCompleted(state);
        //mPendingSavedState = null; // we don't need this anymore
    }

    private void layoutChildren(RecyclerView.Recycler recycler) {
        View child;
        int posX, posY;

        // find start offset
        if (getChildCount() == 0) {
            posX = getPaddingLeft();
            posY = mVerticalScrollPositionOffset;
            if (mFirstAtBottom)
                posY += getVerticalSpace() + getPaddingTop() - mDecoratedChildHeight;
        } else {
            child = mFirstAtBottom ? getBottomView() : getTopView();

            posX = getDecoratedLeft(child);
            posY = getDecoratedTop(child);
        }
        /*
         * Detach all existing views from the layout.
         * detachView() is a lightweight operation that we can use to
         * quickly reorder views without a full add/remove.
         */
        SparseArray<View> viewCache = new SparseArray<>(getChildCount());

        //Cache all views by their existing position, before updating counts
        for (int i = 0; i < getChildCount(); i++) {
            child = getChildAt(i);
            if (child == null)
                continue;
            int position = adapterPosition(child);
            viewCache.put(position, child);
        }

        //Temporarily detach all views.
        // Views we still need will be added back with the proper child index.
        for (int i = 0; i < viewCache.size(); i++) {
            detachView(viewCache.valueAt(i));
        }

        // layout
        for (int i = 0; i < mVisibleCount; i += 1) {
            int adapterPos = adapterPosition(i);

            //Layout this position
            child = viewCache.get(adapterPos);
            if (child == null) {
                /*
                 * The Recycler will give us either a newly constructed view,
                 * or a recycled view it has on-hand. In either case, the
                 * view will already be fully bound to the data by the
                 * adapter for us.
                 */
                child = recycler.getViewForPosition(adapterPos);
                addView(child);
                /*
                 * It is prudent to measure/layout each new view we
                 * receive from the Recycler. We don't have to do
                 * this for views we are just re-arranging.
                 */
                measureChildWithMargins(child, 0, 0);
                layoutChildView(child, posX, posY);
                Log.d(TAG, "child #" + i + " pos=" + adapterPos + " (" + child.getLeft() + " " + child.getTop() + " " + child.getRight() + " " + child.getBottom() + ") " + getDebugName(child));
            } else {
                attachView(child);
                viewCache.remove(adapterPos);
            }

            posY += mFirstAtBottom ? -mDecoratedChildHeight : mDecoratedChildHeight;
        }
    }

    private void layoutChildView(View view, int left, int top) {
        int leftOffset = left;
        int topOffset = top;
//        if (mFirstAtBottom) {
//            // align the child using the bottom left corner
//            topOffset -= mDecoratedChildHeight;
//        }
        layoutDecorated(view, leftOffset, topOffset,
                leftOffset + mDecoratedChildWidth,
                topOffset + mDecoratedChildHeight);
    }

    /*
     * Rather than continuously checking how many views we can fit
     * based on scroll offsets, we simplify the math by computing the
     * visible grid as what will initially fit on screen, plus one.
     */
    private void updateWindowSizing() {
        int verticalSpace = getVerticalSpace();
        mVisibleCount = (verticalSpace / mDecoratedChildHeight) + 1;
        if (verticalSpace % mDecoratedChildWidth > 0) {
            mVisibleCount++;
        }

        //Allow minimum value for small data sets
        if (mVisibleCount > getItemCount()) {
            mVisibleCount = getItemCount();
        }

        Log.i(TAG, "verticalSpace=" + verticalSpace + " mVisibleCount=" + mVisibleCount);
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

    /**
     * First (or last) item from the adapter that can be displayed at the top of the list
     *
     * @return index from adapter
     */
    private int topAdapterItemIdx() {
        boolean reverse = false;
        if (mFirstAtBottom)
            reverse = true;
        if (mReverseAdapter)
            reverse = !reverse;
        return reverse ? (getItemCount() - 1) : 0;
    }

    /**
     * First (or last) item from the adapter that can be displayed at the bottom of the list
     *
     * @return index from adapter
     */
    private int bottomAdapterItemIdx() {
        boolean reverse = false;
        if (mFirstAtBottom)
            reverse = true;
        if (mReverseAdapter)
            reverse = !reverse;
        return reverse ? 0 : (getItemCount() - 1);
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
            if (!topBoundReached && (childTop - dy) > 0) {
                int prevPos = adapterPosition(topView);
                prevPos -= topAdapterItemIdx() < bottomAdapterItemIdx() ? +1 : -1;
                View nextView = recycler.getViewForPosition(prevPos);
                // add view and expect `onLayoutChildren` to position it
                addView(nextView);
                measureChildWithMargins(nextView, 0, 0);
                int posX = topView.getLeft();
                int posY = childTop - mDecoratedChildHeight;
                layoutChildView(nextView, posX, posY);
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
            if (!bottomBoundReached && (childBottom - dy) < getHeight()) {
                int nextPos = adapterPosition(bottomView);
                nextPos += topAdapterItemIdx() < bottomAdapterItemIdx() ? +1 : -1;
                View nextView = recycler.getViewForPosition(nextPos);
                // add view and expect `onLayoutChildren` to position it
                addView(nextView);
                measureChildWithMargins(nextView, 0, 0);
                int posX = bottomView.getLeft();
                int posY = childBottom;
                layoutChildView(nextView, posX, posY);
            }
        } else {
            amount = dy;
        }

        offsetChildrenVertical(-amount);
        mVerticalScrollPositionOffset -= amount;

        /*
         * Return value determines if a boundary has been reached
         * (for edge effects and flings). If returned value does not
         * match original delta (passed in), RecyclerView will draw
         * an edge effect.
         */
        return amount;
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
}
