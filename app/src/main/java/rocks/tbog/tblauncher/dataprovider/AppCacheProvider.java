package rocks.tbog.tblauncher.dataprovider;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.Timer;

public class AppCacheProvider implements IProvider<AppEntry> {
    final static String TAG = "AppCP";
    final private AppsHandler appsHandler;

    public AppCacheProvider(@NonNull AppsHandler handler) {
        appsHandler = handler;
    }

    @WorkerThread
    @Override
    public void requestResults(String query, ISearcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);
        // notify that the tags are loaded
        appsHandler.runWhenLoaded(latch::countDown);
        // wait for the tags to load
        try {
            latch.await();
        } catch (InterruptedException e) {
            Log.e(TAG, "waiting for TagsHandler", e);
        }

        final Collection<AppEntry> entries = appsHandler.getAllApps();

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        EntryToResultUtils.tagsCheckResults(entries, fuzzyScore, searcher);
    }

    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public Timer getLoadDuration() {
        return null;
    }

    @Override
    public void setDirty() {
        // do nothing, we already have the full list of items
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return false;
    }

    @Override
    public AppEntry findById(@NonNull String id) {
        return null;
    }

    @Nullable
    @Override
    public List<AppEntry> getPojos() {
        return null;
    }
}
