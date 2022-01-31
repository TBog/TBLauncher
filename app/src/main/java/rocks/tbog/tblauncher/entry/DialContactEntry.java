package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.normalizer.PhoneNormalizer;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

public class DialContactEntry extends ContactEntry {
    public static final String SCHEME = ContactEntry.SCHEME + "dial/";

    private int customIcon = 0;

    public DialContactEntry() {
        super(SCHEME);
    }

    @Override
    public String getHistoryId() {
        // Dial number should not appear in history
        return "";
    }

    @NonNull
    @Override
    public String getIconCacheId() {
        // use same id for any dialed phone
        return id + "/ic" + customIcon;
    }

    public void setCustomIcon() {
        customIcon += 1;
    }

    public void clearCustomIcon() {
        customIcon = 0;
    }

    public boolean hasCustomIcon() {
        return customIcon > 0;
    }

    @Override
    protected Drawable getIconDrawable(Context ctx) {
        if (hasCustomIcon()) {
            IconsHandler iconsHandler = TBApplication.getApplication(ctx).iconsHandler();
            Drawable drawable = iconsHandler.getCustomIcon(this);
            if (drawable != null)
                return drawable;
            else
                iconsHandler.restoreDefaultIcon(this);
        }
        return super.getIconDrawable(ctx);
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        List<ContentLoadHelper.CategoryItem> categoryTitle = PrefCache.getResultPopupOrder(context);
        for (ContentLoadHelper.CategoryItem categoryItem : categoryTitle) {
            final int titleStringId = categoryItem.textId;

            if (titleStringId == R.string.popup_title_customize) {
                adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));
            }
        }

        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }

        return inflatePopupMenu(context, adapter);
    }

    @Override
    protected boolean popupMenuClickHandler(@NonNull final View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        if (stringId == R.string.menu_custom_icon) {
            Context ctx = view.getContext();
            TBApplication.behaviour(ctx).launchCustomIconDialog(this);
            return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    public void setPhone(String phone) {
        if (phone != null) {
            this.phone = phone;
            this.normalizedPhone = PhoneNormalizer.simplifyPhoneNumber(phone);
        }
    }
}
