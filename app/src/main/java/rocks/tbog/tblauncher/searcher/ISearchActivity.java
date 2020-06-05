package rocks.tbog.tblauncher.searcher;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;

public interface ISearchActivity {
    void displayLoader(boolean b);

    @NonNull
    Context getContext();

    /**
     * Called after the search task finished
     */
    void resetTask();

    /**
     * Called when the searcher found no results
     */
    void clearAdapter();

    /**
     * Called when searcher found results
     */
    void updateAdapter(List<? extends EntryItem> results, boolean isRefresh);

    /**
     * Called when user removed/hidden app
     */
    void removeResult(EntryItem result);

    /**
     * Show only results matching filter text
     * @param text to filter for
     */
    void filterResults(String text);

    /**
     * Should we search the tags?
     * @return true if tags should be searched
     */
    boolean tagsEnabled();
}
