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

import java.util.ArrayList;
import java.util.Set;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.UserHandle;

public class LoadAppEntry extends LoadEntryItem<AppEntry> {

    //private final TagsHandler tagsHandler;

    public LoadAppEntry(Context context) {
        super(context, "app://");
        //tagsHandler = TBApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<AppEntry> doInBackground(Void... params) {
        long start = System.nanoTime();

        ArrayList<AppEntry> apps = new ArrayList<>();

        Context ctx = context.get();
        if (ctx == null) {
            return apps;
        }

        Set<String> excludedAppList = TBApplication.getApplication(ctx).getDataHandler().getExcluded();
        Set<String> excludedFromHistoryAppList = TBApplication.getApplication(ctx).getDataHandler().getExcludedFromHistory();

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            // Handle multi-profile support introduced in Android 5 (#542)
            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                UserHandle user = new UserHandle(manager.getSerialNumberForUser(profile), profile);
                for (LauncherActivityInfo activityInfo : launcher.getActivityList(null, profile)) {
                    ApplicationInfo appInfo = activityInfo.getApplicationInfo();

                    String id = user.addUserSuffixToString(pojoScheme + appInfo.packageName + "/" + activityInfo.getName(), '/');

                    boolean isExcluded = false;//excludedAppList.contains(user.getComponentName(appInfo.packageName, activityInfo.getName()));
                    boolean isExcludedFromHistory = false;//excludedFromHistoryAppList.contains(id);

                    AppEntry app = new AppEntry(id, appInfo.packageName, activityInfo.getName(), user,
                            isExcluded, isExcludedFromHistory);

                    app.setName(activityInfo.getLabel().toString());

                    //app.setTags(tagsHandler.getTags(app.id));

                    apps.add(app);
                }
            }
        } else {
            PackageManager manager = ctx.getPackageManager();

            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            for (ResolveInfo info : manager.queryIntentActivities(mainIntent, 0)) {
                ApplicationInfo appInfo = info.activityInfo.applicationInfo;
                String id = pojoScheme + appInfo.packageName + "/" + info.activityInfo.name;
                boolean isExcluded = false;//excludedAppList.contains(EntryItem.getComponentName(appInfo.packageName, info.activityInfo.name, new UserHandle()));
                boolean isExcludedFromHistory = false;//excludedFromHistoryAppList.contains(id);

                AppEntry app = new AppEntry(id, appInfo.packageName, info.activityInfo.name, new UserHandle(), isExcluded, isExcludedFromHistory);

                app.setName(info.loadLabel(manager).toString());

                //app.setTags(tagsHandler.getTags(app.id));

                apps.add(app);
            }
        }

        long end = System.nanoTime();
        Log.i("time", Long.toString((end - start) / 1000000) + " milliseconds to list apps");

        return apps;
    }
}
