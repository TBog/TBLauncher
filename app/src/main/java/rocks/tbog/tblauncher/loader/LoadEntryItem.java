package rocks.tbog.tblauncher.loader;

import android.content.Context;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.WorkAsync.AsyncTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.entry.EntryItem;

public abstract class LoadEntryItem<T extends EntryItem> extends AsyncTask<Void, ArrayList<T>> {

    final WeakReference<Context> context;
    private WeakReference<Provider<T>> weakProvider;

    LoadEntryItem(Context context) {
        super();
        this.context = new WeakReference<>(context);
    }

    public void setProvider(Provider<T> provider) {
        this.weakProvider = new WeakReference<>(provider);
    }

    @NonNull
    public abstract String getScheme();

    protected abstract ArrayList<T> doInBackground(Void param);

    protected void onPostExecute(ArrayList<T> result) {
        Provider<T> provider = weakProvider.get();
        if (provider != null) {
            provider.loadOver(result);
        }
    }

    public void execute() {
        TaskRunner.executeOnExecutor(DataHandler.EXECUTOR_PROVIDERS, this);
    }
}
