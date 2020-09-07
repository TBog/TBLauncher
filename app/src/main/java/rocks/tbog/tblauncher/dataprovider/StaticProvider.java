package rocks.tbog.tblauncher.dataprovider;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.Searcher;

public abstract class StaticProvider<T extends EntryItem> implements IProvider {
    @NonNull
    final List<T> pojos;

    StaticProvider(@NonNull List<T> list) {
        pojos = list;
    }

    @Override
    public void requestResults(String s, Searcher searcher) {
    }

    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void setDirty() {
        // do nothing, we already have the full list of items
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public EntryItem findById(@NonNull String id) {
        for (EntryItem pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    @Override
    public List<? extends EntryItem> getPojos() {
        if (BuildConfig.DEBUG)
            return Collections.unmodifiableList(pojos);
        return pojos;
    }
}
