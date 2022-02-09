package rocks.tbog.tblauncher.entry;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.WorkerThread;

import java.util.List;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.RootHandler;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public final class AppEntry extends EntryWithTags {

    public static final String SCHEME = "app://";
    @NonNull
    public final ComponentName componentName;
    @NonNull
    private final UserHandleCompat userHandle;

    private long customIcon = 0;
    private int cacheIconId = 0;
    private boolean hiddenByUser = false;
    private boolean excludedFromHistory = false;

    public AppEntry(@NonNull ComponentName component, @NonNull UserHandleCompat user) {
        this(component.getPackageName(), component.getClassName(), user);
    }

    public AppEntry(@NonNull String packageName, @NonNull String activityName, @NonNull UserHandleCompat user) {
        super(generateAppId(packageName, activityName, user));
        componentName = new ComponentName(packageName, activityName);
        userHandle = user;
    }

    /**
     * Generate a unique {@link AppEntry} id from {@link ComponentName} and {@link UserHandleCompat}
     *
     * @param component component {@link ComponentName}
     * @param user      user handle
     * @return unique id with SCHEME prefix
     */
    @NonNull
    public static String generateAppId(@NonNull ComponentName component, @NonNull UserHandleCompat user) {
        return SCHEME + user.getUserComponentName(component);
    }

    @NonNull
    public static String generateAppId(@NonNull String packageName, @NonNull String activityName, @NonNull UserHandleCompat user) {
        return SCHEME + user.getUserComponentName(packageName, activityName);
    }

    @NonNull
    @Override
    public String getIconCacheId() {
        return id + cacheIconId;
    }

    public String getUserComponentName() {
        return userHandle.getUserComponentName(componentName);
    }

    public String getPackageName() {
        return componentName.getPackageName();
    }

    public boolean isHiddenByUser() {
        return hiddenByUser;
    }

    public void setHiddenByUser(boolean hiddenByUser) {
        this.hiddenByUser = hiddenByUser;
    }

    @Override
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

    @WorkerThread
    public Drawable getIconDrawable(Context context) {
        IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
        if (customIcon != 0) {
            Drawable drawable = iconsHandler.getCustomIcon(getUserComponentName());
            if (drawable != null)
                return drawable;
            else
                iconsHandler.restoreDefaultIcon(this);
        }
        return iconsHandler.getDrawableIconForPackage(componentName, userHandle);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        return userHandle.getRealHandle();
    }

    public void setCustomIcon(long dbId) {
        customIcon = dbId;
        cacheIconId += 1;
    }

    public void clearCustomIcon() {
        customIcon = 0;
        cacheIconId = 0;
    }

    public long getCustomIcon() {
        return customIcon;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private static final int[] RESULT_LAYOUT = {R.layout.item_app, R.layout.item_grid, R.layout.item_quick_list};

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
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST)) {
            displayListResult(view, drawFlags);
        } else {
            displayGridResult(view, drawFlags);
        }
    }

    private void displayGridResult(@NonNull View view, int drawFlags) {
        TextView nameView = view.findViewById(android.R.id.text1);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME)) {
            ResultViewHelper.displayHighlighted(relevanceSource, normalizedName, getName(), relevance, nameView);
            nameView.setVisibility(View.VISIBLE);
        } else
            nameView.setVisibility(View.GONE);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        ImageView bottomRightIcon = view.findViewById(android.R.id.icon2);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ColorFilter colorFilter = ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);

            if (bottomRightIcon != null) {
                if (isHiddenByUser()) {
                    bottomRightIcon.setVisibility(View.VISIBLE);
                    bottomRightIcon.setImageResource(R.drawable.ic_eye_crossed);
                    bottomRightIcon.setColorFilter(colorFilter);
                } else {
                    bottomRightIcon.setVisibility(View.GONE);
                }
            }
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
            if (bottomRightIcon != null)
                bottomRightIcon.setVisibility(View.GONE);
        }

        ResultViewHelper.applyPreferences(drawFlags, nameView, appIcon);
    }

    private void displayListResult(@NonNull View view, int drawFlags) {
        final Context context = view.getContext();

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
        ImageView bottomRightIcon = view.findViewById(android.R.id.icon2);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            ColorFilter colorFilter = ResultViewHelper.setIconColorFilter(appIcon, drawFlags);
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);

            if (isHiddenByUser()) {
                bottomRightIcon.setColorFilter(colorFilter);
                bottomRightIcon.setVisibility(View.VISIBLE);
                bottomRightIcon.setImageResource(R.drawable.ic_eye_crossed);
            } else {
                bottomRightIcon.setVisibility(View.GONE);
            }
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
            bottomRightIcon.setVisibility(View.GONE);
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

        ResultViewHelper.applyPreferences(drawFlags, nameView, tagsView, appIcon);
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
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        List<ContentLoadHelper.CategoryItem> categoryTitle = PrefCache.getResultPopupOrder(context);

        for (ContentLoadHelper.CategoryItem categoryItem : categoryTitle) {
            int titleStringId = categoryItem.textId;
            if (titleStringId == R.string.popup_title_hist_fav) {
                adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
                //adapter.add(new LinearAdapter.Item(context, R.string.menu_exclude));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_remove_history));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_quick_list_add));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_quick_list_remove));
                if (isHiddenByUser())
                    adapter.add(new LinearAdapter.Item(context, R.string.menu_show));
                else
                    adapter.add(new LinearAdapter.Item(context, R.string.menu_hide));
            } else if (titleStringId == R.string.popup_title_customize) {
                adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_customize));
                if (getTags().isEmpty())
                    adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_add));
                else
                    adapter.add(new LinearAdapter.Item(context, R.string.menu_tags_edit));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_app_rename));
                adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));
            } else if (titleStringId == R.string.popup_title_link) {
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
                RootHandler rootHandler = TBApplication.rootHandler(context);
                if (rootHandler.isRootActivated() && rootHandler.isRootAvailable()) {
                    adapter.add(new LinearAdapter.Item(context, R.string.menu_app_hibernate));
                }
            } else if (titleStringId == R.string.popup_title_shortcut_dynamic) {
                int shortcutCount = 0;
                if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    List<ShortcutInfo> list = ShortcutUtil.getShortcut(context, getPackageName(), FLAG_MATCH_MANIFEST | FLAG_MATCH_DYNAMIC);
                    for (ShortcutInfo info : list) {
                        CharSequence label = info.getLongLabel();
                        if (label == null)
                            label = info.getShortLabel();
                        if (label == null)
                            continue;
                        if (shortcutCount == 0)
                            adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_shortcut_dynamic));
                        adapter.add(new ShortcutItem(label.toString(), info));
                        shortcutCount += 1;
                    }
                }
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
        Context ctx = view.getContext();
        if (item instanceof ShortcutItem) {
            TBApplication.behaviour(ctx).beforeLaunchOccurred();
            final ShortcutInfo shortcutInfo = ((ShortcutItem) item).shortcutInfo;
            parentView.postDelayed(() -> {
                Activity activity = Utilities.getActivity(parentView);
                if (activity == null)
                    return;

                ShortcutEntry.doOreoLaunch(activity, parentView, shortcutInfo);

                TBApplication.behaviour(activity).afterLaunchOccurred();
            }, Behaviour.LAUNCH_DELAY);

            return true;
        }
        switch (stringId) {
//            case R.string.menu_remove_history:
//                ResultHelper.removeFromResultsAndHistory();
//                return true;
            case R.string.menu_app_details:
                launchAppDetails(ctx, parentView);
                return true;
            case R.string.menu_app_store:
                launchAppStore(ctx, parentView);
                return true;
            case R.string.menu_app_uninstall:
                launchUninstall(ctx);
                return true;
            case R.string.menu_app_hibernate:
                hibernate(ctx);
                return true;
//            case R.string.menu_app_hibernate:
//                hibernate(context, appPojo);
//                return true;
            case R.string.menu_exclude: {
                LinearAdapter adapter = new LinearAdapter();
                ListPopup menu = ListPopup.create(ctx, adapter);

                adapter.add(new LinearAdapter.Item(ctx, R.string.menu_exclude_history));
                adapter.add(new LinearAdapter.Item(ctx, R.string.menu_exclude_kiss));

                menu.setOnItemClickListener((a, v, pos) -> {
                    LinearAdapter.MenuItem menuItem = ((LinearAdapter) a).getItem(pos);
                    @StringRes int id = 0;
                    if (menuItem instanceof LinearAdapter.Item) {
                        id = ((LinearAdapter.Item) a.getItem(pos)).stringId;
                    }
                    switch (id) {
                        case R.string.menu_exclude_history:
                            //excludeFromHistory(v.getContext(), appPojo());
                            Toast.makeText(ctx, "Not Implemented", Toast.LENGTH_LONG).show();
                            break;
                        case R.string.menu_exclude_kiss:
                            //excludeFromKiss(v.getContext(), appPojo(), parent);
                            Toast.makeText(ctx, "Work in progress", Toast.LENGTH_LONG).show();
                            break;
                    }
                });
                menu.show(parentView);
                TBApplication.getApplication(ctx).registerPopup(menu);
                return true;
            }
            case R.string.menu_hide:
                if (TBApplication.dataHandler(ctx).addToHidden(this)) {
                    setHiddenByUser(true);
                    TBApplication.behaviour(ctx).refreshSearchRecord(this);
                    //Toast.makeText(ctx, "App "+getName()+" hidden from search", Toast.LENGTH_LONG).show();
                }
                break;
            case R.string.menu_show:
                if (TBApplication.dataHandler(ctx).removeFromHidden(this)) {
                    setHiddenByUser(false);
                    TBApplication.behaviour(ctx).refreshSearchRecord(this);
                    //Toast.makeText(ctx, "App "+getName()+" shown in searches", Toast.LENGTH_LONG).show();
                }
                break;
            case R.string.menu_tags_add:
            case R.string.menu_tags_edit:
                TBApplication.behaviour(ctx).launchEditTagsDialog(this);
                return true;
            case R.string.menu_app_rename:
                launchRenameDialog(ctx);
                return true;
            case R.string.menu_custom_icon:
                TBApplication.behaviour(ctx).launchCustomIconDialog(this);
                return true;
        }

        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context context = v.getContext();
        // If AppResult, find the icon
        View potentialIcon = v.findViewById(android.R.id.icon);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;

                // We're on a modern Android and can display activity animations
                Bundle startActivityOptions = Utilities.makeStartActivityOptions(potentialIcon);
                Rect sourceBounds = Utilities.getOnScreenRect(potentialIcon);
                launcher.startMainActivity(componentName, getRealHandle(), sourceBounds, startActivityOptions);
            } else {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setComponent(componentName);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                Utilities.setIntentSourceBounds(intent, v);
                Bundle startActivityOptions = Utilities.makeStartActivityOptions(potentialIcon);
                context.startActivity(intent, startActivityOptions);
            }
        } catch (ActivityNotFoundException | NullPointerException | SecurityException e) {
            // Application was just removed?
            // (null pointer exception can be thrown on Lollipop+ when app is missing)
            Toast.makeText(context, context.getString(R.string.application_not_found, componentName.flattenToShortString()), Toast.LENGTH_LONG).show();
        }
    }

    private void launchRenameDialog(@NonNull Context ctx) {
        DialogHelper.makeRenameDialog(ctx, getName(), (dialog, name) -> {
            // Set new name
            setName(name);
            Context context = dialog.getContext();
            TBApplication app = TBApplication.getApplication(context);
            app.getDataHandler().renameApp(getUserComponentName(), name);
            app.behaviour().refreshSearchRecord(AppEntry.this);

            // Show toast message
            String msg = context.getResources().getString(R.string.app_rename_confirmation, getName());
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
        })
                .setTitle(R.string.title_app_rename)
                .setNeutralButton(R.string.custom_name_set_default, (dialog, which) -> {
                    Context context = dialog.getContext();
                    String name = null;
                    PackageManager pm = context.getPackageManager();
                    try {
                        ApplicationInfo applicationInfo = pm.getApplicationInfo(getPackageName(), 0);
                        name = applicationInfo.loadLabel(pm).toString();
                    } catch (PackageManager.NameNotFoundException ignored) {
                    }
                    if (name != null) {
                        setName(name);
                        TBApplication app = TBApplication.getApplication(context);
                        app.getDataHandler().removeRenameApp(getUserComponentName(), name);
                        app.behaviour().refreshSearchRecord(AppEntry.this);

                        // Show toast message
                        String msg = context.getString(R.string.app_rename_confirmation, getName());
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }

                    dialog.dismiss();
                })
                .show();
    }

    /**
     * Open an activity displaying details regarding the current package
     */
    private void launchAppDetails(Context context, View view) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TBApplication.behaviour(context).beforeLaunchOccurred();
            view.postDelayed(() -> {
                Activity activity = Utilities.getActivity(view);
                if (activity == null)
                    return;
                LauncherApps launcher = (LauncherApps) activity.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                Rect bounds = Utilities.getOnScreenRect(view);
                Bundle opts = Utilities.makeStartActivityOptions(view);
                launcher.startAppDetailsActivity(componentName, userHandle.getRealHandle(), bounds, opts);

                TBApplication.behaviour(activity).afterLaunchOccurred();
            }, Behaviour.LAUNCH_DELAY);
        } else {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", getPackageName(), null));
            TBApplication.behaviour(context).launchIntent(view, intent);
        }
    }

    private void launchAppStore(Context context, View view) {
        TBApplication.behaviour(context).beforeLaunchOccurred();
        view.postDelayed(() -> {
            Activity activity = Utilities.getActivity(view);
            if (activity == null)
                return;
            Rect bound = Utilities.getOnScreenRect(view);
            Bundle startActivityOptions = Utilities.makeStartActivityOptions(view);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            try {
                intent.setSourceBounds(bound);
                activity.startActivity(intent, startActivityOptions);
            } catch (ActivityNotFoundException ignored) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
                intent.setSourceBounds(bound);
                activity.startActivity(intent, startActivityOptions);
            }
            TBApplication.behaviour(activity).afterLaunchOccurred();
        }, Behaviour.LAUNCH_DELAY);
    }

    /**
     * Open an activity to uninstall the app package
     */
    private void launchUninstall(Context context) {
        Intent intent = new Intent(Intent.ACTION_DELETE,
                Uri.fromParts("package", getPackageName(), null));
        context.startActivity(intent);
    }

    private void hibernate(Context context) {
        String msg = context.getResources().getString(R.string.toast_hibernate_completed);
        if (!TBApplication.rootHandler(context).hibernateApp(getPackageName())) {
            msg = context.getResources().getString(R.string.toast_hibernate_error);
//        } else {
//            TBApplication.dataHandler(context).getAppProvider().reload(false);
        }

        Toast.makeText(context, String.format(msg, getName()), Toast.LENGTH_SHORT).show();
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
