package rocks.tbog.tblauncher.ui;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;

public class DialogWrapper extends AppCompatDialog {

    private OnWindowFocusChanged mOnWindowFocusChanged = null;

    public interface OnWindowFocusChanged {
        void onWindowFocusChanged(@NonNull DialogWrapper dialog, boolean hasFocus);
    }

    public DialogWrapper(Context context) {
        super(context);
    }

    public DialogWrapper(Context context, int theme) {
        super(context, theme);
    }

    protected DialogWrapper(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public void setOnWindowFocusChanged(@Nullable OnWindowFocusChanged callback) {
        mOnWindowFocusChanged = callback;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mOnWindowFocusChanged != null)
            mOnWindowFocusChanged.onWindowFocusChanged(this, hasFocus);
    }
}
