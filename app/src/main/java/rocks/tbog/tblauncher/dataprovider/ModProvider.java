package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ICustomIconEntry;
import rocks.tbog.tblauncher.handler.DataHandler;

/**
 * This provider is loaded last and is responsible for setting custom names and icons
 */
public class ModProvider extends DBProvider<EntryItem> {
    private List<ModRecord> recordList;

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
            DBProvider<EntryItem> provider = weakProvider.get();
            if (provider instanceof ModProvider) {
                ModProvider modProvider = (ModProvider) provider;
                // get list of records that have modifications from DB
                modProvider.recordList = dataHandler.getMods();
                // return an empty list of the correct size
                // we'll put the content in onPostExecute to ensure `dataHandler.getPojo` is up to date
                return new ArrayList<>(modProvider.recordList.size());
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<EntryItem> entryItems) {
            DBProvider<EntryItem> provider = weakProvider.get();
            if (provider instanceof ModProvider) {
                ModProvider modProvider = (ModProvider) provider;
                DataHandler dataHandler = TBApplication.getApplication(provider.context).getDataHandler();
                // get EntryItem from ModRecord
                for (ModRecord fav : modProvider.recordList) {
                    EntryItem entry = dataHandler.getPojo(fav.record);
                    if (entry == null)
                        continue;

                    if (entry instanceof ICustomIconEntry) {
                        if (fav.hasCustomIcon() && !((ICustomIconEntry) entry).hasCustomIcon())
                            ((ICustomIconEntry) entry).setCustomIcon();
                    }
                    if (fav.hasCustomName())
                        entry.setName(fav.displayName);

                    entryItems.add(entry);
                }
            }
            super.onPostExecute(entryItems);
        }
    }
}
