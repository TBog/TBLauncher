package rocks.tbog.tblauncher.result;

import java.util.ArrayList;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.TaskRunner;
import rocks.tbog.tblauncher.utils.Utilities;

public class LoadDataForAdapter extends TaskRunner.AsyncTask<Void, ArrayList<EntryItem>> {
    private final EntryAdapter adapter;
    private final LoadInBackground task;

    public interface LoadInBackground {
        ArrayList<EntryItem> loadInBackground();
    }

    public LoadDataForAdapter(EntryAdapter adapter, LoadInBackground loadInBackground) {
        super();
        this.adapter = adapter;
        task = loadInBackground;
    }

    @Override
    protected ArrayList<EntryItem> doInBackground(Void param) {
        ArrayList<EntryItem> data = task.loadInBackground();
        data.trimToSize();
        return data;
    }

    @Override
    protected void onPostExecute(ArrayList<EntryItem> data) {
        if (data == null)
            return;
        adapter.addAll(data);
    }

    public void execute() {
        Utilities.executeAsync(this);
    }
}
