package rocks.tbog.tblauncher.searcher;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import rocks.tbog.tblauncher.result.Result;

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
    void updateAdapter(ArrayList<Result> results, boolean isRefresh, String query);

    /**
     * Called when user removed/hidden app
     */
    void removeResult(Result result);
}
