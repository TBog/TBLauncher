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

    public DialogFragment<Output> putArgString(@Nullable String key, @Nullable String value) {
        Bundle args = getArguments();
        if (args == null)
            args = new Bundle();
        args.putString(key, value);
        setArguments(args);
        return this;
    }

    public DialogFragment<Output> putArgLong(@Nullable String key, long value) {
        Bundle args = getArguments();
        if (args == null)
            args = new Bundle();
        args.putLong(key, value);
        setArguments(args);
        return this;
    }

    public DialogFragment<Output> putArgInt(@Nullable String key, int value) {
        Bundle args = getArguments();
        if (args == null)
            args = new Bundle();
        args.putInt(key, value);
        setArguments(args);
        return this;
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

    public void onButtonClick(@NonNull Button button) {
        switch (button) {
            case POSITIVE:
                if (mOnPositiveClickListener != null)
                    mOnPositiveClickListener.onButtonClick(this, button);
                break;
            case NEGATIVE:
                if (mOnNegativeClickListener != null)
                    mOnNegativeClickListener.onButtonClick(this, button);
                break;
            case NEUTRAL:
            default:
                if (mOnNeutralClickListener != null)
                    mOnNeutralClickListener.onButtonClick(this, button);
                break;
        }
        dismiss();
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
        themeWrapper.getTheme().resolveAttribute(com.google.android.material.R.attr.alertDialogTheme, outValue, true);
        int dialogStyle = outValue.resourceId;
        DialogWrapper dialog = new DialogWrapper(themeWrapper, dialogStyle);
        //Log.i(TAG, "dialog=" + dialog);
        Log.i(TAG, "dialog.context=" + dialog.getContext());
        return dialog;
    }

    @NonNull
    public View inflateLayoutRes(@NonNull LayoutInflater inflater, @Nullable ViewGroup container) {
        //Log.i(TAG, "context=" + getContext());
        //Log.i(TAG, "dialog.context=" + dialog.getContext());
        Log.i(TAG, "inflater.context=" + inflater.getContext());
        return inflater.inflate(layoutRes(), container, false);
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

        View view = inflateLayoutRes(inflater, container);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        createButtonBar(view, inflater);

        return view;
    }

    protected void setupDefaultButtonOkCancel(Context context) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (!isStateSaved()) {
            args.putCharSequence("btnPositiveText", context.getText(android.R.string.ok));
            args.putCharSequence("btnNegativeText", context.getText(android.R.string.cancel));
            setArguments(args);
        }
    }

    protected void setupDefaultButtonOk(Context context) {
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        if (!isStateSaved()) {
            args.putCharSequence("btnPositiveText", context.getText(android.R.string.ok));
            setArguments(args);
        }
    }

    private void createButtonBar(View view, LayoutInflater inflater) {
        Bundle args = getArguments();
        if (args == null)
            return;

        CharSequence btnPositiveText = args.getCharSequence("btnPositiveText", "");
        CharSequence btnNegativeText = args.getCharSequence("btnNegativeText", "");
        CharSequence btnNeutralText = args.getCharSequence("btnNeutralText", "");
        if (btnPositiveText.length() == 0 && btnNegativeText.length() == 0 && btnNeutralText.length() == 0)
            return;

        View buttonPanel = resolvePanel(view, inflater);
        if (buttonPanel == null) {
            Log.e(TAG, "failed to inflate button bar");
            return;
        }

        TextView button1 = buttonPanel.findViewById(android.R.id.button1);
        TextView button2 = buttonPanel.findViewById(android.R.id.button2);
        TextView button3 = buttonPanel.findViewById(android.R.id.button3);

        if (btnPositiveText.length() == 0) {
            button1.setVisibility(View.GONE);
        } else {
            button1.setVisibility(View.VISIBLE);
            button1.setText(btnPositiveText);
            button1.setOnClickListener(v -> onButtonClick(Button.POSITIVE));
        }

        if (btnNegativeText.length() == 0) {
            button2.setVisibility(View.GONE);
        } else {
            button2.setVisibility(View.VISIBLE);
            button2.setText(btnNegativeText);
            button2.setOnClickListener(v -> onButtonClick(Button.NEGATIVE));
        }

        if (btnNeutralText.length() == 0) {
            button3.setVisibility(View.GONE);
            View spacer = buttonPanel.findViewById(R.id.spacer);
            if (spacer != null)
                spacer.setVisibility(View.GONE);
        } else {
            button3.setVisibility(View.VISIBLE);
            button3.setText(btnNeutralText);
            button3.setOnClickListener(v -> onButtonClick(Button.NEUTRAL));
        }
    }

    @Nullable
    private ViewGroup resolvePanel(@NonNull View view, @NonNull LayoutInflater inflater) {
        View buttonPanel = view.findViewById(R.id.buttonPanel);
        if (buttonPanel instanceof ViewGroup)
            return (ViewGroup) buttonPanel;
        if (view instanceof ViewGroup) {
            buttonPanel = inflater.inflate(R.layout.ok_cancel_button_bar, (ViewGroup) view, false);
            ((ViewGroup) view).addView(buttonPanel);
            return (ViewGroup) buttonPanel;
        }
        return null;
    }

    @Nullable
    public <T extends View> T findViewById(@IdRes int id) {
        return requireDialog().findViewById(id);
    }
}
