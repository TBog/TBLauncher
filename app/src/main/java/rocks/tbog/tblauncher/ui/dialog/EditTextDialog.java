package rocks.tbog.tblauncher.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.textfield.TextInputLayout;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.DialogWrapper;

public class EditTextDialog extends DialogFragment<CharSequence> {
    private static final String TAG = EditTextDialog.class.getSimpleName();

    private EditTextDialog() {
        super();
    }

    @Override
    protected int layoutRes() {
        return R.layout.dialog_rename;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // make sure we use the dialog context
        LayoutInflater dialogInflater = inflater.cloneInContext(requireDialog().getContext());
        return super.onCreateView(dialogInflater, container, savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.setClipToOutline(true);
        }

        // read and apply the arguments
        Bundle args = getArguments() != null ? getArguments() : new Bundle();
        CharSequence initialText = args.getCharSequence("initialText", "");
        CharSequence titleText = args.getCharSequence("titleText", "");
        CharSequence hintText = args.getCharSequence("hintText", "");
        //hint
        if (hintText.length() != 0) {
            TextInputLayout textInputLayout = view.findViewById(android.R.id.hint);
            textInputLayout.setHintEnabled(true);
            textInputLayout.setHint(hintText);
        }
        // initial text
        {
            EditText textView = view.findViewById(R.id.rename);
            textView.setText(initialText);
            textView.setOnEditorActionListener((v, actionId, event) -> {
                if (event == null) {
                    if (actionId != EditorInfo.IME_ACTION_NONE) {
                        final CharSequence name = v.getText();
                        if (name.length() == 0) {
                            dismiss();
                            return true;
                        }
                        onConfirm(name);
                        return true;
                    }
                } else if (event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event.getAction() == KeyEvent.ACTION_UP) {
                        final CharSequence name = v.getText();
                        onConfirm(name);
                    }
                    return true;
                }
                return false;
            });
            textView.requestFocus();
        }
        // title
        {
            TextView textView = view.findViewById(android.R.id.title);
            textView.setText(titleText);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof DialogWrapper) {
            ((DialogWrapper) dialog).setOnWindowFocusChanged((dlg, hasFocus) -> {
                if (hasFocus) {
                    dlg.setOnWindowFocusChanged(null);
                    View view = dlg.getCurrentFocus();
                    if (view == null)
                        view = dlg.findViewById(android.R.id.content);
                    WindowInsetsControllerCompat ctrl = view != null ? ViewCompat.getWindowInsetsController(view) : null;
                    if (ctrl != null)
                        ctrl.show(WindowInsetsCompat.Type.ime());
                    else
                        Log.e(TAG, "failed to show keyboard");
                }
            });
        }
    }

    public static class Builder {
        private final Context mContext;
        private final Bundle mArgs = new Bundle();
        private EditTextDialog mDialog = null;
        private OnButtonClickListener<CharSequence> mClickPositive = null;
        private OnButtonClickListener<CharSequence> mClickNegative = null;
        private OnButtonClickListener<CharSequence> mClickNeutral = null;
        private OnConfirmListener<CharSequence> mOnConfirm = null;

        public Builder(@NonNull Context context) {
            super();
            mContext = context;
        }

        public Builder setTitle(@Nullable CharSequence title) {
            mArgs.putCharSequence("titleText", title);
            return this;
        }

        public Builder setTitle(@StringRes int titleId) {
            mArgs.putCharSequence("titleText", mContext.getText(titleId));
            return this;
        }

        public Builder setHint(@Nullable CharSequence hint) {
            mArgs.putCharSequence("hintText", hint);
            return this;
        }

        public Builder setHint(@StringRes int hintId) {
            return setHint(mContext.getText(hintId));
        }

        public Builder setInitialText(@Nullable CharSequence text) {
            mArgs.putCharSequence("initialText", text);
            return this;
        }

        public Builder setPositiveButton(@StringRes int btnTextId, OnButtonClickListener<CharSequence> onClickListener) {
            mArgs.putCharSequence("btnPositiveText", mContext.getText(btnTextId));
            mClickPositive = onClickListener;
            if (mDialog != null)
                mDialog.setOnPositiveClickListener(mClickPositive);
            return this;
        }

        public Builder setNegativeButton(@StringRes int btnTextId, OnButtonClickListener<CharSequence> onClickListener) {
            mArgs.putCharSequence("btnNegativeText", mContext.getText(btnTextId));
            mClickNegative = onClickListener;
            if (mDialog != null)
                mDialog.setOnNegativeClickListener(mClickNegative);
            return this;
        }

        public Builder setNeutralButton(@StringRes int btnTextId, OnButtonClickListener<CharSequence> onClickListener) {
            mArgs.putCharSequence("btnNeutralText", mContext.getText(btnTextId));
            mClickNeutral = onClickListener;
            if (mDialog != null)
                mDialog.setOnNeutralClickListener(mClickNeutral);
            return this;
        }

        public Builder setConfirmListener(@StringRes int positiveTextId, OnConfirmListener<CharSequence> onConfirm) {
            mArgs.putCharSequence("btnPositiveText", mContext.getText(positiveTextId));
            mClickPositive = (dialog, button) -> {
                EditText input = dialog.findViewById(R.id.rename);
                dialog.onConfirm(input != null ? input.getText() : null);
            };
            mOnConfirm = onConfirm;
            if (mDialog != null) {
                mDialog.setOnPositiveClickListener(mClickPositive);
                mDialog.setOnConfirmListener(mOnConfirm);
            }
            return this;
        }

        @NonNull
        public EditTextDialog getDialog() {
            if (mDialog == null) {
                mDialog = new EditTextDialog();
                mDialog.setArguments(mArgs);
                mDialog.setOnPositiveClickListener(mClickPositive);
                mDialog.setOnNegativeClickListener(mClickNegative);
                mDialog.setOnNeutralClickListener(mClickNeutral);
                mDialog.setOnConfirmListener(mOnConfirm);
            }
            return mDialog;
        }

        public void show() {
            EditTextDialog dialog = getDialog();
            Behaviour.showDialog(mContext, dialog, "dialog_rename");
        }
    }
}
