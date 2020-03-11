package rocks.tbog.tblauncher.utils;

import android.content.Context;

public class UIColors {
    public static final int COLOR_DEFAULT = 0xFF4caf50;
    // Source: https://material.io/guidelines/style/color.html#color-color-palette
    public static final int[] COLOR_LIST = new int[]{
            0xFF4CAF50,
            0xFFD32F2F,
            0xFFC2185B,
            0xFF7B1FA2,
            0xFF512DA8,
            0xFF303F9F,
            0xFF1976D2,
            0xFF0288D1,
            0xFF0097A7,
            0xFF00796B,
            0xFF388E3C,
            0xFF689F38,
            0xFFAFB42B,
            0xFFFBC02D,
            0xFFFFA000,
            0xFFF57C00,
            0xFFE64A19,
            0xFF5D4037,
            0xFF616161,
            0xFF455A64,
            0xFF000000
    };

    public static int getPrimaryColor(Context context) {
        return COLOR_DEFAULT;
    }
}
