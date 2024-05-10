package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SquareImageView extends androidx.appcompat.widget.AppCompatImageView {
    private static final String TAG = "SqImgView";

    protected int mComputedSize = -1;
    //protected boolean mRequestLayout = false;

    public SquareImageView(@NonNull Context context) {
        super(context);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    interface ToString<T> {
        @NonNull
        String fromMode(T input);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int size;
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        ToString<Integer> toString = input -> {
            switch (MeasureSpec.getMode(input)) {
                case MeasureSpec.UNSPECIFIED:
                    return "UNSPECIFIED";
                case MeasureSpec.EXACTLY:
                    return "EXACTLY";
                case MeasureSpec.AT_MOST:
                    return "AT_MOST";
                default:
                    return Integer.toString(input);
            }
        };
        // Log.v(TAG, Integer.toHexString(System.identityHashCode(this))
        //     + " measured=" + getMeasuredWidth() + "x" + getMeasuredHeight()
        //     + "\n\t"
        //     + " widthMode=" + toString.fromMode(widthMode)
        //     + " widthSize=" + widthSize
        //     + "\n\t"
        //     + " heightMode=" + toString.fromMode(heightMode)
        //     + " heightSize=" + heightSize
        // );
        if (widthMode == MeasureSpec.EXACTLY && widthSize > 0) {
            size = widthSize;
            mComputedSize = size;
        } else if (heightMode == MeasureSpec.EXACTLY && heightSize > 0) {
            size = heightSize;
            mComputedSize = size;
        } else if (heightMode == MeasureSpec.AT_MOST && widthMode == MeasureSpec.UNSPECIFIED) {
            if (mComputedSize > 0) {
                mComputedSize = Math.min(mComputedSize, heightSize);
                size = mComputedSize;
            } else {
                size = heightSize;
            }
        } else if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.UNSPECIFIED) {
            if (mComputedSize > 0) {
                mComputedSize = Math.min(widthSize, mComputedSize);
                size = mComputedSize;
            } else {
                size = widthSize;
            }
        } else {
            int minSize = Math.min(widthSize, heightSize);
            mComputedSize = Math.min(mComputedSize, minSize);
            size = mComputedSize > 0 ? mComputedSize : minSize;
        }

        final int widthSizeAndState = resolveSizeAndState(size, widthMeasureSpec, 0);
        final int heightSizeAndState = resolveSizeAndState(size, heightMeasureSpec, 0);
        widthSize = widthSizeAndState & MEASURED_SIZE_MASK;
        heightSize = heightSizeAndState & MEASURED_SIZE_MASK;

        if ((widthSizeAndState & MEASURED_STATE_TOO_SMALL) != 0 || (heightSizeAndState & MEASURED_STATE_TOO_SMALL) != 0) {
            Log.d(TAG, Integer.toHexString(System.identityHashCode(this))
                + " mark for re-layout"
                + " | too small " + widthSize + "×" + heightSize
                + " | size=" + size);
            setMeasuredDimension(widthSizeAndState, heightSizeAndState);
            post(this::requestLayout);
            return;
        }

        int finalWidthSpec;
        int finalHeightSpec;

        if (mComputedSize > 0) {
            finalWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);
            finalHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.EXACTLY);
        } else {
            Log.d(TAG, Integer.toHexString(System.identityHashCode(this))
                + " AT_MOST " + widthSize + "×" + heightSize
                + " | size=" + size);
            finalWidthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.AT_MOST);
            finalHeightSpec = MeasureSpec.makeMeasureSpec(heightSize, MeasureSpec.AT_MOST);
        }

        // let super method call `setMeasuredDimension`
        super.onMeasure(finalWidthSpec, finalHeightSpec);

        if (mComputedSize > 0 && (getMeasuredWidth() != size || getMeasuredHeight() != size)) {
            Log.d(TAG, Integer.toHexString(System.identityHashCode(this))
                + " mark for re-layout"
                + " | measured at " + getMeasuredWidth() + "×" + getMeasuredHeight()
                + " | " + toString.fromMode(finalWidthSpec) + " " + widthSize + "×" + heightSize
                + " | size=" + size);
            post(this::requestLayout);
        }
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
