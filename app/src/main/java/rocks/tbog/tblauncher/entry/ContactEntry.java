package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
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
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
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

    // Is this number a home (local) number ?
    public final Boolean homeNumber;

    public StringNormalizer.Result normalizedNickname = null;

    private String nickname = "";

    public ContactEntry(String id, String lookupKey, String phone, StringNormalizer.Result normalizedPhone,
                        Uri iconUri, Boolean primary, int timesContacted, Boolean starred,
                        Boolean homeNumber) {
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

    public boolean isPrimary() {
        return primary;
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
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
        else
            nameView.setVisibility(View.GONE);

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, AsyncSetEntryIcon.class);
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
        contactPhone.setTextColor(UIColors.getResultText2Color(context));
        ResultViewHelper.displayHighlighted(relevanceSource, normalizedPhone, phone, relevance, contactPhone);

        // Contact nickname
        TextView contactNickname = view.findViewById(R.id.item_contact_nickname);
        contactNickname.setTextColor(UIColors.getResultTextColor(context));
        if (getNickname().isEmpty()) {
            contactNickname.setVisibility(View.GONE);
        } else {
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedNickname, getNickname(), relevance, contactNickname);
        }

        // Contact photo
        ImageView contactIcon = view.findViewById(android.R.id.icon);

        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            if (PrefCache.modulateContactIcons(context))
                ResultViewHelper.setIconColorFilter(contactIcon, drawFlags);
            contactIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, contactIcon, AsyncSetEntryIcon.class);
        } else {
            contactIcon.setImageDrawable(null);
            contactIcon.setVisibility(View.GONE);
        }

//        contactIcon.assignContactUri(Uri.withAppendedPath(
//                ContactsContract.Contacts.CONTENT_LOOKUP_URI,
//                String.valueOf(contactPojo.lookupKey)));
//        contactIcon.setExtraOnClickListener(new View.OnClickListener() {
//
//            @Override
//            public void onClick(View v) {
//                recordLaunch(v.getContext(), queryInterface);
//            }
//        });

        int contactActionColor = UIColors.getContactActionColor(context);
        // Phone action
        ImageButton phoneButton = view.findViewById(R.id.item_contact_action_phone);
        phoneButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY);
        // Message action
        ImageButton messageButton = view.findViewById(R.id.item_contact_action_message);
        messageButton.setColorFilter(contactActionColor, PorterDuff.Mode.MULTIPLY);

        PackageManager pm = context.getPackageManager();

        if (pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            phoneButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.VISIBLE);
            phoneButton.setOnClickListener(v -> {
                ResultHelper.recordLaunch(this, context);
                ResultHelper.launchCall(v.getContext(), v, phone);
            });

            messageButton.setOnClickListener(v -> {
                ResultHelper.recordLaunch(this, context);
                ResultHelper.launchMessaging(this, v);
            });

            if (homeNumber)
                messageButton.setVisibility(View.INVISIBLE);
            else
                messageButton.setVisibility(View.VISIBLE);

        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }

        ResultViewHelper.applyPreferences(drawFlags, contactName, contactPhone, contactIcon);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        SharedPreferences settingPrefs = PreferenceManager.getDefaultSharedPreferences(v.getContext());
        boolean callContactOnClick = settingPrefs.getBoolean("call-contact-on-click", false);

        if (callContactOnClick) {
            ResultHelper.launchCall(context, v, phone);
        } else {
            ResultHelper.launchContactView(this, context, v);
        }
    }

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
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

}
