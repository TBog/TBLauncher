package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.ColorUtils;
import androidx.preference.PreferenceManager;

public final class UIColors {
    public static final int COLOR_DEFAULT = 0xFF3cb371;
    private static int CACHED_SYSTEM_ACCENT = 0;
    private static int CACHED_COLOR_HIGHLIGHT = 0;
    private static int CACHED_COLOR_RESULT_TEXT = 0;
    private static int CACHED_COLOR_RESULT_TEXT2 = 0;
    private static int CACHED_COLOR_QL_TOGGLE = 0;
    private static int CACHED_RIPPLE_QL = 0;
    private static int CACHED_COLOR_CONTACT_ACTION = 0;
    private static int CACHED_COLOR_SEARCH_TEXT = 0;
    private static int CACHED_COLOR_SEARCH_ICON = 0;
    private static Integer CACHED_BACKGROUND_RESULT_LIST = null;
    private static int CACHED_RIPPLE_RESULT_LIST = 0;
    private static Integer CACHED_BACKGROUND_ICON = null;
    private static Integer CACHED_COLOR_POPUP_BORDER = null;
    private static Integer CACHED_COLOR_POPUP_BACKGROUND = null;
    private static int CACHED_RIPPLE_POPUP = 0;
    private static int CACHED_COLOR_POPUP_TEXT = 0;
    private static int CACHED_COLOR_POPUP_TITLE = 0;

    private UIColors() {
    }

    public static void resetCache() {
        CACHED_SYSTEM_ACCENT = 0;
        CACHED_COLOR_HIGHLIGHT = 0;
        CACHED_COLOR_RESULT_TEXT = 0;
        CACHED_COLOR_RESULT_TEXT2 = 0;
        CACHED_COLOR_QL_TOGGLE = 0;
        CACHED_RIPPLE_QL = 0;
        CACHED_COLOR_CONTACT_ACTION = 0;
        CACHED_COLOR_SEARCH_TEXT = 0;
        CACHED_COLOR_SEARCH_ICON = 0;
        CACHED_BACKGROUND_RESULT_LIST = null;
        CACHED_RIPPLE_RESULT_LIST = 0;
        CACHED_BACKGROUND_ICON = null;
        CACHED_COLOR_POPUP_BORDER = null;
        CACHED_COLOR_POPUP_BACKGROUND = null;
        CACHED_RIPPLE_POPUP = 0;
        CACHED_COLOR_POPUP_TEXT = 0;
        CACHED_COLOR_POPUP_TITLE = 0;
    }

    public static int getDefaultColor(Context context) {
        return COLOR_DEFAULT;
    }

    @ColorInt
    public static int getThemeColor(Context context, @AttrRes int idRes) {
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        theme.resolveAttribute(idRes, typedValue, true);
        return typedValue.data;
    }

    private static int getSystemAccent(Context context) {
        int color = COLOR_DEFAULT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            color = getThemeColor(context, android.R.attr.colorAccent);
        }
        // Oxygen OS accent color, also used by some custom ROMs now
        String propertyValue = Utilities.getSystemProperty("persist.sys.theme.accentcolor", "");
        if (!propertyValue.isEmpty()) {
            if (!propertyValue.startsWith("#"))
                propertyValue = "#" + propertyValue;
            try {
                color = Color.parseColor(propertyValue);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return color;
    }

    @ColorInt
    public static int getColor(SharedPreferences pref, String key, @ColorInt int defaultColor) {
        return pref.getInt(key, defaultColor);
    }

    @ColorInt
    public static int getColor(SharedPreferences pref, String key) {
        return getColor(pref, key, COLOR_DEFAULT);
    }

    public static int getAlpha(SharedPreferences pref, String key) {
        return pref.getInt(key, 0xFF) & 0xFF;
    }

    public static int setAlpha(int color, int alpha) {
        return color & 0x00ffffff | ((alpha & 0xFF) << 24);
    }

    /**
     * Returns the relative luminance of a color.
     * Code adapted from <i>ColorUtils::calculateLuminance(@ColorInt int color)</i>
     * https://android.googlesource.com/platform/frameworks/base/+/master/core/java/com/android/internal/graphics/ColorUtils.java
     *
     * @return a value between 0 (darkest black) and 1 (lightest white)
     */
    @FloatRange(from = 0.f, to = 1.f)
    public static float luminance(@ColorInt int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);

        // Convert RGB components to its CIE XYZ representative components.
        float sr = r / 255f;
        sr = sr < 0.04045f ? sr / 12.92f : (float) Math.pow((sr + 0.055) / 1.055, 2.4);
        float sg = g / 255f;
        sg = sg < 0.04045f ? sg / 12.92f : (float) Math.pow((sg + 0.055) / 1.055, 2.4);
        float sb = b / 255f;
        sb = sb < 0.04045f ? sb / 12.92f : (float) Math.pow((sb + 0.055) / 1.055, 2.4);

        return (sr * 0.2126f + sg * 0.7152f + sb * 0.0722f);
    }

    public static boolean isColorLight(@ColorInt int color) {
        return luminance(color) > .5f;
    }

    /**
     * Darken or lighten the color. For amount 2 the result is white, for 1 color is unchanged, for 0 result is balck
     *
     * @param color  color to be changed
     * @param amount [0..2] - less than 1 to darken and grater to lighten
     */
    public static int modulateColorLightness(@ColorInt int color, @FloatRange(from = 0.f, to = 2.f) float amount) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        if (amount <= 1f)
            hsl[2] = Math.max(0f, hsl[2] * amount);
        else {
            final float ratio = amount - 1f;
            final float inverseRatio = 1f - ratio;
            hsl[2] = Math.min(1f, hsl[2] * inverseRatio + 1f * ratio);
        }
        return ColorUtils.HSLToColor(hsl);
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

    public static int getSystemAccentColor(Context context) {
        if (CACHED_SYSTEM_ACCENT == 0) {
            int accent = getSystemAccent(context);
            CACHED_SYSTEM_ACCENT = setAlpha(accent, 0xFF);
        }
        return CACHED_SYSTEM_ACCENT;
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

    public static int getQuickListToggleColor(SharedPreferences pref) {
        if (CACHED_COLOR_QL_TOGGLE == 0) {
            int highlightColor = getColor(pref, "quick-list-toggle-color");
            CACHED_COLOR_QL_TOGGLE = setAlpha(highlightColor, 0xFF);
        }
        return CACHED_COLOR_QL_TOGGLE;
    }

    public static int getQuickListRipple(SharedPreferences pref) {
        if (CACHED_RIPPLE_QL == 0) {
            int color = UIColors.getColor(pref, "quick-list-ripple-color");
            int alpha = 0xFF;
            CACHED_RIPPLE_QL = setAlpha(color, alpha);
        }
        return CACHED_RIPPLE_QL;
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

    public static int getResultListBackground(Context context) {
        if (CACHED_BACKGROUND_RESULT_LIST == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            return getResultListBackground(pref);
        }
        return CACHED_BACKGROUND_RESULT_LIST;
    }

    public static int getResultListBackground(SharedPreferences pref) {
        if (CACHED_BACKGROUND_RESULT_LIST == null) {
            int color = UIColors.getColor(pref, "result-list-color");
            int alpha = UIColors.getAlpha(pref, "result-list-alpha");
            CACHED_BACKGROUND_RESULT_LIST = setAlpha(color, alpha);
        }
        return CACHED_BACKGROUND_RESULT_LIST;
    }

    public static int getResultListRipple(Context context) {
        if (CACHED_RIPPLE_RESULT_LIST == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = UIColors.getColor(pref, "result-ripple-color");
            int alpha = 0xFF;
            CACHED_RIPPLE_RESULT_LIST = setAlpha(color, alpha);
        }
        return CACHED_RIPPLE_RESULT_LIST;
    }

    public static int getIconBackground(Context context) {
        if (CACHED_BACKGROUND_ICON == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = UIColors.getColor(pref, "icon-background");
            CACHED_BACKGROUND_ICON = setAlpha(color, 0xFF);
        }
        return CACHED_BACKGROUND_ICON;
    }

    public static int getPopupBorderColor(Context context) {
        if (CACHED_COLOR_POPUP_BORDER == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = pref.getInt("popup-border-argb", 0);
            if (color == 0) {
                color = getSystemAccentColor(context);
                pref.edit().putInt("popup-border-argb", color).apply();
            }
            CACHED_COLOR_POPUP_BORDER = color;
        }
        return CACHED_COLOR_POPUP_BORDER;
    }

    public static int getPopupBackgroundColor(Context context) {
        if (CACHED_COLOR_POPUP_BACKGROUND == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            CACHED_COLOR_POPUP_BACKGROUND = UIColors.getColor(pref, "popup-background-argb");
        }
        return CACHED_COLOR_POPUP_BACKGROUND;
    }

    public static int getPopupRipple(Context context) {
        if (CACHED_RIPPLE_POPUP == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = UIColors.getColor(pref, "popup-ripple-color");
            CACHED_RIPPLE_POPUP = setAlpha(color, 0xFF);
        }
        return CACHED_RIPPLE_POPUP;
    }

    public static int getPopupTextColor(Context context) {
        if (CACHED_COLOR_POPUP_TEXT == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = UIColors.getColor(pref, "popup-text-color");
            CACHED_COLOR_POPUP_TEXT = setAlpha(color, 0xFF);
        }
        return CACHED_COLOR_POPUP_TEXT;
    }

    public static int getPopupTitleColor(Context context) {
        if (CACHED_COLOR_POPUP_TITLE == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            int color = UIColors.getColor(pref, "popup-title-color");
            CACHED_COLOR_POPUP_TITLE = setAlpha(color, 0xFF);
        }
        return CACHED_COLOR_POPUP_TITLE;
    }

    public static Drawable getPreviewDrawable(int color, int border, float radius) {
        float luminance = UIColors.luminance(color);
        int borderColor = UIColors.modulateColorLightness(color, 2.f * (1.f - luminance));

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(radius);
        drawable.setStroke(border, borderColor);
        drawable.setColor(color);

        return drawable;
    }
}
