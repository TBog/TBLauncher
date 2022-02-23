package rocks.tbog.tblauncher.searcher;

import androidx.annotation.WorkerThread;

import rocks.tbog.tblauncher.entry.EntryItem;

public interface ISearcher {
    @WorkerThread
    boolean addResult(EntryItem... items);

    boolean tagsEnabled();
}
