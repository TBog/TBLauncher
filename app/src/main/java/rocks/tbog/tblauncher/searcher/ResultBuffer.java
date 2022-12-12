package rocks.tbog.tblauncher.searcher;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.entry.EntryItem;

public class ResultBuffer<T extends EntryItem> implements ISearcher {
    private final boolean tagsEnabled;
    private final ArrayList<T> entryItems = new ArrayList<>(0);
    Class<T> typeClass;

    public ResultBuffer(boolean tagsEnabled, Class<T> typeClass) {
        this.tagsEnabled = tagsEnabled;
        this.typeClass = typeClass;
    }

    public Collection<T> getEntryItems() {
        return entryItems;
    }

    @Override
    public boolean addResult(Collection<? extends EntryItem> items) {
        boolean result = false;
        for (EntryItem item : items)
            result |= entryItems.add(typeClass.cast(item));
        return result;
    }

    @Override
    public boolean tagsEnabled() {
        return tagsEnabled;
    }
}
