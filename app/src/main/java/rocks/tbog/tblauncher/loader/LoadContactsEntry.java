package rocks.tbog.tblauncher.loader;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;

public class LoadContactsEntry extends LoadEntryItem<ContactEntry> {

    public LoadContactsEntry(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public String getScheme() {
        return ContactEntry.SCHEME;
    }

    @Override
    protected ArrayList<ContactEntry> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<ContactEntry> contacts = new ArrayList<>();
        Context c = context.get();
        if (c == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if (!Permission.checkPermission(c, Permission.PERMISSION_READ_CONTACTS)) {
            return contacts;
        }

        // Run query
        Cursor cur = context.get().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                        ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER,
                        ContactsContract.CommonDataKinds.Phone.STARRED,
                        ContactsContract.CommonDataKinds.Phone.IS_PRIMARY,
                        ContactsContract.Contacts.PHOTO_ID,
                        ContactsContract.Contacts._ID}, null, null, null);

        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactEntry>> mapContacts = new HashMap<>();

        if (cur != null) {
            if (cur.getCount() > 0) {
                int lookupIndex = cur.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int timesContactedIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TIMES_CONTACTED);
                int displayNameIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int numberIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int starredIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED);
                int isPrimaryIndex = cur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);
                int photoIdIndex = cur.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
                int contactIdIndex = cur.getColumnIndex(ContactsContract.Contacts._ID);

                while (cur.moveToNext()) {
                    String lookupKey = cur.getString(lookupIndex);
                    int timesContacted = cur.getInt(timesContactedIndex);
                    String name = cur.getString(displayNameIndex);
                    int contactId = cur.getInt(contactIdIndex);

                    String phone = cur.getString(numberIndex);
                    if (phone == null) {
                        phone = "";
                    }

                    StringNormalizer.Result normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
                    boolean starred = cur.getInt(starredIndex) != 0;
                    boolean primary = cur.getInt(isPrimaryIndex) != 0;
                    String photoId = cur.getString(photoIdIndex);
                    Uri icon = null;
                    if (photoId != null) {
                        icon = ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                                Long.parseLong(photoId));
                    }

                    ContactEntry contact = new ContactEntry(getScheme() + contactId + '/' + phone,
                            lookupKey, phone, normalizedPhone, icon, primary, timesContacted,
                            starred, false);

                    contact.setName(name);

                    if (name != null) {
                        Set<ContactEntry> phones = mapContacts.get(contact.lookupKey);
                        if (phones == null) {
                            mapContacts.put(contact.lookupKey, phones = new HashSet<>());
                        }
                        phones.add(contact);
                    }
                }
            }
            cur.close();
        }

        // Retrieve contacts' nicknames
        Cursor nickCursor = context.get().getContentResolver().query(
                ContactsContract.Data.CONTENT_URI,
                new String[]{
                        ContactsContract.CommonDataKinds.Nickname.NAME,
                        ContactsContract.Data.LOOKUP_KEY},
                ContactsContract.Data.MIMETYPE + "= ?",
                new String[]{ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE},
                null);

        if (nickCursor != null) {
            if (nickCursor.getCount() > 0) {
                int lookupKeyIndex = nickCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int nickNameIndex = nickCursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME);
                while (nickCursor.moveToNext()) {
                    String lookupKey = nickCursor.getString(lookupKeyIndex);
                    String nick = nickCursor.getString(nickNameIndex);

                    if (nick != null && lookupKey != null) {
                        Set<ContactEntry> phones = mapContacts.get(lookupKey);
                        if (phones != null)
                            for (ContactEntry contact : phones) {
                                contact.setNickname(nick);
                            }
                    }
                }
            }
            nickCursor.close();
        }

//        Cursor imCursor = context.get().getContentResolver().query(ContactsContract.Data.CONTENT_URI
//                , new String[]{ContactsContract.Data.LOOKUP_KEY, ContactsContract.CommonDataKinds.Phone.NUMBER}
//                , ContactsContract.Data.MIMETYPE + "= ? AND " + ContactsContract.RawContacts.ACCOUNT_TYPE + "= ?"
//                , new String[]{
//                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
//                        "com.whatsapp"
//                }
//                , null);
//
//        if(imCursor != null) {
//            int lookupIndex = imCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
//            int phoneIndex = imCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
//            while (imCursor.moveToNext()) {
//                String lookupKey = imCursor.getString(lookupIndex);
//                String phone = imCursor.getString(phoneIndex);
//
//                if (phone != null && lookupKey != null && mapContacts.containsKey(lookupKey)) {
//                    for (ContactsPojo contact : mapContacts.get(lookupKey)) {
//                        if(contact.phone.equals(phone)) {
//                            contact.hasIM = true;
//                        }
//                    }
//                }
//            }
//            imCursor.close();
//        }

        for (Set<ContactEntry> phones : mapContacts.values()) {
            // Find primary phone and add this one.
            boolean hasPrimary = false;
            for (ContactEntry contact : phones) {
                if (contact.isPrimary()) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If no primary available, add all (excluding duplicates).
            if (!hasPrimary) {
                HashSet<String> added = new HashSet<>(phones.size());
                for (ContactEntry contact : phones) {
                    if (added.add(contact.normalizedPhone.toString()))
                        contacts.add(contact);
                }
            }
        }
        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list contacts");
        return contacts;
    }
}
