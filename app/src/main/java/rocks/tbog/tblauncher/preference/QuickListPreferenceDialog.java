package rocks.tbog.tblauncher.preference;

import android.os.Bundle;
import android.view.View;

import rocks.tbog.tblauncher.quicklist.EditQuickList;

public class QuickListPreferenceDialog extends BasePreferenceDialog {

    private final EditQuickList mEditor = new EditQuickList();

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

        mEditor.bindView(view);
    }
}
