package rocks.tbog.tblauncher.dataprovider;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.Searcher;

/**
 * Unlike normal providers, simple providers are not Android Services but classic Android class
 * Android Services are expensive to create, and use a lot of memory,
 * so whenever we can, we avoid using them.
 */
public abstract class SimpleProvider<T extends EntryItem> implements IProvider<T> {

    @Override
    public void requestResults(String s, Searcher searcher) {
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public final boolean isLoaded() {
        return true;
    }

    @Override
    public void setDirty() {
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public boolean mayFindById(@NotNull String id) {
        return false;
    }

    @Override
    public T findById(@NotNull String id) {
        return null;
    }

    @Override
    public List<T> getPojos() {
        return null;
    }
}
