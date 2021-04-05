package rocks.tbog.tblauncher.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UITheme;

public abstract class DialogFragment<Output> extends androidx.fragment.app.DialogFragment {
    private static final String TAG = "DialogFrag";
    private OnDismissListener<Output> mOnDismissListener = null;
    private OnConfirmListener<Output> mOnConfirmListener = null;

    @LayoutRes
    protected abstract int layoutRes();

    public interface OnDismissListener<T> {
        void onDismiss(@NonNull DialogFragment<T> dialog);
    }

    public interface OnConfirmListener<T> {
        void onConfirm(@Nullable T output);
    }

    public void setOnDismissListener(OnDismissListener<Output> listener) {
        mOnDismissListener = listener;
    }

    public void setOnConfirmListener(OnConfirmListener<Output> listener) {
        mOnConfirmListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        if (mOnDismissListener != null)
            mOnDismissListener.onDismiss(this);
        super.onDismiss(dialog);
    }

    public void onConfirm(@Nullable Output output) {
        if (mOnConfirmListener != null)
            mOnConfirmListener.onConfirm(output);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //Log.i(TAG, "---> onCreate <---");
        super.onCreate(savedInstanceState);
        int theme = UITheme.getDialogTheme(requireContext());
        if (theme == UITheme.ID_NULL)
            theme = R.style.NoTitleDialogTheme;
        setStyle(DialogFragment.STYLE_NO_FRAME, theme);
        //Log.i(TAG, "theme=" + getTheme());
        //Log.i(TAG, "context=" + getContext());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //Log.i(TAG, "---> onCreateDialog <---");
        Context themeWrapper = new ContextThemeWrapper(requireContext(), getTheme());
        TypedValue outValue = new TypedValue();
        themeWrapper.getTheme().resolveAttribute(R.attr.alertDialogTheme, outValue, true);
        int dialogStyle = outValue.resourceId;
        Dialog dialog = new DialogWrapper(themeWrapper, dialogStyle);
        //Log.i(TAG, "dialog=" + dialog);
        Log.i(TAG, "dialog.context=" + dialog.getContext());
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Log.i(TAG, "---> onCreateView <---");
        Dialog dialog = requireDialog();
        //Log.i(TAG, "dialog=" + dialog);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.7f);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCanceledOnTouchOutside(true);

        //Log.i(TAG, "context=" + getContext());
        //Log.i(TAG, "dialog.context=" + dialog.getContext());
        Log.i(TAG, "inflater.context=" + inflater.getContext());
        View view = inflater.inflate(layoutRes(), container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        return view;
    }
}
