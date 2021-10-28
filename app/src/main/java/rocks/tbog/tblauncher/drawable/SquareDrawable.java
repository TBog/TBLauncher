package rocks.tbog.tblauncher.drawable;

import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * This drawable is best used when the view is set to have
 * adjustViewBounds="true"
 * scaleType="fitCenter"
 * either width or height have a size or match_parent
 * Set Intrinsic size to 1x1 if you want to use scaleType="fitXY" in the view
 */

public abstract class SquareDrawable extends Drawable {
    protected final Paint mPaint;

//    @Override
//    public int getIntrinsicWidth() {
//        return 1;
//    }
//
//    @Override
//    public int getIntrinsicHeight() {
//        return 1;
//    }

    public SquareDrawable() {
        super();
        mPaint = new Paint();
        mPaint.setColor(0xffffffff);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(false);
    }

    @Override
    public void setAlpha(int alpha) {
        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    protected Rect getCenterRect(@NonNull Rect bounds) {
        Rect rect = new Rect();
        rect.set(bounds);

        // make it a square and center the content
        if (rect.width() != rect.height()) {
            int size = Math.min(rect.width(), rect.height());
            int rad = size / 2;
            // compute width
            rect.left = rect.centerX() - rad;
            rect.right = rect.left + size;
            // compute height
            rect.top = rect.centerY() - rad;
            rect.bottom = rect.top + size;
        }

        return rect;
    }
}
