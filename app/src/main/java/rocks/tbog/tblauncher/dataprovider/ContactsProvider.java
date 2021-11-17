package rocks.tbog.tblauncher.dataprovider;

import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.loader.LoadContactsEntry;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class ContactsProvider extends Provider<ContactEntry> {
    private final static String TAG = "ContactsProvider";
    private final ContentObserver cObserver = new ContentObserver(null) {

        @Override
        public void onChange(boolean selfChange) {
            //reload contacts
            Log.i(TAG, "Contacts changed, reloading provider.");
            reload(true);
        }
    };

    public void reload(boolean cancelCurrentLoadTask) {
        super.reload(cancelCurrentLoadTask);
        if (!isLoaded() && !isLoading())
            this.initialize(new LoadContactsEntry(this));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // register content observer if we have permission
        if (Permission.checkPermission(this, Permission.PERMISSION_READ_CONTACTS)) {
            getContentResolver().registerContentObserver(ContactsContract.Contacts.CONTENT_URI, false, cObserver);
        } else {
            Permission.askPermission(Permission.PERMISSION_READ_CONTACTS, new Permission.PermissionResultListener() {
                @Override
                public void onGranted() {
                    // Great! Reload the contact provider. We're done :)
                    reload(true);
                }

                @Override
                public void onDenied() {
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ContactsProvider.this);
                    pref.edit().putBoolean("enable-contacts", false).apply();
                }
            });
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //deregister content observer
        getContentResolver().unregisterContentObserver(cObserver);
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ContactEntry pojo : pojos) {
            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.setRelevance(pojo.normalizedName, matchInfo);

            if (pojo.normalizedNickname != null) {
                matchInfo = fuzzyScore.match(pojo.normalizedNickname.codePoints);
                if (matchInfo.match && (!match || matchInfo.score > pojo.getRelevance())) {
                    match = true;
                    pojo.setRelevance(pojo.normalizedNickname, matchInfo);
                }
            }

            if (!match && pojo.normalizedPhone != null && queryNormalized.length() > 2) {
                // search for the phone number
                matchInfo = fuzzyScore.match(pojo.normalizedPhone.codePoints);
                match = matchInfo.match;
                pojo.setRelevance(pojo.normalizedPhone, matchInfo);
            }

            if (match) {
                int boost = Math.min(30, pojo.timesContacted);
                if (pojo.starred) {
                    boost += 40;
                }
                pojo.boostRelevance(boost);
                if (!searcher.addResult(pojo))
                    return;
            }
        }
    }

    /**
     * Find a ContactsPojo from a phoneNumber
     * If many contacts match, the one most often contacted will be returned
     *
     * @param phoneNumber phone number to find (will be normalized)
     * @return a contactpojo, or null.
     */
    public ContactEntry findByPhone(String phoneNumber) {
        StringNormalizer.Result simplifiedPhoneNumber = PhoneNormalizer.simplifyPhoneNumber(phoneNumber);

        for (ContactEntry pojo : pojos) {
            if (pojo.normalizedPhone.equals(simplifiedPhoneNumber)) {
                return pojo;
            }
        }

        return null;
    }
}
