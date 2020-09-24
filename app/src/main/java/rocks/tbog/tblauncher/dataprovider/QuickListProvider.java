package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.PlaceholderEntry;

public class QuickListProvider extends DBProvider<EntryItem> {

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
        return super.getPojos();
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
            ArrayList<FavRecord> list = dataHandler.getFavorites();
            ArrayList<EntryItem> quickList = new ArrayList<>(list.size());
            // get EntryItem from FavRecord
            for (FavRecord fav : list) {
                if (!fav.isInQuickList())
                    continue;
                EntryItem entry = new PlaceholderEntry(fav.record);
                quickList.add(entry);
            }

            return quickList;
        }

        @Override
        protected void onPostExecute(List<EntryItem> entryItems) {
            super.onPostExecute(entryItems);
            DBProvider<EntryItem> provider = weakProvider.get();
            if (provider != null)
                TBApplication.quickList(provider.context).onFavoritesChanged();
        }
    }
}
