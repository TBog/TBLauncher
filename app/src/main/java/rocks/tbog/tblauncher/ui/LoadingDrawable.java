package rocks.tbog.tblauncher.ui;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.Animatable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;

import java.util.ArrayList;

/**
 * This drawable is best used when the view is set to have
 * adjustViewBounds="true"
 * scaleType="fitCenter"
 * either width or height have a size or match_parent
 * Set Intrinsic size to 1x1 if you want to use scaleType="fitXY" in the view
 */

public class LoadingDrawable extends SquareDrawable implements Animatable, ValueAnimator.AnimatorUpdateListener {
    private static final float SHAPE_SIZE_PERCENT = 0.22f;
    //private static final float CORNER_SMOOTHING_PERCENT = 0.05f;

    private final ArrayList<Shape> mShapeList = new ArrayList<>(0);
    private final Path mShapePath;
    private ValueAnimator mShapeListAnimator = null;

    public LoadingDrawable() {
        super();
        mShapePath = new Path();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
//        mPaint.setColor(0x7F0000ff);
//        canvas.drawRect(0, 0, (float)canvas.getWidth(), (float)canvas.getHeight(), mPaint);
//        mPaint.setColor(0x7Fff0000);
//        canvas.drawRect(mRect, mPaint);
//        mPaint.setColor(0x7F00ff00);
        //mPaint.setPathEffect(new CornerPathEffect(mRect.width() * CORNER_SMOOTHING_PERCENT));
        canvas.drawPath(mShapePath, mPaint);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

        Rect rect = getCenterRect(bounds);

        // generate shapes
        mShapeList.clear();
        //if (mShapeList.isEmpty())
        {
            int size = rect.width();

            int shapeSize = (int) (size * SHAPE_SIZE_PERCENT);
            int padding = (size - 3 * shapeSize) / 6;

            mShapeList.ensureCapacity(3 * 3);

            int posY = rect.top + padding;
            for (int x = 0; x < 3; x += 1) {
                int posX = rect.left + padding;
                for (int y = 0; y < 3; y += 1) {
                    mShapeList.add(new Shape(shapeSize, shapeSize, posX, posY));
                    posX += padding + padding + shapeSize;
                }
                posY += padding + padding + shapeSize;
            }

            updatePath(0f);
        }
    }

    @Override
    public void start() {
        if (mShapeListAnimator == null) {
            mShapeListAnimator = ValueAnimator.ofFloat(0, 360);
            mShapeListAnimator.setDuration(3000);
            mShapeListAnimator.addUpdateListener(this);
            mShapeListAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mShapeListAnimator.setInterpolator(new LinearInterpolator());
        }

        if (mShapeListAnimator.isRunning())
            return;

//        if (mAnimator.isPaused())
//            mAnimator.resume();
//        else
        mShapeListAnimator.start();
        mPaint.setAntiAlias(true);
    }

    @Override
    public void stop() {
        if (mShapeListAnimator != null)
            mShapeListAnimator.end();
        mPaint.setAntiAlias(false);
        invalidateSelf();
    }

    @Override
    public boolean isRunning() {
        if (mShapeListAnimator == null)
            return false;
        return mShapeListAnimator.isRunning();
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        float value = (Float) animation.getAnimatedValue();
        updatePath(value);
    }

    private void updatePath(float value) {
        mShapePath.reset();
        for (Shape shape : mShapeList)
            shape.addToPath(mShapePath, value);

        invalidateSelf();
    }

    private static class Shape {
        private final Rect mRect;
        private final Matrix mat = new Matrix();
        private final float[] mPoints = new float[8];

        Shape(int width, int height, int posX, int posY) {
            mRect = new Rect(0, 0, width, height);
            mRect.offset(posX, posY);
        }

        void addToPath(Path path, float angle) {
            // top-left
            mPoints[0] = mRect.left;
            mPoints[1] = mRect.top;
            // top-right
            mPoints[2] = mRect.right;
            mPoints[3] = mRect.top;
            // bottom-right
            mPoints[4] = mRect.right;
            mPoints[5] = mRect.bottom;
            // bottom-left
            mPoints[6] = mRect.left;
            mPoints[7] = mRect.bottom;

            //mat.reset();
            mat.setRotate(angle, mRect.centerX(), mRect.centerY());
            mat.mapPoints(mPoints);

            path.moveTo(mPoints[0], mPoints[1]);
            path.lineTo(mPoints[2], mPoints[3]);
            path.lineTo(mPoints[4], mPoints[5]);
            path.lineTo(mPoints[6], mPoints[7]);
            path.close();
        }
    }
}
