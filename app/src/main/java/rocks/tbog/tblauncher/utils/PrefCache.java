package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import androidx.annotation.DrawableRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.quicklist.QuickList;

public class PrefCache {

    private final static ArraySet<String> PREF_THAT_REQUIRE_MIGRATION = new ArraySet<>(Arrays.asList(
        "result-list-color", "result-list-alpha",
        "notification-bar-color", "notification-bar-alpha",
        "search-bar-color", "search-bar-alpha",
        "quick-list-color", "quick-list-alpha",
        "result-list-rounded", "search-bar-rounded"
    ));

    private static int RESULT_HISTORY_SIZE = 0;
    private static int RESULT_HISTORY_ADAPTIVE = 0;
    private static int RESULT_SEARCHER_CAP = -1;
    private static int LOADING_ICON_RES = 0; // Resources.ID_NULL
    private static Boolean FUZZY_SEARCH_TAGS = null;
    private static Boolean TAGS_MENU_ICONS = null;
    private static Boolean TAGS_MENU_UNTAGGED = null;
    private static int TAGS_MENU_UNTAGGED_IDX = -1;
    private static List<ContentLoadHelper.CategoryItem> RESULT_POPUP_ORDER = null;

    private PrefCache() {
    }

    public static void resetCache() {
        RESULT_HISTORY_SIZE = 0;
        RESULT_HISTORY_ADAPTIVE = 0;
        RESULT_SEARCHER_CAP = -1;
        LOADING_ICON_RES = 0;
        FUZZY_SEARCH_TAGS = null;
        TAGS_MENU_ICONS = null;
        TAGS_MENU_UNTAGGED = null;
        TAGS_MENU_UNTAGGED_IDX = -1;
        RESULT_POPUP_ORDER = null;
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

    public static boolean showTagsMenuIcons(Context context) {
        if (TAGS_MENU_ICONS == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            TAGS_MENU_ICONS = pref.getBoolean("tags-menu-icons", false);
        }
        return TAGS_MENU_ICONS;
    }

    public static boolean showTagsMenuUntagged(Context context) {
        if (TAGS_MENU_UNTAGGED == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            TAGS_MENU_UNTAGGED = pref.getBoolean("tags-menu-untagged", false);
            try {
                TAGS_MENU_UNTAGGED_IDX = Integer.parseInt(pref.getString("tags-menu-untagged-index", "0"));
            } catch (Exception ignored) {
            }
        }
        return TAGS_MENU_UNTAGGED;
    }

    public static int getTagsMenuUntaggedIndex(Context context) {
        if (TAGS_MENU_UNTAGGED_IDX == -1) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            try {
                TAGS_MENU_UNTAGGED_IDX = Integer.parseInt(pref.getString("tags-menu-untagged-index", "0"));
            } catch (Exception ignored) {
            }
        }
        return TAGS_MENU_UNTAGGED_IDX;
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

    @LayoutRes
    public static int getSearchBarLayout(SharedPreferences pref) {
        String layout = pref.getString("search-bar-layout", null);
        if ("btn-text-menu".equals(layout))
            return R.layout.search_bar;
        if ("pill-search".equals(layout))
            return R.layout.search_pill;
        return 0;
    }

    public static boolean searchBarHasTimer(SharedPreferences pref) {
        String layout = pref.getString("search-bar-layout", null);
        return "pill-search".equals(layout);
    }

    public static QuickList.QuickListPosition getDockPosition(SharedPreferences pref) {
        String position = pref.getString("quick-list-position", null);
        if (position != null) {
            switch (position) {
                case "above-result-list":
                    return QuickList.QuickListPosition.POSITION_ABOVE_RESULTS;
                case "under-result-list":
                    return QuickList.QuickListPosition.POSITION_UNDER_RESULTS;
                case "under-search-bar":
                    return QuickList.QuickListPosition.POSITION_UNDER_SEARCH_BAR;
                default:
                    break;
            }
        }
        return QuickList.QuickListPosition.POSITION_UNDER_RESULTS;
    }

    public static boolean linkCloseKeyboardToBackButton(SharedPreferences preferences) {
        return preferences.getBoolean("behaviour-link-close-keyboard-back-button", true);
    }

    @DrawableRes
    public static int getLoadingIconRes(@NonNull Context context) {
        if (LOADING_ICON_RES == 0) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            String iconName = pref.getString("loading-icon", null);
            if (iconName == null)
                iconName = "none";
            switch (iconName) {
                case "arrows":
                    LOADING_ICON_RES = R.drawable.ic_loading_arrows;
                    break;
                case "pulse":
                    LOADING_ICON_RES = R.drawable.ic_loading_pulse;
                    break;
                case "none":
                default:
                    LOADING_ICON_RES = android.R.color.transparent;
                    break;
            }
        }
        return LOADING_ICON_RES;
    }

    @NonNull
    public static Drawable getLoadingIconDrawable(@NonNull Context context) {
        @DrawableRes
        int loadingIconRes = getLoadingIconRes(context);
        Drawable loadingIcon = AppCompatResources.getDrawable(context, loadingIconRes);
        if (loadingIcon == null)
            loadingIcon = new ColorDrawable(Color.TRANSPARENT);
        return loadingIcon;
    }

    public static boolean modulateContactIcons(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean("matrix-contacts", false);
    }

    public static List<ContentLoadHelper.CategoryItem> getResultPopupOrder(@NonNull Context context) {
        if (RESULT_POPUP_ORDER == null) {
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
            ContentLoadHelper.OrderedMultiSelectListData data = ContentLoadHelper.generateResultPopupContent(context, pref);
            List<String> orderedValues = data.getOrderedListValues();
            RESULT_POPUP_ORDER = new ArrayList<>(orderedValues.size());
            for (String orderValue : orderedValues) {
                String value = PrefOrderedListHelper.getOrderedValueName(orderValue);
                for (ContentLoadHelper.CategoryItem categoryItem : ContentLoadHelper.RESULT_POPUP_CATEGORIES) {
                    if (categoryItem.value.equals(value)) {
                        RESULT_POPUP_ORDER.add(categoryItem);
                        break;
                    }
                }
            }
        }
        return RESULT_POPUP_ORDER;
    }

    public static boolean firstAtBottom(SharedPreferences preferences) {
        return preferences.getBoolean("result-first-at-bottom", true);
    }

    public static boolean rightToLeft(SharedPreferences preferences) {
        return preferences.getBoolean("result-right-to-left", true);
    }

    public static boolean getResultFadeOut(SharedPreferences pref) {
        return pref.getBoolean("result-fading-edge", false);
    }

    public static int getDockColumnCount(SharedPreferences pref) {
        return pref.getInt("quick-list-columns", 1);
    }

    public static int getDockRowCount(SharedPreferences pref) {
        return pref.getInt("quick-list-rows", 1);
    }

    public static boolean isMigrateRequired(@NonNull SharedPreferences pref) {
        Map<String, ?> allPref = pref.getAll();
        for (String key : PREF_THAT_REQUIRE_MIGRATION)
            if (allPref.containsKey(key))
                return true;
        return false;
    }

    public static boolean migratePreferences(@NonNull Context context, @NonNull SharedPreferences pref) {
        HashMap<String, Object> prefMapCopy = new HashMap<>(pref.getAll());
        SharedPreferences.Editor editor = pref.edit();
        boolean changesMade = migratePreferences(context, prefMapCopy, editor);
        editor.apply();
        return changesMade;
    }

    public static boolean migratePreferences(@NonNull Context context, @NonNull HashMap<String, Object> entries, @NonNull SharedPreferences.Editor editor) {
        Resources res = context.getResources();
        boolean changesMade;
        changesMade = migrateColor(entries, editor, "result-list");
        changesMade = migrateColor(entries, editor, "notification-bar") || changesMade;
        changesMade = migrateColor(entries, editor, "search-bar") || changesMade;
        changesMade = migrateColor(entries, editor, "quick-list") || changesMade;

        int defaultCornerRadius = UISizes.px2dp(context, res.getDimensionPixelSize(R.dimen.result_corner_radius));
        changesMade = migrateToggleToValue(entries, editor, "result-list-rounded", "result-list-radius", 0, defaultCornerRadius) || changesMade;
        changesMade = migrateToggleToValue(entries, editor, "search-bar-rounded", "search-bar-radius", 0, defaultCornerRadius) || changesMade;

        return changesMade;
    }

    private static boolean migrateColor(@NonNull HashMap<String, Object> entries, @NonNull SharedPreferences.Editor editor, String key) {
        String keyColor = key + "-color";
        String keyAlpha = key + "-alpha";
        Object color = entries.get(keyColor);
        Object alpha = entries.get(keyAlpha);
        if (color instanceof Integer && alpha instanceof Integer) {
            int argb = UIColors.setAlpha((Integer) color, (Integer) alpha);
            String keyARGB = key + "-argb";
            editor
                .remove(keyColor)
                .remove(keyAlpha)
                .putInt(keyARGB, argb);
            Log.d("pref", "migrate `" + key + "` from " +
                "(alpha=0x" + Integer.toHexString((Integer) alpha) + " color=0x" + Integer.toHexString((Integer) color) + ")" +
                " to argb=0x" + Integer.toHexString(argb));
            return true;
        }
        return false;
    }

    private static boolean migrateToggleToValue(HashMap<String, Object> entries, SharedPreferences.Editor editor, String keyToggle, String keyValue, int valueOff, int valueOn) {
        Object toggle = entries.get(keyToggle);
        if (toggle instanceof Boolean) {
            int value = ((Boolean) toggle) ? valueOn : valueOff;
            editor
                .remove(keyToggle)
                .putInt(keyValue, value);
            Log.d("pref", "migrate `" + keyToggle + "` from " +
                "value=" + toggle + " to `" + keyValue + "` value=" + value);
            return true;
        }
        return false;
    }
}
