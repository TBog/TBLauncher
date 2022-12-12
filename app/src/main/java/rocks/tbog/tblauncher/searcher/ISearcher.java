package rocks.tbog.tblauncher.searcher;

import androidx.annotation.WorkerThread;

import java.util.Collection;

import rocks.tbog.tblauncher.entry.EntryItem;

public interface ISearcher {
    @WorkerThread
    boolean addResult(Collection<? extends EntryItem> items);

    boolean tagsEnabled();
}
