package rocks.tbog.tblauncher.utils;

import android.view.ViewGroup;

public class DebugString {

    public static String layoutParamSize(int size) {
        switch (size) {
            case ViewGroup.LayoutParams.MATCH_PARENT:
                return "MATCH_PARENT";
            case ViewGroup.LayoutParams.WRAP_CONTENT:
                return "WRAP_CONTENT";
            default:
                return String.valueOf(size);
        }
    }
}
