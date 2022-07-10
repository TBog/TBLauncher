package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.util.TypedValue;
import android.view.ViewGroup;

import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.ui.CutoutFactory;

public final class UISizes {
    // cached sizes are in pixels so we don't need to convert after each get*
    private static int CACHED_SIZE_RESULT_TEXT = 0;
    private static int CACHED_SIZE_RESULT_TEXT2 = 0;
    private static int CACHED_SIZE_RESULT_ICON = 0;
    private static int CACHED_SIZE_TAGS_MENU_ICON = 0;
    private static int CACHED_SIZE_DOCK_ICON = 0;
    private static int CACHED_SIZE_STATUS_BAR = 0;
    private static int CACHED_RADIUS_POPUP_CORNER = -1;
    private static Float CACHED_RADIUS_POPUP_SHADOW = null;
    private static Float CACHED_DX_POPUP_SHADOW = null;
    private static Float CACHED_DY_POPUP_SHADOW = null;
    private static Integer CACHED_HEIGHT_RESULT_LIST_ROW = null;
    private static Float CACHED_RADIUS_RESULT_LIST_SHADOW = null;
    private static Float CACHED_DX_RESULT_LIST_SHADOW = null;
    private static Float CACHED_DY_RESULT_LIST_SHADOW = null;

    private UISizes() {
    }

    public static void resetCache() {
        CACHED_SIZE_RESULT_TEXT = 0;
        CACHED_SIZE_RESULT_TEXT2 = 0;
        CACHED_SIZE_RESULT_ICON = 0;
        CACHED_SIZE_TAGS_MENU_ICON = 0;
        CACHED_SIZE_DOCK_ICON = 0;
        CACHED_SIZE_STATUS_BAR = 0;
        CACHED_RADIUS_POPUP_CORNER = -1;
        CACHED_RADIUS_POPUP_SHADOW = null;
        CACHED_DX_POPUP_SHADOW = null;
        CACHED_DY_POPUP_SHADOW = null;
        CACHED_HEIGHT_RESULT_LIST_ROW = null;
        CACHED_RADIUS_RESULT_LIST_SHADOW = null;
        CACHED_DX_RESULT_LIST_SHADOW = null;
        CACHED_DY_RESULT_LIST_SHADOW = null;
    }

    public static int sp2px(Context context, int size) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, context.getResources().getDisplayMetrics());
        return Math.max(1, (int) (px + .5f));
    }

    public static int dp2px(Context context, int size) {
        if (size == 0)
            return 0;
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
        return Math.max(1, (int) (px + .5f));
    }

    public static int px2dp(Context context, int pixelSize) {
        if (pixelSize == 0)
            return 0;
        // Get the screen's density scale
        final float scale = context.getResources().getDisplayMetrics().density;
        // Convert the DIPs to pixels, based on density scale
        return Math.max(1, (int) (pixelSize * scale + .5f));
    }

    public static int getResultTextSize(Context context) {
        if (CACHED_SIZE_RESULT_TEXT == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_size_text);
            final int size = pref.getInt("result-text-size", defaultSize);
            CACHED_SIZE_RESULT_TEXT = sp2px(context, size);
        }
        return CACHED_SIZE_RESULT_TEXT;
    }

    public static int getResultText2Size(Context context) {
        if (CACHED_SIZE_RESULT_TEXT2 == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_size_text2);
            final int size = pref.getInt("result-text2-size", defaultSize);
            CACHED_SIZE_RESULT_TEXT2 = sp2px(context, size);
        }
        return CACHED_SIZE_RESULT_TEXT2;
    }

    public static int getResultIconSize(Context context) {
        if (CACHED_SIZE_RESULT_ICON == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_size_icon);
            final int size = pref.getInt("result-icon-size", defaultSize);
            CACHED_SIZE_RESULT_ICON = dp2px(context, Math.max(1, size));
        }
        return CACHED_SIZE_RESULT_ICON;
    }

    public static int getResultListRadius(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int radius = pref.getInt("result-list-radius", -1);
        if (radius < 0)
            return context.getResources().getDimensionPixelSize(R.dimen.result_corner_radius);
        return dp2px(context, radius);
    }

    public static int getResultListMarginVertical(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("result-list-margin-vertical", 0);
        return dp2px(context, margin);
    }

    public static int getResultListMarginHorizontal(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("result-list-margin-horizontal", 0);
        return dp2px(context, margin);
    }

    public static int getResultListRowHeight(Context context) {
        if (CACHED_HEIGHT_RESULT_LIST_ROW == null) {
            SharedPreferences pref = TBApplication.getApplication(context).preferences();
            boolean manual = pref.getBoolean("result-list-row-height-manual", false);
            if (manual) {
                int height = pref.getInt("result-list-row-height", 0);
                if (height <= 0)
                    CACHED_HEIGHT_RESULT_LIST_ROW = ViewGroup.LayoutParams.WRAP_CONTENT;
                else
                    CACHED_HEIGHT_RESULT_LIST_ROW = dp2px(context, height);
            } else {
                int iconSize = getResultIconSize(context);
                int resultMargin = context.getResources().getDimensionPixelSize(R.dimen.result_margin_vertical);
                int iconMargin = context.getResources().getDimensionPixelSize(R.dimen.icon_margin_vertical);
                CACHED_HEIGHT_RESULT_LIST_ROW = iconSize + 2 * resultMargin + 2 * iconMargin;
            }
        }
        return CACHED_HEIGHT_RESULT_LIST_ROW;
    }

    public static float getResultListShadowRadius(Context context) {
        if (CACHED_RADIUS_RESULT_LIST_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_radius);
            final float size = pref.getFloat("result-shadow-radius", defaultSize);
            CACHED_RADIUS_RESULT_LIST_SHADOW = size < 0.001f ? 0f : size;
        }
        return CACHED_RADIUS_RESULT_LIST_SHADOW;
    }

    public static float getResultListShadowOffsetHorizontal(Context context) {
        if (CACHED_DX_RESULT_LIST_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dx);
            final float size = pref.getFloat("result-shadow-dx", defaultSize);
            CACHED_DX_RESULT_LIST_SHADOW = Math.abs(size) < 0.001f ? 0f : size;
        }
        return CACHED_DX_RESULT_LIST_SHADOW;
    }

    public static float getResultListShadowOffsetVertical(Context context) {
        if (CACHED_DY_RESULT_LIST_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dy);
            final float size = pref.getFloat("result-shadow-dy", defaultSize);
            CACHED_DY_RESULT_LIST_SHADOW = Math.abs(size) < 0.001f ? 0f : size;
        }
        return CACHED_DY_RESULT_LIST_SHADOW;
    }

    public static int getTagsMenuIconSize(Context context) {
        if (CACHED_SIZE_TAGS_MENU_ICON == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_size_icon);
            final int size = pref.getInt("tags-menu-icon-size", defaultSize);
            CACHED_SIZE_TAGS_MENU_ICON = dp2px(context, Math.max(1, size));
        }
        return CACHED_SIZE_TAGS_MENU_ICON;
    }

    public static int getDockMaxIconSize(Context context) {
        if (CACHED_SIZE_DOCK_ICON == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_size_icon);
            final int size = pref.getInt("quick-list-icon-size", defaultSize);
            CACHED_SIZE_DOCK_ICON = dp2px(context, Math.max(1, size));
        }
        return CACHED_SIZE_DOCK_ICON;
    }

    public static int getStatusBarSize(Context context) {
        if (CACHED_SIZE_STATUS_BAR == 0) {
            CACHED_SIZE_STATUS_BAR = CutoutFactory.StatusBarCutout.getStatusBarHeight(context);
        }
        return CACHED_SIZE_STATUS_BAR;
    }

    public static int getPopupCornerRadius(Context context) {
        if (CACHED_RADIUS_POPUP_CORNER == -1) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_corner_radius);
            final int size = pref.getInt("popup-corner-radius", defaultSize);
            CACHED_RADIUS_POPUP_CORNER = dp2px(context, size);
        }
        return CACHED_RADIUS_POPUP_CORNER;
    }

    public static float getPopupShadowRadius(Context context) {
        if (CACHED_RADIUS_POPUP_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_radius);
            final float size = pref.getFloat("popup-shadow-radius", defaultSize);
            CACHED_RADIUS_POPUP_SHADOW = size < 0.001f ? 0f : size;
        }
        return CACHED_RADIUS_POPUP_SHADOW;
    }

    public static float getPopupShadowOffsetHorizontal(Context context) {
        if (CACHED_DX_POPUP_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dx);
            final float size = pref.getFloat("popup-shadow-dx", defaultSize);
            CACHED_DX_POPUP_SHADOW = Math.abs(size) < 0.001f ? 0f : size;
        }
        return CACHED_DX_POPUP_SHADOW;
    }

    public static float getPopupShadowOffsetVertical(Context context) {
        if (CACHED_DY_POPUP_SHADOW == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dy);
            final float size = pref.getFloat("popup-shadow-dy", defaultSize);
            CACHED_DY_POPUP_SHADOW = Math.abs(size) < 0.001f ? 0f : size;
        }
        return CACHED_DY_POPUP_SHADOW;
    }

//    /**
//     * Example usage: `int size = UISizes.getTextAppearanceTextSize(context, android.R.attr.textAppearanceMedium);`
//     *
//     * @param context        we need the context to get the theme
//     * @param textAppearance text size of what attribute
//     * @return text size
//     */
//    public static int getTextAppearanceTextSize(Context context, @AttrRes int textAppearance) {
//        int size = 0;
//        TypedValue appearance = new TypedValue();
//        if (context.getTheme().resolveAttribute(textAppearance, appearance, true)) {
//            TypedArray ta = context.obtainStyledAttributes(appearance.resourceId, new int[]{android.R.attr.textSize});
//            size = ta.getDimensionPixelSize(0, size);
//            ta.recycle();
//        }
//        return (size == 0) ? sp2px(context, 12) : size;
//    }

    public static int getSearchBarRadius(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int radius = pref.getInt("search-bar-radius", 0);
        return dp2px(context, radius);
    }

    public static int getSearchBarMarginVertical(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("search-bar-margin-vertical", 0);
        return dp2px(context, margin);
    }

    public static int getSearchBarMarginHorizontal(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("search-bar-margin-horizontal", 0);
        return dp2px(context, margin);
    }

    public static float getSearchBarShadowRadius(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_radius);
        final float size = pref.getFloat("search-bar-shadow-radius", defaultSize);
        return size < 0.001f ? 0f : size;
    }

    public static float getSearchBarShadowOffsetHorizontal(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dx);
        final float size = pref.getFloat("search-bar-shadow-dx", defaultSize);
        return Math.abs(size) < 0.001f ? 0f : size;
    }

    public static float getSearchBarShadowOffsetVertical(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        final float defaultSize = getFloatResource(context.getResources(), R.dimen.default_result_shadow_dy);
        final float size = pref.getFloat("search-bar-shadow-dy", defaultSize);
        return Math.abs(size) < 0.001f ? 0f : size;
    }

    public static int getQuickListMarginVertical(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("quick-list-margin-vertical", 0);
        return dp2px(context, margin);
    }

    public static int getQuickListMarginHorizontal(Context context) {
        SharedPreferences pref = TBApplication.getApplication(context).preferences();
        int margin = pref.getInt("quick-list-margin-horizontal", 0);
        return dp2px(context, margin);
    }

    private static float getFloatResource(@NonNull Resources resources, @DimenRes int resId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return resources.getFloat(resId);
        } else {
            final TypedValue value = new TypedValue();
            try {
                resources.getValue(resId, value, true);
                if (value.type == TypedValue.TYPE_FLOAT) {
                    return value.getFloat();
                }
                throw new Resources.NotFoundException("Resource ID #0x" + Integer.toHexString(resId)
                    + " type #0x" + Integer.toHexString(value.type) + " is not valid");
            } catch (Resources.NotFoundException e) {
                return 0f;
            }
        }
    }
}
