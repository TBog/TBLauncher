package rocks.tbog.tblauncher.preference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.DeviceAdmin;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.SettingsActivity;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.db.XmlExport;
import rocks.tbog.tblauncher.utils.FileUtils;
import rocks.tbog.tblauncher.utils.UITheme;
import rocks.tbog.tblauncher.utils.Utilities;

public class ConfirmDialog extends BasePreferenceDialog {

    private static final String TAG = "Dialog";

    public static ConfirmDialog newInstance(String key) {
        ConfirmDialog fragment = new ConfirmDialog();
        final Bundle b = new Bundle(1);
        b.putString(ARG_KEY, key);
        fragment.setArguments(b);

        return fragment;
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (!positiveResult)
            return;
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        switch (key) {
            case "device-admin": {
                final Context context = requireContext();
                if (DeviceAdmin.isAdminActive(context)) {
                    DeviceAdmin.removeActiveAdmin(context);
                } else {
                    Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, DeviceAdmin.getAdminComponent(context));
                    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, getString(R.string.device_admin_explanation));
                    startActivityForResult(intent, SettingsActivity.ENABLE_DEVICE_ADMIN);
                }
                break;
            }
            case "generate-theme": {
                UITheme.generateAndApplyColors(requireContext());
                break;
            }
            case "reset-matrix": {
                final Context context = requireContext();
                preference
                        .getPreferenceManager()
                        .getSharedPreferences()
                        .edit()
                        .remove("icon-scale-red")
                        .remove("icon-scale-green")
                        .remove("icon-scale-blue")
                        .remove("icon-scale-alpha")
                        .remove("icon-hue")
                        .remove("icon-contrast")
                        .remove("icon-brightness")
                        .remove("icon-saturation")
                        .commit();

                PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
                TBApplication.drawableCache(context).clearCache();
                TBApplication.behaviour(context).refreshSearchRecords();
                TBApplication.quickList(context).reload();
                break;
            }
            case "reset-preferences":
                preference.getPreferenceManager().getSharedPreferences().edit().clear().commit();
                PreferenceManager.setDefaultValues(requireContext(), R.xml.preferences, true);
                PreferenceManager.setDefaultValues(requireContext(), R.xml.preference_features, true);
                break;
            case "reset-cached-app-icons":
                TBApplication.iconsHandler(getContext()).resetCachedAppIcons();
                break;
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
            case "export-modifications":
                FileUtils.sendSettingsFile(requireActivity(), "modifications");
                break;
            case "export-apps":
                FileUtils.sendSettingsFile(requireActivity(), "applications");
                break;
            case "export-interface":
                FileUtils.sendSettingsFile(requireActivity(), "interface");
                break;
            case "export-widgets":
                FileUtils.sendSettingsFile(requireActivity(), "widgets");
                break;
            case "export-history":
                FileUtils.sendSettingsFile(requireActivity(), "history");
                break;
            case "export-backup":
                FileUtils.sendSettingsFile(requireActivity(), "backup");
                break;
            case "unlimited-search-cap": {
                SharedPreferences pref = preference.getPreferenceManager().getSharedPreferences();
                pref.edit().putInt("result-search-cap", 0).apply();
                break;
            }
            default:
                Log.w(TAG, "Unexpected key `" + key + "`");
        }
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        final String key = preference.getKey();

        switch (key) {
            case "device-admin":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.device_admin_disable);
                ((TextView) view.findViewById(android.R.id.text2)).setVisibility(View.GONE);
                break;
            case "generate-theme":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.generate_theme_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.generate_theme_description);
                break;
            case "reset-matrix":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.reset_matrix_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.reset_matrix_description);
                break;
            case "reset-preferences":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.reset_preferences_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.reset_preferences_description);
                break;
            case "reset-cached-app-icons":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.reset_cached_app_icons_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.reset_cached_app_icons_description);
                break;
            case "exit-app":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.exit_the_app_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.exit_the_app_description);
                break;
            case "reset-default-launcher":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.reset_default_launcher_confirm);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.reset_default_launcher_description);
                break;
            case "export-tags":
            case "export-modifications":
            case "export-apps":
            case "export-interface":
            case "export-widgets":
            case "export-history":
            case "export-backup":
                ((TextView) view.findViewById(android.R.id.text1)).setText(R.string.export_xml);
                ((TextView) view.findViewById(android.R.id.text2)).setText(R.string.export_description);
                break;
        }
    }

    @WorkerThread
    @SuppressLint("RestrictedApi")
    private static PreferenceGroup loadAllPreferences(@NonNull Context context) {
        boolean looperCreated = false;
        if (Looper.myLooper() == null) {
            //because inflateFromResource needs a looper and we don't have one, we make one
            Looper.prepare();
            looperCreated = true;
        }

        // load the preference XML
        PreferenceManager manager = new PreferenceManager(context);
        PreferenceGroup root = manager.inflateFromResource(context, R.xml.preferences, null);
        // add `R.xml.preference_features` to rootPreference even if it means we'll get some duplicated key errors
        // it's easier to handle only one root
        manager.inflateFromResource(context, R.xml.preference_features, root.findPreference("feature-holder"));

        // we don't need the looper anymore
        if (looperCreated) {
            Looper.myLooper().quitSafely();
            Looper.loop();
        }
        return root;
    }

    @Override
    public void onStart() {
        super.onStart();
        CustomDialogPreference preference = (CustomDialogPreference) getPreference();
        TaskRunner.AsyncRunnable asyncWrite = null;
        final String key = preference.getKey();

        switch (key) {
            case "device-admin": {
                if (!DeviceAdmin.isAdminActive(requireContext())) {
                    ((AlertDialog) requireDialog()).getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                }
                break;
            }
            case "export-tags":
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null)
                        FileUtils.writeSettingsFile(activity, "tags", w -> XmlExport.tagsXml(activity, w));
                };
                break;
            case "export-modifications":
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null)
                        FileUtils.writeSettingsFile(activity, "modifications", w -> XmlExport.modificationsXml(activity, w));
                };
                break;
            case "export-apps":
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null)
                        FileUtils.writeSettingsFile(activity, "applications", w -> XmlExport.applicationsXml(activity, w));
                };
                break;
            case "export-interface": {
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null) {
                        final PreferenceGroup rootPreference = loadAllPreferences(activity);
                        FileUtils.writeSettingsFile(activity, "interface", w -> XmlExport.interfaceXml(rootPreference, w));
                    }
                };
                break;
            }
            case "export-widgets": {
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null)
                        FileUtils.writeSettingsFile(activity, "widgets", w -> XmlExport.widgetsXml(activity, w));
                };
                break;
            }
            case "export-history": {
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null)
                        FileUtils.writeSettingsFile(activity, "history", w -> XmlExport.historyXml(activity, w));
                };
                break;
            }
            case "export-backup": {
                asyncWrite = t -> {
                    final Activity activity = Utilities.getActivity(getContext());
                    if (activity != null) {
                        final PreferenceGroup rootPreference = loadAllPreferences(activity);
                        FileUtils.writeSettingsFile(activity, "backup", w -> XmlExport.backupXml(rootPreference, w));
                    }
                };
                break;
            }
        }
        if (asyncWrite != null) {
            {
                Dialog dialog = getDialog();
                // disable positive button while we generate the file
                if (dialog instanceof AlertDialog)
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
            }
            Utilities.runAsync(getLifecycle(), asyncWrite, (t) -> {
                Activity activity = Utilities.getActivity(getContext());
                Dialog dialog = getDialog();
                // enable positive button after we generate the file
                if (activity != null && dialog instanceof AlertDialog)
                    ((AlertDialog) dialog).getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            });
        }
    }
}
