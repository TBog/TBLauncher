package rocks.tbog.tblauncher.entry;

import android.annotation.TargetApi;
import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;

import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

public final class AppEntry extends EntryWithTags {

    public static final String SCHEME = "app://";
    @NonNull
    public final ComponentName componentName;
    @NonNull
    private final UserHandleCompat userHandle;

    private long customIcon = 0;
    private boolean excluded = false;
    private boolean excludedFromHistory = false;

    public AppEntry(@NonNull String id, @NonNull String packageName, @NonNull String className, @NonNull UserHandleCompat userHandle) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + AppEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.componentName = new ComponentName(packageName, className);
        this.userHandle = userHandle;
    }

    public String getUserComponentName() {
        return userHandle.getUserComponentName(componentName);
    }

    public String getPackageName() {
        return componentName.getPackageName();
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public boolean isExcludedFromHistory() {
        return excludedFromHistory;
    }

    public void setExcludedFromHistory(boolean excludedFromHistory) {
        this.excludedFromHistory = excludedFromHistory;
    }

    public boolean canUninstall() {
        return userHandle.isCurrentUser();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<LauncherActivityInfo> getActivityList(LauncherApps launcher) {
        return launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void launchAppDetails(LauncherApps launcher) {
        launcher.startAppDetailsActivity(componentName, userHandle.getRealHandle(), null, null);
    }

    @WorkerThread
    public Drawable getIconDrawable(Context context) {
        IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
        if (customIcon != 0) {
            Drawable drawable = iconsHandler.getCustomIcon(getUserComponentName(), customIcon);
            if (drawable != null)
                return drawable;
            else
                iconsHandler.restoreAppIcon(this);
        }
        return iconsHandler.getDrawableIconForPackage(componentName, userHandle);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        return userHandle.getRealHandle();
    }

    public void setCustomIcon(long dbId) {
        customIcon = dbId;
    }

    public void clearCustomIcon() {
        customIcon = 0;
    }

    public long getCustomIcon() {
        return customIcon;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_app :
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
        TextView nameView = view.findViewById(android.R.id.text1);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
        else
            nameView.setVisibility(View.GONE);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }
    }

    private void displayListResult(@NonNull View view, int drawFlags) {
        Context context = view.getContext();

        TextView nameView = view.findViewById(R.id.item_app_name);
        ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);

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

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }

        //TODO: enable notification badges
//        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
//            SharedPreferences notificationPrefs = context.getSharedPreferences(NotificationListener.NOTIFICATION_PREFERENCES_NAME, Context.MODE_PRIVATE);
//            ImageView notificationView = view.findViewById(R.id.item_notification_dot);
//            notificationView.setVisibility(notificationPrefs.contains(getPackageName()) ? View.VISIBLE : View.GONE);
//            notificationView.setTag(getPackageName());
//
//            int primaryColor = UIColors.getPrimaryColor(context);
//            notificationView.setColorFilter(primaryColor);
//        }
    }

    static class ShortcutItem extends LinearAdapter.ItemString {
        @NonNull
        ShortcutInfo shortcutInfo;

        public ShortcutItem(@NonNull String string, @NonNull ShortcutInfo info) {
            super(string);
            shortcutInfo = info;
        }
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, final ResultAdapter resultAdapter, View parentView) {
//        if (!(context instanceof TBLauncherActivity) || ((TBLauncherActivity) context).isViewingSearchResults()) {
//            adapter.add(new ListPopup.Item(context, R.string.menu_remove));
//        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            List<ShortcutInfo> list = ShortcutUtil.getShortcut(context, getPackageName(), FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED | FLAG_MATCH_DYNAMIC);
            for (ShortcutInfo info : list) {
                CharSequence label = info.getLongLabel();
                if (label == null)
                    label = info.getShortLabel();
                if (label == null)
                    continue;
                adapter.add(new ShortcutItem(label.toString(), info));
            }
        }

        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_exclude));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_add));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_favorites_remove));

        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_customize));
        if (getTags().isEmpty())
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_add));
        else
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_edit));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_app_rename));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));

        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_link));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_app_details));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_app_store));

        try {
            // app installed under /system can't be uninstalled
            ApplicationInfo ai;
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                //LauncherActivityInfo info = launcher.getActivityList(this.appPojo().packageName, this.appPojo().userHandle.getRealHandle()).get(0);
                LauncherActivityInfo info = getActivityList(launcher).get(0);
                ai = info.getApplicationInfo();

            } else {
                ai = context.getPackageManager().getApplicationInfo(getPackageName(), 0);
            }

            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && canUninstall()) {
                adapter.add(new LinearAdapter.Item(context, R.string.menu_app_uninstall));
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException e) {
            // should not happen
        }

        // append root menu if available
//        if (TBApplication.getApplication(context).getRootHandler().isRootActivated() && TBApplication.getApplication(context).getRootHandler().isRootAvailable()) {
//            adapter.add(new ListPopup.Item(context, R.string.menu_app_hibernate));
//        }

        return inflatePopupMenu(adapter, context);
    }

    @Override
    protected boolean popupMenuClickHandler(@NonNull final Context context, @NonNull LinearAdapter.MenuItem item, int stringId) {
        if (item instanceof ShortcutItem) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcherApps != null;
                launcherApps.startShortcut(((ShortcutItem) item).shortcutInfo, null, null);
            }
            return true;
        }
        switch (stringId) {
            case R.string.menu_app_details:
                launchAppDetails(context);
                return true;
            case R.string.menu_app_store:
                launchAppStore(context);
                return true;
            case R.string.menu_app_uninstall:
                launchUninstall(context);
                return true;
//            case R.string.menu_app_hibernate:
//                hibernate(context, appPojo);
//                return true;
            case R.string.menu_exclude:

                //TODO: Change PopupMenu to ListPopup
//                final int EXCLUDE_HISTORY_ID = 0;
//                final int EXCLUDE_KISS_ID = 1;
//                PopupMenu popupExcludeMenu = new PopupMenu(context, parentView);
//                //Adding menu items
//                popupExcludeMenu.getMenu().add(EXCLUDE_HISTORY_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_history);
//                popupExcludeMenu.getMenu().add(EXCLUDE_KISS_ID, Menu.NONE, Menu.NONE, R.string.menu_exclude_kiss);
//                //registering popup with OnMenuItemClickListener
//                popupExcludeMenu.setOnMenuItemClickListener(item -> {
//                    switch (item.getGroupId()) {
//                        case EXCLUDE_HISTORY_ID:
//                            excludeFromHistory(context, appPojo());
//                            return true;
//                        case EXCLUDE_KISS_ID:
//                            excludeFromKiss(context, appPojo(), parent);
//                            return true;
//                    }
//
//                    return true;
//                });
//
//                popupExcludeMenu.show();
                return true;
            case R.string.menu_tags_add:
            case R.string.menu_tags_edit:
                TBApplication.behaviour(context).launchEditTagsDialog(this);
                return true;
            case R.string.menu_app_rename:
                launchRenameDialog(context);
                return true;
            case R.string.menu_custom_icon:
                TBApplication.behaviour(context).launchCustomIconDialog(this);
                return true;
        }

        return super.popupMenuClickHandler(context, item, stringId);
    }

    @Override
    public void doLaunch(@NonNull View v) {
        Context context = v.getContext();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                View potentialIcon = null;
                Bundle opts = null;

                // We're on a modern Android and can display activity animations
                // If AppResult, find the icon
                potentialIcon = v.findViewById(android.R.id.icon);
                if (potentialIcon == null) {
                    // If favorite, find the icon
                    potentialIcon = v.findViewById(R.id.favorite);
                }

                if (potentialIcon != null) {
                    // If we got an icon, we create options to get a nice animation
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        opts = ActivityOptions.makeClipRevealAnimation(potentialIcon, 0, 0, potentialIcon.getMeasuredWidth(), potentialIcon.getMeasuredHeight()).toBundle();
                    }
                }
                if (opts == null && potentialIcon != null)
                    ActivityOptions.makeScaleUpAnimation(potentialIcon, 0, 0, potentialIcon.getMeasuredWidth(), potentialIcon.getMeasuredHeight());

                Rect sourceBounds = Utilities.getOnScreenRect(potentialIcon);
                launcher.startMainActivity(componentName, getRealHandle(), sourceBounds, opts);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(componentName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Utilities.setIntentSourceBounds(intent, v);
                context.startActivity(intent);
            }
        } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, R.string.application_not_found, Toast.LENGTH_LONG).show();
        }
    }

    private void launchRenameDialog(@NonNull Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.app_rename_title));

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
            TBApplication.getApplication(context).getDataHandler().renameApp(getUserComponentName(), newName);

            // Show toast message
            String msg = context.getResources().getString(R.string.app_rename_confirmation, getName());
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();

            // We'll need to reset the list view to its previous transcript mode,
            // but it has to happen *after* the keyboard is hidden, otherwise scroll will be reset
            // Let's wait for half a second, that's ugly but we don't have any other option :(
//            final Handler handler = new Handler();
//            handler.postDelayed(() -> parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);

            dialog.dismiss();
        });
        builder.setNeutralButton(R.string.custom_name_set_default, (dialog, which) -> {
            String name = null;
            PackageManager pm = context.getPackageManager();
            try {
                ApplicationInfo applicationInfo = pm.getApplicationInfo(getPackageName(), 0);
                name = applicationInfo.loadLabel(pm).toString();
            } catch (PackageManager.NameNotFoundException ignored) {
            }
            if (name != null) {
                setName(name);
                TBApplication.getApplication(context).getDataHandler().removeRenameApp(getUserComponentName(), name);

                // Show toast message
                String msg = context.getResources().getString(R.string.app_rename_confirmation, getName());
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
//            final Handler handler = new Handler();
//            handler.postDelayed(() -> parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL), 500);
        });

        //parent.updateTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        ((TextView) dialog.findViewById(R.id.rename)).setText(getName());
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            launchAppDetails(launcher);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            context.startActivity(intent);
        }
    }

    private void launchAppStore(Context context) {
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
        } catch (android.content.ActivityNotFoundException anfe) {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }

    /**
     * Open an activity to uninstall the app package
     */
    private void launchUninstall(Context context) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", getPackageName(), null));
        context.startActivity(intent);
    }

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        public Drawable getDrawable(Context context) {
            AppEntry appEntry = (AppEntry) entryItem;
            //TODO: enable Google Calendar Icon
//            if (GoogleCalendarIcon.GOOGLE_CALENDAR.equals(appEntry.packageName)) {
//                // Google Calendar has a special treatment and displays a custom icon every day
//                icon = GoogleCalendarIcon.getDrawable(context, appEntry.activityName);
//            }

            return appEntry.getIconDrawable(context);
        }
    }
}
