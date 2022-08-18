package rocks.tbog.tblauncher;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.activity.ComponentActivity;

import java.util.Arrays;
import java.util.Collections;

import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.dataprovider.SearchProvider;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.result.ListResultAdapter;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UISizes;

public class SearchEngineGrid implements ISearcher {
    private SearchProvider mSearchProvider;
    private GridView mGridView;
    private ListResultAdapter mGridAdapter;
    private String mQuery;

    public void loadProvider(ComponentActivity activity) {
        var loader = new SearchProviderLoader();
        var task = TaskRunner.newTask(activity.getLifecycle(),
            t -> loader.workAsync(SearchEngineGrid.this),
            t -> loader.whenDone(SearchEngineGrid.this));
        DataHandler.EXECUTOR_PROVIDERS.execute(task);
    }

    public void initLayout(GridView gridView, ListResultAdapter gridAdapter) {
        Context ctx = gridView.getContext();
        mGridView = gridView;
        mGridAdapter = gridAdapter;

        gridView.setAdapter(gridAdapter);
        CustomizeUI.setResultListPref(gridView, true);
        int columnWidth = UISizes.getResultIconSize(ctx);
        gridView.setColumnWidth(columnWidth);

        gridView.setOnItemClickListener(this::onItemClick);
        gridView.setOnItemLongClickListener(this::onItemLongClick);
    }

    public void setQuery(String query) {
        mQuery = query;
    }

    public void setVisibility(int visibility) {
        mGridView.setVisibility(visibility);
        if (visibility == View.INVISIBLE && !mGridAdapter.isEmpty()) {
            mQuery = null;
            mGridAdapter.setItems(Collections.emptyList());
        }
    }

    public void updateAdapter(SharedPreferences pref) {
        if (mQuery == null || mQuery.isEmpty()) {
            mGridAdapter.setItems(Collections.emptyList());
        } else if (mSearchProvider != null && pref.getBoolean("search-engine-grid", false)) {
            mSearchProvider.requestResults(mQuery, this);
        }
        if (mGridAdapter.isEmpty())
            setVisibility(View.INVISIBLE);
    }

    @Override
    public boolean addResult(EntryItem... items) {
        mGridAdapter.setItems(Arrays.asList(items));
        return false;
    }

    @Override
    public boolean tagsEnabled() {
        return false;
    }

    private void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getAdapter() != mGridAdapter)
            return;
        var result = mGridAdapter.getItem(position);
        ResultHelper.launch(view, result);
    }

    private boolean onItemLongClick(AdapterView<?> parent, View v, int position, long id) {
        if (parent.getAdapter() != mGridAdapter)
            return false;

        var result = mGridAdapter.getItem(position);
        ListPopup menu = result.getPopupMenu(v);

        // check if menu contains elements and if yes show it
        if (!menu.getAdapter().isEmpty()) {
            TBApplication.getApplication(v.getContext()).registerPopup(menu);
            menu.show(v);
            return true;
        }

        return false;
    }

    private static class SearchProviderLoader {
        private SearchProvider searchProvider = null;

        protected void workAsync(SearchEngineGrid searchEngineGrid) {
            Context context = searchEngineGrid.mGridView.getContext();
            searchProvider = new SearchProvider(context, true, false);
            var modList = TBApplication.dataHandler(context).getMods();
            for (var mod : modList) {
                if (mod.hasCustomIcon() && searchProvider.mayFindById(mod.record)) {
                    var entry = searchProvider.findById(mod.record);
                    if (entry != null)
                        entry.setCustomIcon();
                }
            }
        }

        protected void whenDone(SearchEngineGrid searchEngineGrid) {
            searchEngineGrid.mSearchProvider = searchProvider;
        }
    }
}
