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
    // Source: https://material.io/guidelines/style/color.html#color-color-palette
    public static final int[] COLOR_LIST = new int[]{
            0xFFf44336, //red
            0xFFe91e63, //pink
            0xFF9c27b0, //purple
            0xFF673ab7, //deep purple
            0xFF3f51b5, //indigo
            0xFF2196f3, //blue
            0xFF03a9f4, //light blue
            0xFF00bcd4, //cyan
            0xFF009688, //teal
            0xFF4caf50, //green
            0xFF8bc34a, //light green
            0xFFcddc39, //lime
            0xFFffeb3b, //yellow
            0xFFffc107, //amber
            0xFFff9800, //orange
            0xFFff5722, //deep orange
            0xFF795548, //brown
            0xFF9e9e9e, //grey
            0xFF607d8b, //blue gray
            0xFF000080, //navy
            0xFF000000, //black
            0xFFffffff  //white
    };

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
