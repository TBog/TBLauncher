package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SquareImageView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "SqImgView";

    protected int mComputedSize = -1;
    protected boolean mRequestLayout = false;

    public SquareImageView(@NonNull Context context) {
        super(context);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize;
        int heightSize;

        int measuredWidth = getMeasuredWidth();
        int measuredHeight = getMeasuredHeight();

        int size;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        widthSize = MeasureSpec.getSize(widthMeasureSpec);
        heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            size = widthSize;
            mComputedSize = size;
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            size = heightSize;
            mComputedSize = size;
        } else if (heightMode == MeasureSpec.AT_MOST && widthMode == MeasureSpec.UNSPECIFIED) {
            size = mComputedSize > 0 ? mComputedSize : heightSize;
        } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.UNSPECIFIED) {
            size = mComputedSize > 0 ? mComputedSize : widthSize;
        } else {
            size = mComputedSize > 0 ? mComputedSize : Math.min(widthSize, heightSize);
        }

        if (!mRequestLayout && mComputedSize > 0 && (measuredHeight != size || measuredWidth != size)) {
            Log.d(TAG, Integer.toHexString(System.identityHashCode(this))
                + " mark for re-layout"
                + " | measured before " + measuredWidth + "×" + measuredHeight
                + " | size=" + mComputedSize);
            mRequestLayout = true;
        }

        int finalMeasureSpec = MeasureSpec.makeMeasureSpec(size, MeasureSpec.EXACTLY);
        super.onMeasure(finalMeasureSpec, finalMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        int width = right - left;
        int height = bottom - top;
        super.onLayout(changed, left, top, right, bottom);

        if (width == mComputedSize && height == mComputedSize && !mRequestLayout)
            return;

        // we need the pre-draw listener because requestLayout should not be called while in the layout phase
        getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                getViewTreeObserver().removeOnPreDrawListener(this);
                int width = getWidth();
                int height = getHeight();
                if (width != height || width != mComputedSize || mRequestLayout) {
                    Log.d(TAG, Integer.toHexString(System.identityHashCode(SquareImageView.this))
                        + " requesting new layout " + width + "×" + height
                        + " size=" + mComputedSize + " mark=" + mRequestLayout);
                    mRequestLayout = false;
                    requestLayout();
                    return false;
                }
                return true;
            }
        });
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldWidth, int oldHeight) {
        if (oldWidth == oldHeight && oldWidth == mComputedSize) {
            Log.d(TAG, Integer.toHexString(System.identityHashCode(this)) + " onSizeChanged to "
                + w + "×" + h + " from " + oldWidth + "×" + oldHeight + " | reset computed size");
            // reset computed size
            mComputedSize = -1;
        }
    }
}
