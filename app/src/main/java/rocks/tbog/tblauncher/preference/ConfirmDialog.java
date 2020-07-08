package rocks.tbog.tblauncher.preference;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.DialogPreference;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;

public class ConfirmDialog extends PreferenceDialogFragmentCompat {

    public static ConfirmDialog newInstance(String key) {
        ConfirmDialog fragment = new ConfirmDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        switch (key) {
            case "exit-app":
                //getActivity().finishAffinity();
                System.exit(0);
                break;
            case "reset-default-launcher":
                TBApplication.resetDefaultLauncherAndOpenChooser(getContext());
                break;
        }
    }

    @Override
    protected View onCreateDialogView(Context context) {
        View root = super.onCreateDialogView(context);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        switch (key) {
            case "exit-app":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.exit_the_app_confirm);
                ((TextView) root.findViewById(android.R.id.text2)).setText(R.string.exit_the_app_description);
                break;
            case "reset-default-launcher":
                ((TextView) root.findViewById(android.R.id.text1)).setText(R.string.reset_default_launcher_confirm);
                ((TextView) root.findViewById(android.R.id.text2)).setText(R.string.reset_default_launcher_description);
                break;
        }

        return root;
    }
}
