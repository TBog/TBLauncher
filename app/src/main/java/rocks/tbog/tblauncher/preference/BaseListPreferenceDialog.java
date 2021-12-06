package rocks.tbog.tblauncher.preference;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.utils.DialogHelper;

public class BaseListPreferenceDialog extends ListPreferenceDialogFragmentCompat {

    public static BaseListPreferenceDialog newInstance(String key) {
        final BaseListPreferenceDialog fragment = new BaseListPreferenceDialog();
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
