package rocks.tbog.tblauncher.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.EditSearchHint;

public class EditSearchHintPreferenceDialog extends BasePreferenceDialog {

    private EditSearchHint mEditor = null;

    public static EditSearchHintPreferenceDialog newInstance(String key) {
        EditSearchHintPreferenceDialog fragment = new EditSearchHintPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mEditor = new ViewModelProvider(this).get(EditSearchHint.class);
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
            default:
                dismiss();
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mEditor.onStartLifecycle(requireDialog(), this);
    }
}
