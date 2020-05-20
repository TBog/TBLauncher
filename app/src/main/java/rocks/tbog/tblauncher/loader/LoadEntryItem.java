package rocks.tbog.tblauncher.loader;

import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import rocks.tbog.tblauncher.dataprovider.Provider;
import rocks.tbog.tblauncher.entry.EntryItem;

public abstract class LoadEntryItem<T extends EntryItem> extends AsyncTask<Void, Void, ArrayList<T>> {

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

    @Override
    protected void onPostExecute(ArrayList<T> result) {
        super.onPostExecute(result);
        Provider<T> provider = weakProvider.get();
        if(provider != null) {
            provider.loadOver(result);
        }
    }

}
