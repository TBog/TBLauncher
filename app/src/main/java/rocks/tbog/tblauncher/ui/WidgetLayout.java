package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.appwidget.AppWidgetHostView;
import android.content.ComponentName;
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
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;

public class WidgetLayout extends ViewGroup {
    /**
     * These are used for computing child frames based on their gravity.
     */
    private final Rect mTmpContainerRect = new Rect();
    private final Rect mTmpChildRect = new Rect();
    private final Point mPageCount = new Point(1, 1);
    private final ArrayList<OnAfterLayoutTask> mAfterLayoutTaskList = new ArrayList<>(1);

    public interface OnAfterLayoutTask {
        void onAfterLayout();
    }

    public enum Handle {
        MOVE_FREE,
        MOVE_AXIAL,
        RESIZE_DIAGONAL,
        RESIZE_AXIAL,
        MOVE_FREE_RESIZE_AXIAL,
        RESIZE_DIAGONAL_MOVE_AXIAL,
        DISABLED;

        public boolean isMove() {
            return this == MOVE_FREE || this == MOVE_AXIAL;
        }

        public boolean isResize() {
            return this == RESIZE_DIAGONAL || this == RESIZE_AXIAL;
        }

        public boolean isMoveResize() {
            return this == MOVE_FREE_RESIZE_AXIAL || this == RESIZE_DIAGONAL_MOVE_AXIAL;
        }
    }


    public WidgetLayout(Context context) {
        this(context, null);
    }

    public WidgetLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public WidgetLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public WidgetLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        setClipChildren(true);
        setClipToPadding(true);
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
     * Add a one time run task
     *
     * @param task what to run
     */
    public void addOnAfterLayoutTask(OnAfterLayoutTask task) {
        mAfterLayoutTaskList.add(task);
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

//    public boolean isHandleEnabled(View widgetView) {
//        return indexOfChild(widgetView) == -1;
//    }

    @NonNull
    public Handle getHandleType(View widgetView) {
        int viewIndex = indexOfChild(widgetView);
        if (viewIndex == -1) {
            for (int idx = 0; idx < getChildCount(); idx += 1) {
                View child = getChildAt(idx);
                if (child instanceof ViewGroup) {
                    viewIndex = ((ViewGroup) child).indexOfChild(widgetView);
                    if (viewIndex != -1) {
                        // we keep the handle type in the tag
                        Object tag = child.getTag();
                        if (tag instanceof Handle)
                            return (Handle) child.getTag();
                        throw new IllegalStateException("widget view tag should hold the Handle");
                    }
                }
            }
        }
        return Handle.DISABLED;
    }

    public void disableHandle(View widgetView) {
        enableHandle(widgetView, Handle.DISABLED);
    }

    public void enableHandle(View widgetView, Handle handle) {
        convertAutoPositionTo(LayoutParams.Placement.MARGIN_TL_AS_POSITION);

        ViewGroup widgetHandle = null;
        // remove widget view from this layout
        int viewIndex = indexOfChild(widgetView);
        // if the widget is already wrapped by the handle
        if (viewIndex == -1) {
            for (int idx = 0; idx < getChildCount(); idx += 1) {
                View child = getChildAt(idx);
                if (child instanceof ViewGroup) {
                    viewIndex = ((ViewGroup) child).indexOfChild(widgetView);
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
            {
                LayoutParams lp = new LayoutParams((LayoutParams) widgetView.getLayoutParams());
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                widgetHandle.setLayoutParams(lp);
            }
            {
                LayoutParams lp = (LayoutParams) widgetView.getLayoutParams();
                lp.setMargins(0, 0, 0, 0);
                widgetView.setLayoutParams(lp);
            }
            // add the widget view to the handle layout as the first child
            widgetHandle.addView(widgetView, 0);
            // add the handle layout to this layout
            addView(widgetHandle, viewIndex);
        }

        // use the tag to keep the handle type
        widgetHandle.setTag(handle);

        switch (handle) {
            case DISABLED: {
                final LayoutParams lp = (LayoutParams) widgetHandle.getLayoutParams();
                lp.width = widgetView.getWidth();
                lp.height = widgetView.getHeight();

                int idx = indexOfChild(widgetHandle);
                widgetHandle.removeViewAt(0);
                removeViewAt(idx);
                addView(widgetView, idx);
                widgetView.setLayoutParams(lp);
                break;
            }
            case MOVE_FREE:
                setupCornerHandles(widgetHandle, R.drawable.ic_handle_move, sMoveListener, true);
                break;
            case MOVE_AXIAL:
                setupLineHandles(widgetHandle, R.drawable.ic_handle_move, sMoveListener, true);
                break;
            case RESIZE_DIAGONAL:
                setupCornerHandles(widgetHandle, R.drawable.ic_handle_resize_bl, sResizeListener, true);
                break;
            case RESIZE_AXIAL:
                setupLineHandles(widgetHandle, R.drawable.ic_handle_resize_l, sResizeListener, true);
                break;
            case MOVE_FREE_RESIZE_AXIAL:
                setupCornerHandles(widgetHandle, R.drawable.ic_handle_move, sMoveListener, false);
                setupLineHandles(widgetHandle, R.drawable.ic_handle_resize_l, sResizeListener, false);
                break;
            case RESIZE_DIAGONAL_MOVE_AXIAL:
                setupCornerHandles(widgetHandle, R.drawable.ic_handle_resize_bl, sResizeListener, false);
                setupLineHandles(widgetHandle, R.drawable.ic_handle_move, sMoveListener, false);
                break;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setupCornerHandles(ViewGroup widgetHandle, @DrawableRes int ic_handle_corner, OnTouchListener touchListener, boolean hideOthers) {
        if (hideOthers) {
            widgetHandle.findViewById(R.id.handle_left).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_top).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_right).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_bottom).setVisibility(GONE);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_top_left);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle_corner);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_top_right);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle_corner);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_bottom_right);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle_corner);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_bottom_left);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle_corner);
            image.setOnTouchListener(touchListener);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private static void setupLineHandles(ViewGroup widgetHandle, @DrawableRes int ic_handle, OnTouchListener touchListener, boolean hideOthers) {
        if (hideOthers) {
            widgetHandle.findViewById(R.id.handle_top_left).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_top_right).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_bottom_right).setVisibility(GONE);
            widgetHandle.findViewById(R.id.handle_bottom_left).setVisibility(GONE);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_left);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_top);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_right);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle);
            image.setOnTouchListener(touchListener);
        }
        {
            ImageView image = widgetHandle.findViewById(R.id.handle_bottom);
            image.setVisibility(VISIBLE);
            image.setImageResource(ic_handle);
            image.setOnTouchListener(touchListener);
        }
    }

    private void convertAutoPositionTo(LayoutParams.Placement placement) {
        final int childCount = getChildCount();
        for (int childIdx = 0; childIdx < childCount; childIdx++) {
            final View child = getChildAt(childIdx);
            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.placement != LayoutParams.Placement.AUTO)
                continue;
            lp.placement = placement;
            lp.leftMargin = child.getLeft();
            lp.topMargin = child.getTop();
            //lp.rightMargin = child.getRight();
            //lp.bottomMargin = child.getBottom();
            child.setLayoutParams(lp);
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
        int count = getChildCount();

        int width = Math.max(getSuggestedMinimumWidth(), MeasureSpec.getSize(widthMeasureSpec));
        int height = Math.max(getSuggestedMinimumHeight(), MeasureSpec.getSize(heightMeasureSpec));

        // Iterate through all children and measure them.
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() == GONE)
                continue;
            ViewGroup.LayoutParams lp = child.getLayoutParams();

            // Measure the child.
            final int childWidthMeasureSpec;
            if (lp.width == ViewGroup.LayoutParams.WRAP_CONTENT)
                childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            else
                childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec, 0, lp.width);

            final int childHeightMeasureSpec;
            if (lp.height == ViewGroup.LayoutParams.WRAP_CONTENT)
                childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
            else
                childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec, 0, lp.height);
            child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            //TODO: check that child is not out of bounds
        }

        for (int pagePosition : LayoutParams.PAGE_POSITIONS) {
            pageLayout(pagePosition, width, height, false);
            width = Math.max(width, mTmpContainerRect.width());
            height = Math.max(height, mTmpContainerRect.height());
        }
        int resolvedWidth = resolveSize(width, widthMeasureSpec);
        int resolvedHeight = resolveSize(height, heightMeasureSpec);
        setMeasuredDimension(resolvedWidth, resolvedHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int pageWidth = right - left;
        final int pageHeight = bottom - top;
        if (mPageCount.x == 1 && mPageCount.y == 1) {
            pageLayout(LayoutParams.PAGE_MIDDLE, pageWidth, pageHeight, true);
        } else if (mPageCount.x > 1 && mPageCount.y == 1) {
            for (int pagePos : LayoutParams.PAGE_POSITIONS_HORIZONTAL) {
                pageLayout(pagePos, pageWidth, pageHeight, true);
            }
        } else if (mPageCount.x == 1 && mPageCount.y > 1) {
            for (int pagePos : LayoutParams.PAGE_POSITIONS_VERTICAL) {
                pageLayout(pagePos, pageWidth, pageHeight, true);
            }
        } else {
            for (int pagePos : LayoutParams.PAGE_POSITIONS)
                pageLayout(pagePos, pageWidth, pageHeight, true);
        }
        callAfterLayout();
    }

    protected void callAfterLayout() {
        for (OnAfterLayoutTask afterLayout : mAfterLayoutTaskList)
            afterLayout.onAfterLayout();
        mAfterLayoutTaskList.clear();
    }

    @Nullable
    public AppWidgetHostView getWidget(int appWidgetId) {
        for (int idx = 0; idx < getChildCount(); idx += 1) {
            View child = getChildAt(idx);
            if (child instanceof AppWidgetHostView && ((AppWidgetHostView) child).getAppWidgetId() == appWidgetId) {
                return (AppWidgetHostView) child;
            }
            if (child instanceof ViewGroup) {
                View view = ((ViewGroup) child).getChildAt(0);
                if (view instanceof AppWidgetHostView && ((AppWidgetHostView) view).getAppWidgetId() == appWidgetId) {
                    return (AppWidgetHostView) view;
                }
            }
        }
        return null;
    }

    @Nullable
    public View getPlaceholder(@NonNull ComponentName provider) {
        for (int idx = 0; idx < getChildCount(); idx += 1) {
            View child = getChildAt(idx);
            if (provider.equals(child.getTag()))
                return child;
        }
        return null;
    }

    public void addPlaceholder(View placeholder, ComponentName provider) {
        addView(placeholder);
        placeholder.setTag(provider);
    }

    public void removeWidget(AppWidgetHostView view) {
        disableHandle(view);
        removeView(view);
    }

    public boolean removeWidget(int appWidgetId) {
        for (int idx = 0; idx < getChildCount(); idx += 1) {
            View child = getChildAt(idx);
            if (child instanceof AppWidgetHostView && ((AppWidgetHostView) child).getAppWidgetId() == appWidgetId) {
                removeView(child);
                return true;
            }
            if (child instanceof ViewGroup) {
                View view = ((ViewGroup) child).getChildAt(0);
                if (view instanceof AppWidgetHostView && ((AppWidgetHostView) view).getAppWidgetId() == appWidgetId) {
                    removeWidget((AppWidgetHostView) view);
                    return true;
                }
            }
        }
        return false;
    }

    private int getLeftMarginForPage(int page, int width) {
        if (mPageCount.x == 1)
            return 0;
        int leftOfCenter = mPageCount.x / 2;
        if (page == LayoutParams.PAGE_LEFT)
            return 0;
        if (page == LayoutParams.PAGE_RIGHT)
            return leftOfCenter * (width + 1);
        return leftOfCenter * width;
    }

    private int getTopMarginForPage(int page, int height) {
        if (mPageCount.y == 1)
            return 0;
        if (page == LayoutParams.PAGE_UP)
            return 0;
        int leftOfCenter = mPageCount.y / 2;
        if (page == LayoutParams.PAGE_DOWN)
            return leftOfCenter * (height + 1);
        return leftOfCenter * height;
    }

    private void pageLayout(int page, int width, int height, boolean childLayout) {
        mTmpContainerRect.setEmpty();
        final int pageTop = getTopMarginForPage(page, height);
        final int pageLeft = getLeftMarginForPage(page, width);
        // apply padding
        final int pageWidth = width - getPaddingLeft() - getPaddingRight();
        final int pageHeight = height - getPaddingTop() - getPaddingBottom();
        int autoX = 0;
        int autoY = 0;
        int maxChildY = 0;
        final int childCount = getChildCount();
        for (int childIdx = 0; childIdx < childCount; childIdx++) {
            final View child = getChildAt(childIdx);
            if (child.getVisibility() == GONE)
                continue;

            final LayoutParams lp = (LayoutParams) child.getLayoutParams();
            if (lp.screenPage != page)
                continue;

            mTmpChildRect.set(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());

            switch (lp.placement) {
                case AUTO:
                    mTmpChildRect.offset(autoX, autoY);
                    if (mTmpChildRect.right > pageWidth) {
                        mTmpChildRect.offset(-autoX, -autoY);
                        autoX = 0;
                        autoY = maxChildY;
                        mTmpChildRect.offset(autoX, autoY);
                    }
                    if (mTmpChildRect.bottom > pageHeight) {
                        mTmpChildRect.offset(-autoX, -autoY);
                        autoY = 0;
                        maxChildY = mTmpContainerRect.bottom;
                        mTmpChildRect.offset(autoX, autoY);
                    }


                    autoX = mTmpChildRect.right;

                    maxChildY = Math.max(maxChildY, mTmpChildRect.bottom);
                    break;
                case MARGIN_TL_AS_POSITION:
                    mTmpChildRect.offset(lp.leftMargin, lp.topMargin);
            }

            // apply page offset
            mTmpChildRect.offset(pageLeft, pageTop);
            // apply page padding
            mTmpChildRect.offset(getPaddingLeft(), getPaddingTop());

            // Place the child.
            if (childLayout)
                child.layout(mTmpChildRect.left, mTmpChildRect.top, mTmpChildRect.right, mTmpChildRect.bottom);
            mTmpContainerRect.union(mTmpChildRect);
        }
    }

    private static final OnTouchListener sMoveListener = new OnTouchListener() {
        final PointF mDownPos = new PointF();

        @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getActionMasked();
            final View parent = (View) v.getParent();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    mDownPos.set(event.getRawX(), event.getRawY());
                    return true;
                case MotionEvent.ACTION_MOVE: {
                    final float xMove = event.getRawX() - mDownPos.x;
                    final float yMove = event.getRawY() - mDownPos.y;

                    int gravity = ((FrameLayout.LayoutParams) v.getLayoutParams()).gravity;
                    boolean horizontal = ((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT);
                    boolean vertical = ((gravity & Gravity.TOP) == Gravity.TOP) || ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM);
                    if (horizontal)
                        parent.setTranslationX(xMove);
                    if (vertical)
                        parent.setTranslationY(yMove);
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    final LayoutParams lp = (LayoutParams) parent.getLayoutParams();

                    lp.leftMargin += (int) parent.getTranslationX();
                    lp.topMargin += (int) parent.getTranslationY();
                    parent.setTranslationX(0f);
                    parent.setTranslationY(0f);

                    parent.setLayoutParams(lp);
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    parent.setTranslationX(0f);
                    parent.setTranslationY(0f);
                    return true;
                }
            }
            return false;
        }
    };

    private static final OnTouchListener sResizeListener = new OnTouchListener() {
        final Point mDownSize = new Point();
        final Point mDownMargin = new Point();
        final PointF mDownPos = new PointF();

        @SuppressLint({"RtlHardcoded", "ClickableViewAccessibility"})
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final ViewGroup parent = (ViewGroup) v.getParent();
            final View widgetView = parent.getChildAt(0);
            final int action = event.getActionMasked();
            switch (action) {
                case MotionEvent.ACTION_DOWN: {
                    {
                        final ViewGroup.LayoutParams lp = widgetView.getLayoutParams();
                        mDownSize.set(lp.width, lp.height);
                    }
                    {
                        final LayoutParams lp = (LayoutParams) parent.getLayoutParams();
                        mDownMargin.set(lp.leftMargin, lp.topMargin);
                    }
                    mDownPos.set(event.getRawX(), event.getRawY());
                    return true;
                }
                case MotionEvent.ACTION_MOVE: {
                    int xMove = (int) (event.getRawX() - mDownPos.x + .5f);
                    int yMove = (int) (event.getRawY() - mDownPos.y + .5f);

                    int gravity = ((FrameLayout.LayoutParams) v.getLayoutParams()).gravity;
                    // move widget handler
                    {
                        final LayoutParams lp = (LayoutParams) parent.getLayoutParams();
                        boolean changed = false;
                        if ((gravity & Gravity.LEFT) == Gravity.LEFT) {
                            lp.leftMargin = mDownMargin.x + xMove;
                            xMove = -xMove;
                            changed = true;
                        }
                        if ((gravity & Gravity.TOP) == Gravity.TOP) {
                            lp.topMargin = mDownMargin.y + yMove;
                            yMove = -yMove;
                            changed = true;
                        }
                        if (changed)
                            parent.setLayoutParams(lp);
                    }
                    // resize widget
                    {
                        final ViewGroup.LayoutParams lp = widgetView.getLayoutParams();
                        boolean horizontal = ((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT);
                        boolean vertical = ((gravity & Gravity.TOP) == Gravity.TOP) || ((gravity & Gravity.BOTTOM) == Gravity.BOTTOM);
                        if (horizontal)
                            lp.width = mDownSize.x + xMove;
                        if (vertical)
                            lp.height = mDownSize.y + yMove;
                        widgetView.setLayoutParams(lp);
                    }
                    //requestLayout();
                    return true;
                }
                case MotionEvent.ACTION_UP: {
                    return true;
                }
                case MotionEvent.ACTION_CANCEL: {
                    {
                        final ViewGroup.LayoutParams lp = widgetView.getLayoutParams();
                        lp.width = mDownSize.x;
                        lp.height = mDownSize.y;
                        widgetView.setLayoutParams(lp);
                    }
                    {
                        final LayoutParams lp = (LayoutParams) parent.getLayoutParams();
                        lp.leftMargin = mDownMargin.x;
                        lp.topMargin = mDownMargin.y;
                        parent.setLayoutParams(lp);
                    }
                    return true;
                }
            }
            return false;
        }
    };

    public static class LayoutParams extends ViewGroup.MarginLayoutParams {

        /**
         * The screen/page to put this view into
         */
        public static final int PAGE_MIDDLE = 0;
        public static final int PAGE_LEFT = 1;
        public static final int PAGE_RIGHT = 2;
        public static final int PAGE_UP = 4;
        public static final int PAGE_DOWN = 8;
        public static final int[] PAGE_POSITIONS = new int[]{PAGE_LEFT, PAGE_UP, PAGE_MIDDLE, PAGE_RIGHT, PAGE_DOWN};
        public static final int[] PAGE_POSITIONS_HORIZONTAL = new int[]{PAGE_LEFT, PAGE_MIDDLE, PAGE_RIGHT};
        public static final int[] PAGE_POSITIONS_VERTICAL = new int[]{PAGE_UP, PAGE_MIDDLE, PAGE_DOWN};

        public enum Placement {
            AUTO,
            MARGIN_TL_AS_POSITION,
        }

        public int screenPage = PAGE_MIDDLE;
        public Placement placement = Placement.AUTO;

        public LayoutParams(Context ctx, AttributeSet attrs) {
            super(ctx, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(LayoutParams source) {
            this((ViewGroup.MarginLayoutParams) source);
            screenPage = source.screenPage;
            placement = source.placement;
        }
    }
}
