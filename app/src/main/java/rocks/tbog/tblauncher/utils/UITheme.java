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

    private static final String[] PREF_BACKGROUND = {
        "icon-background-argb",
        "notification-bar-argb",
        "search-bar-argb",
        "result-list-argb",
        "quick-list-argb",
        "popup-background-argb",
    };

    private static final String[] PREF_HIGHLIGHT = {
        "search-bar-ripple-color",
        "search-bar-cursor-argb",
        "result-ripple-color",
        "result-highlight-color",
        "quick-list-toggle-color",
        "quick-list-ripple-color",
        "popup-border-argb",
        "popup-ripple-color",
    };

    private static final String[] PREF_FOREGROUND = {
        "search-bar-text-color",
        "search-bar-icon-color",
        "contact-action-color",
        "result-text-color",
        "popup-text-color",
        "popup-title-color",
    };

    private static final String[] PREF_FOREGROUND2 = {
        "result-text2-color",
    };

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

    public static void applyColorsThemeSimple(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final int colorBg = UIColors.getColor(pref, "primary-color");
        final int colorFg = UIColors.getColor(pref, "secondary-color");
        final int colorHl = UIColors.getTextContrastColor(colorBg);
        final float lumBg = UIColors.luminance(colorBg);
        final float lumFg = UIColors.luminance(colorFg);
        final int colorFg2;
        if (lumBg > .5f && lumFg > .5f)
            colorFg2 = UIColors.modulateColorLightness(colorFg, .2f);
        else if (lumBg > .5f)
            colorFg2 = UIColors.modulateColorLightness(colorFg, 2.f * (1.f - lumFg));
        else if (lumFg > .5f)
            colorFg2 = UIColors.modulateColorLightness(colorFg, 1.9f);
        else
            colorFg2 = UIColors.getTextContrastColor(colorBg);

        SharedPreferences.Editor editor = pref.edit();

        setColor(editor, PREF_BACKGROUND, colorBg, 0xCD);
        setColor(editor, PREF_HIGHLIGHT, colorHl, 0xFF);
        setColor(editor, PREF_FOREGROUND, colorFg, 0xFF);
        setColor(editor, PREF_FOREGROUND2, colorFg2, 0xFF);

        editor.apply();
    }

    public static void applyColorsThemeHighlight(Context context) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        final int colorBg = UIColors.getColor(pref, "primary-color");
        final int colorHl = UIColors.getColor(pref, "secondary-color");
        final int colorFg = UIColors.getTextContrastColor(colorBg);
        final float lumFg = UIColors.luminance(colorFg);
        final int colorFg2 = UIColors.modulateColorLightness(colorFg, 2.f * (1.f - lumFg));

        SharedPreferences.Editor editor = pref.edit();

        setColor(editor, PREF_BACKGROUND, colorBg, 0xCD);
        setColor(editor, PREF_HIGHLIGHT, colorHl, 0xFF);
        setColor(editor, PREF_FOREGROUND, colorFg, 0xFF);
        setColor(editor, PREF_FOREGROUND2, colorFg2, 0xFF);

        editor.apply();
    }

    private static void setColor(@NonNull SharedPreferences.Editor editor, String[] colorList, int color, int alpha) {
        for (String prefName : colorList) {
            final int prefColor;
            if (prefName.endsWith("-argb")) {
                prefColor = UIColors.setAlpha(color, alpha);
            } else {
                prefColor = UIColors.setAlpha(color, 0);
            }
            editor.putInt(prefName, prefColor);
        }
    }
}
