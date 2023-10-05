package rocks.tbog.tblauncher.searcher;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ValuedHistoryRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.MapCompat;

/**
 * AsyncTask retrieving data from the providers and updating the view
 *
 * @author dorvaryn
 */
public class QuerySearcher extends Searcher {
    private final String trimmedQuery;
    private final HashMap<String, Integer> knownIds = new HashMap<>();

    public QuerySearcher(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
        trimmedQuery = query.trim();
    }

    @Override
    public boolean addResult(Collection<? extends EntryItem> pojos) {
        // Give a boost if item was previously selected for this query
        for (EntryItem pojo : pojos) {
            int historyRecord = MapCompat.getOrDefault(knownIds, pojo.id, 0);
            if (historyRecord != 0) {
                pojo.boostRelevance(25 * historyRecord);
            }
        }

        // call super implementation to update the adapter
        return super.addResult(pojos);
    }

    /**
     * Called on the background thread
     */
    @WorkerThread
    @Override
    protected Void doInBackground(Void param) {
        ISearchActivity searchActivity = activityWeakReference.get();
        Context context = searchActivity != null ? searchActivity.getContext() : null;
        if (context == null)
            return null;

        // Have we ever made the same query and selected something ?
        List<ValuedHistoryRecord> lastIdsForQuery = DBHelper.getPreviousResultsForQuery(context, trimmedQuery);
        knownIds.clear();
        for (ValuedHistoryRecord id : lastIdsForQuery) {
            knownIds.put(id.record, (int) id.value);
        }

        // Request results via "addResult"
        TBApplication.getApplication(context).getDataHandler().requestResults(trimmedQuery, this);
        return null;
    }
}
