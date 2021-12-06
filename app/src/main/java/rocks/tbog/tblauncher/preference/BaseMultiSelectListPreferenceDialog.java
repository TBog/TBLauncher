package rocks.tbog.tblauncher.preference;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.utils.DialogHelper;

public class BaseMultiSelectListPreferenceDialog extends MultiSelectListPreferenceDialogFragmentCompat {

    public static BaseMultiSelectListPreferenceDialog newInstance(String key) {
        final BaseMultiSelectListPreferenceDialog fragment = new BaseMultiSelectListPreferenceDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);
        return fragment;
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        super.onPrepareDialogBuilder(builder);
        DialogHelper.setCustomTitle(builder, getPreference().getDialogTitle());
    }

    @Override
    public void onStart() {
        super.onStart();
        DialogHelper.setButtonBarBackground(requireDialog());
    }
}
