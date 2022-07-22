package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.utils.Timer;

public abstract class DBProvider<T extends EntryItem> implements IProvider<T> {
    final Context context;
    protected List<T> entryList = new ArrayList<>();

    private boolean mIsLoaded = false;
    private DBLoader<T> mLoadTask = null;
    protected final Timer mTimer = new Timer();

    public DBProvider(Context context) {
        this.context = context;
    }

    @Override
    public void requestResults(String query, ISearcher searcher) {
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        if (!cancelCurrentLoadTask && mLoadTask != null)
            return;
        setDirty();
        Log.i(Provider.TAG, "Starting provider: " + this.getClass().getSimpleName());
        mTimer.start();
        mLoadTask = newLoadTask();
        mLoadTask.execute();
    }

    protected abstract DBLoader<T> newLoadTask();

    @Override
    public boolean isLoaded() {
        return mIsLoaded;
    }

    @Override
    public Timer getLoadDuration() {
        return mTimer;
    }

    protected void setLoaded() {
        mIsLoaded = true;
    }

    @Override
    public void setDirty() {
        // mark this as not loaded and wait for DataHandler to call reload
        mIsLoaded = false;
        if (mLoadTask != null)
            mLoadTask.cancel(true);
        mLoadTask = null;
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_2;
    }

    /**
     * Whether or not this provider may be able to find a pojo with the specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query; does not guarantee it will!
     */
    @Override
    public boolean mayFindById(@NonNull String id) {
        return false;
    }

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    @Override
    public T findById(@NonNull String id) {
        for (T entryItem : entryList) {
            if (entryItem.id.equals(id)) {
                return entryItem;
            }
        }
        return null;
    }

    @Nullable
    @Override
    public List<T> getPojos() {
        if (BuildConfig.DEBUG)
            return Collections.unmodifiableList(entryList);
        return entryList;
    }

    protected abstract static class DBLoader<T extends EntryItem> extends AsyncTask<Void, List<T>> {
        protected final WeakReference<DBProvider<T>> weakProvider;

        public DBLoader(DBProvider<T> provider) {
            super();
            weakProvider = new WeakReference<>(provider);
        }

        @Nullable
        protected Context getContext() {
            DBProvider<T> provider = weakProvider.get();
            return provider != null ? provider.context : null;
        }

        @WorkerThread
        @Override
        protected List<T> doInBackground(Void param) {
            Context ctx = getContext();
            if (ctx == null)
                return null;

            DataHandler dataHandler = TBApplication.getApplication(ctx).getDataHandler();

            return getEntryItems(dataHandler);
        }

        @WorkerThread
        abstract List<T> getEntryItems(DataHandler dataHandler);

        @MainThread
        @Override
        protected void onPostExecute(List<T> entryItems) {
            DBProvider<T> provider = weakProvider.get();
            if (entryItems == null || provider == null || provider.mLoadTask != this)
                return;

            // get the result
            provider.entryList = entryItems;

            // mark the provider as loaded
            provider.setLoaded();
            provider.mLoadTask = null;

            provider.mTimer.stop();
            Log.i("time", "Time to load " + provider.getClass().getSimpleName() + ": " + provider.mTimer);

            Intent i = new Intent(TBLauncherActivity.LOAD_OVER);
            provider.context.sendBroadcast(i);
        }

        public void execute() {
            TaskRunner.executeOnExecutor(DataHandler.EXECUTOR_PROVIDERS, this);
        }
    }
}
