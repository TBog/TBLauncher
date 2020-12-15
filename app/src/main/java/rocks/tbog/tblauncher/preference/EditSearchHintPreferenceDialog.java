package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.EditSearchHint;

public class EditSearchHintPreferenceDialog extends PreferenceDialogFragmentCompat {

    private final EditSearchHint mEditor = new EditSearchHint();

    public static EditSearchHintPreferenceDialog newInstance(String key) {
        EditSearchHintPreferenceDialog fragment = new EditSearchHintPreferenceDialog();
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

        Context context = requireContext();
        String key = getPreference().getKey();
        switch (key) {
            case "reset-search-hint":
                mEditor.loadDefaults(context);
                mEditor.bindEditView(view);
                break;
            case "edit-search-hint":
                mEditor.loadData(context, PreferenceManager.getDefaultSharedPreferences(context));
                mEditor.bindEditView(view);
                break;
            case "add-search-hint":
                mEditor.loadData(context, PreferenceManager.getDefaultSharedPreferences(context));
                mEditor.bindAddView(view);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mEditor.onStart();
    }
}
