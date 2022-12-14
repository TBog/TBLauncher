package rocks.tbog.tblauncher.searcher;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

public class HistorySearcher extends Searcher {

    DBHelper.HistoryMode mHistoryMode;

    public HistorySearcher(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
        mHistoryMode = DataHandler.getHistoryMode(query);
    }

    @Override
    protected Void doInBackground(Void param) {

        ISearchActivity searchActivity = activityWeakReference.get();
        Activity activity = searchActivity != null ? Utilities.getActivity(searchActivity.getContext()) : null;
        if (activity == null)
            return null;

//        int maxResults = getMaxResultCount(activity);

        var processedPojos = getResultQueue();
        processedPojos.clear();
        List<EntryItem> history = getHistory(activity, mHistoryMode);
        int order = history.size();
        for (EntryItem item : history) {
            item.setRelevance(item.normalizedName, null);
            item.boostRelevance(order--);

            //addResult(item);
            processedPojos.add(item);
            if (processedPojos.size() > maxResults)
                processedPojos.poll();
        }
        return null;
    }

    static List<EntryItem> getHistory(@NonNull Context context, DBHelper.HistoryMode mode) {
        DataHandler dataHandler = TBApplication.dataHandler(context);
        Set<String> exclude;
        // exclude favorites
        {
            exclude = new HashSet<>();
            for (ModRecord rec : dataHandler.getMods()) {
                if (rec.isInQuickList())
                    exclude.add(rec.record);
            }
        }
        int itemCount = PrefCache.getResultHistorySize(context);
        return dataHandler.getHistory(itemCount, mode, false, exclude);
    }

}
