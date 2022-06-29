package rocks.tbog.tblauncher.ui;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import rocks.tbog.tblauncher.R;

public class WindowInsetsHelper implements KeyboardHandler {
    private final WindowInsetsControllerCompat controller;
    private final View mRoot;

    /**
     * Initialize WindowInsetsControllerCompat. It's best to have the root as an EditText to
     * simplify the `showKeyboard` code
     *
     * @param root any view in the window. Used to get the context and window token.
     */
    public WindowInsetsHelper(View root) {
        if (root == null)
            throw new IllegalStateException("WindowInsetsHelper root == null");
        mRoot = root;
        controller = ViewCompat.getWindowInsetsController(root);
        if (controller == null)
            throw new IllegalStateException("WindowInsetsController == null");
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE);
    }

    @Override
    public void showKeyboard() {
        // on KitKat `controller.show` is no-op
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            Context ctx = mRoot.getContext();
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            View view = mRoot;
            if (view.isInEditMode() || view.onCheckIsTextEditor()) {
                view.requestFocus();
            } else {
                Window window = findWindow(ctx);
                if (window != null) {
                    // we should display the keyboard for the currently focused view
                    view = window.getCurrentFocus();
                } else {
                    view = null;
                }
            }

            // Fallback on finding the first EditText
            if (view == null) {
                view = findTextEditor(mRoot);
            }
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        } else {
            controller.show(WindowInsetsCompat.Type.ime());
        }
    }

    private static Window findWindow(Context ctx) {
        Context context = ctx;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                Window window = ((Activity) context).getWindow();
                if (window != null)
                    return window;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }

    private static View findTextEditor(View root) {
        if (root instanceof ViewGroup) {
            int childCount = ((ViewGroup) root).getChildCount();
            // search this level
            for (int childIdx = 0; childIdx < childCount; childIdx += 1) {
                View child = ((ViewGroup) root).getChildAt(childIdx);
                if (child.onCheckIsTextEditor() || child instanceof EditText)
                    return child;
            }
            // look deeper
            for (int childIdx = 0; childIdx < childCount; childIdx += 1) {
                View child = ((ViewGroup) root).getChildAt(childIdx);
                child = findTextEditor(child);
                if (child.onCheckIsTextEditor() || child instanceof EditText)
                    return child;
            }
        }
        return root;
    }

    @Override
    public void hideKeyboard() {
        // on KitKat `controller.hide` is no-op
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            Context ctx = mRoot.getContext();
            InputMethodManager imm = (InputMethodManager) ctx.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(mRoot.getWindowToken(), 0);
        } else {
            controller.hide(WindowInsetsCompat.Type.ime());
        }

        // we need to keep focus on some window view or else the keyboard may eat the next back press
        // example: "Multiling O Keyboard + emoji" by Honso (kl.ime.oh) will not send the key event KEYCODE_BACK
        // after we hide the keyboard by scrolling
        Window window = findWindow(mRoot.getContext());
        if (window != null)
            window.getDecorView().requestFocus();
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
        // On devices running API 20 and below, getRootWindowInsets always returns null.
        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(view);
        if (insets != null)
            return insets.isVisible(WindowInsetsCompat.Type.ime());
        // in the unlikely case we can't get the insets, assume we have a keyboard if the bottom padding is greater than 150px
        return getKeyboardHeight(view) > 150;
    }
}
