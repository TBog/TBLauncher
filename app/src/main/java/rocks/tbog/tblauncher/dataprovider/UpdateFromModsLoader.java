package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.StaticEntry;

public class UpdateFromModsLoader<T extends StaticEntry> extends DBProvider.DBLoader<T> {
    private final T[] mEntries;
    private final int[] mNames;

    public UpdateFromModsLoader(DBProvider<T> provider, T[] entries, int[] names) {
        super(provider);
        this.mEntries = entries;
        this.mNames = names;
    }

    @Override
    public List<T> getEntryItems(DataHandler dataHandler) {
        DBProvider<T> provider = weakProvider.get();
        if (provider == null)
            return null;
        Context context = provider.context;

        ArrayList<T> output = new ArrayList<>(mEntries.length);
        // copy static entries to returned list, also update the names
        for (int idx = 0; idx < mEntries.length; idx++) {
            T entry = mEntries[idx];
            entry.setName(context.getString(mNames[idx]));
            output.add(entry);
        }

        List<ModRecord> mods = dataHandler.getMods();
        // update custom settings from favorites
        for (ModRecord mod : mods) {
            T entry = null;
            if (provider.mayFindById(mod.record)) {
                for (T e : output) {
                    if (e.id.equals(mod.record)) {
                        entry = e;
                        break;
                    }
                }
            }
            if (entry != null) {
                if (mod.hasCustomName())
                    entry.setName(mod.displayName);
                if (mod.hasCustomIcon())
                    entry.setCustomIcon();
            }
        }

        return output;
    }
}
