package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.TBApplication;
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

//    @Override
//    public List<EntryItem> getPojos() {
//        boolean needsSorting = false;
//
//        Collection<ModRecord> recordIds = mQuickListFavRecords.values();
//        boolean remakeEntries = false;
//        if (entryList.size() == recordIds.size()) {
//            for (EntryItem entryItem : entryList) {
//                if (!mQuickListFavRecords.containsKey(entryItem.id)) {
//                    Log.d(TAG, "remake: not found " + entryItem.id);
//                    remakeEntries = true;
//                    break;
//                }
//            }
//        } else {
//            Log.d(TAG, "remake: " + entryList.size() + " \u2260 " + recordIds.size());
//            remakeEntries = true;
//        }
//        if (remakeEntries) {
//            needsSorting = true;
//            entryList.clear();
//            // make them all placeholders, we'll replace later
//            for (ModRecord fav : recordIds) {
//                PlaceholderEntry entry = new PlaceholderEntry(fav.record, fav.position);
//                entry.setName(fav.displayName);
//                if (fav.hasCustomIcon())
//                    entry.setCustomIcon();
//                entryList.add(entry);
//            }
//        }
//
//        ArrayList<EntryItem> toAdd = new ArrayList<>();
//        DataHandler dataHandler = TBApplication.dataHandler(context);
//
//        // replace placeholders with the correct entry
//        for (Iterator<EntryItem> iterator = entryList.iterator(); iterator.hasNext(); ) {
//            EntryItem entryItem = iterator.next();
//            if (entryItem instanceof PlaceholderEntry) {
//                needsSorting = true;
//                entryItem = dataHandler.getPojo(entryItem.id);
//                if (entryItem != null) {
//                    toAdd.add(entryItem);
//                    iterator.remove();
//                }
//            }
//        }
//        entryList.addAll(toAdd);
//        // if we have replaced some PlaceholderEntry then we need to sort again
//        if (needsSorting) {
//            // sort entryList
//            Collections.sort(entryList, (o1, o2) -> {
//                ModRecord p1 = mQuickListFavRecords.get(o1.id);
//                ModRecord p2 = mQuickListFavRecords.get(o2.id);
//                if (p1 == null || p1.position == null || p2 == null || p2.position == null)
//                    return 0;
//                return p1.position.compareTo(p2.position);
//            });
//        }
//        return super.getPojos();
//    }

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
            int quickListSize = 0;
            // count only items in the QuickList
            for (ModRecord rec : records) {
                if (rec.isInQuickList())
                    quickListSize += 1;
            }

            ArrayList<EntryItem> quickList = new ArrayList<>(quickListSize);
            // get EntryItem from ModRecord
            for (ModRecord fav : records) {
                if (!fav.isInQuickList())
                    continue;
                PlaceholderEntry entry = new PlaceholderEntry(fav.record, fav.position);
                entry.setName(fav.displayName);
                if (fav.hasCustomIcon())
                    entry.setCustomIcon();
                quickList.add(entry);
            }

            Collections.sort(quickList, (o1, o2) -> {
                String p1 = ((PlaceholderEntry) o1).position;
                String p2 = ((PlaceholderEntry) o2).position;
                if (p1 == null || p2 == null)
                    return 0;
                return p1.compareTo(p2);
            });

            return quickList;
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
                    TBApplication.quickList(provider.context).reload();
                }
            });
        }
    }
}
