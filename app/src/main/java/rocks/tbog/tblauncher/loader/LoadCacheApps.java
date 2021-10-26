package rocks.tbog.tblauncher.loader;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.utils.Timer;

public class LoadCacheApps extends LoadEntryItem<AppEntry> {
    private final static String TAG = "LCApps";
    private final AppsHandler appsHandler;

    public LoadCacheApps(Context context) {
        super(context);
        TBApplication app = TBApplication.getApplication(context);
        // call this here in case the AppsHandler is not yet loaded
        appsHandler = app.appsHandler();
    }

    @NonNull
    @Override
    public String getScheme() {
        return AppEntry.SCHEME;
    }

    @Override
    protected ArrayList<AppEntry> doInBackground(Void param) {
        Log.d(TAG, "doInBackground");
        final Context context = this.context.get();
        // timer start
        Timer timer = Timer.startMilli();

        final CountDownLatch latch = new CountDownLatch(1);
        // notify that the tags are loaded
        appsHandler.runWhenLoaded(latch::countDown);
        // wait for the tags to load
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "waiting for TagsHandler", e);
        }

        // function to time
        final ArrayList<AppEntry> pojos;
        if (context != null)
            pojos = getApps(context, appsHandler);
        else
            pojos = new ArrayList<>(0);

        // timer end
        timer.stop();

        Log.i("time", timer + " to load (" + pojos.size() + ") cached apps");
        return pojos;
    }

    @NonNull
    private static ArrayList<AppEntry> getApps(@NonNull Context context, @NonNull AppsHandler appsHandler) {
        Collection<AppEntry> appEntries = appsHandler.getAllApps();
        Log.d(TAG, "appsHandler.getAllApps.size=" + appEntries.size());
        if (appEntries.isEmpty()) {
            // cache is empty, load system apps now
            LoadAppEntry.SystemAppLoader loader = new LoadAppEntry.SystemAppLoader(context);
            return loader.getAppList();
        }
        return new ArrayList<>(appEntries);
    }
}
