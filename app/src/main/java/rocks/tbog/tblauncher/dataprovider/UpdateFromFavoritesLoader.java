package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.StaticEntry;

public class UpdateFromFavoritesLoader<T extends StaticEntry> extends DBProvider.DBLoader<T> {
    private final T[] mEntries;
    private final int[] mNames;

    public UpdateFromFavoritesLoader(DBProvider<T> provider, T[] entries, int[] names) {
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

        List<FavRecord> favorites = dataHandler.getFavorites();
        // update custom settings from favorites
        for (FavRecord fav : favorites) {
            T entry = null;
            if (provider.mayFindById(fav.record)) {
                for (T e : output) {
                    if (e.id.equals(fav.record)) {
                        entry = e;
                        break;
                    }
                }
            }
            if (entry != null) {
                if (fav.hasCustomName())
                    entry.setName(fav.displayName);
                if (fav.hasCustomIcon())
                    entry.setCustomIcon();
            }
        }

        return output;
    }
}
