package rocks.tbog.tblauncher.utils;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.res.Configuration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
//import android.webkit.WebView;

import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

/**
 * Created by KhanhDuong on 8/14/2015.
 * Author Khanh Duong
 * Email khanhduong@innoria.com
 * Company www.innoria.com
 */
public class DeviceUtils {
    /**
     * get device id base on current system config
     * @param context
     * @return
     */
    public static String getDeviceId(Context context) {
        //get IMEI first
        String imei = getIMEI(context);
        if (!TextUtils.isEmpty(imei)) {
            return encodeMD5(imei);
        }
        Log.e("DeviceUtils", "getDeviceId, IMEI is NULL. start get device serial");
        //get Android device serial for device without telephony
        String serialNum = getDeviceSerial(context);
        if (!TextUtils.isEmpty(serialNum)) {
            return encodeMD5(serialNum);
        }
        Log.e("DeviceUtils", "getDeviceId, SERIAL is NULL. start get device Android_ID");
        //get Android_ID. not trust, it's reseted after factory reset
        String androidId = getAndroidId(context);
        if (!TextUtils.isEmpty(androidId)) {
            return encodeMD5(androidId);
        }
        Log.e("DeviceUtils", "getDeviceId, Can not generate device ID");
        return null;
    }

    /**
     * get device serial id.
     * @param context
     * @return
     */
    @SuppressLint("HardwareIds")
    public static String getDeviceSerial(Context context) {
        String serial = null;

        try {
            @SuppressLint("PrivateApi")
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            serial = (String)get.invoke(c, "ro.serialno");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return serial;
    }

    @Nullable
    private static String getIMEI(Context context) {
//        try {
//            TelephonyManager telephonyManager = (TelephonyManager) context
//                    .getSystemService(Context.TELEPHONY_SERVICE);
//            @SuppressLint("HardwareIds")
//            String imei = telephonyManager.getDeviceId();
//
//            return imei;
//        } catch (SecurityException e) {
//            Log.e("", "TELEPHONY_SERVICE", e);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
        return null;
    }

//    /**
//     * this method will generate the special id with WIFI MAC Address
//     *
//     * @param context
//     * @return
//     */
//    public static String getDeviceIDWithWifiMACAddress(Context context, String saltCode) {
//        if (saltCode == null) {
//            saltCode = "_JonnyKenAndRuby_";
//        }
//        String deviceId = getIMEI(context);
//        try {
//            // DEVICE_ID // Requires READ_PHONE_STATE
//            if (deviceId == null) {
//                deviceId = "";
//            }
//
//            // DEVICE SERIAL
//            String deviceSerial = getDeviceSerial(context);
//            if (deviceSerial == null) {
//                deviceSerial = "";
//            }
//
//            String androidId = getAndroidId(context);
//            if (androidId == null) {
//                androidId = "";
//            }
//
//            // WIFI MAC ADDRESS: android.permission.ACCESS_WIFI_STATE
//            WifiManager wm = (WifiManager)context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//            String wifiMAC = wm.getConnectionInfo().getMacAddress();
//
//            // sum
//            deviceId = deviceId + saltCode + androidId + saltCode + wifiMAC + saltCode + deviceSerial;
//            deviceId = encodeMD5(deviceId);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        Log.d("KST", "DeviceID With WifiMAC: " + deviceId);
//        return deviceId;
//    }

    /**
     * get Android ID of device
     * @param context
     * @return
     */
    public static String getAndroidId(Context context) {
        @SuppressLint("HardwareIds")
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        return androidId;
    }

//    /**
//     * this method will generate the special id with
//     * Bluetooth MAC Address.
//     *
//     * @param context
//     * @return
//     */
//    public static String getDeviceIDWithBluetoothAddress(Context context, String saltCode) {
//        if (saltCode == null) {
//            saltCode = "_JonnyKenAndRuby_";
//        }
//        String deviceId = getIMEI(context);
//        try {
//            // DEVICE_ID // Requires READ_PHONE_STATE
//            if (deviceId == null) {
//                deviceId = "";
//            }
//
//            // DEVICE SERIAL
//            String serial = getDeviceSerial(context);
//            if (serial == null) {
//                serial = "";
//            }
//
//            // Bluetooth MAC address android.permission.BLUETOOTH required
//            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            String bluetoothMAC = bluetoothAdapter.getAddress();
//
//            // sum
//            deviceId = deviceId + saltCode + bluetoothMAC + saltCode + serial;
//            deviceId = encodeMD5(deviceId);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return deviceId;
//    }

    /**
     * help us convert to MD5 hash value.
     * @param text
     * @return
     */
    public static String encodeMD5(String text) {
        MessageDigest m = null;
        try {
            m = MessageDigest.getInstance("MD5");
            m.update(text.getBytes(), 0, text.length());
            byte p_md5Data[] = m.digest();

            String m_szUniqueID = new String();
            for (int i=0;i<p_md5Data.length;i++) {
                int b =  (0xFF & p_md5Data[i]);
                // if it is a single digit, make sure it have 0 in front (proper padding)
                if (b <= 0xF) m_szUniqueID+="0";
                // add number to string
                m_szUniqueID += Integer.toHexString(b);
            }
            m_szUniqueID = m_szUniqueID.toUpperCase(Locale.getDefault());
            return m_szUniqueID;
        } catch (Exception e) {
            e.printStackTrace();
            try {
                return UUID.nameUUIDFromBytes(text.getBytes("utf8")).toString();
            } catch (UnsupportedEncodingException e1) {
                e1.printStackTrace();
            }
        }
        return text;
    }

    /**
     * get screen width
     *
     * @return
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int screenW = outMetrics.widthPixels;

        return screenW;
    }

    /**
     * get screen height
     *
     * @return
     */
    public static int getScreenHeight(Context context) {
        DisplayMetrics outMetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(outMetrics);
        int screenH = outMetrics.heightPixels;

        return screenH;
    }

    /**
     * convert dp values to px
     *
     * @param context
     * @param dp
     * @return
     */
    public static int convertDpToPx(Context context, int dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }

    /**
     * convert px values to dp
     *
     * @param context
     * @param px
     * @return
     */
    public static int convertPxToDp(Context context, int px) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)((px / displayMetrics.density) + 0.5);
    }

    /**
     * convert dp values to px with float type
     *
     * @param context
     * @param dp
     * @return
     */
    public static int convertDpToPx(Context context, float dp) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (int)((dp * displayMetrics.density) + 0.5);
    }

    /**
     * get device name
     * @param context
     * @return
     */
    public static String getDeviceName(Context context) {
        return Build.MODEL;
    }

    /**
     * get URL encoded device name
     * @return
     */
    public static String getURLEncodedDeviceName() {
        String terminalName = Build.MODEL;
        try {
            terminalName = URLEncoder.encode(terminalName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return terminalName;
    }

    /**
     * show device information.
     *
     * @param tagOfLogCat this is for export to logcat.
     * @param context
     */
    public static void showDeviceInfo(String tagOfLogCat, Context context) {
        if (tagOfLogCat == null || tagOfLogCat.equals("")) {
            tagOfLogCat = "Innoria";
        }
        Log.i(tagOfLogCat, "===Start show device information===");
        // resolution.
        int width = DeviceUtils.getScreenWidth(context);
        int height = DeviceUtils.getScreenHeight(context);
        Log.i(tagOfLogCat, "screenW=" + width);
        Log.i(tagOfLogCat, "screenH=" + height);
        // check density.
        Log.i(tagOfLogCat, "density=" + context.getResources().getDisplayMetrics().density);
        Log.i(tagOfLogCat, "scaledDensity="
                + context.getResources().getDisplayMetrics().scaledDensity);
        Log.i(tagOfLogCat, "screenLayoutType=" + getScreenLayoutTypeText(context));

        Log.i(tagOfLogCat, "densityDpi=" + getDensityDpi(context));
        Log.i(tagOfLogCat, "densityType=" + getDensityDpiText(context));
        // check device.
        Log.i(tagOfLogCat, "MANUFACTURER=" + Build.MANUFACTURER);
        Log.i(tagOfLogCat, "DEVICE=" + Build.DEVICE);
        Log.i(tagOfLogCat, "MODEL=" + Build.MODEL);
        Log.i(tagOfLogCat, "PRODUCT=" + Build.PRODUCT);
        Log.i(tagOfLogCat, "DISPLAY=" + Build.DISPLAY);
        Log.i(tagOfLogCat, "BRAND=" + Build.BRAND);
        Log.i(tagOfLogCat, "CPU_ABI=" + Build.CPU_ABI);
        Log.i(tagOfLogCat, "BOARD=" + Build.BOARD);
        Log.i(tagOfLogCat, "SDK INT=" + Build.VERSION.SDK_INT);
        Log.i(tagOfLogCat, "SDK RELEASE=" + Build.VERSION.RELEASE);
        Log.i(tagOfLogCat, "SDK CODENAME=" + Build.VERSION.CODENAME);
        Log.i(tagOfLogCat, "SDK INCREMENTAL=" + Build.VERSION.INCREMENTAL);
        //Log.i(tagOfLogCat, "User Agent=" + new WebView(context).getSettings().getUserAgentString());
        Log.i(tagOfLogCat, "===End show device information===");
    }

    public static int getDensityDpi(Context context) {
        return context.getResources().getDisplayMetrics().densityDpi;
    }

    public static String getDensityDpiText(Context context) {
        int density = getDensityDpi(context);
        String text = "DENSITY_MEDIUM";
        switch (density) {
            case DisplayMetrics.DENSITY_HIGH:
                text = "DisplayMetrics.DENSITY_HIGH";
                break;
            case DisplayMetrics.DENSITY_LOW:
                text = "DisplayMetrics.DENSITY_LOW";
                break;
            case DisplayMetrics.DENSITY_MEDIUM:
                text = "DisplayMetrics.DENSITY_MEDIUM";
                break;
            case DisplayMetrics.DENSITY_TV:
                text = "DisplayMetrics.DENSITY_TV";
                break;
            case DisplayMetrics.DENSITY_XHIGH:
                text = "DisplayMetrics.DENSITY_XHIGH";
                break;
            case DisplayMetrics.DENSITY_XXHIGH:
                text = "DisplayMetrics.DENSITY_XXHIGH";
                break;
            default:
                break;
        }
        return text;
    }
    /**
     * Returns the language code for this Locale or the empty string if no
     * language was set
     *
     * @return
     */
    public static String getLanguageCode() {
        return Locale.getDefault().getLanguage().toLowerCase(Locale.getDefault());
    }

    /**
     * get screen size type
     * @param context
     * @return
     */
    public static int getScreenLayoutType(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK);
    }


    /**
     * get screen size as text.
     * @param context
     * @return
     */
    public static String getScreenLayoutTypeText(Context context) {
        int screenType = getScreenLayoutType(context);
        String screenTypeS = "SCREENLAYOUT_SIZE_UNDEFINED";
        switch (screenType) {
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                screenTypeS = "SCREENLAYOUT_SIZE_LARGE";
                break;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                screenTypeS = "SCREENLAYOUT_SIZE_NORMAL";
                break;
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                screenTypeS = "SCREENLAYOUT_SIZE_SMALL";
                break;
            case 4:
                screenTypeS = "SCREENLAYOUT_SIZE_XLARGE";
                break;
            case Configuration.SCREENLAYOUT_SIZE_UNDEFINED:
                screenTypeS = "SCREENLAYOUT_SIZE_UNDEFINED";
                break;
            default:
                break;
        }
        return screenTypeS;
    }

    /**
     * check this device is tablet or not.
     * @param context
     * @return
     */
    public static boolean isTablet(Context context) {
        return isXLarge(context) | isLarge(context);
    }

    /**
     * check device is large screen
     * @param context
     * @return
     */
    public static boolean isLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE;
    }

    /**
     * check device is Xlarge screen.
     * @param context
     * @return
     */
    public static boolean isXLarge(Context context) {
        return (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == 4; // 4
        // is
        // xlarge
    }

//    /**
//     * check support honey com (sdk_int >= 11) tablet
//     * @return
//     */
//    public static boolean isSupportedHoneycombTablet(Context context) {
//        return hasHoneycomb() && isTablet(context);
//    }
//
//
//    /**
//     * check support froyo
//     * @return
//     */
//    public static boolean hasFroyo() {
//        // Can use static final constants like FROYO, declared in later versions
//        // of the OS since they are inlined at compile time. This is guaranteed
//        // behavior.
//        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
//    }
//
//    public static boolean hasGingerbread() {
//        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
//    }
//
//    /**
//     * check support honey com (sdk_int >= 11)
//     * @return
//     */
//    public static boolean hasHoneycomb() {
//        // return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
//        return Build.VERSION.SDK_INT >= 11;
//    }
//
//    /**
//     * check support honey com (sdk_int >= 12)
//     * @return
//     */
//    public static boolean hasHoneycombMR1() {
//        // return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
//        return Build.VERSION.SDK_INT >= 12;
//    }
//
//    /**
//     * check support Jelly bean (sdk_int >= 16)
//     * @return
//     */
//    public static boolean hasJellyBean() {
//        // return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
//        return Build.VERSION.SDK_INT >= 16;
//    }
}