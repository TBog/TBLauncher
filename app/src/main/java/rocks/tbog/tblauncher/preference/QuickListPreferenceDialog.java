package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.EditQuickList;

public class QuickListPreferenceDialog extends PreferenceDialogFragmentCompat {

    private final EditQuickList mEditor = new EditQuickList();
    private View mDialogView = null;

    public static QuickListPreferenceDialog newInstance(String key) {
        QuickListPreferenceDialog fragment = new QuickListPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;
        mEditor.applyChanges(requireContext());
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mDialogView = view;
        mEditor.bindView(view);
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
    }
}
