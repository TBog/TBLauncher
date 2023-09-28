package rocks.tbog.tblauncher.preference;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.Arrays;

public abstract class EditAddResetPreferenceDialog extends BasePreferenceDialog {
    private static final String TAG = EditAddResetPreferenceDialog.class.getSimpleName();

    protected EditAddResetEditor mEditor = null;

    @Nullable
    public static <T extends EditAddResetPreferenceDialog> T newInstance(String key, Class<T> clazz) {
        T fragment;
        try {
            fragment = clazz.newInstance();
        } catch (IllegalAccessException | java.lang.InstantiationException e) {
            Log.e(TAG, "no constructor?", e);
            return null;
        }
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @NonNull
    protected abstract String[] getEditAddResetKeys();

    @Nullable
    protected abstract EditAddResetEditor newEditor();

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mEditor = newEditor();
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
        int keyIndex = Arrays.asList(getEditAddResetKeys()).indexOf(key);
        switch (keyIndex) {
            case 0: // edit
                mEditor.loadData(context, PreferenceManager.getDefaultSharedPreferences(context));
                mEditor.bindEditView(view);
                break;
            case 1: // add
                mEditor.loadData(context, PreferenceManager.getDefaultSharedPreferences(context));
                mEditor.bindAddView(view);
                break;
            case 2: // reset
                mEditor.loadDefaults(context);
                mEditor.bindEditView(view);
                break;
            default:
                Toast.makeText(context, "`" + key + "` not found", Toast.LENGTH_SHORT).show();
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
