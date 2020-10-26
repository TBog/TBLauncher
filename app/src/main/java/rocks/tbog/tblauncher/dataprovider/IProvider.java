package rocks.tbog.tblauncher.dataprovider;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.Searcher;

/**
 * Public interface exposed by every KISS data provider
 */
public interface IProvider<T extends EntryItem> {
    int LOAD_STEP_1 = 0;
    int LOAD_STEP_2 = 1;
    int[] LOAD_STEPS = new int[] {LOAD_STEP_1, LOAD_STEP_2};

    /**
     * Post search results for the given query string to the searcher
     *
     * @param s        Some string query (usually provided by an user)
     * @param searcher The receiver of results
     */
    @WorkerThread
    void requestResults(String s, Searcher searcher);

    /**
     * Reload the data stored in this provider
     * <p>
     * `"fr.neamar.summon.LOAD_OVER"` will be emitted once the reload is complete. The data provider
     * will stay usable (using it's old data) during the reload.
     * @param cancelCurrentLoadTask pass true to stop current loading task and start another;
     *                              pass false to do nothing if already loading
     */
    void reload(boolean cancelCurrentLoadTask);

    /**
     * Indicate whether this provider has already loaded it's data
     * <p>
     * If this method returns `false` then the client may listen for the
     * `"fr.neamar.summon.LOAD_OVER"` intent for notification of when the provider is ready.
     *
     * @return Is the provider ready to process search results?
     */
    boolean isLoaded();

    /**
     * Indicate that some providers have reloaded and this one may need to also reload
     */
    void setDirty();

    /**
     * Return the loading step for this provider
     * @return one of the LOAD_STEPS
     */
    int getLoadStep();

    /**
     * Tells whether or not this provider may be able to find the pojo with
     * specified id
     *
     * @param id id we're looking for
     * @return true if the provider can handle the query ; does not guarantee it
     * will!
     */
    boolean mayFindById(@NonNull String id);

    /**
     * Try to find a record by its id
     *
     * @param id id we're looking for
     * @return null if not found
     */
    T findById(@NonNull String id);

    /**
     * Get a list of all pojos, do not modify this list!
     *
     * @return list of all entries
     */
    List<T> getPojos();
}
