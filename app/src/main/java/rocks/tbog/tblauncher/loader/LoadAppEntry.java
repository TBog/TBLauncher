package rocks.tbog.tblauncher.loader;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.UserManager;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class LoadAppEntry extends LoadEntryItem<AppEntry> {

    private HashMap<String, AppRecord> dbApps = null;
    private ArrayList<AppRecord> pendingChanges = null;
    private final TagsHandler tagsHandler;

    public LoadAppEntry(Context context) {
        super(context);
        tagsHandler = TBApplication.tagsHandler(context);
    }

    @NonNull
    @Override
    public String getScheme() {
        return AppEntry.SCHEME;
    }

    //    static class PendingAppList {
//        final ArrayList<AppRecord> appRecords = new ArrayList<>();
//        final ArrayList<UserHandle> userHandles = new ArrayList<>();
//
//        void add(AppRecord appRecord, UserHandle userHandle) {
//            appRecords.add(appRecord);
//            userHandles.add(userHandle);
//        }
//
//        int size() {
//            return appRecords.size();
//        }
//
//        AppRecord getAppRecord(int index) {
//            return appRecords.get(index);
//        }
//
//        UserHandle getUserHandle(int index) {
//            return userHandles.get(index);
//        }
//    }

    @Override
    protected ArrayList<AppEntry> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<AppEntry> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        DataHandler dataHandler = TBApplication.getApplication(ctx).getDataHandler();

//        Set<String> excludedAppList = dataHandler.getExcluded();
//        Set<String> excludedFromHistoryAppList = dataHandler.getExcludedFromHistory();

        dbApps = dataHandler.getCachedApps();
        pendingChanges = new ArrayList<>(0);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            if (manager != null && launcher != null)
                // Handle multi-profile support introduced in Android 5 (#542)
                for (android.os.UserHandle profile : manager.getUserProfiles()) {
                    UserHandleCompat user = new UserHandleCompat(manager.getSerialNumberForUser(profile), profile);
                    for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                        ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                        String displayName = activityInfo.getLabel().toString();
                        AppEntry app = processApp(displayName, appInfo.packageName, activityInfo.getName(), user);

                        apps.add(app);
                    }
                }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                UserHandleCompat user = new UserHandleCompat();
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                String displayName = info.loadLabel(manager).toString();
                AppEntry app = processApp(displayName, appInfo.packageName, info.activityInfo.name, user);

                apps.add(app);
            }
        }

        // add new apps to database
        dataHandler.updateAppCache(pendingChanges);
        pendingChanges.clear();

        for (Map.Entry<String, AppRecord> entry : dbApps.entrySet()) {
            AppRecord rec = entry.getValue();
            if ((rec.flags & AppRecord.FLAG_VALIDATED) == AppRecord.FLAG_VALIDATED)
                continue;
            pendingChanges.add(rec);
        }

        // remove apps from database
        dataHandler.removeAppCache(pendingChanges);
        pendingChanges = null;
        dbApps = null;

        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");

        return apps;
    }

    @NonNull
    private AppEntry processApp(String appName, String packageName, String activityName, UserHandleCompat user) {
        String componentName = user.getUserComponentName(packageName, activityName);
        AppRecord rec = dbApps.get(componentName);
        if (rec == null) {
            rec = new AppRecord();
            rec.componentName = componentName;
            rec.displayName = appName;
            pendingChanges.add(rec);
        }
        if (!rec.hasCustomName() && !appName.equals(rec.displayName)) {
            rec.displayName = appName;
            pendingChanges.add(rec);
        }

        rec.flags |= AppRecord.FLAG_VALIDATED;

        String id = getScheme() + componentName;
//        boolean isExcluded = excludedAppList.contains(componentName);
//        boolean isExcludedFromHistory = excludedFromHistoryAppList.contains(id);
        AppEntry app = new AppEntry(id, packageName, activityName, user);

        if (rec.hasCustomName())
            app.setName(rec.displayName);
        else
            app.setName(user.getBadgedLabelForUser(context.get(), appName));
        if (rec.hasCustomIcon())
            app.setCustomIcon(rec.dbId);
        app.setTags(tagsHandler.getTags(app.id));

        return app;
    }
}
