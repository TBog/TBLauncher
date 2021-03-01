package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.PlaceholderEntry;

public class QuickListProvider extends DBProvider<EntryItem> {
    private final HashMap<String, String> mFavPosition = new HashMap<>();

    public QuickListProvider(Context context) {
        super(context);
    }

    @Override
    public List<EntryItem> getPojos() {
        ArrayList<EntryItem> toAdd = new ArrayList<>();
        DataHandler dataHandler = TBApplication.dataHandler(context);
        for (Iterator<EntryItem> iterator = entryList.iterator(); iterator.hasNext(); ) {
            EntryItem entryItem = iterator.next();
            if (entryItem instanceof PlaceholderEntry) {
                entryItem = dataHandler.getPojo(entryItem.id);
                if (entryItem != null)
                    toAdd.add(entryItem);
                iterator.remove();
            }
        }
        entryList.addAll(toAdd);
        // if we have replaced some PlaceholderEntry then we need to sort again
        if (!toAdd.isEmpty() && entryList.size() != toAdd.size())
        {
            // sort entryList
            Collections.sort(entryList, (o1, o2) -> {
                String p1 = mFavPosition.get(o1.id);
                String p2 = mFavPosition.get(o2.id);
                if (p1 == null || p2 == null)
                    return 0;
                return p1.compareTo(p2);
            });
        }
        return super.getPojos();
    }

    private void setRecords(ArrayList<FavRecord> favRecords) {
        mFavPosition.clear();
        for (FavRecord rec : favRecords)
            mFavPosition.put(rec.record, rec.position);
    }

    @Override
    protected DBLoader<EntryItem> newLoadTask() {
        return new QuickListLoader(this);
    }

    private static class QuickListLoader extends DBProvider.DBLoader<EntryItem> {
        private ArrayList<FavRecord> mFavRecords = null;

        public QuickListLoader(DBProvider<EntryItem> provider) {
            super(provider);
        }

        @Override
        List<EntryItem> getEntryItems(DataHandler dataHandler) {
            mFavRecords = dataHandler.getFavorites();
            ArrayList<EntryItem> quickList = new ArrayList<>(mFavRecords.size());
            // get EntryItem from FavRecord
            for (FavRecord fav : mFavRecords) {
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
                if (mFavRecords != null)
                    ((QuickListProvider) provider).setRecords(mFavRecords);
                TBApplication.quickList(provider.context).onFavoritesChanged();
            }
        }
    }
}
