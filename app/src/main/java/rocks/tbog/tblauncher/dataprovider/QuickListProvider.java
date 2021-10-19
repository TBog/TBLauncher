package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.PlaceholderEntry;
import rocks.tbog.tblauncher.utils.Utilities;

public class QuickListProvider extends DBProvider<EntryItem> {
    private final static String TAG = QuickListProvider.class.getSimpleName();
    private final HashMap<String, ModRecord> mQuickListFavRecords = new HashMap<>();
    private QuickListRecordLoader mQuickListRecordLoadTask;

    public QuickListProvider(Context context) {
        super(context);

        mQuickListRecordLoadTask = new QuickListRecordLoader(this);
        Utilities.runAsync(mQuickListRecordLoadTask);
    }


    @Override
    public List<EntryItem> getPojos() {
        boolean needsSorting = false;

        Collection<ModRecord> recordIds = mQuickListFavRecords.values();
        boolean remakeEntries = false;
        if (entryList.size() == recordIds.size()) {
            for (EntryItem entryItem : entryList) {
                if (!mQuickListFavRecords.containsKey(entryItem.id)) {
                    Log.d(TAG, "remake: not found " + entryItem.id);
                    remakeEntries = true;
                    break;
                }
            }
        } else {
            Log.d(TAG, "remake: " + entryList.size() + " \u2260 " + recordIds.size());
            remakeEntries = true;
        }
        if (remakeEntries) {
            needsSorting = true;
            entryList.clear();
            // make them all placeholders, we'll replace later
            for (ModRecord fav : recordIds) {
                PlaceholderEntry entry = new PlaceholderEntry(fav.record);
                entry.setName(fav.displayName);
                if (fav.hasCustomIcon())
                    entry.setCustomIcon();
                entryList.add(entry);
            }
        }

        ArrayList<EntryItem> toAdd = new ArrayList<>();
        DataHandler dataHandler = TBApplication.dataHandler(context);

        // replace placeholders with the correct entry
        for (Iterator<EntryItem> iterator = entryList.iterator(); iterator.hasNext(); ) {
            EntryItem entryItem = iterator.next();
            if (entryItem instanceof PlaceholderEntry) {
                needsSorting = true;
                entryItem = dataHandler.getPojo(entryItem.id);
                if (entryItem != null) {
                    toAdd.add(entryItem);
                    iterator.remove();
                }
            }
        }
        entryList.addAll(toAdd);
        // if we have replaced some PlaceholderEntry then we need to sort again
        if (needsSorting) {
            // sort entryList
            Collections.sort(entryList, (o1, o2) -> {
                ModRecord p1 = mQuickListFavRecords.get(o1.id);
                ModRecord p2 = mQuickListFavRecords.get(o2.id);
                if (p1 == null || p1.position == null || p2 == null || p2.position == null)
                    return 0;
                return p1.position.compareTo(p2.position);
            });
        }
        return super.getPojos();
    }

    private void setRecords(ArrayList<ModRecord> modRecords) {
        synchronized (mQuickListFavRecords) {
            mQuickListFavRecords.clear();
            for (ModRecord rec : modRecords)
                mQuickListFavRecords.put(rec.record, rec);
        }
        setLoaded();
        Log.d(TAG, "setRecords loaded " + modRecords.size() + " record(s)");
        TBApplication.quickList(context).reload();
    }

    @Override
    protected DBLoader<EntryItem> newLoadTask() {
        return new QuickListLoader(this);
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        mQuickListRecordLoadTask = new QuickListRecordLoader(this);
        Utilities.runAsync(mQuickListRecordLoadTask);
        super.reload(cancelCurrentLoadTask);
    }

    private static class QuickListRecordLoader extends AsyncTask<Void, ArrayList<ModRecord>> {
        private final WeakReference<QuickListProvider> weakProvider;

        protected QuickListRecordLoader(QuickListProvider quickListProvider) {
            super();
            weakProvider = new WeakReference<>(quickListProvider);
        }

        @Override
        protected ArrayList<ModRecord> doInBackground(Void input) {
            QuickListProvider provider = weakProvider.get();
            if (provider == null)
                return null;
            ArrayList<ModRecord> records = DBHelper.getMods(provider.context);
            // keep only items in the QuickList
            for (Iterator<ModRecord> iterator = records.iterator(); iterator.hasNext(); ) {
                ModRecord rec = iterator.next();
                if (!rec.isInQuickList())
                    iterator.remove();
            }
            records.trimToSize();
            return records;
        }

        @Override
        protected void onPostExecute(ArrayList<ModRecord> output) {
            QuickListProvider provider = weakProvider.get();
            if (provider == null || output == null || provider.mQuickListRecordLoadTask != this)
                return;
            provider.setRecords(output);
        }
    }

    private static class QuickListLoader extends DBProvider.DBLoader<EntryItem> {

        public QuickListLoader(DBProvider<EntryItem> provider) {
            super(provider);
        }

        @Override
        List<EntryItem> getEntryItems(DataHandler dataHandler) {
            DBProvider<EntryItem> provider = weakProvider.get();
            if (!(provider instanceof QuickListProvider))
                return null;
            ArrayList<ModRecord> records = null;
            try {
                records = ((QuickListProvider) provider).mQuickListRecordLoadTask.get();
            } catch (Throwable throwable) {
                Log.e(TAG, "wait for QuickList records", throwable);
            }
            if (records == null)
                return null;
            ArrayList<EntryItem> quickList = new ArrayList<>(records.size());
            // get EntryItem from ModRecord
            for (ModRecord fav : records) {
                if (!fav.isInQuickList())
                    continue;
                PlaceholderEntry entry = new PlaceholderEntry(fav.record);
                entry.setName(fav.displayName);
                if (fav.hasCustomIcon())
                    entry.setCustomIcon();
                quickList.add(entry);
            }

            return quickList;
        }

        @Override
        protected void onPostExecute(List<EntryItem> entryItems) {
            super.onPostExecute(entryItems);
            DBProvider<EntryItem> provider = weakProvider.get();
            if (provider != null) {
                TBApplication.quickList(provider.context).reload();
            }
        }
    }
}
