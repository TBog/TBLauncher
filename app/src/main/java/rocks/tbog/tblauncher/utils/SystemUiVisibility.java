package rocks.tbog.tblauncher.utils;

import android.os.Build;
import android.view.View;

import androidx.annotation.RequiresApi;

public class SystemUiVisibility {

    // Note that some of these constants are new as of API 16 (Jelly Bean)
    // and API 19 (KitKat). It is safe to use them, as they are inlined
    // at compile-time and do nothing on earlier devices.
    private static int REMOVE_STATUS_AND_NAVIGATION = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private static int SHOW_SYSTEM_BARS = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

    private static int setFlags(int flags, int visibility) {
        return flags | visibility;
    }

    private static int clearFlags(int flags, int visibility) {
        return flags & ~visibility;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setLightStatusBar(View view) {
        int flags = view.getSystemUiVisibility();
        flags = setFlags(flags, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        view.setSystemUiVisibility(flags);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void clearLightStatusBar(View view) {
        int flags = view.getSystemUiVisibility();
        flags = clearFlags(flags, View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        view.setSystemUiVisibility(flags);
    }

    public static void setFullscreen(View view) {
        int flags = view.getSystemUiVisibility();
        flags = clearFlags(flags, SHOW_SYSTEM_BARS);

        // removal of status and navigation bar
        flags = setFlags(flags, REMOVE_STATUS_AND_NAVIGATION);
        view.setSystemUiVisibility(flags);
    }

    public static void clearFullscreen(View view) {
        int flags = view.getSystemUiVisibility();
        flags = clearFlags(flags, REMOVE_STATUS_AND_NAVIGATION);

        // Show the system bar
        flags = setFlags(flags, SHOW_SYSTEM_BARS);
        view.setSystemUiVisibility(flags);
    }
}
