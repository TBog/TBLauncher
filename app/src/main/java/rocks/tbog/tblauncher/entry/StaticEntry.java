package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.result.AsyncSetEntryDrawable;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class StaticEntry extends EntryItem {

    private static final int[] RESULT_LAYOUT = {R.layout.item_builtin, R.layout.item_grid, R.layout.item_dock};

    @DrawableRes
    protected int iconResource;
    protected int customIcon;

    public StaticEntry(@NonNull String id, @DrawableRes int icon) {
        super(id);
        iconResource = icon;
    }

    @NonNull
    @Override
    public String getIconCacheId() {
        return id + customIcon;
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {

        List<ContentLoadHelper.CategoryItem> categoryTitle = PrefCache.getResultPopupOrder(context);

        for (ContentLoadHelper.CategoryItem categoryItem : categoryTitle) {
            int titleStringId = categoryItem.textId;
            if (titleStringId == R.string.popup_title_customize) {
                adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_customize));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_action_rename));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));
            }
        }

        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_quick_list_remove));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }

        return inflatePopupMenu(context, adapter);
    }

    @Override
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        Context ctx = view.getContext();
        switch (stringId) {
            case R.string.menu_action_rename:
                launchRenameDialog(ctx);
                return true;
            case R.string.menu_custom_icon:
                TBApplication.behaviour(ctx).launchCustomIconDialog(this);
                return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    private void launchRenameDialog(@NonNull Context c) {
        DialogHelper.makeRenameDialog(c, getName(), (dialog, newName) -> {
                Context ctx = dialog.getContext();

                // Set new name
                setName(newName);
                TBApplication app = TBApplication.getApplication(ctx);
                app.getDataHandler().renameStaticEntry(this, newName);
                app.behaviour().refreshSearchRecord(StaticEntry.this);

                // Show toast message
                String msg = ctx.getString(R.string.entry_rename_confirmation, getName());
                Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
            })
            .setTitle(R.string.title_static_rename)
            .setNeutralButton(R.string.custom_name_set_default, (dialog, which) -> {
                Context ctx = dialog.requireContext();
                TBApplication app = TBApplication.getApplication(ctx);
                DataHandler dataHandler = app.getDataHandler();
                // restore default name
                String name = dataHandler.renameStaticEntry(this, null);

                if (name != null) {
                    app.behaviour().refreshSearchRecord(StaticEntry.this);

                    // Show toast message
                    String msg = ctx.getString(R.string.entry_rename_confirmation, getName());
                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                }

                dialog.dismiss();
            })
            .show();
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

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        nameView.setText(getName());
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            nameView.setVisibility(View.VISIBLE);
        else
            nameView.setVisibility(View.GONE);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class, StaticEntry.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST))
            ResultViewHelper.applyListRowPreferences((ViewGroup) view);
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
    public boolean isExcludedFromHistory() {
        return true;
    }

    @WorkerThread
    public Drawable getIconDrawable(Context context) {
        if (hasCustomIcon()) {
            IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
            Drawable drawable = iconsHandler.getCustomIcon(this);
            if (drawable != null)
                return drawable;
            else
                iconsHandler.restoreDefaultIcon(this);
        }
        return getDefaultDrawable(context);
    }

    public Drawable getDefaultDrawable(Context context) {
        return AppCompatResources.getDrawable(context, iconResource);
    }

    public static class AsyncSetEntryIcon extends AsyncSetEntryDrawable<StaticEntry> {
        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull StaticEntry staticEntry) {
            super(image, drawFlags, staticEntry);
        }

        @Override
        public Drawable getDrawable(Context context) {
            Drawable drawable = entryItem.getIconDrawable(context);
            if (!entryItem.hasCustomIcon()) {
                drawable = DrawableCompat.wrap(drawable);
                int color = Utilities.checkFlag(drawFlags, FLAG_DRAW_WHITE_BG) ? Color.BLACK : Color.WHITE;
                DrawableCompat.setTint(drawable, color);
            }
            return drawable;
        }
    }
}
