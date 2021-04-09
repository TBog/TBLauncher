package rocks.tbog.tblauncher.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.Animatable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.R;
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
        // make sure we use the dialog context
        inflater = inflater.cloneInContext(requireDialog().getContext());
        View root = super.onCreateView(inflater, container, savedInstanceState);
        if (!(root instanceof ViewGroup))
            return root;
        Window window = requireDialog().getWindow();
        if (window != null)
            window.setBackgroundDrawableResource(R.drawable.dialog_background_dark);
        Context context = inflater.getContext();
        // add progress indicator
        {
            ImageView loading = new ImageView(context);
            loading.setImageResource(R.drawable.ic_loading_arrows);
            if (loading.getDrawable() instanceof Animatable)
                ((Animatable) loading.getDrawable()).start();
            //loading.setImageDrawable(DrawableUtils.getProgressBarIndeterminate(context));
            loading.setId(android.R.id.progress);
            ((ViewGroup) root).addView(loading);
            ViewGroup.LayoutParams params = loading.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = UISizes.getResultIconSize(context) * 2;
            loading.setLayoutParams(params);
        }
        // add button bar, just like the preferences dialog
        mView = inflater.inflate(R.layout.ok_cancel_button_bar, (ViewGroup) root, true);
        // while we wait, we wait, not cancel
        Dialog dialog = requireDialog();
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
            View button = view.findViewById(android.R.id.button1);
            button.setOnClickListener(v -> {
                onConfirm(null);
                dismiss();
            });
            button.setEnabled(false);
        }

        // CANCEL button
        {
            View button = view.findViewById(android.R.id.button2);
            button.setVisibility(View.GONE);
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
