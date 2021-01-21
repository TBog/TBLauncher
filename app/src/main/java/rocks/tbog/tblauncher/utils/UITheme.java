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
}
