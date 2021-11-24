package rocks.tbog.tblauncher.entry;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.preference.PreferenceManager;

import java.io.FileNotFoundException;
import java.io.InputStream;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.PackageManagerUtils;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public final class ContactEntry extends EntryItem {
    public static final String SCHEME = "contact://";
    public final String lookupKey;

    public final String phone;
    //phone without special characters
    public final StringNormalizer.Result normalizedPhone;
    public final Uri iconUri;

    // Is this a primary phone?
    private final boolean primary;

    // How many times did we phone this contact?
    public final int timesContacted;

    // Is this contact starred ?
    public final Boolean starred;

    // Is this number a home (local / landline) number? We can't send messages to this.
    public final Boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    private ImData imData;

    public ContactEntry(String id, String lookupKey, String phone,
                        StringNormalizer.Result normalizedPhone, Uri iconUri, Boolean primary,
                        int timesContacted, Boolean starred, Boolean homeNumber) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ContactEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.lookupKey = lookupKey;
        this.phone = phone;
        this.normalizedPhone = normalizedPhone;
        this.iconUri = iconUri;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
        this.homeNumber = homeNumber;
    }

    public ContactEntry(String id, String lookupKey, Uri iconUri, Boolean primary,
                        int timesContacted, Boolean starred, Boolean homeNumber) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ContactEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.lookupKey = lookupKey;
        this.phone = null;
        this.normalizedPhone = null;
        this.iconUri = iconUri;
        this.primary = primary;
        this.timesContacted = timesContacted;
        this.starred = starred;
        this.homeNumber = homeNumber;
    }

    public static ContactEntry newPhoneContact(long contactId, String phone, StringNormalizer.Result normalizedPhone, String lookupKey, Uri icon, boolean primary, boolean starred) {
        String entryId = SCHEME + contactId + '/' + phone;
        return new ContactEntry(entryId, lookupKey, phone, normalizedPhone, icon, primary, 0, starred, false);
    }

    public static ContactEntry newGenericContact(long contactId, String shortMimeType, long id, String lookupKey, Uri icon, boolean primary, boolean starred) {
        String entryId = SCHEME + contactId + '/' + shortMimeType + '/' + id;
        return new ContactEntry(entryId, lookupKey, icon, primary, 0, starred, false);
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

    public boolean isPrimary() {
        return primary;
    }

    public void setIm(ImData imData) {
        this.imData = imData;
    }

    public ImData getImData() {
        return imData;
    }

    public boolean isHomeNumber() {
        return homeNumber;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_contact :
            (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid :
                R.layout.item_quick_list);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST)) {
            displayListResult(view, drawFlags);
        } else {
            displayGridResult(view, drawFlags);
        }
    }

    private void displayGridResult(@NonNull View view, int drawFlags) {
        final Context context = view.getContext();
        // Contact name
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(context));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
            nameView.setVisibility(View.VISIBLE);
        } else
            nameView.setVisibility(View.GONE);

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            else
                ResultViewHelper.removeIconColorFilter(contactIcon);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, SetContactIconAsync.class);
        } else {
            contactIcon.setImageDrawable(null);
            contactIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, contactIcon);
    }

    private void displayListResult(@NonNull View view, int drawFlags) {
        final Context context = view.getContext();
        // Contact name
        TextView contactName = view.findViewById(R.id.item_contact_name);
        contactName.setTextColor(UIColors.getResultTextColor(context));
        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, contactName);

        // Contact phone
        TextView contactPhone = view.findViewById(R.id.item_contact_phone);
        if (phone != null) {
            contactPhone.setVisibility(View.VISIBLE);
            contactPhone.setTextColor(UIColors.getResultText2Color(context));
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedPhone, phone, relevance, contactPhone);
        } else if (getImData() != null && getImData().label != null) {
            contactPhone.setVisibility(View.VISIBLE);
            contactPhone.setTextColor(UIColors.getResultText2Color(context));
            contactPhone.setText(getImData().label);
        } else {
            contactPhone.setVisibility(View.GONE);
        }

        // Contact nickname
        {
            TextView contactNickname = view.findViewById(R.id.item_contact_nickname);
            contactNickname.setTextColor(UIColors.getResultTextColor(context));
            if (TextUtils.isEmpty(nickname)) {
                contactNickname.setVisibility(View.GONE);
            } else {
                contactNickname.setVisibility(View.VISIBLE);
                ResultViewHelper.displayHighlighted(relevanceSource, normalizedNickname, nickname, relevance, contactNickname);
            }
        }

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            else
                ResultViewHelper.removeIconColorFilter(contactIcon);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, SetContactIconAsync.class);
        } else {
            contactIcon.setImageDrawable(null);
            contactIcon.setVisibility(View.GONE);
        }

        final int contactActionColor = UIColors.getContactActionColor(context);
        final PackageManager pm = context.getPackageManager();
        boolean hasPhone = phone != null && pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);

        // Phone action
        {
            ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
            if (hasPhone) {
                phoneButton.setVisibility(View.VISIBLE);
                phoneButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY);
                phoneButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchCall(v.getContext(), v, phone);
                });
            } else {
                phoneButton.setVisibility(View.GONE);
            }
        }

        // Message action
        {
            ImageButton messageButton = view.findViewById(R.id.item_contact_action_message);
            if (hasPhone && !isHomeNumber()) {
                messageButton.setVisibility(View.VISIBLE);
                messageButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY);
                messageButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchMessaging(this, v);
                });
            } else {
                messageButton.setVisibility(View.GONE);
            }
        }

        // Open action
        {
            ImageButton openButton = view.findViewById(R.id.item_contact_action_open);
            if (getImData() != null) {
                openButton.setVisibility(View.VISIBLE);
                openButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY);
                openButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchIm(getImData(), v);
                });
            } else {
                openButton.setVisibility(View.GONE);
            }
        }

        // App icon
        {
            final ImageView appIcon = view.findViewById(android.R.id.icon2);
            if (getImData() != null) {
                appIcon.setVisibility(View.VISIBLE);
                ResultViewHelper.setIconAsync(drawFlags, this, appIcon, SetAppIconAsync.class);
            } else {
                appIcon.setVisibility(View.GONE);
            }
        }

        ResultViewHelper.applyPreferences(drawFlags, contactName, contactPhone, contactIcon);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        boolean callContactOnClick = settingPrefs.getBoolean("call-contact-on-click", false);

        if (callContactOnClick) {
            if (phone != null)
                ResultHelper.launchCall(context, v, phone);
            else if (getImData() != null)
                ResultHelper.launchIm(getImData(), v);
        } else {
            ResultHelper.launchContactView(this, context, v);
        }
    }

    public static class SetContactIconAsync extends ResultViewHelper.AsyncSetEntryDrawable {
        public SetContactIconAsync(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        protected Drawable getDrawable(Context ctx) {
            Uri iconUri = ((ContactEntry) entryItem).iconUri;
            Drawable drawable = null;
            if (iconUri != null)
                try {
                    InputStream inputStream = ctx.getContentResolver().openInputStream(iconUri);
                    drawable = Drawable.createFromStream(inputStream, iconUri.toString());
                } catch (FileNotFoundException ignored) {
                }
            if (drawable == null) {
                drawable = AppCompatResources.getDrawable(ctx, R.drawable.ic_contact_placeholder);
                if (drawable == null)
                    drawable = new ColorDrawable(UIColors.getDefaultColor(ctx));
            }
            return TBApplication.iconsHandler(ctx).applyContactMask(ctx, drawable);
        }
    }

    public static class SetAppIconAsync extends ResultViewHelper.AsyncSetEntryDrawable {
        public SetAppIconAsync(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        protected Drawable getDrawable(Context context) {
            IconsHandler iconsHandler = TBApplication.iconsHandler(context);
            ImData imData = ((ContactEntry) entryItem).getImData();
            Drawable appDrawable;
            ComponentName componentName = TBApplication.mimeTypeCache(context).getComponentName(context, imData.getMimeType());
            if (componentName != null) {
                appDrawable = iconsHandler.getDrawableIconForPackage(PackageManagerUtils.getLaunchingComponent(context, componentName), UserHandleCompat.CURRENT_USER);
            } else {
                // This should never happen, let's just return the generic activity icon
                appDrawable = context.getPackageManager().getDefaultActivityIcon();
            }
            return appDrawable;
        }
    }

    // TODO: move to separate class, which package?
    public static class ImData {
        private final long id;
        private final String mimeType;
        private final String label;

        private String identifier;
        // IM name without special characters
        private StringNormalizer.Result normalizedIdentifier;

        public ImData(String mimeType, long id, String label) {
            this.mimeType = mimeType;
            this.id = id;
            this.label = label;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            if (identifier != null) {
                // Set the actual user-friendly name
                this.identifier = identifier;
                this.normalizedIdentifier = StringNormalizer.normalizeWithResult(this.identifier, false);
            } else {
                this.identifier = null;
                this.normalizedIdentifier = null;
            }
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getId() {
            return id;
        }

        public StringNormalizer.Result getNormalizedIdentifier() {
            return normalizedIdentifier;
        }
    }
}
