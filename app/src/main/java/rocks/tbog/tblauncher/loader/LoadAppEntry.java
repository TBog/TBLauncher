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
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class LoadAppEntry extends LoadEntryItem<AppEntry> {

    public LoadAppEntry(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public String getScheme() {
        return AppEntry.SCHEME;
    }

    @Override
    protected ArrayList<AppEntry> doInBackground(Void param) {
        SystemAppLoader loader = new SystemAppLoader(context.get());
        //List<AppEntry> currentApplications = TBApplication.dataHandler(context.get()).getApplications();

        // timer start
        Timer timer = Timer.startMilli();
        // function to time
        ArrayList<AppEntry> apps = loader.getAppList();
        // timer end
        timer.stop();

        Log.i("time", timer + " to list apps");
        return apps;
    }

    public static class SystemAppLoader {
        private Map<String, AppRecord> dbApps = null;
        private ArrayList<AppRecord> pendingChanges = null;
        @Nullable
        private final Context ctx;

        SystemAppLoader(@Nullable Context context) {
            ctx = context;
        }

        @NonNull
        public ArrayList<AppEntry> getAppList() {
            ArrayList<AppEntry> apps = new ArrayList<>(0);

            if (ctx == null) {
                return apps;
            }

            AppsHandler appsHandler = TBApplication.appsHandler(ctx);

            dbApps = appsHandler.getAppRecords(ctx);
            pendingChanges = new ArrayList<>(0);

            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                if (manager != null && launcher != null) {
                    // Handle multi-profile support introduced in Android 5 (#542)
                    for (android.os.UserHandle profile : manager.getUserProfiles()) {
                        UserHandleCompat user = new UserHandleCompat(manager.getSerialNumberForUser(profile), profile);
                        List<LauncherActivityInfo> activityList = launcher.getActivityList(null, profile);
                        apps.ensureCapacity(apps.size() + activityList.size());
                        Log.i("App", "getActivityList(" + profile + ") found " + activityList.size() + " app(s)");
                        for (LauncherActivityInfo activityInfo : activityList) {
                            ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                            String displayName = activityInfo.getLabel().toString();
                            if (displayName.equals(appInfo.packageName))
                                displayName = activityInfo.getName();

                            AppEntry app = processApp(displayName, appInfo.packageName, activityInfo.getName(), user);

                            apps.add(app);
                        }
                    }
                }
            } else {
                PackageManager manager = ctx.getPackageManager();

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> activityList = manager.queryIntentActivities(mainIntent, 0);
                apps.ensureCapacity(apps.size() + activityList.size());
                Log.i("App", "queryIntentActivities found " + activityList.size() + " app(s)");
                for (ResolveInfo info : activityList) {
                    UserHandleCompat user = UserHandleCompat.CURRENT_USER;
                    ApplicationInfo appInfo = info.activityInfo.applicationInfo;

                    String displayName = info.loadLabel(manager).toString();
                    AppEntry app = processApp(displayName, appInfo.packageName, info.activityInfo.name, user);

                    apps.add(app);
                }
            }

            Log.i("App", "LoadAppPojos found " + apps.size() + " app(s)");

            // add new apps to database
            appsHandler.updateAppCache(pendingChanges, null);
            pendingChanges.clear();

            for (Map.Entry<String, AppRecord> entry : dbApps.entrySet()) {
                AppRecord rec = entry.getValue();
                if (rec.isFlagSet(AppRecord.FLAG_VALIDATED))
                    continue;
                pendingChanges.add(rec);
            }

            // remove apps from database
            appsHandler.updateAppCache(null, pendingChanges);
            pendingChanges = null;
            dbApps = null;

            AppsHandler.setTagsForApps(apps, TBApplication.tagsHandler(ctx));

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

            rec.addFlags(AppRecord.FLAG_VALIDATED);

            AppEntry app = new AppEntry(packageName, activityName, user);

            if (rec.hasCustomName())
                app.setName(rec.displayName);
            else
                app.setName(user.getBadgedLabelForUser(ctx, appName));
            if (rec.hasCustomIcon())
                app.setCustomIcon(rec.dbId);
            app.setHiddenByUser(rec.isHidden());

            return app;
        }
    }
}
