package rocks.tbog.tblauncher.entry;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.PreferenceManager;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;


public final class ShortcutEntry extends EntryWithTags {

    public static final String SCHEME = "shortcut://";

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
    public ShortcutEntry(@NonNull ShortcutInfo shortcutInfo) {
        super(ShortcutEntry.SCHEME + shortcutInfo.getId());

        dbId = 0;
        packageName = shortcutInfo.getPackage();
        shortcutData = shortcutInfo.getId();
        mShortcutInfo = shortcutInfo;
    }

    /**
     * @return shortcut id generated from shortcut name
     */
    public static String generateShortcutId(String shortcutName) {
        return SCHEME + shortcutName.toLowerCase(Locale.ROOT);
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

    public Bitmap getIcon(Context context) {
        byte[] iconBlob = DBHelper.getShortcutIcon(context, this.dbId);

        if (iconBlob == null) {
            return null;
        }

        return BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout(int drawFlags) {
//        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_shortcut :
//                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid_shortcut :
//                        R.layout.item_quick_list);
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_shortcut : R.layout.item_grid_shortcut;
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
        TextView nameView = view.findViewById(android.R.id.text1);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
        else
            nameView.setVisibility(View.GONE);

        ImageView icon1 = view.findViewById(android.R.id.icon1);
        ImageView icon2 = view.findViewById(android.R.id.icon2);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            icon1.setVisibility(View.VISIBLE);
            icon2.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(this, icon1, AsyncSetEntryIcon.class);
        } else {
            icon1.setImageDrawable(null);
            icon2.setImageDrawable(null);
            icon1.setVisibility(View.GONE);
            icon2.setVisibility(View.GONE);
        }
    }

    private void displayListResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        TextView shortcutName = view.findViewById(R.id.item_app_name);

        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, shortcutName);

        TextView tagsView = view.findViewById(R.id.item_app_tag);

        // Hide tags view if tags are empty
        if (getTags().isEmpty()) {
            tagsView.setVisibility(View.GONE);
        } else if (ResultViewHelper.displayHighlighted(relevanceSource, getTags(), relevance, tagsView, context)
                || Utilities.checkFlag(drawFlags, FLAG_DRAW_TAGS)) {
            tagsView.setVisibility(View.VISIBLE);
        } else {
            tagsView.setVisibility(View.GONE);
        }

        final ImageView shortcutIcon = view.findViewById(R.id.item_shortcut_icon);
        final ImageView appIcon = view.findViewById(R.id.item_app_icon);

        // Retrieve package icon for this shortcut
        final PackageManager packageManager = context.getPackageManager();
        Drawable appDrawable = null;
        try {
            List<ResolveInfo> packages = null;
            if (!isOreoShortcut()) {
                Intent intent = Intent.parseUri(shortcutData, 0);
                packages = packageManager.queryIntentActivities(intent, 0);
            }
            if (packages != null && !packages.isEmpty()) {
                ResolveInfo mainPackage = packages.get(0);
                String packageName = mainPackage.activityInfo.applicationInfo.packageName;
                String activityName = mainPackage.activityInfo.name;
                ComponentName className = new ComponentName(packageName, activityName);
                appDrawable = context.getPackageManager().getActivityIcon(className);
            } else {
                // Can't make sense of the intent URI (Oreo shortcut, or a shortcut from an activity that was removed from an installed app)
                // Retrieve app icon
                appDrawable = packageManager.getApplicationIcon(packageName);
            }
        } catch (PackageManager.NameNotFoundException | URISyntaxException e) {
            Log.e("Shortcut", "get app shortcut icon", e);
        }

        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            Bitmap icon = getIcon(context);
            if (icon != null) {
                BitmapDrawable drawable = new BitmapDrawable(context.getResources(), icon);
                shortcutIcon.setImageDrawable(drawable);
                appIcon.setImageDrawable(appDrawable);
            } else {
                // No icon for this shortcut, use app icon
                shortcutIcon.setImageDrawable(appDrawable);
                appIcon.setImageResource(R.drawable.ic_send);
            }
            if (!prefs.getBoolean("subicon-visible", true)) {
                appIcon.setVisibility(View.GONE);
            }
        } else {
            appIcon.setImageDrawable(null);
            shortcutIcon.setImageDrawable(null);
        }
    }

    @Override
    public void doLaunch(@NonNull View view) {
        Context context = view.getContext();
        if (isOreoShortcut()) {
            // Oreo shortcuts
            doOreoLaunch(context, view);
        } else {
            // Pre-oreo shortcuts
            try {
                Intent intent = Intent.parseUri(shortcutData, Intent.URI_INTENT_SCHEME);
                Utilities.setIntentSourceBounds(intent, view);

                context.startActivity(intent);
            } catch (Exception e) {
                // Application was just removed?
                Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private void doOreoLaunch(Context context, View v) {
        final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;

        // Only the default launcher is allowed to start shortcuts
        if (!launcherApps.hasShortcutHostPermission()) {
            Toast.makeText(context, context.getString(R.string.shortcuts_no_host_permission), Toast.LENGTH_LONG).show();
            return;
        }

        if (mShortcutInfo != null) {
            launcherApps.startShortcut(mShortcutInfo, Utilities.getOnScreenRect(v), null);
            return;
        }

//        LauncherApps.ShortcutQuery query = new LauncherApps.ShortcutQuery();
//        query.setPackage(packageName);
//        query.setShortcutIds(Collections.singletonList(getOreoId()));
//        query.setQueryFlags(FLAG_MATCH_DYNAMIC | FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);
//
//        List<UserHandle> userHandles = launcherApps.getProfiles();
//
//        // Find the correct UserHandle, and launch the shortcut.
//        for (UserHandle userHandle : userHandles) {
//            List<ShortcutInfo> shortcuts = launcherApps.getShortcuts(query, userHandle);
//            if (shortcuts != null && shortcuts.size() > 0 && shortcuts.get(0).isEnabled()) {
//                launcherApps.startShortcut(shortcuts.get(0), Utilities.getOnScreenRect(v), null);
//                return;
//            }
//        }

        // Application removed? Invalid shortcut? Shortcut to an app on an unmounted SD card?
        Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
    }

    @Override
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, ResultAdapter parent, View parentView) {
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
        return inflatePopupMenu(adapter, context);
    }

    @Override
    boolean popupMenuClickHandler(@NonNull Context context, @NonNull LinearAdapter.MenuItem item, int stringId) {
        switch (stringId) {
            case R.string.menu_remove_shortcut:
                TBApplication.getApplication(context).getDataHandler().removeShortcut(this);
                //TODO: update the adapter now, don't wait for the shortcut reload to finish
                Toast.makeText(context, "Shortcut `" + getName() + "` removed.\nPlease wait for the action to propagate", Toast.LENGTH_LONG).show();
                return true;
            case R.string.menu_tags_add:
            case R.string.menu_tags_edit:
                TBApplication.behaviour(context).launchEditTagsDialog(this);
                return true;
            case R.string.menu_shortcut_rename:
                launchRenameDialog(context);
                return true;
        }
        return super.popupMenuClickHandler(context, item, stringId);
    }

    private void launchRenameDialog(@NonNull Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.shortcut_rename_title));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.rename_dialog);
        } else {
            builder.setView(View.inflate(context, R.layout.rename_dialog, null));
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

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        Drawable appDrawable = null;

        public AsyncSetEntryIcon(ImageView image) {
            super(image);
        }

        @Override
        public Drawable getDrawable(EntryItem entry, Context context) {
            ShortcutEntry shortcutEntry = (ShortcutEntry) entry;
            // If no icon found for this shortcut, use app icon
            final PackageManager packageManager = context.getPackageManager();
            try {
                List<ResolveInfo> packages = null;
                if (!shortcutEntry.isOreoShortcut()) {
                    Intent intent = Intent.parseUri(shortcutEntry.shortcutData, 0);
                    packages = packageManager.queryIntentActivities(intent, 0);
                }
                if (packages != null && !packages.isEmpty()) {
                    ResolveInfo mainPackage = packages.get(0);
                    String packageName = mainPackage.activityInfo.applicationInfo.packageName;
                    String activityName = mainPackage.activityInfo.name;
                    UserHandleCompat user = new UserHandleCompat();
                    ComponentName className = new ComponentName(packageName, activityName);
                    String appId = AppEntry.SCHEME + user.getUserComponentName(className);
                    appDrawable = TBApplication.drawableCache(context).getCachedDrawable(appId);
                    if (appDrawable == null) {
                        appDrawable = context.getPackageManager().getActivityIcon(className);
                    }
                } else {
                    // Can't make sense of the intent URI (Oreo shortcut, or a shortcut from an activity that was removed from an installed app)
                    // Retrieve app icon
                    appDrawable = packageManager.getApplicationIcon(shortcutEntry.packageName);
                }
            } catch (PackageManager.NameNotFoundException | URISyntaxException e) {
                Log.e("Shortcut", "get app shortcut icon", e);
            }
            Bitmap icon = shortcutEntry.getIcon(context);
            return icon != null ? new BitmapDrawable(context.getResources(), icon) : null;
        }

        @Override
        protected void onPostExecute(Drawable drawable) {
            // get ImageView before calling super
            ImageView icon1 = getImageView();
            super.onPostExecute(drawable);
            if (icon1 != null && icon1.getParent() instanceof View) {
                ImageView icon2 = ((View) icon1.getParent()).findViewById(android.R.id.icon2);
                if (drawable != null) {
                    icon2.setImageDrawable(appDrawable);
                } else {
                    icon1.setImageDrawable(appDrawable);
                    icon2.setImageResource(R.drawable.ic_send);
                }
            }
        }
    }

}
