package rocks.tbog.tblauncher.handler;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.AppCacheProvider;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class AppsHandler {
    private static final String TAG = AppsHandler.class.getSimpleName();
    private final TBApplication mApplication;
    private final HashMap<String, AppEntry> mAppsCache = new HashMap<>();
    private boolean mIsLoaded = false;
    private final ArrayDeque<Runnable> mAfterLoadedTasks = new ArrayDeque<>(2);

    public AppsHandler(TBApplication application) {
        this.mApplication = application;
        loadFromDB(false);
    }

    public void loadFromDB(boolean wait) {
        Log.d(TAG, "loadFromDB(wait= " + wait + " )");

        synchronized (this) {
            mIsLoaded = false;
        }

        final Timer timer = Timer.startMilli();
        final HashMap<String, AppEntry> apps = new HashMap<>();
        final Runnable load = () -> {
            TagsHandler tagsHandler = mApplication.tagsHandler();
            Context context = getContext();
            Map<String, AppRecord> dbApps = DBHelper.getAppsData(context);
            apps.clear();

            // convert from AppRecord to AppEntry
            for (AppRecord rec : dbApps.values()) {
                AppEntry appEntry = record2app(context, rec);
                apps.put(appEntry.id, appEntry);
            }
            setTagsForApps(apps.values(), tagsHandler);
        };

        final Runnable apply = () -> {
            synchronized (AppsHandler.this) {
                mAppsCache.clear();
                mAppsCache.putAll(apps);
                mIsLoaded = true;

                timer.stop();
                Log.d("time", "Time to load all DB apps: " + timer);

                // run and remove tasks
                Runnable task;
                while (null != (task = mAfterLoadedTasks.poll()))
                    task.run();
            }
        };

        if (wait) {
            load.run();
            apply.run();
        } else
            Utilities.runAsync((t) -> load.run(), (t) -> apply.run());
    }

    public void runWhenLoaded(@NonNull Runnable task) {
        synchronized (this) {
            if (mIsLoaded)
                task.run();
            else
                mAfterLoadedTasks.add(task);
        }
    }

    @WorkerThread
    public static void setTagsForApps(@NonNull Collection<AppEntry> apps, @NonNull TagsHandler tagsHandler) {
        tagsHandler.runWhenLoaded(() -> {
            Log.d(TAG, "set " + apps.size() + " cached app(s) tags");
            for (AppEntry appEntry : apps)
                appEntry.setTags(tagsHandler.getTags(appEntry.id));
        });
    }

    @NonNull
    private static AppEntry record2app(@NonNull Context context, @NonNull AppRecord rec) {
        UserHandleCompat user = UserHandleCompat.fromComponentName(context, rec.componentName);
        ComponentName cn = UserHandleCompat.unflattenComponentName(rec.componentName);
        AppEntry appEntry = new AppEntry(cn, user);

        if (rec.hasCustomName())
            appEntry.setName(rec.displayName);
        else
            appEntry.setName(user.getBadgedLabelForUser(context, rec.displayName));
        if (rec.hasCustomIcon())
            appEntry.setCustomIcon(rec.dbId);

        return appEntry;
    }

    private Context getContext() {
        return mApplication;
    }

    @NonNull
    public Collection<AppEntry> getAllApps() {
        synchronized (AppsHandler.this) {
            if (!mIsLoaded)
                return Collections.emptyList();
        }
        return Collections.unmodifiableCollection(mAppsCache.values());
    }

    public AppCacheProvider getCacheProvider() {
        return new AppCacheProvider(this);
    }

    @NonNull
    public Map<String, AppRecord> getAppRecords(@NonNull Context context) {
        return DBHelper.getAppsData(context);
    }

    public void updateAppCache(@Nullable ArrayList<AppRecord> insertOrUpdate, @Nullable ArrayList<AppRecord> remove) {
        if (insertOrUpdate != null && insertOrUpdate.size() > 0)
            DBHelper.insertOrUpdateApps(getContext(), insertOrUpdate);
        if (remove != null && remove.size() > 0)
            DBHelper.deleteApps(getContext(), remove);
    }
}
