package rocks.tbog.tblauncher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

public interface PermissionsManager {
    enum PermissionGroup {
        Calendar,
        Location,
        Contacts,
        ExternalStorage,
        Notifications,
        AppShortcuts,
    }


    void requestPermission(AppCompatActivity context, PermissionGroup permissionGroup);

    /**
     * Check if this permission is granted right now without receiving further updates
     * about the granted state.
     * @return true if the given permission group is fully granted
     */
    Boolean checkPermissionOnce(PermissionGroup permissionGroup);

    void onRequestPermissionsResult(
        int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults
    );

    void onResume();

    Boolean hasPermission(PermissionGroup permissionGroup);

    /**
     * Special function for the Notification listener to report its status.
     * May not be called by anything else.
     */
    void reportNotificationListenerState(Boolean running);
}
