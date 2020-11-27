package rocks.tbog.tblauncher.entry;

import android.annotation.TargetApi;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;


public final class ShortcutEntry extends EntryWithTags {

    public static final String SCHEME = "shortcut://";
    private static final String TAG = "shortcut";

    private final long dbId;
    @NonNull
    public final String packageName;
    @NonNull
    public final String shortcutData;
    @Nullable
    public final ShortcutInfo mShortcutInfo;

    public ShortcutEntry(@NonNull String id, long dbId, @NonNull String packageName, @NonNull String shortcutData) {
        super(id);

        this.dbId = dbId;
        this.packageName = packageName;
        this.shortcutData = shortcutData;
        mShortcutInfo = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public ShortcutEntry(long dbId, @NonNull ShortcutInfo shortcutInfo) {
        super(ShortcutEntry.SCHEME + shortcutInfo.getId());

        this.dbId = dbId;
        packageName = shortcutInfo.getPackage();
        shortcutData = shortcutInfo.getId();
        mShortcutInfo = shortcutInfo;
    }

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(long dbId, String shortcutName) {
        return SCHEME + dbId + "/" + shortcutName.toLowerCase(Locale.ROOT);
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public boolean isOreoShortcut() {
        return mShortcutInfo != null;
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return shortcutData;
    }

    public Drawable getIcon(@NonNull Context context) {
        byte[] iconBlob = DBHelper.getShortcutIcon(context, this.dbId);

        if (iconBlob == null) {
            return null;
        }

        Bitmap bitmap = BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
        if (bitmap == null)
            return null;

        return TBApplication.iconsHandler(context).applyShortcutMask(context, bitmap);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_shortcut :
                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid_shortcut :
                        R.layout.item_quick_list_shortcut);
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
        drawFlags |= FLAG_RELOAD;
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
        else
            nameView.setVisibility(View.GONE);

        ImageView icon1 = view.findViewById(android.R.id.icon1);
        ImageView icon2 = view.findViewById(android.R.id.icon2);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            icon1.setVisibility(View.VISIBLE);
            icon2.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, icon1, AsyncSetEntryIcon.class);
        } else {
            icon1.setImageDrawable(null);
            icon2.setImageDrawable(null);
            icon1.setVisibility(View.GONE);
            icon2.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, icon1);
    }

    private void displayListResult(@NonNull View view, int drawFlags) {
        drawFlags |= FLAG_RELOAD;
        Context context = view.getContext();

        TextView shortcutName = view.findViewById(R.id.item_app_name);
        shortcutName.setTextColor(UIColors.getResultTextColor(view.getContext()));

        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, shortcutName);

        TextView tagsView = view.findViewById(R.id.item_app_tag);
        tagsView.setTextColor(UIColors.getResultText2Color(view.getContext()));

        // Hide tags view if tags are empty
        if (getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (ResultViewHelper.displayHighlighted(relevanceSource, getTags(), relevance, tagsView, context)
                || Utilities.checkFlag(drawFlags, FLAG_DRAW_TAGS)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView shortcutIcon = view.findViewById(android.R.id.icon1);
        final ImageView appIcon = view.findViewById(android.R.id.icon2);

        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            shortcutIcon.setVisibility(View.VISIBLE);
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, shortcutIcon, AsyncSetEntryIcon.class);
        } else {
            shortcutIcon.setImageDrawable(null);
            appIcon.setImageDrawable(null);
            shortcutIcon.setVisibility(View.GONE);
            appIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, shortcutName, tagsView, shortcutIcon);
    }

    @Override
    public void doLaunch(@NonNull View view) {
        Context context = view.getContext();
        if (isOreoShortcut()) {
            // Oreo shortcuts
            doOreoLaunch(context, view, mShortcutInfo);
        } else {
            doShortcutLaunch(context, view, shortcutData);
        }
    }

    public static void doShortcutLaunch(@NonNull Context context, @NonNull View view, @NonNull String shortcutData) {
        View potentialIcon = view.findViewById(android.R.id.icon);
        if (potentialIcon == null) {
            // If favorite, find the icon
            potentialIcon = view.findViewById(R.id.favorite);
        }
        Bundle startActivityOptions = Utilities.makeStartActivityOptions(potentialIcon);

        // Non-oreo shortcuts
        try {
            Intent intent = Intent.parseUri(shortcutData, Intent.URI_INTENT_SCHEME);
            Utilities.setIntentSourceBounds(intent, potentialIcon);

            context.startActivity(intent, startActivityOptions);
        } catch (Exception e) {
            // Application was just removed?
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static void doOreoLaunch(@NonNull Context context, @NonNull View v, @Nullable ShortcutInfo shortcutInfo) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        // Only the default launcher is allowed to start shortcuts
        if (!launcherApps.hasShortcutHostPermission()) {
            Toast.makeText(context, context.getString(R.string.shortcuts_no_host_permission), Toast.LENGTH_LONG).show();
            return;
        }

        View potentialIcon = v.findViewById(android.R.id.icon);
        if (potentialIcon == null) {
            // If favorite, find the icon
            potentialIcon = v.findViewById(R.id.favorite);
        }

        Bundle startActivityOptions = Utilities.makeStartActivityOptions(potentialIcon);
        Rect sourceBounds = Utilities.getOnScreenRect(potentialIcon);

        if (shortcutInfo != null) {
            try {
                launcherApps.startShortcut(shortcutInfo, sourceBounds, startActivityOptions);
                return;
            } catch (ActivityNotFoundException e) {
                Log.e(TAG, "startShortcut", e);
            }
        }

        // Application removed? Invalid shortcut? Shortcut to an app on an unmounted SD card?
        Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
    }

    @Override
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_remove_shortcut));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_add));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_remove));
        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_customize));
        if (getTags().isEmpty())
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_add));
        else
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_edit));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_shortcut_rename));
        //adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));

        if (Utilities.checkFlag(flags, FLAG_POPUP_MENU_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }

        return inflatePopupMenu(context, adapter);
    }

    @Override
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        Context ctx = view.getContext();
        switch (stringId) {
            case R.string.menu_remove_shortcut:
                TBApplication.dataHandler(ctx).removeShortcut(this);
                //TODO: update the adapter now, don't wait for the shortcut reload to finish
                Toast.makeText(ctx, "Shortcut `" + getName() + "` removed.\nPlease wait for the action to propagate", Toast.LENGTH_LONG).show();
                return true;
            case R.string.menu_tags_add:
            case R.string.menu_tags_edit:
                TBApplication.behaviour(ctx).launchEditTagsDialog(this);
                return true;
            case R.string.menu_shortcut_rename:
                launchRenameDialog(ctx);
                return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    private void launchRenameDialog(@NonNull Context ctx) {
        ContextThemeWrapper context = new ContextThemeWrapper(ctx, R.style.NoTitleDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.title_shortcut_rename));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.dialog_rename);
        } else {
            builder.setView(View.inflate(context, R.layout.dialog_rename, null));
        }

        builder.setPositiveButton(R.string.custom_name_rename, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);

            // Set new name
            String newName = input.getText().toString().trim();
            setName(newName);
            TBApplication.getApplication(context).getDataHandler().renameShortcut(this, newName);

            // Show toast message
            String msg = context.getResources().getString(R.string.shortcut_rename_confirmation, getName());
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
        });

        //parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        ((TextView) dialog.findViewById(R.id.rename)).setText(getName());
    }

    @WorkerThread
    public static Drawable getAppDrawable(@NonNull Context context, @NonNull String shortcutData, @NonNull String packageName, @Nullable ShortcutInfo shortcutInfo, boolean isBadge) {
        Drawable appDrawable = null;
        final PackageManager packageManager = context.getPackageManager();
        try {
            List<ResolveInfo> activities = null;
            if (shortcutInfo == null) {
                Intent intent = Intent.parseUri(shortcutData, 0);
                activities = packageManager.queryIntentActivities(intent, 0);
            }
            IconsHandler iconsHandler = TBApplication.iconsHandler(context);
            if (activities != null && !activities.isEmpty()) {
                ResolveInfo mainPackage = activities.get(0);
                String packName = mainPackage.activityInfo.applicationInfo.packageName;
                String actName = mainPackage.activityInfo.name;
                ComponentName className = new ComponentName(packName, actName);
                appDrawable = isBadge
                        ? iconsHandler.getDrawableBadgeForPackage(className, UserHandleCompat.CURRENT_USER)
                        : iconsHandler.getDrawableIconForPackage(className, UserHandleCompat.CURRENT_USER);
            } else {
                // Can't make sense of the intent URI (Oreo shortcut, or a shortcut from an activity that was removed from an installed app)
                // Retrieve app icon
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                    if (shortcutInfo != null) {
                        UserHandleCompat user = new UserHandleCompat(context, shortcutInfo.getUserHandle());
                        ComponentName componentName = shortcutInfo.getActivity();
                        appDrawable = isBadge
                                ? iconsHandler.getDrawableBadgeForPackage(componentName, user)
                                : iconsHandler.getDrawableIconForPackage(componentName, user);
                    }
                }
            }
            if (appDrawable == null) {
                appDrawable = packageManager.getApplicationIcon(packageName);
                iconsHandler.getIconPack().applyBackgroundAndMask(context, appDrawable, false);
            }
        } catch (PackageManager.NameNotFoundException | URISyntaxException e) {
            Log.e("Shortcut", "get app shortcut icon", e);
        }
        return appDrawable;
    }

    public static void setIcons(int drawFlags, @NonNull ImageView icon1, Drawable shortcutDrawable, Drawable appDrawable) {
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON_BADGE)) {
            if (icon1.getParent() instanceof View) {
                ImageView icon2 = ((View) icon1.getParent()).findViewById(android.R.id.icon2);
                if (shortcutDrawable != null) {
                    icon2.setImageDrawable(appDrawable);
                } else {
                    // If no icon found for this shortcut, use app icon
                    icon1.setImageDrawable(appDrawable);
                    icon2.setImageResource(R.drawable.ic_send);
                }
            }
        } else {
            if (shortcutDrawable == null) {
                // If no icon found for this shortcut, use app icon
                icon1.setImageDrawable(appDrawable);
            }
        }
    }

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        Drawable subIcon = null;

        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        public Drawable getDrawable(Context context) {
            ShortcutEntry shortcutEntry = (ShortcutEntry) entryItem;
            Drawable icon = shortcutEntry.getIcon(context);
            if (icon == null) {
                subIcon = ContextCompat.getDrawable(context, R.drawable.ic_send);
                return getAppDrawable(context, shortcutEntry.shortcutData, shortcutEntry.packageName, shortcutEntry.mShortcutInfo, false);
            } else {
                subIcon = getAppDrawable(context, shortcutEntry.shortcutData, shortcutEntry.packageName, shortcutEntry.mShortcutInfo, true);
            }
            return icon;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            // get ImageView before calling super
            ImageView icon1 = getImageView();
            super.onPostExecute(drawable);
            if (icon1 != null)
                setIcons(drawFlags, icon1, drawable, subIcon);
        }
    }

}
