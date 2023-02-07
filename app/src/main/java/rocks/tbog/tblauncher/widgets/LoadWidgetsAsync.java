package rocks.tbog.tblauncher.widgets;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;

import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class LoadWidgetsAsync extends ViewHolderListAdapter.LoadAsyncList<MenuItem, ItemWidget.InfoViewHolder, WidgetListAdapter> {
    @Nullable
    public Runnable whenDone = null;

    public LoadWidgetsAsync(@NonNull WidgetListAdapter adapter, @NonNull LoadInBackground<MenuItem> loadInBackground) {
        super(adapter, loadInBackground);
    }

    @Override
    protected void onPostExecute(Collection<MenuItem> data) {
        super.onPostExecute(data);
        if (whenDone != null)
            whenDone.run();
    }
}
