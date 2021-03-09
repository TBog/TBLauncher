package rocks.tbog.tblauncher;

import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

public class DeviceAdmin extends DeviceAdminReceiver {

    @Override
    public void onEnabled(@NonNull Context context, @NonNull Intent intent) {
        super.onEnabled(context, intent);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean("device-admin", true).apply();
    }

    @Override
    public void onDisabled(@NonNull Context context, @NonNull Intent intent) {
        super.onDisabled(context, intent);
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putBoolean("device-admin", false).apply();
    }

    @NonNull
    public static ComponentName getAdminComponent(@NonNull Context context) {
        return new ComponentName(context, DeviceAdmin.class);
    }

    public static boolean isAdminActive(@NonNull Context context) {
        Object service = context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (service instanceof DevicePolicyManager) {
            DevicePolicyManager dpm = (DevicePolicyManager) service;
            return dpm.isAdminActive(getAdminComponent(context));
        }
        return false;
    }

    public static void removeActiveAdmin(@NonNull Context context) {
        Object service = context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (service instanceof DevicePolicyManager) {
            DevicePolicyManager dpm = (DevicePolicyManager) service;
            dpm.removeActiveAdmin(getAdminComponent(context));
        }
    }

    public static void lockScreen(@NonNull Context context) {
        Object service = context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (service instanceof DevicePolicyManager) {
            DevicePolicyManager dpm = (DevicePolicyManager) service;
            dpm.lockNow();
        }
    }
}
