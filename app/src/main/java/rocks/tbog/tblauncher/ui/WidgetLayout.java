package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;

import static rocks.tbog.tblauncher.ui.WidgetLayout.LayoutParams.SCREEN_POS;

public class WidgetLayout extends ViewGroup {
    /**
     * These are used for computing child frames based on their gravity.
     */
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();
    private final Point mPageCount = new Point(1, 1);

    public enum Handle {
        MOVE,
        RESIZE,
        DISABLE_ALL,
    }

    public WidgetLayout(Context context) {
        super(context);
    }

    public WidgetLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WidgetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WidgetLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setPageCount(int horizontal, int vertical) {
        if (horizontal < 1 || vertical < 1)
            throw new IllegalStateException("Page count must be >= 1");
        mPageCount.set(horizontal, vertical);
    }

    public int getHorizontalPageCount() {
        return mPageCount.x;
    }

    public int getVerticalPageCount() {
        return mPageCount.y;
    }

    /**
     * Set current page
     *
     * @param pageX horizontal page to show
     * @param pageY vertical page to show
     */
    public void scrollToPage(float pageX, float pageY) {
        final int pageWidth = getWidth() / mPageCount.x;
        final int pageHeight = getHeight() / mPageCount.y;
        final float x = pageWidth * (mPageCount.x - 1) * pageX;
        final float y = pageHeight * (mPageCount.y - 1) * pageY;
        scrollTo((int) x, (int) y);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void enableHandle(View view, Handle handle) {
        ViewGroup widgetHandle = null;
        // remove widget view from this layout
        int viewIndex = indexOfChild(view);
        // if the widget is already wrapped by the handle
        if (viewIndex == -1) {
            for (int idx = 0; idx < getChildCount(); idx += 1) {
                View child = getChildAt(idx);
                if (child instanceof ViewGroup) {
                    viewIndex = ((ViewGroup) child).indexOfChild(view);
                    if (viewIndex != -1) {
                        widgetHandle = (ViewGroup) child;
                        break;
                    }
                }
            }
            // can't find the widget handle
            if (widgetHandle == null)
                return;
        } else {
            removeViewAt(viewIndex);
            // inflate the widget handle layout
            widgetHandle = (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.widget_handle, this, false);
            //ViewCompat.setElevation(widgetHandle, 10f);

            // add the widget view to the handle layout as the first child
            widgetHandle.addView(view, 0);
            // add the handle layout to this layout
            addView(widgetHandle, viewIndex);
        }

        if (handle == Handle.DISABLE_ALL) {
            int idx = indexOfChild(widgetHandle);
            widgetHandle.removeViewAt(0);
            removeViewAt(idx);
            addView(view, idx);
        }

        if (handle == Handle.MOVE) {
            OnTouchListener moveListener = new OnTouchListener() {
                final PointF mDownPos = new PointF();

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    final int action = event.getActionMasked();
                    switch (action) {
                        case MotionEvent.ACTION_DOWN:
                            mDownPos.set(event.getRawX(), event.getRawY());
                            break;
                        case MotionEvent.ACTION_MOVE: {
                            View parent = (View) v.getParent();
                            float xMove = event.getRawX() - mDownPos.x;
                            float yMove = event.getRawY() - mDownPos.y;

                            mDownPos.set(event.getRawX(), event.getRawY());
                            xMove += parent.getTranslationX();
                            yMove += parent.getTranslationY();

                            parent.setTranslationX(xMove);
                            parent.setTranslationY(yMove);

                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            View parent = (View) v.getParent();
                            LayoutParams lp = (LayoutParams) parent.getLayoutParams();
                            lp.leftMargin = (int) parent.getTranslationX();
                            lp.topMargin = (int) parent.getTranslationY();

                            parent.setLayoutParams(lp); // this will call requestLayout

                            break;
                        }
                    }
                    return true;
                }
            };

            widgetHandle.findViewById(R.id.handle_top_left).setOnTouchListener(moveListener);
            widgetHandle.findViewById(R.id.handle_top_right).setOnTouchListener(moveListener);
            widgetHandle.findViewById(R.id.handle_bottom_right).setOnTouchListener(moveListener);
            widgetHandle.findViewById(R.id.handle_bottom_left).setOnTouchListener(moveListener);
        }
    }

    /**
     * This prevents the pressed state from appearing when the user is actually trying to scroll the content.
     *
     * @return true
     */
    @Override
    public boolean shouldDelayChildPressedState() {
        return TBApplication.liveWallpaper(getContext()).isPreferenceWPDragAnimate();
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        if (p instanceof LinearLayout.LayoutParams)
            return new LayoutParams((LinearLayout.LayoutParams) p);
        return new LayoutParams(new LinearLayout.LayoutParams(p));
    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int maxTotalWidth = 0;
        int maxTotalHeight = 0;
        int count = getChildCount();
        // Iterate through all children and measure them.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            // Measure the child.
            measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0);

            maxTotalWidth += child.getMeasuredWidth();
            maxTotalHeight += child.getMeasuredHeight();
        }
        int width = Math.max(getSuggestedMinimumWidth(), MeasureSpec.getSize(widthMeasureSpec));
        int height = Math.max(getSuggestedMinimumHeight(), MeasureSpec.getSize(heightMeasureSpec));
        for (int screenIdx = 0; screenIdx < SCREEN_POS.length; screenIdx += 1) {
            screenLayout(SCREEN_POS[screenIdx], 0, 0, width, height, false);
            width = Math.max(width, mTmpContainerRect.width());
            height = Math.max(height, mTmpContainerRect.height());
        }
        int resolvedWidth = resolveSize(width, widthMeasureSpec);
        int resolvedHeight = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (mPageCount.x > 1 && mPageCount.y == 1) {
            horizontalLayout(changed, left, top, right, bottom);
            return;
        }
        final int count = getChildCount();

        // These are the far left and right edges in which we are performing layout.
        int leftPos = getPaddingLeft();
        int rightPos = getLayoutParams().width - getPaddingRight();

        // This is the middle region inside of the gutter.
        final int screenWidth = right - left;
        final int middleLeft = leftPos + screenWidth;
        final int middleRight = rightPos - screenWidth;

        // These are the top and bottom edges in which we are performing layout.
        final int parentTop = getPaddingTop();
        final int parentBottom = bottom - top - getPaddingBottom();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            // Compute the frame in which we are placing this child.
            if (lp.screen == LayoutParams.SCREEN_LEFT) {
                mTmpContainerRect.left = leftPos + lp.leftMargin;
                mTmpContainerRect.right = middleLeft - lp.rightMargin;
            } else if (lp.screen == LayoutParams.SCREEN_RIGHT) {
                mTmpContainerRect.right = rightPos - lp.rightMargin;
                mTmpContainerRect.left = middleRight + lp.leftMargin;
            } else {
                mTmpContainerRect.left = middleLeft + lp.leftMargin;
                mTmpContainerRect.right = middleRight - lp.rightMargin;
            }
            mTmpContainerRect.top = parentTop + lp.topMargin;
            mTmpContainerRect.bottom = parentBottom - lp.bottomMargin;

            // Use the child's gravity and size to determine its final frame within its container.
            Gravity.apply(Gravity.FILL, width, height, mTmpContainerRect, mTmpChildRect);

            // Place the child.
            child.layout(mTmpChildRect.left, mTmpChildRect.top,
                    mTmpChildRect.right, mTmpChildRect.bottom);
        }
    }

    private void horizontalLayout(boolean changed, int left, int top, int right, int bottom) {
        final int screenWidth = right - left;
//        Rect screen = new Rect(left, top, right, bottom);
//        {
//            final int wholeWidth = screenWidth * mPageCount.x;
//            final int centerX = screen.centerX();
//            screen.left = centerX - wholeWidth / 2;
//            screen.right = screen.left + wholeWidth;
//        }

        for (int screenIdx = 0; screenIdx < SCREEN_POS.length; screenIdx += 1) {
            // add padding
            int screenLeft = left + getPaddingLeft();
            int screenTop = top + getPaddingTop();
            int screenRight = right - getPaddingRight();
            int screenBottom = bottom - getPaddingBottom();

            screenLayout(SCREEN_POS[screenIdx], screenLeft, screenTop, screenRight, screenBottom, true);
            left += screenWidth;
        }
    }

    private void screenLayout(int position, int left, int top, int right, int bottom, boolean childLayout) {
        mTmpContainerRect.setEmpty();
        int x = left;
        int y = top;
        int maxY = top;
        final int childCount = getChildCount();
        for (int childIdx = 0; childIdx < childCount; childIdx++) {
            final View child = getChildAt(childIdx);
            if (child.getVisibility() == GONE)
                continue;

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.screen != position)
                continue;

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();
            mTmpChildRect.set(0, 0, width, height);
            mTmpChildRect.offset(x, y);
            if (mTmpChildRect.right > right) {
                mTmpChildRect.offset(-x, -y);
                x = left;
                y = Math.min(maxY, bottom);
                mTmpChildRect.offset(x, y);
            }
            if (mTmpChildRect.bottom > bottom) {
                mTmpChildRect.offset(-x, -y);
                y = top;
                maxY = mTmpContainerRect.bottom;
                mTmpChildRect.offset(x, y);
            }

            // Place the child.
            if (childLayout)
                child.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right, mTmpChildRect.bottom);
            mTmpContainerRect.union(mTmpChildRect);

            maxY = Math.max(maxY, mTmpChildRect.bottom);
        }
    }

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /**
         * The screen/page to put this view into
         */
        public static final int SCREEN_MIDDLE = 0;
        public static final int SCREEN_LEFT = 1;
        public static final int SCREEN_RIGHT = 2;
        public static final int[] SCREEN_POS = new int[]{LayoutParams.SCREEN_LEFT, LayoutParams.SCREEN_MIDDLE, LayoutParams.SCREEN_RIGHT};

        public static final int PLACE_AUTO = 0;
        public static final int PLACE_MARGIN = 0;

        public int screen = SCREEN_MIDDLE;
        public int placement = PLACE_AUTO;

        public LayoutParams(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
            throw new IllegalStateException("not designed to inflate from xml");
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            this((ViewGroup.MarginLayoutParams) source);
            screen = source.screen;
            placement = source.placement;
        }
    }
}
