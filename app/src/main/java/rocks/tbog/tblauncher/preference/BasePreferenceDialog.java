package rocks.tbog.tblauncher.preference;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.utils.DialogHelper;

public abstract class BasePreferenceDialog extends PreferenceDialogFragmentCompat {
    private View mDialogView = null;

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        mDialogView = view;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        DialogHelper.setCustomTitle(builder, getPreference().getDialogTitle());
    }

    @Override
    public void onStart() {
        super.onStart();

        // hack to have the LinearLayout weight work
        ViewParent parent = mDialogView != null ? mDialogView.getParent() : null;
        while (parent instanceof ViewGroup) {
            ViewGroup layout = (ViewGroup) parent;
            ViewGroup.LayoutParams params = layout.getLayoutParams();
            if (params.height != ViewGroup.LayoutParams.MATCH_PARENT) {
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.height = ViewGroup.LayoutParams.MATCH_PARENT;
                layout.setLayoutParams(params);
            }
            if (layout.getId() == android.R.id.content)
                break;
            parent = parent.getParent();
        }

        DialogHelper.setButtonBarBackground(requireDialog());
    }

    @Override
    public void onDestroyView() {
        mDialogView = null;
        super.onDestroyView();
    }
}
