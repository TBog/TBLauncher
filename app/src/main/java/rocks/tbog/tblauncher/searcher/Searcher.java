package rocks.tbog.tblauncher.searcher;


import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class Searcher extends AsyncTask<Void, Void> implements ISearcher {
    // define a different thread than the default AsyncTask thread or else we will block everything else that uses AsyncTask while we search
    public static final ExecutorService SEARCH_THREAD = Executors.newSingleThreadExecutor();
    protected static final int INITIAL_CAPACITY = 50;
    protected final WeakReference<ISearchActivity> activityWeakReference;
    protected final PriorityQueue<EntryItem> processedPojos;
    protected final int maxResults;
    private final boolean tagsEnabled;
    private long start;
    /**
     * Set to true when we are simply refreshing current results (scroll will not be reset)
     * When false, we reset the scroll back to the last item in the list
     */
    private boolean isRefresh = false;
    @NonNull
    protected final String query;

    public Searcher(ISearchActivity activity, @NonNull String query) {
        super();
        this.query = query;
        activityWeakReference = new WeakReference<>(activity);
        processedPojos = getPojoProcessor(activity);
        tagsEnabled = PrefCache.getFuzzySearchTags(activity.getContext());
        maxResults = getMaxResultCount(activity.getContext());
    }

    @Nullable
    public Context getContext() {
        ISearchActivity activity = activityWeakReference.get();
        return activity != null ? activity.getContext() : null;
    }

    @NonNull
    public String getQuery() {
        return query;
    }

    protected PriorityQueue<EntryItem> getPojoProcessor(ISearchActivity activity) {
        return new PriorityQueue<>(INITIAL_CAPACITY, EntryItem.RELEVANCE_COMPARATOR);
    }

    protected int getMaxResultCount(Context context) {
        return PrefCache.getResultSearcherCap(context);
    }

    /**
     * This is called from the background thread by the providers
     * @param pojos
     */
    @WorkerThread
    @Override
    public boolean addResult(Collection<? extends EntryItem> pojos) {
        if (isCancelled())
            return false;

        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return false;

        processedPojos.addAll(pojos);
        while (processedPojos.size() > maxResults)
            processedPojos.poll();

        return true;
    }

    @CallSuper
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        start = System.currentTimeMillis();

        displayActivityLoader();
    }

    private void displayActivityLoader() {
        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return;

        searchActivity.displayLoader(true);
    }

    @Override
    protected void onPostExecute(Void param) {
        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return;

        // Loader should still be displayed until all the providers have finished loading
        searchActivity.displayLoader(!TBApplication.getApplication(activity).getDataHandler().fullLoadOverSent());

        if (this.processedPojos.isEmpty()) {
            searchActivity.clearAdapter();
        } else {
            PriorityQueue<EntryItem> queue = this.processedPojos;
            ArrayList<EntryItem> results = new ArrayList<>(queue.size());
            while (queue.peek() != null) {
                results.add(queue.poll());
            }

            searchActivity.updateAdapter(results, isRefresh);
        }

        searchActivity.resetTask();

        long time = System.currentTimeMillis() - start;
        Log.v("Timing", "Time to run query `" + query + "` on " + getClass().getSimpleName() + " to completion: " + time + "ms");
    }

    public void setRefresh(boolean refresh) {
        isRefresh = refresh;
    }

    @Override
    public boolean tagsEnabled() {
        return tagsEnabled;
    }

    public void execute() {
        TaskRunner.executeOnExecutor(Searcher.SEARCH_THREAD, this);
    }
}
