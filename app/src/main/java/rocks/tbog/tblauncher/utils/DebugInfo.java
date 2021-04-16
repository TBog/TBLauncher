package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

public class DebugInfo {

    public static boolean widgetAdd(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("debug-widget-add-info", false);
    }

    public static boolean widgetInfo(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("debug-widget-info", false);
    }

    public static boolean itemRelevance(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("debug-item-relevance", false);
    }

    public static boolean keyboardScrollHiderTouch(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("debug-ksh-touch", false);
    }
}
