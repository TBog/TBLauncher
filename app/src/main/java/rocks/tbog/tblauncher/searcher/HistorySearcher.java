package rocks.tbog.tblauncher.searcher;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.EntryItem;

public class HistorySearcher extends Searcher {

    DBHelper.HistoryMode mHistoryMode;

    public HistorySearcher(ISearchActivity activity, @NonNull String query) {
        super(activity, query);
        mHistoryMode = DataHandler.getHistoryMode(query);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        ISearchActivity activity = activityWeakReference.get();
        Context context = activity != null ? activity.getContext() : null;
        if (context == null)
            return null;

        processedPojos.clear();
        List<EntryItem> history = getHistory(context, mHistoryMode);
        int order = history.size();
        for (EntryItem item : history) {
            item.setRelevance(item.normalizedName, null);
            item.boostRelevance(order--);
            processedPojos.add(item);
        }
        return null;
    }

    static List<EntryItem> getHistory(@NonNull Context context, DBHelper.HistoryMode mode) {
        DataHandler dataHandler = TBApplication.dataHandler(context);
        Set<String> exclude;
        // exclude favorites
        {
            exclude = new HashSet<>();
            for (FavRecord rec : dataHandler.getFavorites()) {
                if (rec.isInQuickList())
                    exclude.add(rec.record);
            }
        }
        int itemCount = PrefCache.getResultHistorySize(context);
        return dataHandler.getHistory(itemCount, mode, false, exclude);
    }

}
