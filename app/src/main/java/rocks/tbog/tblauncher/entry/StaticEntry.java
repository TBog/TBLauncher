package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.drawable.DrawableCompat;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ActionProvider;
import rocks.tbog.tblauncher.dataprovider.FilterProvider;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class StaticEntry extends EntryItem {
    @DrawableRes
    protected int iconResource;
    protected boolean customIcon;

    public StaticEntry(@NonNull String id, @DrawableRes int icon) {
        super(id);
        iconResource = icon;
    }

    @Override
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {

        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_add));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_remove));

        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_customize));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_action_rename));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));

        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
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
            TBApplication.getApplication(ctx).getDataHandler().renameStaticEntry(id, newName);

            // Show toast message
            String msg = ctx.getString(R.string.app_rename_confirmation, getName());
            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
        })
                .setTitle(R.string.title_static_rename)
                .setNeutralButton(R.string.custom_name_set_default, (dialog, which) -> {
                    Context ctx = dialog.getContext();
                    DataHandler dataHandler = TBApplication.dataHandler(ctx);
                    dataHandler.renameStaticEntry(id, null);

                    String name = null;

                    //TODO: get the original name by recreating the StaticProvider
                    {
                        ActionProvider actionProvider = dataHandler.getActionProvider();
                        if (actionProvider != null && actionProvider.mayFindById(id))
                            name = new ActionProvider(ctx).findById(id).getName();
                    }
                    {
                        FilterProvider filterProvider = dataHandler.getFilterProvider();
                        if (filterProvider != null && filterProvider.mayFindById(id))
                            name = new FilterProvider(ctx).findById(id).getName();
                    }
                    if (name != null) {
                        setName(name);

                        // Show toast message
                        String msg = ctx.getString(R.string.app_rename_confirmation, getName());
                        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                    } else {
                        // can't find the default name. Reload providers and hope to get the name
                        dataHandler.reloadProviders();
                    }

                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_builtin :
                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid :
                        R.layout.item_quick_list);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            nameView.setText(getName());
        else
            nameView.setVisibility(View.GONE);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
    }

    public void setCustomIcon() {
        customIcon = true;
    }

    public void clearCustomIcon() {
        customIcon = false;
    }

    @WorkerThread
    public Drawable getIconDrawable(Context context) {
        if (customIcon) {
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

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        public Drawable getDrawable(Context context) {
            StaticEntry entry = (StaticEntry) entryItem;
            Drawable drawable = entry.getIconDrawable(context);
            if (!entry.customIcon) {
                drawable = DrawableCompat.wrap(drawable);
                int color = Utilities.checkFlag(drawFlags, FLAG_DRAW_WHITE_BG) ? Color.BLACK : Color.WHITE;
                DrawableCompat.setTint(drawable, color);
            }
            return drawable;
        }
    }
}
