package rocks.tbog.tblauncher.searcher;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.TagSortEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.utils.Utilities;

public class SortedTagSearcher extends Searcher {
    private final EntryWithTags.TagDetails tagDetails;
    private final TagSortEntry.TagDetails tagSort;
    private final HashSet<String> foundIdSet = new HashSet<>();
    private Map<EntryItem, Integer> order = null;

    public SortedTagSearcher(ISearchActivity activity, @NonNull String tagSortId) {
        super(activity, tagSortId);
        tagSort = new TagSortEntry.TagDetails(tagSortId);
        tagDetails = new EntryWithTags.TagDetails(tagSort.name);
    }

    @Override
    protected PriorityQueue<EntryItem> newResultQueue() {
        return new PriorityQueue<>(INITIAL_CAPACITY, tagSort.getComparator());
    }

    @WorkerThread
    @Override
    public boolean addResult(Collection<? extends EntryItem> pojos) {
        if (isCancelled())
            return false;

        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return false;

        for (EntryItem entryItem : pojos) {
            if (entryItem instanceof EntryWithTags) {
                if (((EntryWithTags) entryItem).getTags().contains(tagDetails)) {
                    addProcessedPojo((EntryWithTags) entryItem);
                }
            }
        }
        return true;
    }

    private void addProcessedPojo(EntryWithTags entryItem) {
        // if id already processed, skip it
        if (!foundIdSet.add(entryItem.id))
            return;

        if (order != null) {
            var priority = order.get(entryItem);
            if (priority != null)
                entryItem.boostRelevance(priority);
        }

        var processedPojos = getResultQueue();
        processedPojos.add(entryItem);
        if (processedPojos.size() > maxResults)
            processedPojos.poll();
    }

    @NonNull
    private DBHelper.HistoryMode getHistoryMode() {
        switch (tagSort.order) {
            case TagSortEntry.HISTORY_REC:
                return DBHelper.HistoryMode.RECENCY;
            case TagSortEntry.HISTORY_FREQ:
                return DBHelper.HistoryMode.FREQUENCY;
            case TagSortEntry.HISTORY_FREC:
                return DBHelper.HistoryMode.FRECENCY;
            case TagSortEntry.HISTORY_ADAPTIVE:
            default:
                return DBHelper.HistoryMode.ADAPTIVE;
        }
    }

    @WorkerThread
    @Override
    protected Void doInBackground(Void param) {
        ISearchActivity searchActivity = activityWeakReference.get();
        Context context = searchActivity != null ? searchActivity.getContext() : null;
        if (context == null)
            return null;

        DataHandler dh = TBApplication.dataHandler(context);

        switch (tagSort.order) {
            case TagSortEntry.SORT_AZ:
            case TagSortEntry.SORT_ZA:
                // Request all results via "addResult", the queue will sort by name
                dh.requestAllRecords(this);
                break;
            case TagSortEntry.HISTORY_REC:
            case TagSortEntry.HISTORY_FREQ:
            case TagSortEntry.HISTORY_FREC:
            case TagSortEntry.HISTORY_ADAPTIVE: {
                // boost relevance according to history order in `addProcessedPojo`
                order = dh.getHistoryOrder(getHistoryMode(), Collections.emptySet());
                // Request all results via "addResult", the queue will sort by relevance
                dh.requestAllRecords(this);
                break;
            }
            default:
                break;
        }
        return null;
    }
}
