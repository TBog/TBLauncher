package rocks.tbog.tblauncher.broadcast;

import android.content.SharedPreferences;
import android.os.Build;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ContactsProvider;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.handler.DataHandler;

@RequiresApi(api = Build.VERSION_CODES.N)
public class IncomingCallScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(@NonNull Call.Details callDetails) {
        respondToCall(callDetails, new CallResponse.Builder().build());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (prefs.getBoolean("enable-phone-history", false) && callDetails.getHandle() != null) {
            String phoneNumber = callDetails.getHandle().getSchemeSpecificPart();
            if (!TextUtils.isEmpty(phoneNumber)) {
                DataHandler dataHandler = TBApplication.getApplication(this).getDataHandler();
                ContactsProvider contactsProvider = dataHandler.getContactsProvider();
                if (contactsProvider != null) {
                    ContactEntry contactPojo = contactsProvider.findByPhone(phoneNumber);
                    if (contactPojo != null) {
                        dataHandler.addToHistory(contactPojo.getHistoryId());
                    }
                }
            }
        }
    }
}
