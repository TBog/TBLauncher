package rocks.tbog.tblauncher.preference;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceDialogFragmentCompat;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.XmlExport;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.Utilities;

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
                TBApplication.resetDefaultLauncherAndOpenChooser(requireContext());
                break;
            case "export-tags":
                FileUtils.sendSettingsFile(requireActivity(), "tags");
                break;
            case "export-favs":
                FileUtils.sendSettingsFile(requireActivity(), "favorites");
                break;
            case "export-apps":
                FileUtils.sendSettingsFile(requireActivity(), "applications");
                break;
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        switch (key) {
            case "exit-app":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.exit_the_app_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.exit_the_app_description);
                break;
            case "reset-default-launcher":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.reset_default_launcher_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.reset_default_launcher_description);
                break;
            case "export-tags":
            case "export-favs":
            case "export-apps":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.export_xml);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.export_description);
                break;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();
        switch (key) {
            case "export-tags":
            case "export-favs":
            case "export-apps":
                //
            {
                Dialog dialog = getDialog();
                // disable positive button while we generate the file
                if (dialog instanceof AlertDialog)
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
            Utilities.runAsync((t) -> {
                Activity activity = Utilities.getActivity(getContext());
                if (activity == null)
                    return;
                FileUtils.writeSettingsFile(activity, "tags", w -> XmlExport.tagsXml(activity, w));
                FileUtils.writeSettingsFile(activity, "favorites", w -> XmlExport.favoritesXml(activity, w));
                FileUtils.writeSettingsFile(activity, "applications", w -> XmlExport.applicationsXml(activity, w));
            }, (t) -> {
                Activity activity = Utilities.getActivity(getContext());
                Dialog dialog = getDialog();
                // enable positive button after we generate the file
                if (activity != null && dialog instanceof AlertDialog)
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            });
            break;
        }
    }
}
