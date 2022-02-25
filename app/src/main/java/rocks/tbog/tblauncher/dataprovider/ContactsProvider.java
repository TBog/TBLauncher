package rocks.tbog.tblauncher.dataprovider;

import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceManager;

import java.util.Collection;

import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.loader.LoadContactsEntry;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearcher;
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
    public void requestResults(String query, ISearcher searcher) {
        for (ContactEntry pojo : pojos)
            pojo.resetResultInfo();

        EntryToResultUtils.recursiveWordCheck(pojos, query, searcher, ContactsProvider::checkResults, ContactEntry.class);
    }

    @WorkerThread
    public static void checkResults(Collection<ContactEntry> entries, FuzzyScore fuzzyScore, ISearcher searcher) {
        Log.d(TAG, "checkResults count=" + entries.size() + " " + fuzzyScore);

        for (ContactEntry entry : entries) {
            FuzzyScore.MatchInfo scoreInfo = fuzzyScore.match(entry.normalizedName.codePoints);

            StringNormalizer.Result matchedText = entry.normalizedName;
            FuzzyScore.MatchInfo matchedInfo = FuzzyScore.MatchInfo.copyOrNewInstance(scoreInfo, null);

            if (entry.normalizedNickname != null) {
                scoreInfo = fuzzyScore.match(entry.normalizedNickname.codePoints);
                if (scoreInfo.match && (!matchedInfo.match || scoreInfo.score > matchedInfo.score)) {
                    matchedText = entry.normalizedNickname;
                    matchedInfo = FuzzyScore.MatchInfo.copyOrNewInstance(scoreInfo, matchedInfo);
                }
            }
            if (!matchedInfo.match && entry.normalizedPhone != null && fuzzyScore.getPatternLength() > 2) {
                // search for the phone number
                scoreInfo = fuzzyScore.match(entry.normalizedPhone.codePoints);
                if (scoreInfo.match && scoreInfo.score > matchedInfo.score) {
                    matchedText = entry.normalizedPhone;
                    matchedInfo = FuzzyScore.MatchInfo.copyOrNewInstance(scoreInfo, matchedInfo);
                }
            }

            entry.addResultMatch(matchedText, matchedInfo);

            if (matchedInfo.match) {
                int boost = Math.min(30, entry.getTimesContacted());
                if (entry.isStarred()) {
                    boost += 40;
                }
                entry.boostRelevance(boost);
                if (!searcher.addResult(entry))
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
