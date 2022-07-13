package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UISizes;

public class CustomizeShadowView extends View {
    private final Rect targetRect = new Rect();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float offsetX = 0f;
    private float offsetY = 0f;
    private float shadowRadius;
    private int shadowColor;
    private int textColor;
    private float textSize;
    private int bgColor1;
    private int bgColor2;
    private final int _gridSize;
    private final float _offsetScale = 5f;
    private final int _padding;
    private final int _width;
    private final int _height;
    private final float _sampleRadius;
    private final float _sampleFrameRadius;
    private final float _sampleShadowRadius;
    private String text;
    private OnOffsetChanged onOffsetChanged = null;
    private final int colorSampleFrame;
    private final int colorSampleShadow;

    public interface OnOffsetChanged {
        void onOffsetChanged(float dx, float dy);
    }

    public CustomizeShadowView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomizeShadowView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        _gridSize = getResources().getInteger(R.integer.shadow_preview_grid);
        _padding = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_panel_margin);
        _width = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_hsv_size) + _padding * 2;
        _height = getResources().getDimensionPixelSize(R.dimen.mm2d_cc_hsv_size) + _padding * 2;
        _sampleRadius = getResources().getDimension(R.dimen.mm2d_cc_sample_radius);
        _sampleFrameRadius = _sampleRadius + getResources().getDimension(R.dimen.mm2d_cc_sample_frame);
        _sampleShadowRadius = _sampleFrameRadius + getResources().getDimension(R.dimen.mm2d_cc_sample_shadow);
        colorSampleFrame = getResources().getColor(R.color.mm2d_cc_sample_frame);
        colorSampleShadow = getResources().getColor(R.color.mm2d_cc_sample_shadow);

        shadowRadius = 5f;
        shadowColor = 0xFF000000;
        textColor = 0xFFffffff;
        textSize = UISizes.sp2px(context, getResources().getInteger(R.integer.default_size_text));
        text = getResources().getText(R.string.shadow_offset_preview).toString();

        paint.setTextAlign(Paint.Align.CENTER);
        setLayerType(LAYER_TYPE_SOFTWARE, paint);

        bgColor1 = shadowColor;
        bgColor2 = textColor;
    }

    public void setShadowParameters(float radius, float dx, float dy, int color) {
        shadowRadius = radius;
        offsetX = dx;
        offsetY = dy;
        shadowColor = color | 0xFF000000;
        invalidate();
    }

    public void setTextParameters(@Nullable CharSequence text, int color, int size) {
        if (text != null)
            this.text = text.toString();
        textColor = color;
        textSize = size;
        invalidate();
    }

    public void setOnOffsetChanged(OnOffsetChanged listener) {
        onOffsetChanged = listener;
    }

    public void setBackgroundParameters(int bgColor1, int bgColor2) {
        this.bgColor1 = bgColor1;
        this.bgColor2 = bgColor2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (targetRect.isEmpty())
            targetRect.set(0, 0, getWidth(), getHeight());

        paint.setColor(bgColor1);
        canvas.drawRect(targetRect, paint);

        //draw checker board
        {
            float size = targetRect.width() / (float) _gridSize;
            paint.setColor(bgColor2);
            for (int y = 0; y < _gridSize; y += 1)
                for (int x = 0; x < _gridSize; x += 1)
                    if ((x + y) % 2 == 0) {
                        float cX = targetRect.left + x * size;
                        float cY = targetRect.top + y * size;
                        canvas.drawRect(cX, cY, cX + size, cY + size, paint);
                    }
        }

        paint.setShadowLayer(shadowRadius, offsetX, offsetY, shadowColor);

        final float centerX = targetRect.centerX();
        // draw small text
        paint.setColor(textColor);
        paint.setTextSize(textSize);
        canvas.drawText(text, centerX, targetRect.top - paint.ascent(), paint);
        canvas.drawText(text, centerX, targetRect.bottom - paint.descent(), paint);

        // prepare big text
        String[] lines = text.split("\\s");
        paint.setTextSize(targetRect.height() / (lines.length + 2f));
        final float lineHeight = paint.descent() - paint.ascent();
        float lineY = targetRect.centerY() - (lineHeight * lines.length) / 2f;
        // write big text split by whitespace
        for (String line : lines) {
            lineY += lineHeight;
            canvas.drawText(line, centerX, lineY, paint);
        }

        paint.clearShadowLayer();

        // draw offset circle
        var x = (offsetX / _offsetScale * .5f + .5f) * targetRect.width() + targetRect.left;
        var y = (offsetY / _offsetScale * .5f + .5f) * targetRect.height() + targetRect.top;

        paint.setColor(colorSampleShadow);
        canvas.drawCircle(x, y, _sampleShadowRadius, paint);
        paint.setColor(colorSampleFrame);
        canvas.drawCircle(x, y, _sampleFrameRadius, paint);
        paint.setColor(shadowColor);
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
        offsetX = (offsetX * 2f - 1f) * _offsetScale;
        offsetY = (offsetY * 2f - 1f) * _offsetScale;

        // round values to 1/10
        offsetX = Math.round(offsetX * 10f) * .1f;
        offsetY = Math.round(offsetY * 10f) * .1f;

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
