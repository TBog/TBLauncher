package rocks.tbog.tblauncher.searcher;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.HashSet;
import java.util.PriorityQueue;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagSearcher extends Searcher {
    final EntryWithTags.TagDetails tagDetails;
    final HashSet<String> foundIdSet = new HashSet<>();

    public TagSearcher(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
        tagDetails = new EntryWithTags.TagDetails(query);
    }

    @Override
    protected PriorityQueue<EntryItem> newResultQueue() {
        return new PriorityQueue<>(INITIAL_CAPACITY, EntryItem.NAME_COMPARATOR);
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

        var processedPojos = getResultQueue();
        processedPojos.add(entryItem);
        if (processedPojos.size() > maxResults)
            processedPojos.poll();
    }

    @WorkerThread
    @Override
    protected Void doInBackground(Void param) {
        ISearchActivity searchActivity = activityWeakReference.get();
        Context context = searchActivity != null ? searchActivity.getContext() : null;
        if (context == null)
            return null;

        // Request results via "addResult"
        TBApplication.getApplication(context).getDataHandler().requestAllRecords(this);
        return null;
    }
}
