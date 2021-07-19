package rocks.tbog.tblauncher.ui;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import rocks.tbog.tblauncher.R;

public class WindowInsetsHelper implements KeyboardHandler {
    private final WindowInsetsControllerCompat controller;

    public WindowInsetsHelper(View root) {
        super();

        controller = ViewCompat.getWindowInsetsController(root);
        if (controller == null)
            throw new IllegalStateException("WindowInsetsController == null");
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
    }

    @Override
    public void showKeyboard() {
        controller.show(WindowInsetsCompat.Type.ime());
    }

    @Override
    public void hideKeyboard() {
        controller.hide(WindowInsetsCompat.Type.ime());
    }

    public void showSystemBars() {
        controller.show(WindowInsetsCompat.Type.systemBars());
    }

    public void hideSystemBars() {
        controller.hide(WindowInsetsCompat.Type.systemBars());
    }

    @NonNull
    private static View getRootView(View view) {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        ViewGroup rootView = (ViewGroup) view.getRootView();
        ViewGroup rootLayout = rootView.findViewById(R.id.root_layout);
        // child 0 is `R.id.notificationBackground`
        // child 1 is a full-screen ViewGroup that has `android:fitsSystemWindows="true"`
        return rootLayout.getChildAt(1);
    }

    public static int getKeyboardHeight(View view) {
        // we need a root view that has `android:fitsSystemWindows="true"` to find the height of the keyboard
        return getRootView(view).getPaddingBottom();
    }

    public static boolean isKeyboardVisible(View view) {
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        if (insets != null)
            return insets.isVisible(WindowInsetsCompat.Type.ime());
        // in the unlikely case we can't get the insets, assume we have a keyboard if the bottom padding is greater than 150px
        return getKeyboardHeight(view) > 150;
    }
}
