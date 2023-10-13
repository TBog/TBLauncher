package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.PlaceholderEntry;
import rocks.tbog.tblauncher.handler.DataHandler;

public class QuickListProvider extends DBProvider<EntryItem> {
    private final static String TAG = QuickListProvider.class.getSimpleName();

    public QuickListProvider(Context context) {
        super(context);
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    private void fixPlaceholders() {
        DataHandler dataHandler = TBApplication.dataHandler(context);
        int replaceCount = 0;
        for (int idx = 0; idx < entryList.size(); idx += 1) {
            EntryItem entryItem = entryList.get(idx);
            if (entryItem instanceof PlaceholderEntry) {
                entryItem = dataHandler.getPojo(entryItem.id);
                if (entryItem != null) {
                    entryList.set(idx, entryItem);
                    replaceCount += 1;
                }
            }
        }
        Log.i(TAG, "replaced " + replaceCount + "/" + entryList.size() + " placeholder(s)");
    }

    @Override
    protected DBLoader<EntryItem> newLoadTask() {
        return new QuickListLoader(this);
    }

    private static class QuickListLoader extends DBProvider.DBLoader<EntryItem> {
        public QuickListLoader(DBProvider<EntryItem> provider) {
            super(provider);
        }

        @Override
        List<EntryItem> getEntryItems(DataHandler dataHandler) {
            Context context = getContext();
            if (context == null)
                return null;
            ArrayList<ModRecord> records = DBHelper.getMods(context);
            return records
                .stream()
                .filter(ModRecord::isInQuickList)
                .map(fav -> {
                    PlaceholderEntry entry = new PlaceholderEntry(fav.record, fav.position);
                    entry.setName(fav.displayName);
                    if (fav.hasCustomIcon())
                        entry.setCustomIcon();
                    return entry;
                }).sorted((o1, o2) -> {
                    String p1 = ((PlaceholderEntry) o1).position;
                    String p2 = ((PlaceholderEntry) o2).position;
                    if (p1 == null || p2 == null)
                        return 0;
                    return p1.compareTo(p2);
                }).collect(Collectors.toList());
        }

        @Override
        protected void onPostExecute(List<EntryItem> entryItems) {
            super.onPostExecute(entryItems);
            Context context = getContext();
            if (context == null)
                return;
            TBApplication.dataHandler(context).runAfterLoadOver(() -> {
                DBProvider<EntryItem> provider = weakProvider.get();
                if (provider instanceof QuickListProvider) {
                    ((QuickListProvider) provider).fixPlaceholders();
                    TBLauncherActivity launcherActivity = TBApplication.launcherActivity(provider.context);
                    if (launcherActivity != null)
                        launcherActivity.queueDockReload();
                }
            });
        }
    }
}
