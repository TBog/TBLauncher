package rocks.tbog.tblauncher.utils;

import android.view.View;

import rocks.tbog.tblauncher.ui.WindowInsetsHelper;

public class KeyboardToggleHelper extends WindowInsetsHelper {
    public boolean mHiddenByScrolling = false;
    public boolean mRequestOpen = false;

    public KeyboardToggleHelper(View root) {
        super(root);
    }

}
