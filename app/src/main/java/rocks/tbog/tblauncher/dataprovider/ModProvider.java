package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;

public class ModProvider extends DBProvider<EntryItem> {

    public ModProvider(Context context) {
        super(context);
    }

    @Override
    protected DBLoader<EntryItem> newLoadTask() {
        return new FavLoader(this);
    }

    private static class FavLoader extends DBProvider.DBLoader<EntryItem> {

        public FavLoader(DBProvider<EntryItem> provider) {
            super(provider);
        }

        @Override
        List<EntryItem> getEntryItems(DataHandler dataHandler) {
            List<ModRecord> list = dataHandler.getMods();
            ArrayList<EntryItem> favList = new ArrayList<>(list.size());
            // get EntryItem from ModRecord
            for (ModRecord fav : list) {
                EntryItem entry = dataHandler.getPojo(fav.record);
                if (entry == null)
                    continue;
                else if (entry instanceof StaticEntry) {
                    if (fav.hasCustomIcon())
                        ((StaticEntry) entry).setCustomIcon();
                    if (fav.hasCustomName())
                        entry.setName(fav.displayName);
                }
                favList.add(entry);
            }

            return favList;
        }

        @Override
        protected void onPostExecute(List<EntryItem> entryItems) {
            super.onPostExecute(entryItems);
            DBProvider<EntryItem> provider = weakProvider.get();
            if (provider != null)
                TBApplication.quickList(provider.context).reload();
        }
    }
}
