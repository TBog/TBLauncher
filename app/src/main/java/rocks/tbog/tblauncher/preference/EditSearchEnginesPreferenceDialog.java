package rocks.tbog.tblauncher.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.EditSearchEngines;

public class EditSearchEnginesPreferenceDialog extends BasePreferenceDialog {

    private EditSearchEngines mEditor = null;

    public static EditSearchEnginesPreferenceDialog newInstance(String key) {
        EditSearchEnginesPreferenceDialog fragment = new EditSearchEnginesPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mEditor = new ViewModelProvider(this).get(EditSearchEngines.class);
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;
        if (mEditor != null)
            mEditor.applyChanges(requireContext());
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        if (mEditor == null)
            return;

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
        if (mEditor != null)
            mEditor.onStartLifecycle(requireDialog(), this);
    }

}
