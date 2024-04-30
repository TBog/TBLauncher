package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;

public class CustomizeMarginView extends View {
    private final Rect targetRect = new Rect();
    private final Rect windowRect = new Rect();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float offsetX = 0f;
    private float offsetY = 0f;
    private int bgColor1;
    private int bgColor2;
    private float horizontalMargin;
    private float verticalMargin;
    private float offsetScale;
    private final int _padding;
    private final int _width;
    private final int _height;
    private final float _sampleRadius;
    private final float _sampleFrameRadius;
    private OnOffsetChanged onOffsetChanged = null;
    private final int colorSampleFrame;

    public interface OnOffsetChanged {
        void onOffsetChanged(float dx, float dy);
    }

    public CustomizeMarginView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizeMarginView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _padding = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_panel_margin);
        _width = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_hsv_size) + _padding * 2;
        _height = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_hsv_size) + _padding * 2;
        _sampleRadius = getResources().getDimension(R.dimen.mm2d_cc_sample_radius);
        _sampleFrameRadius = _sampleRadius + getResources().getDimension(R.dimen.mm2d_cc_sample_frame);
        colorSampleFrame = getResources().getColor(R.color.mm2d_cc_sample_frame);

        paint.setStrokeWidth(0f);
        setLayerType(LAYER_TYPE_SOFTWARE, paint);

        bgColor1 = 0xFF000000;
        bgColor2 = 0xFFffffff;

        horizontalMargin = _padding * 2;
        verticalMargin = _padding * 2;
        offsetScale = Math.max(horizontalMargin, verticalMargin);
    }

    public void setOnOffsetChanged(OnOffsetChanged listener) {
        onOffsetChanged = listener;
    }

    public void setPreviewColors(int bgColor1, int bgColor2) {
        this.bgColor1 = bgColor1;
        this.bgColor2 = bgColor2;
    }

    public void setMarginParameters(float horizontalMargin, float verticalMargin) {
        this.horizontalMargin = horizontalMargin;
        this.verticalMargin = verticalMargin;
        this.offsetScale = Math.max(horizontalMargin, verticalMargin);
        invalidate();
    }

    public void setOffsetValues(float offsetX, float offsetY) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (targetRect.isEmpty()) {
            canvas.getClipBounds(targetRect);
        }
        windowRect.set(targetRect);
        windowRect.inset(Math.round(horizontalMargin), Math.round(verticalMargin));
        windowRect.offset(Math.round(offsetX), Math.round(offsetY));

        // background
        paint.setColor(bgColor1);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(targetRect, paint);

        // window
        paint.setColor(bgColor2);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawRect(windowRect, paint);

        // draw offset circle
        var x = (offsetX / offsetScale * .5f + .5f) * targetRect.width() + targetRect.left;
        var y = (offsetY / offsetScale * .5f + .5f) * targetRect.height() + targetRect.top;

        paint.setColor(colorSampleFrame);
        canvas.drawCircle(x, y, _sampleFrameRadius, paint);
        paint.setColor(bgColor1);
        canvas.drawCircle(x, y, _sampleRadius, paint);
    }

    private static float clampOffset(float value) {
        return Math.min(1f, Math.max(0f, value));
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        offsetX = clampOffset((event.getX() - targetRect.left) / targetRect.width());
        offsetY = clampOffset((event.getY() - targetRect.top) / targetRect.height());

        // center and scale offset
        offsetX = (offsetX * 2f - 1f) * offsetScale;
        offsetY = (offsetY * 2f - 1f) * offsetScale;

        // round values
        offsetX = Math.round(offsetX);
        offsetY = Math.round(offsetY);

        if (onOffsetChanged != null)
            onOffsetChanged.onOffsetChanged(offsetX, offsetY);
        invalidate();
        return true;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        targetRect.set(
            getPaddingLeft() + _padding,
            getPaddingTop() + _padding,
            getWidth() - getPaddingRight() - _padding,
            getHeight() - getPaddingBottom() - _padding
        );
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final var paddingHorizontal = getPaddingLeft() + getPaddingRight();
        final var paddingVertical = getPaddingTop() + getPaddingBottom();
        final var resizeWidth = MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY;
        final var resizeHeight = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY;

        if (!resizeWidth && !resizeHeight) {
            setMeasuredDimension(
                resolveSizeAndState(
                    Math.max(_width + paddingHorizontal, getSuggestedMinimumWidth()),
                    widthMeasureSpec,
                    MeasureSpec.UNSPECIFIED
                ),
                resolveSizeAndState(
                    Math.max(_height + paddingVertical, getSuggestedMinimumHeight()),
                    heightMeasureSpec,
                    MeasureSpec.UNSPECIFIED
                )
            );
            return;
        }

        var widthSize = resolveAdjustedSize(_width + paddingHorizontal, widthMeasureSpec);
        var heightSize = resolveAdjustedSize(_height + paddingVertical, heightMeasureSpec);
        var actualAspect = (widthSize - paddingHorizontal) / (float) (heightSize - paddingVertical);

        if (Math.abs(actualAspect - 1f) < 0.0000001f) {
            setMeasuredDimension(widthSize, heightSize);
            return;
        }
        if (resizeWidth) {
            final var newWidth = heightSize - paddingVertical + paddingHorizontal;
            if (!resizeHeight) {
                widthSize = resolveAdjustedSize(newWidth, widthMeasureSpec);
            }
            if (newWidth <= widthSize) {
                widthSize = newWidth;
                setMeasuredDimension(widthSize, heightSize);
                return;
            }
        }
        if (resizeHeight) {
            final var newHeight = widthSize - paddingHorizontal + paddingVertical;
            if (!resizeWidth) {
                heightSize = resolveAdjustedSize(newHeight, heightMeasureSpec);
            }
            if (newHeight <= heightSize) {
                heightSize = newHeight;
            }
        }
        setMeasuredDimension(widthSize, heightSize);
    }

    private int resolveAdjustedSize(int desiredSize, int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        switch (specMode) {
            case MeasureSpec.AT_MOST:
                return Math.min(desiredSize, specSize);
            case MeasureSpec.EXACTLY:
                return specSize;
            case MeasureSpec.UNSPECIFIED:
            default:
                return desiredSize;
        }
    }
}
