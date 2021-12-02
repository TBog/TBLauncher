package rocks.tbog.tblauncher.searcher;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceManager;

import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagList extends Searcher {
    final HashSet<String> foundIdSet = new HashSet<>();

    public TagList(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
    }

    @Override
    protected PriorityQueue<EntryItem> getPojoProcessor(ISearchActivity activity) {
        if ("untagged".equals(query))
            return new PriorityQueue<>(INITIAL_CAPACITY, EntryItem.NAME_COMPARATOR);
        return super.getPojoProcessor(activity);
    }

    @WorkerThread
    @Override
    public boolean addResult(EntryItem... pojos) {
        if (isCancelled())
            return false;

        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return false;

        // only allow untagged entries
        for (EntryItem entryItem : pojos) {
            if (entryItem instanceof EntryWithTags) {
                if (((EntryWithTags) entryItem).getTags().isEmpty()) {
                    addProcessedPojo(entryItem);
                }
            }
        }
        return true;
    }

    private void addProcessedPojo(EntryItem entryItem) {
        // if id already processed, skip it
        if (!foundIdSet.add(entryItem.id))
            return;

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

        DataHandler dh = TBApplication.dataHandler(context);

        // Request results via "addResult"
        if ("untagged".equals(query))
            dh.requestAllRecords(this);
        else if ("list".equals(query) || "listReversed".equals(query))
        {
            TagsProvider tagsProvider = dh.getTagsProvider();
            boolean reversed = query.endsWith("Reversed");

            // add tags from the tags menu list
            if (tagsProvider != null) {
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
                List<String> tagOrder = PrefOrderedListHelper.getOrderedList(pref, "tags-menu-list", "tags-menu-order");
                for (String orderValue : tagOrder) {
                    String tagName = PrefOrderedListHelper.getOrderedValueName(orderValue);
                    int order = 10 * PrefOrderedListHelper.getOrderedValueIndex(orderValue);

                    TagEntry tagEntry = tagsProvider.getTagEntry(tagName);
                    tagEntry.setRelevance(tagEntry.normalizedName, null);
                    tagEntry.boostRelevance(reversed ? order : -order);
                    addProcessedPojo(tagEntry);
                }
            }

            // add the show untagged action to the result list
            EntryItem untaggedEntry;
            boolean bAddUntagged = PrefCache.showTagsMenuUntagged(context);
            if (bAddUntagged) {
                untaggedEntry = TBApplication.dataHandler(context).getPojo(ActionEntry.SCHEME + "show/untagged");
                if (untaggedEntry instanceof ActionEntry) {
                    int idx = -1 + 10 * PrefCache.getTagsMenuUntaggedIndex(context);

                    untaggedEntry.setRelevance(untaggedEntry.normalizedName, null);
                    untaggedEntry.boostRelevance(reversed ? idx : -idx);
                    addProcessedPojo(untaggedEntry);
                }
            }
        }
        return null;
    }
}
