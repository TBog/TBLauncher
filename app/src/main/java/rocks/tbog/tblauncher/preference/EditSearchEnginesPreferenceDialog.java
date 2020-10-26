package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.EditSearchEngines;

public class EditSearchEnginesPreferenceDialog extends PreferenceDialogFragmentCompat {

    private final EditSearchEngines mEditor = new EditSearchEngines();

    public static EditSearchEnginesPreferenceDialog newInstance(String key) {
        EditSearchEnginesPreferenceDialog fragment = new EditSearchEnginesPreferenceDialog();
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
            case "reset-search-engines":
                mEditor.loadDefaults(context);
                mEditor.bindEditView(view);
                break;
            case "edit-search-engines":
                mEditor.loadData(context, PreferenceManager.getDefaultSharedPreferences(context));
                mEditor.bindEditView(view);
                break;
            case "add-search-engine":
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
