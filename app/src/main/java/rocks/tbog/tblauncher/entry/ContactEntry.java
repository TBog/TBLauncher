package rocks.tbog.tblauncher.entry;

import static rocks.tbog.tblauncher.customicon.ButtonHelper.BTN_ID_MESSAGE;
import static rocks.tbog.tblauncher.customicon.ButtonHelper.BTN_ID_OPEN;
import static rocks.tbog.tblauncher.customicon.ButtonHelper.BTN_ID_PHONE;

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
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.io.FileNotFoundException;
import java.io.InputStream;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.customicon.ButtonHelper;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.AsyncSetEntryDrawable;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.PackageManagerUtils;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class ContactEntry extends EntryItem {
    public static final String SCHEME = "contact://";
    private static final int[] RESULT_LAYOUT = {R.layout.item_contact, R.layout.item_grid, R.layout.item_dock};
    private static final String BTN_ACTION_PHONE = "phone";
    private static final String BTN_ACTION_MESSAGE = "message";
    private static final String BTN_ACTION_OPEN = "open";
    public String lookupKey;

    protected String phone;
    //phone without special characters
    public StringNormalizer.Result normalizedPhone;
    protected Uri iconUri = null;

    // Is this a primary phone?
    protected boolean primary = false;

    // How many times did we phone this contact?
    protected int timesContacted = 0;

    // Is this contact starred ?
    protected boolean starred = false;

    // Is this number a home (local / landline) number? We can't send messages to this.
    protected boolean homeNumber = false;

    public StringNormalizer.Result normalizedNickname = null;

    protected String nickname = "";

    protected ImData imData;

    public ContactEntry(String id) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ContactEntry.class.getSimpleName() + " id `" + id + "`");
        }
    }

    protected void setNickname(String nickname) {
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

    public boolean isStarred() {
        return starred;
    }

    public ImData getImData() {
        return imData;
    }

    public boolean isHomeNumber() {
        return homeNumber;
    }

    public int getTimesContacted() {
        return timesContacted;
    }

    public String getPhone() {
        return phone != null ? phone : "";
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public static int[] getResultLayout() {
        return RESULT_LAYOUT;
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? RESULT_LAYOUT[0] :
            (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? RESULT_LAYOUT[1] :
                RESULT_LAYOUT[2]);
    }

    @WorkerThread
    protected Drawable getIconDrawable(Context ctx) {
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

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST)) {
            displayListResult(view, drawFlags);
            ResultViewHelper.applyListRowPreferences((ViewGroup) view);
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
            ResultViewHelper.displayHighlighted(relevance, normalizedName, getName(), nameView);
            nameView.setVisibility(View.VISIBLE);
        } else {
            nameView.setText(getName());
            nameView.setVisibility(View.GONE);
        }

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            else
                ResultViewHelper.removeIconColorFilter(contactIcon);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, SetContactIconAsync.class, ContactEntry.class);
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
        ResultViewHelper.displayHighlighted(relevance, normalizedName, getName(), contactName);

        // Contact phone
        TextView contactPhone = view.findViewById(R.id.item_contact_phone);
        if (phone != null) {
            contactPhone.setVisibility(View.VISIBLE);
            contactPhone.setTextColor(UIColors.getResultText2Color(context));
            ResultViewHelper.displayHighlighted(relevance, normalizedPhone, phone, contactPhone);
            ResultViewHelper.applyResultItemShadow(contactPhone);
        } else if (getImData() != null && getImData().label != null) {
            contactPhone.setVisibility(View.VISIBLE);
            contactPhone.setTextColor(UIColors.getResultText2Color(context));
            contactPhone.setText(getImData().label);
            ResultViewHelper.applyResultItemShadow(contactPhone);
        } else {
            contactPhone.setVisibility(View.GONE);
        }

        displayNickname(view);

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            else
                ResultViewHelper.removeIconColorFilter(contactIcon);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, SetContactIconAsync.class, ContactEntry.class);
        } else {
            contactIcon.setImageDrawable(null);
            contactIcon.setVisibility(View.GONE);
        }

        final PackageManager pm = context.getPackageManager();
        boolean hasPhone = phone != null && pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
        displayActions(view, hasPhone);

        // App icon
        {
            final ImageView appIcon = view.findViewById(android.R.id.icon2);
            if (getImData() != null) {
                appIcon.setVisibility(View.VISIBLE);
                // bypass cache or else the app icon is cached as the contact icon
                ResultViewHelper.setIconAsync(drawFlags | FLAG_RELOAD | FLAG_DRAW_NO_CACHE, this, appIcon, SetAppIconAsync.class, ContactEntry.class);
            } else {
                appIcon.setVisibility(View.GONE);
            }
        }

        ResultViewHelper.applyPreferences(drawFlags, contactName, contactPhone, contactIcon);
    }

    private void displayNickname(View root) {
        Context context = root.getContext();
        // Contact nickname
        TextView contactNickname = root.findViewById(R.id.item_contact_nickname);
        contactNickname.setTextColor(UIColors.getResultTextColor(context));
        if (TextUtils.isEmpty(nickname)) {
            contactNickname.setVisibility(View.GONE);
        } else {
            contactNickname.setVisibility(View.VISIBLE);
            ResultViewHelper.displayHighlighted(relevance, normalizedNickname, nickname, contactNickname);
            ResultViewHelper.applyResultItemShadow(contactNickname);
        }
    }

    private void displayActions(View root, boolean hasPhone) {
        Context context = root.getContext();
        final int contactActionColor = UIColors.getContactActionColor(context);
        // Phone action
        {
            ImageButton phoneButton = root.findViewById(R.id.item_contact_action_phone);
            if (hasPhone) {
                phoneButton.setVisibility(View.VISIBLE);
                phoneButton.clearColorFilter();
                ResultViewHelper.setButtonIconAsync(phoneButton, BTN_ID_PHONE, ctx -> {
                    phoneButton.post(() -> phoneButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY));
                    return ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.ic_phone, null);
                });
                phoneButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchCall(v.getContext(), v, phone);
                });
                phoneButton.setOnLongClickListener(v -> showButtonPopup(v, BTN_ID_PHONE, R.drawable.ic_phone));
            } else {
                phoneButton.setVisibility(View.GONE);
            }
        }

        // Message action
        {
            ImageButton messageButton = root.findViewById(R.id.item_contact_action_message);
            if (hasPhone && !isHomeNumber()) {
                messageButton.setVisibility(View.VISIBLE);
                messageButton.clearColorFilter();
                ResultViewHelper.setButtonIconAsync(messageButton, BTN_ID_MESSAGE, ctx -> {
                    messageButton.post(() -> messageButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY));
                    return ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.ic_message, null);
                });
                messageButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchMessaging(this, v);
                });
                messageButton.setOnLongClickListener(v -> showButtonPopup(v, BTN_ID_MESSAGE, R.drawable.ic_message));
            } else {
                messageButton.setVisibility(View.GONE);
            }
        }

        // Open action
        {
            ImageButton openButton = root.findViewById(R.id.item_contact_action_open);
            if (getImData() != null) {
                openButton.setVisibility(View.VISIBLE);
                openButton.clearColorFilter();
                ResultViewHelper.setButtonIconAsync(openButton, BTN_ID_OPEN, ctx -> {
                    openButton.post(() -> openButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY));
                    return ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.ic_send, null);
                });
                openButton.setOnClickListener(v -> {
                    ResultHelper.recordLaunch(this, context);
                    ResultHelper.launchIm(getImData(), v);
                });
                openButton.setOnLongClickListener(v -> showButtonPopup(v, BTN_ID_OPEN, R.drawable.ic_send));
            } else {
                openButton.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        ResultHelper.recordLaunch(this, context);

        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        String btnAction = settingPrefs.getString("default-contact-action", "");

        switch (btnAction) {
            case BTN_ACTION_PHONE:
                if (phone != null)
                    ResultHelper.launchCall(context, v, phone);
                else if (getImData() != null)
                    ResultHelper.launchIm(getImData(), v);
                break;
            case BTN_ACTION_MESSAGE:
                ResultHelper.launchMessaging(this, v);
                break;
            case BTN_ACTION_OPEN:
                if (getImData() != null)
                    ResultHelper.launchIm(getImData(), v);
                else if (phone != null)
                    ResultHelper.launchCall(context, v, phone);
                break;
            default:
                ResultHelper.launchContactView(this, context, v);
                break;
        }
    }

    @NonNull
    private static String getButtonIdFromAction(@NonNull String btnPref) {
        switch (btnPref) {
            case BTN_ACTION_PHONE:
                return BTN_ID_PHONE;
            case BTN_ACTION_MESSAGE:
                return BTN_ID_MESSAGE;
            case BTN_ACTION_OPEN:
                return BTN_ACTION_OPEN;
            default:
                return "";
        }
    }

    private static boolean showButtonPopup(@NonNull View view, @NonNull String buttonId, @DrawableRes int defaultButtonIcon) {
        final Context context = view.getContext();
        ListPopup buttonMenu = getButtonPopup(context, buttonId, defaultButtonIcon);
        return ButtonHelper.showButtonPopup(view, buttonMenu);
    }

    @NonNull
    public static ListPopup getButtonPopup(Context ctx, @NonNull String buttonId, @DrawableRes int defaultButtonIcon) {
        String btnAction = PreferenceManager.getDefaultSharedPreferences(ctx).getString("default-contact-action", "");
        String defaultBtnId = getButtonIdFromAction(btnAction);

        LinearAdapter adapter = new LinearAdapter();
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_custom_icon));
        if (!defaultBtnId.equals(buttonId))
            adapter.add(new LinearAdapter.Item(ctx, R.string.contact_button_set_default));
        else
            adapter.add(new LinearAdapter.Item(ctx, R.string.contact_button_reset_default));

        return ListPopup.create(ctx, adapter).setOnItemClickListener((a, view, pos) -> {
            LinearAdapter.MenuItem menuItem = ((LinearAdapter) a).getItem(pos);
            @StringRes int id = 0;
            if (menuItem instanceof LinearAdapter.Item) {
                id = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            if (id == R.string.menu_custom_icon) {
                TBApplication.behaviour(ctx).launchCustomIconDialog(buttonId, defaultButtonIcon, () -> {
                    // force a result refresh to update the icons from all contact buttons
                    var activity = TBApplication.launcherActivity(ctx);
                    if (activity != null)
                        activity.refreshSearchRecords();

                });
            } else if (id == R.string.contact_button_set_default) {
                SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(ctx);
                var editor = settingPrefs.edit();
                switch (buttonId) {
                    case BTN_ID_PHONE:
                        editor.putString("default-contact-action", BTN_ACTION_PHONE).apply();
                        //editor.putBoolean("call-contact-on-click", true).apply();
                        break;
                    case BTN_ID_MESSAGE:
                        editor.putString("default-contact-action", BTN_ACTION_MESSAGE).apply();
                        break;
                    case BTN_ID_OPEN:
                        editor.putString("default-contact-action", BTN_ACTION_OPEN).apply();
                        break;
                    default:
                        editor.putString("default-contact-action", "").apply();
                        break;
                }
            } else if (id == R.string.contact_button_reset_default) {
                PreferenceManager.getDefaultSharedPreferences(ctx)
                    .edit()
                    .putString("default-contact-action", "")
                    .apply();
            }
        });
    }

    public static class SetContactIconAsync extends AsyncSetEntryDrawable<ContactEntry> {
        public SetContactIconAsync(@NonNull ImageView image, int drawFlags, @NonNull ContactEntry contactEntry) {
            super(image, drawFlags, contactEntry);
        }

        @Override
        protected Drawable getDrawable(Context ctx) {
            return entryItem.getIconDrawable(ctx);
        }
    }

    public static class SetAppIconAsync extends AsyncSetEntryDrawable<ContactEntry> {
        public SetAppIconAsync(@NonNull ImageView image, int drawFlags, @NonNull ContactEntry contactEntry) {
            super(image, drawFlags, contactEntry);
        }

        @Override
        protected Drawable getDrawable(Context context) {
            IconsHandler iconsHandler = TBApplication.iconsHandler(context);
            ImData imData = entryItem.getImData();
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

        public ImData(String mimeType, long id, String label) {
            this.mimeType = mimeType;
            this.id = id;
            this.label = label;
        }

        public String getIdentifier() {
            return identifier;
        }

        public void setIdentifier(String identifier) {
            this.identifier = identifier;
        }

        public String getMimeType() {
            return mimeType;
        }

        public long getId() {
            return id;
        }
    }

    public static class Builder {
        private String name = null;
        private String phone = null;
        private String nickname = null;
        private Uri iconUri = null;
        private ImData imData = null;
        private String shortMimeType = null;
        private String lookupKey = null;
        private boolean primary = false;
        private boolean starred = false;

        private long contactId = 0;
        private long contentId = 0;

        public Builder setContactId(long contactId) {
            this.contactId = contactId;
            return this;
        }

        public Builder setPhone(String phone) {
            this.phone = phone;
            return this;
        }

        public Builder setMimeInfo(long contentId, @NonNull String shortMimeType) {
            this.contentId = contentId;
            this.shortMimeType = shortMimeType;
            return this;
        }

        public Builder setIconUri(Uri iconUri) {
            this.iconUri = iconUri;
            return this;
        }

        public Builder setPrimary(boolean primary) {
            this.primary = primary;
            return this;
        }

        public Builder setStarred(boolean starred) {
            this.starred = starred;
            return this;
        }

        public Builder setLookupKey(String lookupKey) {
            this.lookupKey = lookupKey;
            return this;
        }

        public Builder setName(@NonNull String name) {
            this.name = name;
            return this;
        }

        public Builder setNickname(@NonNull String nickname) {
            this.nickname = nickname;
            return this;
        }

        public Builder setImData(@NonNull ImData imData) {
            this.imData = imData;
            return this;
        }

        public ContactEntry getContact() {
            final String entryId;
            if (shortMimeType != null) {
                // this is a general contact. No phone number.
                entryId = SCHEME + contactId + '/' + shortMimeType + '/' + contentId;
                //entry = new ContactEntry(entryId, lookupKey, icon, primary, 0, starred, false);
            } else {
                // phone contact
                entryId = SCHEME + contactId + '/' + phone;
                //entry = new ContactEntry(entryId, lookupKey, phone, normalizedPhone, icon, primary, 0, starred, false);
            }

            ContactEntry entry = new ContactEntry(entryId);
            entry.lookupKey = lookupKey;
            if (phone != null) {
                entry.phone = phone;
                entry.normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
            }
            if (iconUri != null)
                entry.iconUri = iconUri;
            entry.primary = primary;
            entry.starred = starred;
            entry.setName(name);
            entry.setNickname(nickname);
            if (imData != null)
                entry.imData = imData;

            return entry;
        }
    }
}
