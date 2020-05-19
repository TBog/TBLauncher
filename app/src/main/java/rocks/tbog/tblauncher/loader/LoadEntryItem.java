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
    private WeakReference<Provider<T>> provider;

    LoadEntryItem(Context context) {
        super();
        this.context = new WeakReference<>(context);
    }

    public void setProvider(Provider<T> provider) {
        this.provider = new WeakReference<>(provider);
    }

    @NonNull
    public abstract String getScheme();

    @Override
    protected void onPostExecute(ArrayList<T> result) {
        super.onPostExecute(result);
        if(provider != null) {
            provider.get().loadOver(result);
        }
    }

}
