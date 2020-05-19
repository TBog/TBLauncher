package rocks.tbog.tblauncher.entry;

import android.net.Uri;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;

public final class ContactEntry extends EntryItem {
    public static final String SCHEME = "contact://";
    public final String lookupKey;

    public final String phone;
    //phone without special characters
    public final StringNormalizer.Result normalizedPhone;
    public final Uri icon;

    // Is this a primary phone?
    public final Boolean primary;

    // How many times did we phone this contact?
    public final int timesContacted;

    // Is this contact starred ?
    public final Boolean starred;

    // Is this number a home (local) number ?
    public final Boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactEntry(String id, String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Uri icon, Boolean primary, int timesContacted, Boolean starred,
                        Boolean homeNumber) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ContactEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.icon = icon;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
        this.homeNumber = homeNumber;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        if (nickname != null) {
            // Set the actual user-friendly name
            this.nickname = nickname;
            this.normalizedNickname = StringNormalizer.normalizeWithResult(this.nickname, false);
        } else {
            this.nickname = null;
            this.normalizedNickname = null;
        }
    }
}
