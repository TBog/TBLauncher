package rocks.tbog.tblauncher;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;

import rocks.tbog.tblauncher.dataprovider.ContactsProvider;


public class Permission {
    private static final int PERMISSION_READ_CONTACTS = 0;
    private static final int PERMISSION_CALL_PHONE = 1;

    // Static weak reference to the main activity, this is sadly required
    // to ensure classes requesting permission can access activity.requestPermission()
    //private static WeakReference<TBLauncherActivity> currentActivity = new WeakReference<TBLauncherActivity>(null);

    /**
     * Sometimes, we need to wait for the user to give us permission before we can start an intent.
     * Store the intent here for later use.
     * Ideally, we'd want to use TBLauncherActivity to store this, but TBLauncherActivity has stateNotNeeded=true
     * which means it's always rebuild from scratch, we can't store any state in it.
     * This means that when we use pendingIntent, it's highly likely that by the time we end up using it,
     * currentActivity will have changed
     */
    private static Intent pendingIntent = null;

    /**
     * Try to start the dialer with specified intent, if we have permission already
     * Otherwise, ask for permission and store the intent for future use;
     *
     * @return true if we do have permission already, false if we're asking for permission now and will handle dispatching the intent in the Forwarder
     */
    public static boolean ensureCallPhonePermission(Activity activity, Intent pendingIntent) {
        //TBLauncherActivity activity = Permission.currentActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null && activity.checkSelfPermission(android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{android.Manifest.permission.CALL_PHONE}, Permission.PERMISSION_CALL_PHONE);
            Permission.pendingIntent = pendingIntent;

            return false;
        }

        return true;
    }

    public static boolean checkContactPermission(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void askContactPermission(Activity activity) {
        // If we don't have permission to list contacts, ask for it.
        //TBLauncherActivity activity = Permission.currentActivity.get();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity != null) {
            activity.requestPermissions(new String[]{android.Manifest.permission.READ_CONTACTS}, Permission.PERMISSION_READ_CONTACTS);
        }
    }

//    Permission(TBLauncherActivity activity) {
//        // Store the latest reference to a TBLauncherActivity
//        currentActivity = new WeakReference<>(activity);
//    }

    static void onRequestPermissionsResult(@NonNull TBLauncherActivity activity, int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (grantResults.length == 0) {
            return;
        }

        if (requestCode == PERMISSION_READ_CONTACTS && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Great! Reload the contact provider. We're done :)
            ContactsProvider contactsProvider = TBApplication.getApplication(activity).getDataHandler().getContactsProvider();
            if (contactsProvider != null) {
                contactsProvider.reload();
            }
        } else if (requestCode == PERMISSION_CALL_PHONE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Great! Start the intent we stored for later use.
                activity.startActivity(pendingIntent);
                pendingIntent = null;

                // Record launch to clear search results
                activity.launchOccurred();
            } else {
                Toast.makeText(activity, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }
}
