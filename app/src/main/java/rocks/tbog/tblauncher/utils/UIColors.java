package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

public class UIColors {
    public static final int COLOR_DEFAULT = 0xFF3cb371;
    private static int CACHED_COLOR_HIGHLIGHT = 0;
    private static int CACHED_COLOR_RESULT_TEXT = 0;
    private static int CACHED_COLOR_RESULT_TEXT2 = 0;
    private static int CACHED_COLOR_QL_TOGGLE = 0;
    private static int CACHED_COLOR_CONTACT_ACTION = 0;
    private static int CACHED_COLOR_SEARCH_TEXT = 0;
    private static int CACHED_COLOR_SEARCH_ICON = 0;

    public static void resetCache() {
        CACHED_COLOR_HIGHLIGHT = 0;
        CACHED_COLOR_RESULT_TEXT = 0;
        CACHED_COLOR_RESULT_TEXT2 = 0;
        CACHED_COLOR_QL_TOGGLE = 0;
        CACHED_COLOR_CONTACT_ACTION = 0;
        CACHED_COLOR_SEARCH_TEXT = 0;
        CACHED_COLOR_SEARCH_ICON = 0;
    }

    public static int getDefaultColor(Context context) {
        return COLOR_DEFAULT;
    }

    @ColorInt
    public static int getColor(SharedPreferences pref, String key) {
        return pref.getInt(key, COLOR_DEFAULT);
    }

    public static int getAlpha(SharedPreferences pref, String key) {
        return pref.getInt(key, 0xFF) & 0xFF;
    }

    public static int setAlpha(int color, int alpha) {
        return color & 0x00ffffff | ((alpha & 0xFF) << 24);
    }

    /**
     * Returns the relative luminance of a color.
     * Code adapted from <i>calculateLuminance(@ColorInt int color)</i>
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java
     *
     * @return a value between 0 (darkest black) and 1 (lightest white)
     */
    public static double luminance(@ColorInt int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Convert RGB components to its CIE XYZ representative components.
        double sr = r / 255.0;
        sr = sr < 0.04045 ? sr / 12.92 : Math.pow((sr + 0.055) / 1.055, 2.4);
        double sg = g / 255.0;
        sg = sg < 0.04045 ? sg / 12.92 : Math.pow((sg + 0.055) / 1.055, 2.4);
        double sb = b / 255.0;
        sb = sb < 0.04045 ? sb / 12.92 : Math.pow((sb + 0.055) / 1.055, 2.4);

        return (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
    }

    public static void setStatusBarColor(AppCompatActivity compatActivity, @ColorInt int notificationBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = compatActivity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(notificationBarColor);
        }

        ActionBar actionBar = compatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(notificationBarColor));
        }
    }

    public static int getResultHighlightColor(Context context) {
        if (CACHED_COLOR_HIGHLIGHT == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = getColor(pref, "result-highlight-color");
            CACHED_COLOR_HIGHLIGHT = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_HIGHLIGHT;
    }

    public static int getResultTextColor(Context context) {
        if (CACHED_COLOR_RESULT_TEXT == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = pref.getInt("result-text-color", 0xffffff);
            CACHED_COLOR_RESULT_TEXT = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_RESULT_TEXT;
    }

    public static int getResultText2Color(Context context) {
        if (CACHED_COLOR_RESULT_TEXT2 == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = pref.getInt("result-text2-color", 0xbbffbb);
            CACHED_COLOR_RESULT_TEXT2 = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_RESULT_TEXT2;
    }

    public static int getQuickListToggleColor(Context context) {
        if (CACHED_COLOR_QL_TOGGLE == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = getColor(pref, "quick-list-toggle-color");
            CACHED_COLOR_QL_TOGGLE = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_QL_TOGGLE;
    }

    public static int getContactActionColor(Context context) {
        if (CACHED_COLOR_CONTACT_ACTION == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = getColor(pref, "contact-action-color");
            CACHED_COLOR_CONTACT_ACTION = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_CONTACT_ACTION;
    }

    public static int getSearchTextColor(Context context) {
        if (CACHED_COLOR_SEARCH_TEXT == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = getColor(pref, "search-bar-text-color");
            CACHED_COLOR_SEARCH_TEXT = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_SEARCH_TEXT;
    }

    public static int getSearchIconColor(Context context) {
        if (CACHED_COLOR_SEARCH_ICON == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int highlightColor = getColor(pref, "search-bar-icon-color");
            CACHED_COLOR_SEARCH_ICON = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_SEARCH_ICON;
    }
}
