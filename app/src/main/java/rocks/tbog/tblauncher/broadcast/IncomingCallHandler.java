package rocks.tbog.tblauncher.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ContactsProvider;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.utils.PackageManagerUtils;

public class IncomingCallHandler extends BroadcastReceiver {

    private static final String TAG = IncomingCallHandler.class.getSimpleName();

    @Override
    public void onReceive(final Context context, Intent intent) {
        // Only handle calls received
        if (!"android.intent.action.PHONE_STATE".equals(intent.getAction())) {
            return;
        }

        try {
            DataHandler dataHandler = TBApplication.getApplication(context).getDataHandler();
            ContactsProvider contactsProvider = dataHandler.getContactsProvider();

            // Stop if contacts are not enabled
            if (contactsProvider == null) {
                return;
            }

            if (TelephonyManager.EXTRA_STATE_RINGING.equals(intent.getStringExtra(TelephonyManager.EXTRA_STATE))) {
                String phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

                if (phoneNumber == null) {
                    // Skipping (private call)
                    return;
                }

                ContactEntry contactEntry = contactsProvider.findByPhone(phoneNumber);
                if (contactEntry != null) {
                    dataHandler.addToHistory(contactEntry.getHistoryId());
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Phone Receive Error", e);
        }
    }

    public static void setEnabled(Context context, boolean enabled) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PackageManagerUtils.enableComponent(context, IncomingCallHandler.class, false);
        } else {
            PackageManagerUtils.enableComponent(context, IncomingCallHandler.class, enabled);
        }

    }
}
