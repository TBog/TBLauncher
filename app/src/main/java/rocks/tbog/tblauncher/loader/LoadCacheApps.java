package rocks.tbog.tblauncher.loader;

import android.content.ComponentName;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class LoadCacheApps extends LoadEntryItem<AppEntry> {
    public LoadCacheApps(Context context) {
        super(context);
    }

    @NonNull
    @Override
    public String getScheme() {
        return AppEntry.SCHEME;
    }

    @Override
    protected ArrayList<AppEntry> doInBackground(Void param) {
        final Context context = this.context.get();
        // timer start
        Timer timer = Timer.startMilli();

        // function to time
        final ArrayList<AppEntry> pojos;
        if (context != null)
            pojos = getApps(context);
        else
            pojos = new ArrayList<>(0);

        // timer end
        timer.stop();

        Log.i("time", timer + " to load (" + pojos.size() + ") cached apps");
        return pojos;
    }

    @NonNull
    private static ArrayList<AppEntry> getApps(@NonNull Context context) {
        TBApplication app = TBApplication.getApplication(context);
        HashMap<String, AppRecord> apps = app.getDataHandler().getCachedApps();

        final ArrayList<AppEntry> appEntries = new ArrayList<>(apps.size());

        // convert from AppRecord to AppEntry
        for (AppRecord rec : apps.values()) {
            UserHandleCompat user = UserHandleCompat.fromComponentName(context, rec.componentName);
            String id = AppEntry.SCHEME + rec.componentName;
            ComponentName cn = UserHandleCompat.unflattenComponentName(rec.componentName);
            AppEntry appEntry = new AppEntry(id, cn.getPackageName(), cn.getClassName(), user);
            appEntries.add(appEntry);

            if (rec.hasCustomName())
                appEntry.setName(rec.displayName);
            else
                appEntry.setName(user.getBadgedLabelForUser(context, rec.displayName));
            if (rec.hasCustomIcon())
                appEntry.setCustomIcon(rec.dbId);
            //app.setTags(tagsHandler.getTags(app.id));
        }

        final Semaphore semaphore = new Semaphore(0);
        final TagsHandler tagsHandler = app.tagsHandler();
        tagsHandler.runWhenLoaded(() -> {
            Log.d("App", "set " + appEntries.size() + " cached app(s) tags");
            for (AppEntry appEntry : appEntries)
                appEntry.setTags(tagsHandler.getTags(appEntry.id));

            // notify that the tags are loaded
            semaphore.release();
        });

        // TODO: I don't like this hack. We should have a `Handler` class for app entries.
        // wait for the tags to load
        try {
            semaphore.acquire();
        } catch (InterruptedException ignored) {
        }

        return appEntries;
    }
}
