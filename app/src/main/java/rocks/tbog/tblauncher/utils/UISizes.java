package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;
import android.view.ViewGroup;

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
    private static Integer CACHED_ROW_HEIGHT_RESULT_LIST = null;

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
        CACHED_ROW_HEIGHT_RESULT_LIST = null;
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
        if (CACHED_ROW_HEIGHT_RESULT_LIST == null) {
            SharedPreferences pref = TBApplication.getApplication(context).preferences();
            boolean manual = pref.getBoolean("result-list-row-height-manual", false);
            if (manual) {
                int height = pref.getInt("result-list-row-height", 0);
                if (height <= 0)
                    CACHED_ROW_HEIGHT_RESULT_LIST = ViewGroup.LayoutParams.WRAP_CONTENT;
                else
                    CACHED_ROW_HEIGHT_RESULT_LIST = dp2px(context, height);
            } else {
                int iconSize = getResultIconSize(context);
                int resultMargin = context.getResources().getDimensionPixelSize(R.dimen.result_margin_vertical);
                int iconMargin = context.getResources().getDimensionPixelSize(R.dimen.icon_margin_vertical);
                CACHED_ROW_HEIGHT_RESULT_LIST = iconSize + 2 * resultMargin + 2 * iconMargin;
            }
        }
        return CACHED_ROW_HEIGHT_RESULT_LIST;
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
}
