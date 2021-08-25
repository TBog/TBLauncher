package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;

public class PrefCache {

    private static int RESULT_HISTORY_SIZE = 0;
    private static int RESULT_HISTORY_ADAPTIVE = 0;
    private static int RESULT_SEARCHER_CAP = -1;
    private static Boolean FUZZY_SEARCH_TAGS = null;

    private PrefCache() {
    }

    public static void resetCache() {
        RESULT_HISTORY_SIZE = 0;
        RESULT_HISTORY_ADAPTIVE = 0;
        RESULT_SEARCHER_CAP = -1;
    }

    public static int getResultHistorySize(Context context) {
        if (RESULT_HISTORY_SIZE == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_result_history_size);
            RESULT_HISTORY_SIZE = pref.getInt("result-history-size", defaultSize);
        }
        return RESULT_HISTORY_SIZE;
    }

    public static int getHistoryAdaptive(Context context) {
        if (RESULT_HISTORY_ADAPTIVE == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultSize = context.getResources().getInteger(R.integer.default_result_history_adaptive);
            RESULT_HISTORY_ADAPTIVE = pref.getInt("result-history-adaptive", defaultSize);
        }
        return RESULT_HISTORY_ADAPTIVE;
    }

    public static boolean showWidgetScreenAfterLaunch(SharedPreferences pref) {
        return pref.getBoolean("behaviour-widget-after-launch", true);
    }

    public static boolean clearSearchAfterLaunch(SharedPreferences pref) {
        return pref.getBoolean("behaviour-clear-search-after-launch", true);
    }

    public static boolean linkKeyboardAndSearchBar(SharedPreferences pref) {
        return pref.getBoolean("behaviour-link-keyboard-search-bar", true);
    }

    public static boolean getFuzzySearchTags(Context context) {
        if (FUZZY_SEARCH_TAGS == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            FUZZY_SEARCH_TAGS = pref.getBoolean("fuzzy-search-tags", true);
        }
        return FUZZY_SEARCH_TAGS;
    }

    public static int getResultSearcherCap(Context context) {
        if (RESULT_SEARCHER_CAP == -1) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            final int defaultCap = context.getResources().getInteger(R.integer.default_result_searcher_cap);
            RESULT_SEARCHER_CAP = pref.getInt("result-search-cap", defaultCap);
            if (RESULT_SEARCHER_CAP == 0)
                RESULT_SEARCHER_CAP = Integer.MAX_VALUE;
        }
        return RESULT_SEARCHER_CAP;
    }

    public static boolean modeEmptyQuickListVisible(SharedPreferences preferences) {
        return preferences.getBoolean("dm-empty-quick-list", false);
    }

    public static boolean modeEmptyFullscreen(SharedPreferences preferences) {
        return preferences.getBoolean("dm-empty-fullscreen", true);
    }

    public static boolean modeSearchQuickListVisible(SharedPreferences preferences) {
        return preferences.getBoolean("dm-search-quick-list", true);
    }

    public static boolean modeSearchFullscreen(SharedPreferences preferences) {
        return preferences.getBoolean("dm-search-fullscreen", false);
    }

    @NonNull
    public static String modeSearchOpenResult(SharedPreferences preferences) {
        String result = preferences.getString("dm-search-open-result", null);
        return result == null ? "none" : result;
    }

    public static boolean modeWidgetQuickListVisible(SharedPreferences preferences) {
        return preferences.getBoolean("dm-widget-quick-list", false);
    }

    public static boolean modeWidgetFullscreen(SharedPreferences preferences) {
        return preferences.getBoolean("dm-widget-fullscreen", false);
    }

    public static boolean searchBarAtBottom(SharedPreferences preferences) {
        return preferences.getBoolean("search-bar-at-bottom", true);
    }

    public static boolean linkCloseKeyboardToBackButton(SharedPreferences preferences) {
        return preferences.getBoolean("behaviour-link-close-keyboard-back-button", true);
    }

    @DrawableRes
    public static int getLoadingIconRes(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        String iconName = pref.getString("loading-icon", null);
        if (iconName == null)
            iconName = "none";
        switch (iconName) {
            case "arrows":
                return R.drawable.ic_loading_arrows;
            case "pulse":
                return R.drawable.ic_loading_pulse;
            case "none":
            default:
                return android.R.color.transparent;
        }
    }

    public static boolean modulateContactIcons(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("matrix-contacts", false);
    }
}
