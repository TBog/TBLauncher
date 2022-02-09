package rocks.tbog.tblauncher.dataprovider;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.loader.LoadEntryItem;
import rocks.tbog.tblauncher.utils.Timer;

public abstract class Provider<T extends EntryItem> extends Service implements IProvider<T> {
    final static String TAG = "Provider";

    /**
     * Binder given to clients
     */
    private final IBinder binder = new LocalBinder();
    /**
     * Storage for search items used by this provider
     */
    protected List<T> pojos = Collections.emptyList();
    private boolean loaded = false;
    private LoadEntryItem<T> loader = null;
    /**
     * Scheme used to build ids for the pojos created by this provider
     */
    @NonNull
    private String pojoScheme = "(none)://";

    private final Timer mTimer = new Timer();

    /**
     * (Re-)load the providers resources when the provider has been completely initialized
     * by the Android system
     */
    @Override
    public void onCreate() {
        super.onCreate();

        TBApplication.dataHandler(this).onProviderRecreated(this);
        this.reload(true);
    }

    protected boolean isLoading() {
        return loader != null;
    }

    protected void initialize(@NonNull LoadEntryItem<T> loader) {
        mTimer.start();

        if (this.loader != null)
            this.loader.cancel(false);

        Log.i(TAG, "Starting provider: " + this.getClass().getSimpleName());

        loader.setProvider(this);
        this.loader = loader;
        this.pojoScheme = loader.getScheme();
        this.loader.execute();
    }

    public void reload(boolean cancelCurrentLoadTask) {
        if (!cancelCurrentLoadTask && loader != null)
            return;
        loaded = false;
        // Handled at subclass level
        if (pojos.size() > 0) {
            Log.v(TAG, "Reloading provider: " + this.getClass().getSimpleName());
        }
    }

    @Override
    public void setDirty() {
        // do nothing, we don't depend on any other provider
    }

    @Override
    public boolean isLoaded() {
        return this.loaded;
    }

    @Nullable
    @Override
    public Timer getLoadDuration() {
        return mTimer;
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    public void loadOver(ArrayList<T> results) {
        mTimer.stop();

        Log.i(TAG, "Time to load " + this.getClass().getSimpleName() + ": " + mTimer);

        // Store results
        this.pojos = results;
        this.loaded = true;
        this.loader = null;

        // Broadcast this event
        Intent i = new Intent(TBLauncherActivity.LOAD_OVER);
        this.sendBroadcast(i);
    }

    @NonNull
    public String getScheme() {
        return pojoScheme;
    }

    /**
     * Tells whether or not this provider may be able to find the pojo with
     * specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(pojoScheme);
    }

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    public T findById(@NonNull String id) {
        for (T pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }
        return null;
    }

    @Override
    public List<T> getPojos() {
        if (BuildConfig.DEBUG)
            return Collections.unmodifiableList(pojos);
        return pojos;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public IProvider<T> getService() {
            // Return this instance of the provider so that clients can call public methods
            return Provider.this;
        }
    }
}
