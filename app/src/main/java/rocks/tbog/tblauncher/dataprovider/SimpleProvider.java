package rocks.tbog.tblauncher.dataprovider;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.utils.Timer;

/**
 * Unlike normal providers, simple providers are not Android Services but classic Android class
 * Android Services are expensive to create, and use a lot of memory,
 * so whenever we can, we avoid using them.
 */
public abstract class SimpleProvider<T extends EntryItem> implements IProvider<T> {

    @Override
    public void requestResults(String query, ISearcher searcher) {
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public final boolean isLoaded() {
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
        return null;
    }

    @Nullable
    @Override
    public List<T> getPojos() {
        return null;
    }
}
