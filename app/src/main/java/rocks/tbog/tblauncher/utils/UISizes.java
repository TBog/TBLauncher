package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.TypedValue;

import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.CutoutFactory;

public final class UISizes {
    // cached sizes are in pixels so we don't need to convert after each get*
    private static int CACHED_SIZE_RESULT_TEXT = 0;
    private static int CACHED_SIZE_RESULT_TEXT2 = 0;
    private static int CACHED_SIZE_RESULT_ICON = 0;
    private static int CACHED_SIZE_STATUS_BAR = 0;

    private UISizes() {
    }

    public static void resetCache() {
        CACHED_SIZE_RESULT_TEXT = 0;
        CACHED_SIZE_RESULT_TEXT2 = 0;
        CACHED_SIZE_RESULT_ICON = 0;
        CACHED_SIZE_STATUS_BAR = 0;
    }

    public static int sp2px(Context context, int size) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, size, context.getResources().getDisplayMetrics());
        return Math.max(1, (int) (px + .5f));
    }

    public static int dp2px(Context context, int size) {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, context.getResources().getDisplayMetrics());
        return Math.max(1, (int) (px + .5f));
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
            CACHED_SIZE_RESULT_ICON = dp2px(context, size);
        }
        return CACHED_SIZE_RESULT_ICON;
    }

    public static int getStatusBarSize(Context context) {
        if (CACHED_SIZE_STATUS_BAR == 0) {
            CACHED_SIZE_STATUS_BAR = CutoutFactory.StatusBarCutout.getStatusBarHeight(context);
        }
        return CACHED_SIZE_STATUS_BAR;
    }
}
