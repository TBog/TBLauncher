package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.DialContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.handler.DataHandler;

/**
 * This provider is loaded last and is responsible for setting custom names and icons
 */
public class ModProvider extends DBProvider<EntryItem> {

    public ModProvider(Context context) {
        super(context);
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_3;
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
                    if (fav.hasCustomIcon() && !((StaticEntry) entry).hasCustomIcon())
                        ((StaticEntry) entry).setCustomIcon();
                    if (fav.hasCustomName())
                        entry.setName(fav.displayName);
                }
                else if (entry instanceof SearchEntry) {
                    if (fav.hasCustomIcon() && !((SearchEntry) entry).hasCustomIcon())
                        ((SearchEntry) entry).setCustomIcon();
                } else if (entry instanceof DialContactEntry) {
                    if (fav.hasCustomIcon() && !((DialContactEntry) entry).hasCustomIcon())
                        ((DialContactEntry) entry).setCustomIcon();
                }
                favList.add(entry);
            }

            return favList;
        }
    }
}
