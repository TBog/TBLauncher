package rocks.tbog.tblauncher.ui;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class DialogFragment<Output> extends androidx.fragment.app.DialogFragment {
    private OnDismissListener mOnDismissListener = null;
    private OnConfirmListener<Output> mOnConfirmListener = null;

    @LayoutRes
    protected abstract int layoutRes();

    public interface OnDismissListener {
        void onDismiss(@NonNull DialogFragment dialog);
    }

    public interface OnConfirmListener<T> {
        void onConfirm(@Nullable T output);
    }

    public void setOnDismissListener(OnDismissListener listener) {
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
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_FRAME, 0);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(layoutRes(), container, false);
        Dialog dialog = requireDialog();
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams lp = window.getAttributes();
            lp.dimAmount = 0.7f;
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        }
        dialog.setCanceledOnTouchOutside(true);

        return root;
    }

}
