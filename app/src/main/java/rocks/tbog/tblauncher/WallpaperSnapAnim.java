package rocks.tbog.tblauncher;

import android.graphics.Point;
import android.graphics.PointF;
import android.view.VelocityTracker;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Transformation;

import androidx.annotation.Nullable;

class WallpaperSnapAnim extends Animation {
    private final LiveWallpaper liveWallpaper;
    final PointF mStartOffset = new PointF();
    final PointF mDeltaOffset = new PointF();
    final PointF mVelocity = new PointF();

    WallpaperSnapAnim(LiveWallpaper liveWallpaper) {
        super();
        this.liveWallpaper = liveWallpaper;
        setDuration(500);
        setInterpolator(new DecelerateInterpolator());
    }

    boolean init(@Nullable VelocityTracker velocityTracker) {
        if (velocityTracker == null) {
            mVelocity.set(0.f, 0.f);
        } else {
            mVelocity.set(velocityTracker.getXVelocity(), velocityTracker.getYVelocity());
        }
        //Log.d(TAG, "mVelocity=" + String.format(Locale.US, "%.2f", mVelocity));

        mStartOffset.set(liveWallpaper.getWallpaperOffset());
        //Log.d(TAG, "mStartOffset=" + String.format(Locale.US, "%.2f", mStartOffset));
        final Point windowSize = liveWallpaper.getWindowSize();
        final float expectedPosX = -Math.min(Math.max(mVelocity.x / windowSize.x, -.5f), .5f) + mStartOffset.x;
        final float expectedPosY = -Math.min(Math.max(mVelocity.y / windowSize.y, -.5f), .5f) + mStartOffset.y;
        //Log.d(TAG, "expectedPos=" + String.format(Locale.US, "%.2f %.2f", expectedPosX, expectedPosY));

        SnapInfo si = new SnapInfo(liveWallpaper.isPreferenceWPStickToSides(), liveWallpaper.isPreferenceWPReturnCenter());
        si.init(expectedPosX, expectedPosY);
        si.removeDiagonals(expectedPosX, expectedPosY);

        // compute offset based on stick location
        if (si.stickToTop)
            mDeltaOffset.y = 0.f - mStartOffset.y;
        else if (si.stickToBottom)
            mDeltaOffset.y = 1.f - mStartOffset.y;
        else if (si.stickToCenter)
            mDeltaOffset.y = .5f - mStartOffset.y;

        if (si.stickToLeft)
            mDeltaOffset.x = 0.f - mStartOffset.x;
        else if (si.stickToRight)
            mDeltaOffset.x = 1.f - mStartOffset.x;
        else if (si.stickToCenter)
            mDeltaOffset.x = .5f - mStartOffset.x;

        return si.stickToLeft || si.stickToTop || si.stickToRight || si.stickToBottom || si.stickToCenter;
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        float offsetX = mStartOffset.x + mDeltaOffset.x * interpolatedTime;
        float offsetY = mStartOffset.y + mDeltaOffset.y * interpolatedTime;
        float velocityInterpolator = (float) Math.sqrt(interpolatedTime) * 3.f;
        final Point windowSize = liveWallpaper.getWindowSize();
        if (velocityInterpolator < 1.f) {
            offsetX -= mVelocity.x / windowSize.x * velocityInterpolator;
            offsetY -= mVelocity.y / windowSize.y * velocityInterpolator;
        } else {
            offsetX -= mVelocity.x / windowSize.x * (1.f - 0.5f * (velocityInterpolator - 1.f));
            offsetY -= mVelocity.y / windowSize.y * (1.f - 0.5f * (velocityInterpolator - 1.f));
        }
        liveWallpaper.updateWallpaperOffset(offsetX, offsetY);
    }

    private static class SnapInfo {
        public final boolean stickToSides;
        public final boolean stickToCenter;

        public boolean stickToLeft;
        public boolean stickToTop;
        public boolean stickToRight;
        public boolean stickToBottom;

        public SnapInfo(boolean sidesSnap, boolean centerSnap) {
            stickToSides = sidesSnap;
            stickToCenter = centerSnap;
        }

        public void init(float x, float y) {
            // if we stick only to the center
            float leftStickPercent = -1.f;
            float topStickPercent = -1.f;
            float rightStickPercent = 2.f;
            float bottomStickPercent = 2.f;

            if (stickToSides && stickToCenter) {
                // if we stick to the left, right and center
                leftStickPercent = .2f;
                topStickPercent = .2f;
                rightStickPercent = .8f;
                bottomStickPercent = .8f;
            } else if (stickToSides) {
                // if we stick only to the center
                leftStickPercent = .5f;
                topStickPercent = .5f;
                rightStickPercent = .5f;
                bottomStickPercent = .5f;
            }

            stickToLeft = x <= leftStickPercent;
            stickToTop = y <= topStickPercent;
            stickToRight = x >= rightStickPercent;
            stickToBottom = y >= bottomStickPercent;
        }

        public void removeDiagonals(float x, float y) {
            if (stickToTop) {
                // don't stick to the top-left or top-right corner
                if (stickToLeft) {
                    stickToLeft = x < y;
                    stickToTop = !stickToLeft;
                } else if (stickToRight) {
                    stickToRight = (1.f - x) < y;
                    stickToTop = !stickToRight;
                }
            } else if (stickToBottom) {
                // don't stick to the bottom-left or bottom-right corner
                if (stickToLeft) {
                    stickToLeft = x < y;
                    stickToBottom = !stickToLeft;
                } else if (stickToRight) {
                    stickToRight = (1.f - x) < y;
                    stickToBottom = !stickToRight;
                }
            }
        }
    }
}
