package rocks.tbog.tblauncher.utils;

import android.util.Log;
import android.view.GestureDetector;

import androidx.core.view.GestureDetectorCompat;

import java.lang.reflect.Field;

public final class GestureDetectorHelper {
    private static final String TAG = "TBUtil";

    public static void setGestureDetectorTouchSlop(GestureDetector gestureDetector, int value) {
        try {
            Field f_mTouchSlopSquare = GestureDetector.class.getDeclaredField("mTouchSlopSquare");
            f_mTouchSlopSquare.setAccessible(true);
            f_mTouchSlopSquare.setInt(gestureDetector, value * value);
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.w(TAG, gestureDetector.toString(), e);
        }
    }

    public static void setGestureDetectorTouchSlop(GestureDetectorCompat gestureDetector, int value) {
        try {
            Field f_mImpl = GestureDetectorCompat.class.getDeclaredField("mImpl");
            f_mImpl.setAccessible(true);
            Object mImpl = f_mImpl.get(gestureDetector);
            if (mImpl == null) {
                Log.w(TAG, f_mImpl + " is null");
                return;
            }
            Class<?> c_GDCIJellybeanMr2 = null;
            Class<?> c_GDCIBase = null;
            try {
                c_GDCIJellybeanMr2 = Class.forName(GestureDetectorCompat.class.getName() + "$GestureDetectorCompatImplJellybeanMr2");
                c_GDCIBase = Class.forName(GestureDetectorCompat.class.getName() + "$GestureDetectorCompatImplBase");
            } catch (ClassNotFoundException ignored) {
            }
            if (c_GDCIJellybeanMr2 != null && c_GDCIJellybeanMr2.isInstance(mImpl)) {
                Field f_mDetector = c_GDCIJellybeanMr2.getDeclaredField("mDetector");
                f_mDetector.setAccessible(true);

                Object mDetector = f_mDetector.get(mImpl);
                if (mDetector instanceof GestureDetector)
                    setGestureDetectorTouchSlop((GestureDetector) mDetector, value);
            } else if (c_GDCIBase != null) {
                Field f_mTouchSlopSquare = c_GDCIBase.getDeclaredField("mTouchSlopSquare");
                f_mTouchSlopSquare.setAccessible(true);
                f_mTouchSlopSquare.setInt(mImpl, value * value);
            } else {
                Log.w(TAG, "not handled: " + mImpl.getClass().toString());
            }
        } catch (NoSuchFieldException | IllegalAccessException | NullPointerException e) {
            Log.w(TAG, gestureDetector.getClass().toString(), e);
        }
    }
}
