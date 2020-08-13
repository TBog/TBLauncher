package rocks.tbog.tblauncher.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.ColorInt;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

public class UIColors {
    public static final int COLOR_DEFAULT = 0xFF4caf50;

    public static int getPrimaryColor(Context context) {
        return COLOR_DEFAULT;
    }

    @ColorInt
    public static int getColor(SharedPreferences pref, String key) {
        return pref.getInt(key, COLOR_DEFAULT);
    }

    public static int getAlpha(SharedPreferences pref, String key) {
        return pref.getInt(key, 0xFF) & 0xFF;
    }

    public static int setAlpha(int color, int alpha) {
        return color & 0x00ffffff | ((alpha & 0xFF) << 24);
    }

    public static void setStatusBarColor(AppCompatActivity compatActivity, @ColorInt int notificationBarColor) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = compatActivity.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);

            // Update status bar color
            window.setStatusBarColor(notificationBarColor);
        }

        ActionBar actionBar = compatActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(notificationBarColor));
        }
    }
}
