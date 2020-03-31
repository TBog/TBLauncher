package rocks.tbog.tblauncher.ui;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.view.animation.LinearInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class LoadingDrawable extends Drawable implements Animatable, ValueAnimator.AnimatorUpdateListener {
    private final ArrayList<Shape> mShapeList = new ArrayList<>(0);
    private final Path mShapePath;
    private final Paint mPaint;
    private ValueAnimator mAnimator = null;

    final static private float SHAPE_SIZE_PERCENT = 0.22f;
    final static private float CORNER_SMOOTHING_PERCENT = 0.05f;

    public LoadingDrawable() {
        super();
        mShapePath = new Path();
        mPaint = new Paint();
        mPaint.setColor(0xffffffff);
        //mPaint.setStrokeWidth(3);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(false);
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
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);

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

        // generate shapes
        mShapeList.clear();
        //if (mShapeList.isEmpty())
        {
            int size = rect.width();

            int shapeSize = (int)(size * SHAPE_SIZE_PERCENT);
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
        if (mAnimator == null) {
            mAnimator = ValueAnimator.ofFloat(0, 360);
            mAnimator.setDuration(3000);
            mAnimator.addUpdateListener(this);
            mAnimator.setRepeatCount(ValueAnimator.INFINITE);
            mAnimator.setInterpolator(new LinearInterpolator());
        }

//        if (mAnimator.isPaused())
//            mAnimator.resume();
//        else
        mAnimator.start();
        mPaint.setAntiAlias(true);
    }

    @Override
    public void stop() {
        if (mAnimator != null)
            mAnimator.end();
        mPaint.setAntiAlias(false);
    }

    @Override
    public boolean isRunning() {
        if (mAnimator == null)
            return false;
        return mAnimator.isRunning();
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
        final Rect mRect;
        final Matrix mat = new Matrix();
        final float[] mPoints = new float[8];

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
