package rocks.tbog.tblauncher.ui.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.utils.UISizes;

public class PleaseWaitDialog extends DialogFragment<Void> {
    public static final String ARG_TITLE = "title";
    public static final String ARG_DESCRIPTION = "desc";

    View mView = null;
    Runnable mWork = null;

    public void setWork(@Nullable Runnable work) {
        mWork = work;
    }

    public void onWorkFinished() {
        if (mView == null) {
            onConfirm(null);
            dismiss();
            return;
        }
        // OK button
        {
            View button = mView.findViewById(android.R.id.button1);
            button.setEnabled(true);
        }
        // progress indicator
        {
            View view = mView.findViewById(android.R.id.progress);
            if (view != null)
                view.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected int layoutRes() {
        return R.layout.pref_confirm;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        @NonNull
        Dialog dialog = requireDialog();
        Context context = dialog.getContext();

        setupDefaultButtonOk(context);

        // make sure we use the dialog context
        inflater = inflater.cloneInContext(context);
        mView = super.onCreateView(inflater, container, savedInstanceState);
//        Window window = dialog.getWindow();
//        if (window != null)
//            window.setBackgroundDrawableResource(R.drawable.dialog_background_dark);

        // add progress indicator
        if (mView instanceof ViewGroup) {
            ImageView loading = new ImageView(context);
            loading.setImageResource(R.drawable.ic_loading_arrows);
            if (loading.getDrawable() instanceof Animatable)
                ((Animatable) loading.getDrawable()).start();
            //loading.setImageDrawable(DrawableUtils.getProgressBarIndeterminate(context));
            loading.setId(android.R.id.progress);

            // add progress bar before the button panel
            {
                View buttonPanel = mView.findViewById(R.id.buttonPanel);
                int index = ((ViewGroup) mView).indexOfChild(buttonPanel);
                ((ViewGroup) mView).addView(loading, index);
            }

            ViewGroup.LayoutParams params = loading.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = UISizes.getResultIconSize(context) * 2;
            loading.setLayoutParams(params);
        }

        // while we wait, we wait, not cancel
        dialog.setCanceledOnTouchOutside(false);
        dialog.setCancelable(false);
        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments() != null ? getArguments() : new Bundle();

        // title
        {
            TextView text = view.findViewById(android.R.id.text1);
            String title = args.getString(ARG_TITLE);
            if (title != null)
                text.setText(title);
            else
                text.setText(R.string.please_wait);
        }

        // description
        {
            TextView text = view.findViewById(android.R.id.text2);
            String description = args.getString(ARG_DESCRIPTION);
            if (description != null)
                text.setText(description);
            else
                text.setText("");
        }

        // OK button
        {
            setOnPositiveClickListener((dialog, button) -> onConfirm(null));
            View button = view.findViewById(android.R.id.button1);
            button.setEnabled(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (mWork != null) {
            // start the loading after the dialog is visible
            mView.postDelayed(mWork, 500);
        } else {
            onWorkFinished();
        }
    }
}
