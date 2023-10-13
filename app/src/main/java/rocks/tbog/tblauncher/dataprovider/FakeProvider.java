package rocks.tbog.tblauncher.dataprovider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.utils.Timer;

public class FakeProvider<T extends EntryItem> implements IProvider<T> {
    private final GetEntryItems<T> mEntryProvider;

    public interface GetEntryItems<EntryType> {
        @Nullable
        List<EntryType> getEntryItems();
    }

    public FakeProvider(GetEntryItems<T> getter) {
        mEntryProvider = getter;
    }

    @Override
    public void requestResults(String query, ISearcher searcher) {

    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Nullable
    @Override
    public Timer getLoadDuration() {
        return null;
    }

    @Override
    public void setDirty() {
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return false;
    }

    @Override
    public T findById(@NonNull String id) {
        List<T> entryList = getPojos();
        if (entryList == null)
            return null;
        return entryList
            .stream()
            .filter(entryItem -> entryItem.id.equals(id))
            .findAny()
            .orElse(null);
    }

    @Nullable
    @Override
    public List<T> getPojos() {
        return mEntryProvider.getEntryItems();
    }
}
