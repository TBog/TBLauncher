package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

import rocks.tbog.tblauncher.utils.DeviceUtils;

public class CutoutFactory {

    @Nullable
    public static ICutout getByManufacturer(Context context, String manufacturer) {
        if ("Huawei".equalsIgnoreCase(manufacturer))
            return new HuaweiCutout(context);
        if ("Oppo".equalsIgnoreCase(manufacturer))
            return new OppoCutout(context);
        if ("Vivo".equalsIgnoreCase(manufacturer))
            return new VivoCutout(context);
        if ("Xiaomi".equalsIgnoreCase(manufacturer))
            return new XiaomiCutout(context);

        return null;
    }

    @TargetApi(Build.VERSION_CODES.P)
    @Nullable
    public static ICutout getForAndroidPie(Activity activity) {
        WindowInsets windowInsets =  activity.getWindow().getDecorView().getRootWindowInsets();
        if ( windowInsets == null ) {
            // getRootWindowInsets() must be called after onAttachedToWindow()
            return null;
        }
        final DisplayCutout displayCutout = windowInsets.getDisplayCutout();
        if (displayCutout == null)
            return null;
        return new AndroidPCutout(displayCutout);
    }

    @NonNull
    public static ICutout getStatusBar(Context context) {
        return new StatusBarCutout(context);
    }

    @NonNull
    public static ICutout getNoCutout()
    {
        return new NoCutout();
    }

    private static abstract class ComputeSafeZoneFromCutout implements ICutout {
        @NonNull
        protected final Context context;

        ComputeSafeZoneFromCutout(@NonNull Context context) {
            this.context = context;
        }

        @Override
        public Rect getSafeZone() {
            Rect safe = new Rect(0, 0, 0, 0);
            Rect[] cutout = getCutout();

            if (cutout.length == 1) {
                // assume one notch in screen top and phone in portrait
                safe.top = cutout[0].top + cutout[0].bottom;
            }
//            else if (cutout.length > 1) {
//                throw new RuntimeException();  // not implemented yet.
//            }
            return safe;
        }
    }

    // Huawei https://developer.huawei.com/consumer/cn/devservice/doc/50114
    private static class HuaweiCutout extends ComputeSafeZoneFromCutout {

        HuaweiCutout(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean hasCutout() {
            try {
                ClassLoader classLoader = context.getClassLoader();
                Class class_HwNotchSizeUtil = classLoader.loadClass("com.huawei.android.util.HwNotchSizeUtil");
                @SuppressWarnings("unchecked")
                Method method_hasNotchInScreen = class_HwNotchSizeUtil.getMethod("hasNotchInScreen");
                return (boolean) method_hasNotchInScreen.invoke(class_HwNotchSizeUtil);
            } catch (Exception e) {
                //ignored
            }
            return false;
        }

        @Override
        public Rect[] getCutout() {
            try {
                ClassLoader classLoader = context.getClassLoader();
                Class class_HwNotchSizeUtil = classLoader.loadClass("com.huawei.android.util.HwNotchSizeUtil");
                @SuppressWarnings("unchecked")
                Method method_getNotchSize = class_HwNotchSizeUtil.getMethod("getNotchSize");

                int[] size = (int[]) method_getNotchSize.invoke(class_HwNotchSizeUtil);
                @SuppressWarnings("ConstantConditions")
                int notchWidth = size[0];
                int notchHeight = size[1];
                int screenWidth = DeviceUtils.getScreenWidth(context);

                int x = (screenWidth - notchWidth) >> 1;
                int y = 0;
                Rect rect = new Rect(x, y, x + notchWidth, y + notchHeight);
                return new Rect[]{rect};
            } catch (Exception e) {
                //ignored
            }
            return new Rect[0];
        }
    }

    // Oppo https://open.oppomobile.com/wiki/doc#id=10159
    private static class OppoCutout extends ComputeSafeZoneFromCutout {

        OppoCutout(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean hasCutout() {
            String CutoutFeature = "com.oppo.feature.screen.heteromorphism";
            return context.getPackageManager().hasSystemFeature(CutoutFeature);
        }

        @Override
        public Rect[] getCutout() {
            String value = System.getProperty("ro.oppo.screen.heteromorphism");
            @SuppressWarnings("ConstantConditions")
            String[] texts = value.split("[,:]");
            int[] values = new int[texts.length];

            try {
                for (int i = 0; i < texts.length; ++i)
                    values[i] = Integer.parseInt(texts[i]);
            } catch (NumberFormatException e) {
                values = null;
            }

            if (values != null && values.length == 4) {
                Rect rect = new Rect();
                rect.left = values[0];
                rect.top = values[1];
                rect.right = values[2];
                rect.bottom = values[3];

                return new Rect[]{rect};
            }

            return new Rect[0];
        }
    }

    // Vivo https://dev.vivo.com.cn/documentCenter/doc/145
    private static class VivoCutout extends ComputeSafeZoneFromCutout {

        VivoCutout(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean hasCutout() {
            try {
                ClassLoader clazz = context.getClassLoader();
                @SuppressLint("PrivateApi")
                Class ftFeature = clazz.loadClass("android.util.FtFeature");
                Method[] methods = ftFeature.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equalsIgnoreCase("isFeatureSupport")) {
                        int NOTCH_IN_SCREEN = 0x00000020;  // 表示是否有凹槽
                        int ROUNDED_IN_SCREEN = 0x00000008;  // 表示是否有圆角
                        return (boolean) method.invoke(ftFeature, NOTCH_IN_SCREEN);
                    }
                }
            } catch (Exception e) {
                //ignored
            }
            return false;
        }

        @Override
        public Rect[] getCutout() {
            // throw new RuntimeException();  // not implemented yet.
            return new Rect[0];
        }
    }

    // Xiaomi
    // Oreo: https://dev.mi.com/console/doc/detail?pId=1293
    // Pie: https://dev.mi.com/console/doc/detail?pId=1341
    private static class XiaomiCutout extends ComputeSafeZoneFromCutout {

        XiaomiCutout(@NonNull Context context) {
            super(context);
        }

        @Override
        public boolean hasCutout() {
            // `getprop ro.miui.notch` output 1 if it's a notch screen.
            String text = System.getProperty("ro.miui.notch");
            return "1".equals(text);
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public Rect[] getCutout() {
            Resources res = context.getResources();
            int widthResId = res.getIdentifier("notch_width", "dimen", "android");
            int heightResId = res.getIdentifier("notch_height", "dimen", "android");
            if (widthResId > 0 && heightResId > 0) {
                int notchWidth = res.getDimensionPixelSize(widthResId);
                int notchHeight = res.getDimensionPixelSize(heightResId);

                // one notch in screen top
                int screenWidth = DeviceUtils.getScreenWidth(context);
                int left = (screenWidth - notchWidth) >> 1;
                int right = left + notchWidth;
                int top = 0;
                int bottom = notchHeight;
                Rect rect = new Rect(left, top, right, bottom);
                return new Rect[]{rect};
            }

            return new Rect[0];
        }
    }

    // In case some manufactures' not coming up with a getNotchHeight() method, you can just use the status bar's height. Android has guarantee that notch height is at most the status bar height.
    private static class StatusBarCutout extends ComputeSafeZoneFromCutout {

        StatusBarCutout(@NonNull Context context) {
            super(context);
        }

        static int getStatusBarHeight(Context context) {
            int statusBarHeight = 0;
            Resources res = context.getResources();
            int resourceId = res.getIdentifier("status_bar_height", "dimen", "android");
            if (resourceId > 0) {
                statusBarHeight = res.getDimensionPixelSize(resourceId);
            }
            return statusBarHeight;
        }

        @Override
        public boolean hasCutout() {
            return true;
        }

        @Override
        public Rect[] getCutout() {
            return new Rect[]{new Rect(0, 0, 0, getStatusBarHeight(context))};
        }
    }

    // Android P cutout
    @TargetApi(Build.VERSION_CODES.P)
    private static class AndroidPCutout implements ICutout {

        @NonNull
        final Rect safeInset;

        AndroidPCutout(@NonNull DisplayCutout displayCutout) {
            int left = displayCutout.getSafeInsetLeft();
            int top = displayCutout.getSafeInsetTop();
            int right = displayCutout.getSafeInsetRight();
            int bottom = displayCutout.getSafeInsetBottom();
            safeInset = new Rect(left, top, right, bottom);
        }

        @Override
        public boolean hasCutout() {
            return safeInset.top != 0 || safeInset.bottom != 0 || safeInset.left != 0 || safeInset.right != 0;
        }

        @Override
        public Rect[] getCutout() {
            // throw new RuntimeException();  // not implemented yet. Should return displayCutout.getBoundingRectsAll()
            return new Rect[0];
        }

        @Override
        public Rect getSafeZone() {
            return safeInset;
        }
    }

    private static class NoCutout implements ICutout {
        @Override
        public boolean hasCutout() {
            return false;
        }

        @Override
        public Rect[] getCutout() {
            return new Rect[0];
        }

        @Override
        public Rect getSafeZone() {
            return new Rect(0, 0, 0, 0);
        }
    }
}
