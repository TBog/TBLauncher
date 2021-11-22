package rocks.tbog.tblauncher.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.MimeTypeCache;
import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.MimeTypeUtils;
import rocks.tbog.tblauncher.utils.Timer;

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
    protected ArrayList<ContactEntry> doInBackground(Void param) {
        Timer timer = Timer.startNano();

        ArrayList<ContactEntry> contacts = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return contacts;
        }

        // Skip if we don't have permission to list contacts yet:(
        if (!Permission.checkPermission(ctx, Permission.PERMISSION_READ_CONTACTS)) {
            return contacts;
        }

        // Skip if we don't have any mime types to be shown
        Set<String> mimeTypes = MimeTypeUtils.getActiveMimeTypes(ctx);
        if (mimeTypes.isEmpty()) {
            return contacts;
        }

        final ContentResolver contentResolver = ctx.getContentResolver();
        Map<String, BasicContact> basicContacts = loadBasicContacts(contentResolver);
        Map<Long, BasicRawContact> basicRawContacts = loadRawContacts(contentResolver);

        // Retrieve contacts' nicknames
        Cursor nickCursor = contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            new String[]{
                ContactsContract.CommonDataKinds.Nickname.NAME,
                ContactsContract.Data.LOOKUP_KEY},
            ContactsContract.Data.MIMETYPE + "=?",
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
                        BasicContact basicContact = basicContacts.get(lookupKey);
                        if (basicContact != null) {
                            basicContact.setNickName(nick);
                        }
                    }
                }
            }
            nickCursor.close();
        }

        // get mime type labels
        Map<String, String> mimeLabels = TBApplication.mimeTypeCache(ctx).getUniqueLabels(ctx, mimeTypes);

        // Query all mime types
        for (String mimeType : mimeTypes) {
            Timer timerMimeType = Timer.startNano();
            int sizeBefore = contacts.size();
            if (ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE.equals(mimeType)) {
                contacts.addAll(createPhoneContacts(contentResolver, basicContacts, basicRawContacts));
            } else {
                String mimeLabel = mimeLabels.get(mimeType);
                contacts.addAll(createGenericContacts(mimeType, basicContacts, basicRawContacts, mimeLabel));
            }
            int sizeAfter = contacts.size();
            Log.i("time", timerMimeType + " to list " + (sizeAfter - sizeBefore) + " contact(s) for " + mimeType);
        }

        Log.i("time", timer + " to list " + contacts.size() + " contact(s)");
        return contacts;
    }

    @NonNull
    private static Map<String, BasicContact> loadBasicContacts(@NonNull ContentResolver contentResolver) {
        // Run query
        Cursor contactCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.PHOTO_ID,
                ContactsContract.Contacts.PHOTO_URI},
            null, null, null);

        if (contactCursor == null)
            return Collections.emptyMap();

        if (contactCursor.getCount() == 0) {
            contactCursor.close();
            return Collections.emptyMap();
        }

        // Query basic contact information and keep in memory to prevent duplicates
        Map<String, BasicContact> basicContacts = new HashMap<>(contactCursor.getCount());

        int lookupIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
        int contactIdIndex = contactCursor.getColumnIndex(ContactsContract.Contacts._ID);
        int displayNameIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY);
        int photoIdIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_ID);
        int photoUriIndex = contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_URI);
        while (contactCursor.moveToNext()) {
            BasicContact basicContact = new BasicContact(
                contactCursor.getString(lookupIndex),
                contactCursor.getLong(contactIdIndex),
                contactCursor.getString(displayNameIndex),
                contactCursor.getString(photoIdIndex),
                contactCursor.getString(photoUriIndex)
            );
            basicContacts.put(basicContact.getLookupKey(), basicContact);
        }
        contactCursor.close();

        return basicContacts;
    }

    private static Map<Long, BasicRawContact> loadRawContacts(@NonNull ContentResolver contentResolver) {
        // Query raw contact information and keep in memory to prevent duplicates
        Cursor rawContactCursor = contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            new String[]{ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.STARRED},
            null, null, null);
        if (rawContactCursor == null) {
            return Collections.emptyMap();
        }
        if (rawContactCursor.getCount() == 0) {
            rawContactCursor.close();
            return Collections.emptyMap();
        }
        Map<Long, BasicRawContact> basicRawContacts = new HashMap<>(rawContactCursor.getCount());
        int rawContactIdIndex = rawContactCursor.getColumnIndex(ContactsContract.RawContacts._ID);
        int accountTypeIndex = rawContactCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE);
        int starredIndex = rawContactCursor.getColumnIndex(ContactsContract.RawContacts.STARRED);
        while (rawContactCursor.moveToNext()) {
            BasicRawContact basicRawContact = new BasicRawContact(
                rawContactCursor.getLong(rawContactIdIndex),
                rawContactCursor.getString(accountTypeIndex),
                rawContactCursor.getInt(starredIndex) != 0
            );
            basicRawContacts.put(basicRawContact.getId(), basicRawContact);
        }
        rawContactCursor.close();
        return basicRawContacts;
    }

    private static ArrayList<ContactEntry> createPhoneContacts(@NonNull ContentResolver contentResolver, Map<String, BasicContact> basicContacts, Map<Long, BasicRawContact> basicRawContacts) {
        // Query all phone numbers
        Cursor phoneCursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            new String[]{ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.IS_PRIMARY}, null, null, null);

        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactEntry>> mapContacts = new HashMap<>();

        if (phoneCursor != null) {
            if (phoneCursor.getCount() > 0) {
                int lookupIndex = phoneCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY);
                int rawContactIdIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.RAW_CONTACT_ID);
                int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                int isPrimaryIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.IS_PRIMARY);

                while (phoneCursor.moveToNext()) {
                    String lookupKey = phoneCursor.getString(lookupIndex);
                    BasicContact basicContact = basicContacts.get(lookupKey);
                    long rawContactId = phoneCursor.getLong(rawContactIdIndex);
                    BasicRawContact basicRawContact = basicRawContacts.get(rawContactId);

                    if (basicContact != null && basicRawContact != null) {
                        String name = basicContact.getDisplayName();
                        long contactId = basicContact.getContactId();

                        String phone = phoneCursor.getString(numberIndex);
                        if (phone == null) {
                            phone = "";
                        }

                        StringNormalizer.Result normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
                        boolean starred = basicRawContact.isStarred();
                        boolean primary = phoneCursor.getInt(isPrimaryIndex) != 0;
                        Uri icon = basicContact.getIcon();

                        ContactEntry contact = ContactEntry.newPhoneContact(contactId, phone, normalizedPhone, lookupKey, icon, primary, starred);

                        contact.setName(name);
                        contact.setNickname(basicContact.getNickName());

                        addContactToMap(contact, mapContacts);
                    }
                }
            }
            phoneCursor.close();
        }

        return getFilteredContacts(mapContacts, contact -> contact.normalizedPhone.toString());
    }

    private ArrayList<ContactEntry> createGenericContacts(String mimeType, Map<String, BasicContact> basicContacts, Map<Long, BasicRawContact> basicRawContacts, String mimeLabel) {
        final MimeTypeCache mimeTypeCache = TBApplication.mimeTypeCache(context.get());
        // Prevent duplicates by keeping in memory encountered contacts.
        Map<String, Set<ContactEntry>> mapContacts = new HashMap<>();

        List<String> columns = new ArrayList<>();
        columns.add(ContactsContract.Data.LOOKUP_KEY);
        columns.add(ContactsContract.Data.RAW_CONTACT_ID);
        columns.add(ContactsContract.Data._ID);
        columns.add(ContactsContract.Data.IS_PRIMARY);

        String detailColumn = mimeTypeCache.getDetailColumn(context.get(), mimeType);
        if (detailColumn != null && !columns.contains(detailColumn)) {
            columns.add(detailColumn);
        }

        // Query all entries by mimeType
        Cursor mimeTypeCursor = context.get().getContentResolver().query(
            ContactsContract.Data.CONTENT_URI,
            columns.toArray(new String[]{}),
            ContactsContract.Data.MIMETYPE + "= ?",
            new String[]{mimeType}, null);
        if (mimeTypeCursor != null) {
            if (mimeTypeCursor.getCount() > 0) {
                int lookupIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.LOOKUP_KEY);
                int rawContactIdIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID);
                int idIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data._ID);
                int isPrimaryIndex = mimeTypeCursor.getColumnIndex(ContactsContract.Data.IS_PRIMARY);
                int detailColumnIndex = -1;
                if (detailColumn != null) {
                    detailColumnIndex = mimeTypeCursor.getColumnIndex(detailColumn);
                }
                while (mimeTypeCursor.moveToNext()) {
                    String lookupKey = mimeTypeCursor.getString(lookupIndex);
                    BasicContact basicContact = basicContacts.get(lookupKey);
                    long rawContactId = mimeTypeCursor.getLong(rawContactIdIndex);
                    BasicRawContact basicRawContact = basicRawContacts.get(rawContactId);

                    if (basicContact != null && basicRawContact != null) {
                        long contactId = basicContact.getContactId();
                        long id = mimeTypeCursor.getLong(idIndex);
                        boolean primary = mimeTypeCursor.getInt(isPrimaryIndex) != 0;
                        String label = null;
                        if (detailColumnIndex >= 0) {
                            label = mimeTypeCursor.getString(detailColumnIndex);
                        }
                        if (label == null) {
                            label = mimeTypeCache.getLabel(context.get(), mimeType);
                        }
                        Uri icon = basicContact.getIcon();

                        ContactEntry contact = ContactEntry.newGenericContact(contactId, MimeTypeUtils.getShortMimeType(mimeType), id, lookupKey, icon, primary, basicRawContact.isStarred());

                        contact.setName(basicContact.getDisplayName());
                        contact.setNickname(basicContact.getNickName());
                        ContactEntry.ImData imData = new ContactEntry.ImData(mimeType, id, mimeLabel);
                        imData.setIdentifier(label);
                        contact.setIm(imData);

                        addContactToMap(contact, mapContacts);
                    }
                }
            }
            mimeTypeCursor.close();
        }

        return getFilteredContacts(mapContacts, contact -> contact.getImData().getIdentifier());
    }

    /**
     * add contact to mapContacts, grouped by lookup key
     *
     * @param contact
     * @param mapContacts
     */
    private static void addContactToMap(@NonNull ContactEntry contact, @NonNull Map<String, Set<ContactEntry>> mapContacts) {
        Set<ContactEntry> mimeTypes = mapContacts.get(contact.lookupKey);
        if (mimeTypes == null) {
            mimeTypes = new HashSet<>(1);
            mapContacts.put(contact.lookupKey, mimeTypes);
        }
        mimeTypes.add(contact);
    }

    /**
     * Filter all contacts dependent of fields.
     * Return primary contacts if available.
     * If no primary contacts are available all contacts are returned.
     *
     * @param mapContacts all contacts grouped by lookup key
     * @param idSupplier  id supplier for identifying duplicates
     * @return filtered contacts
     */
    private static ArrayList<ContactEntry> getFilteredContacts(Map<String, Set<ContactEntry>> mapContacts, IdSupplier idSupplier) {
        ArrayList<ContactEntry> contacts = new ArrayList<>();
        // Add phone numbers
        for (Set<ContactEntry> mappedContacts : mapContacts.values()) {
            // Find primary phone and add this one.
            boolean hasPrimary = false;
            for (ContactEntry contact : mappedContacts) {
                if (contact.isPrimary()) {
                    contacts.add(contact);
                    hasPrimary = true;
                    break;
                }
            }

            // If no primary available, add all (excluding duplicates).
            if (!hasPrimary) {
                HashSet<String> added = new HashSet<>(mappedContacts.size());
                for (ContactEntry contact : mappedContacts) {
                    String id = idSupplier.getId(contact);
                    if (id == null) {
                        contacts.add(contact);
                    } else if (!added.contains(id)) {
                        added.add(id);
                        contacts.add(contact);
                    }
                }
            }
        }
        return contacts;
    }

    // TODO: move to separate class, which package?
    @FunctionalInterface
    public interface IdSupplier {
        String getId(ContactEntry contact);
    }

    // TODO: move to separate class, which package?
    private static class BasicContact {
        private final String lookupKey;
        private final long contactId;
        private final String displayName;
        private final String photoId;
        private final String photoUri;
        private String nickName;
        private String mimeTypeLabel;

        private BasicContact(String lookupKey, long contactId, String displayName, String photoId, String photoUri) {
            this.lookupKey = lookupKey;
            this.contactId = contactId;
            this.displayName = displayName;
            this.photoId = photoId;
            this.photoUri = photoUri;
        }

        public String getLookupKey() {
            return lookupKey;
        }

        public long getContactId() {
            return contactId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public void setLabel(String label) {
            mimeTypeLabel = label;
        }

        public Uri getIcon() {
            if (photoUri != null) {
                return Uri.parse(photoUri);
            }
            if (photoId != null) {
                return ContentUris.withAppendedId(ContactsContract.Data.CONTENT_URI,
                    Long.parseLong(photoId));

            }
            return null;
        }
    }

    // TODO: move to separate class, which package?
    private static class BasicRawContact {
        private final long id;
        private final String accountType;
        private final boolean starred;

        private BasicRawContact(long id, String accountType, boolean starred) {
            this.id = id;
            this.accountType = accountType;
            this.starred = starred;
        }

        public long getId() {
            return id;
        }

        public String getAccountType() {
            return accountType;
        }

        public boolean isStarred() {
            return starred;
        }
    }

}
