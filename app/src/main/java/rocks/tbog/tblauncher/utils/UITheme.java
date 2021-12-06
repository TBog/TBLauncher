package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.AnyRes;
import androidx.annotation.NonNull;
import androidx.annotation.StyleRes;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.R;

public class UITheme {
    @AnyRes
    public static final int ID_NULL = 0;

    private UITheme() {
    }

    @StyleRes
    public static int getSettingsTheme(Context context) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String theme = sharedPreferences.getString("settings-theme", null);
        if (theme != null) {
            switch (theme) {
                case "white":
                    return R.style.SettingsTheme_White;
                case "black":
                    return R.style.SettingsTheme_Black;
                case "dark":
                    return R.style.SettingsTheme_DarkBg;
                case "DeepBlues":
                    return R.style.SettingsTheme_DeepBlues;
                default:
                    return R.style.SettingsTheme;
            }
        }
        return ID_NULL;
    }

    @StyleRes
    public static int getDialogTheme(Context context) {
        return getSettingsTheme(context);
    }

    @NonNull
    public static Context getDialogThemedContext(@NonNull Context context) {
        int theme = getDialogTheme(context);
        if (theme == ID_NULL)
            return context;
        return new ContextThemeWrapper(context, theme);
    }

    public static void generateAndApplyColors(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        int colorBg = UIColors.getColor(pref, "primary-color");
        int colorFg = UIColors.getColor(pref, "secondary-color");
        int colorBgContrast = UIColors.getTextContrastColor(colorBg);
        float lumBg = UIColors.luminance(colorBg);
        int colorFg2 = UIColors.modulateColorLightness(colorFg, lumBg > .5f ? 0f : 2f);

        String[] background = {
            "icon-background-argb",
            "notification-bar-color",
            "search-bar-color",
            "result-list-color",
            "quick-list-color",
            "popup-background-argb",
        };

        String[] highlight = {
            "search-bar-ripple-color",
            "search-bar-cursor-argb",
            "result-ripple-color",
            "result-highlight-color",
            "quick-list-toggle-color",
            "quick-list-ripple-color",
            "popup-border-argb",
            "popup-ripple-color",
        };

        String[] foreground = {
            "search-bar-text-color",
            "search-bar-icon-color",
            "contact-action-color",
            "result-text-color",
            "popup-text-color",
            "popup-title-color",
        };

        String[] foreground2 = {
            "result-text2-color",
        };

        String[] alpha = {
            "notification-bar-alpha",
            "search-bar-alpha",
            "result-list-alpha",
            "quick-list-alpha",
        };

        SharedPreferences.Editor editor = pref.edit();

        for (String prefAlpha : alpha) {
            editor.putInt(prefAlpha, 0xFF);
        }

        setColor(editor, background, colorBg);
        setColor(editor, highlight, colorBgContrast);
        setColor(editor, foreground, colorFg);
        setColor(editor, foreground2, colorFg2);

        editor.apply();
    }

    private static void setColor(@NonNull SharedPreferences.Editor editor, String[] colorList, int color) {
        for (String prefColor : colorList) {
            final int argb;
            if (prefColor.endsWith("-argb")) {
                argb = UIColors.setAlpha(color, 0xFF);
            } else {
                argb = UIColors.setAlpha(color, 0);
            }
            editor.putInt(prefColor, argb);
        }
    }
}
