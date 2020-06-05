package rocks.tbog.tblauncher.searcher;


import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;

public abstract class Searcher extends AsyncTask<Void, EntryItem, Void> {
    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD = Executors.newSingleThreadExecutor();
    static final int INITIAL_CAPACITY = 50;
    final WeakReference<ISearchActivity> activityWeakReference;
    private final PriorityQueue<EntryItem> processedPojos;
    private boolean tagsEnabled;
    private long start;
    /**
     * Set to true when we are simply refreshing current results (scroll will not be reset)
     * When false, we reset the scroll back to the last item in the list
     */
    private boolean isRefresh = false;
    protected final String query;

    Searcher(ISearchActivity activity, String query) {
        super();
        this.query = query;
        this.activityWeakReference = new WeakReference<>(activity);
        this.processedPojos = getPojoProcessor(activity);
        this.tagsEnabled = activity.tagsEnabled();
    }

    PriorityQueue<EntryItem> getPojoProcessor(ISearchActivity activity) {
        return new PriorityQueue<>(INITIAL_CAPACITY, new EntryItem.RelevanceComparator());
    }

    /**
     * This is called from the background thread by the providers
     */
    @WorkerThread
    public boolean addResult(EntryItem... pojos) {
        if (isCancelled())
            return false;

        ISearchActivity activity = activityWeakReference.get();
        if (activity == null)
            return false;

        Collections.addAll(this.processedPojos, pojos);
//        int maxResults = getMaxResultCount();
//        while (this.processedPojos.size() > maxResults)
//            this.processedPojos.poll();

        return true;
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();

        displayActivityLoader();
    }

    void displayActivityLoader() {
        ISearchActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        activity.displayLoader(true);
    }

    @Override
    protected void onPostExecute(Void param) {
        ISearchActivity activity = activityWeakReference.get();
        if (activity == null)
            return;

        // Loader should still be displayed until all the providers have finished loading
        activity.displayLoader(!TBApplication.getApplication(activity.getContext()).getDataHandler().allProvidersHaveLoaded);

        if (this.processedPojos.isEmpty()) {
            activity.clearAdapter();
        } else {
            PriorityQueue<EntryItem> queue = this.processedPojos;
            ArrayList<EntryItem> results = new ArrayList<>(queue.size());
            while (queue.peek() != null) {
                results.add(queue.poll());
            }

            activity.updateAdapter(results, isRefresh);
        }

        activity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v("Timing", "Time to run query `" + query + "` on " + getClass().getSimpleName() + " to completion: " + time + "ms");
    }

    public void setRefresh(boolean refresh) {
        isRefresh = refresh;
    }

    public boolean tagsEnabled() {
        return tagsEnabled;
    }
}
