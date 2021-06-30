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
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UITheme;

public abstract class DialogFragment<Output> extends androidx.fragment.app.DialogFragment {
    private static final String TAG = "DialogFrag";
    private OnDismissListener<Output> mOnDismissListener = null;
    private OnConfirmListener<Output> mOnConfirmListener = null;
    private OnButtonClickListener<Output> mOnPositiveClickListener = null;
    private OnButtonClickListener<Output> mOnNeutralClickListener = null;
    private OnButtonClickListener<Output> mOnNegativeClickListener = null;

    public enum Button {
        POSITIVE, NEGATIVE, NEUTRAL
    }

    @LayoutRes
    protected abstract int layoutRes();

    public interface OnDismissListener<T> {
        void onDismiss(@NonNull DialogFragment<T> dialog);
    }

    public interface OnConfirmListener<T> {
        void onConfirm(@Nullable T output);
    }

    public interface OnButtonClickListener<T> {
        void onButtonClick(@NonNull DialogFragment<T> dialog, @NonNull Button button);
    }

    public void setOnDismissListener(OnDismissListener<Output> listener) {
        mOnDismissListener = listener;
    }

    public void setOnConfirmListener(OnConfirmListener<Output> listener) {
        mOnConfirmListener = listener;
    }

    public void setOnPositiveClickListener(OnButtonClickListener<Output> listener) {
        mOnPositiveClickListener = listener;
    }

    public void setOnNegativeClickListener(OnButtonClickListener<Output> listener) {
        mOnNegativeClickListener = listener;
    }

    public void setOnNeutralClickListener(OnButtonClickListener<Output> listener) {
        mOnNeutralClickListener = listener;
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
        DialogWrapper dialog = new DialogWrapper(themeWrapper, dialogStyle);
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

        Window window = dialog.getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_NO_TITLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.7f);
        }
        dialog.setCanceledOnTouchOutside(true);

        //Log.i(TAG, "context=" + getContext());
        //Log.i(TAG, "dialog.context=" + dialog.getContext());
        Log.i(TAG, "inflater.context=" + inflater.getContext());
        View view = inflater.inflate(layoutRes(), container, false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        if (mOnPositiveClickListener != null || mOnNegativeClickListener != null || mOnNeutralClickListener != null)
            createButtonBar(view, inflater);

        return view;
    }

    private void createButtonBar(View view, LayoutInflater inflater) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();

        if (mOnPositiveClickListener != null) {
            TextView button = getButtonBarButton(android.R.id.button1, view, inflater);
            if (button == null)
                throw new IllegalStateException("failed to inflate button1 bar");
            button.setVisibility(View.VISIBLE);
            button.setText(args.getCharSequence("btnPositiveText", ""));
            button.setOnClickListener(v -> mOnPositiveClickListener.onButtonClick(this, Button.POSITIVE));
        }

        if (mOnNegativeClickListener != null) {
            TextView button = getButtonBarButton(android.R.id.button2, view, inflater);
            if (button == null)
                throw new IllegalStateException("failed to inflate button2 bar");
            button.setVisibility(View.VISIBLE);
            button.setText(args.getCharSequence("btnNegativeText", ""));
            button.setOnClickListener(v -> mOnPositiveClickListener.onButtonClick(this, Button.NEGATIVE));
        }

        if (mOnNeutralClickListener != null) {
            TextView button = getButtonBarButton(android.R.id.button3, view, inflater);
            if (button == null)
                throw new IllegalStateException("failed to inflate button3 bar");
            button.setVisibility(View.VISIBLE);
            button.setText(args.getCharSequence("btnNeutralText", ""));
            button.setOnClickListener(v -> mOnPositiveClickListener.onButtonClick(this, Button.NEUTRAL));
        } else {
            View button = view.findViewById(android.R.id.button3);
            if (button instanceof TextView)
                button.setVisibility(View.GONE);
            View spacer = view.findViewById(R.id.spacer);
            if (spacer != null)
                spacer.setVisibility(View.GONE);
        }
    }

    @Nullable
    private TextView getButtonBarButton(@IdRes int button, @NonNull View view, @NonNull LayoutInflater inflater) {
        View btnView = view.findViewById(button);
        if (btnView == null && view instanceof ViewGroup) {
            inflater.inflate(R.layout.ok_cancel_button_bar, (ViewGroup) view, true);
            btnView = view.findViewById(button);
        }
        return btnView instanceof TextView ? (TextView) btnView : null;
    }

    @Nullable
    public <T extends View> T findViewById(@IdRes int id) {
        return requireDialog().findViewById(id);
    }
}
