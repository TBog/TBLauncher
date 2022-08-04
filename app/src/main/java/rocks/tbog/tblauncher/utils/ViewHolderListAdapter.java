package rocks.tbog.tblauncher.utils;

import android.util.Log;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class ViewHolderListAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends ViewHolderAdapter<T, VH> {
    @NonNull
    protected final List<T> mList;

    protected ViewHolderListAdapter(@NonNull Class<? extends VH> viewHolderClass, int listItemLayout, @NonNull List<T> list) {
        super(viewHolderClass, listItemLayout);
        mList = list;
    }

    @LayoutRes
    protected int getItemViewTypeLayout(int viewType) {
        return mListItemLayout;
    }

    @Override
    public T getItem(int position) {
        return mList.get(position);
    }

    @Override
    public int getCount() {
        return mList.size();
    }

    public void addItems(Collection<? extends T> items) {
        mList.addAll(items);
        notifyDataSetChanged();
    }

    public void addItem(T item) {
        mList.add(item);
        notifyDataSetChanged();
    }

    @Nullable
    public <L extends LoadAsyncList<T, ?, ?>> L newLoadAsyncList(@NonNull Class<L> loadAsyncClass, @NonNull LoadAsyncData.LoadInBackground<T> loadInBackground) {
        L loadAsync = null;
        try {
            loadAsync = loadAsyncClass.getDeclaredConstructor(this.getClass(), LoadAsyncData.LoadInBackground.class).newInstance(this, loadInBackground);
        } catch (ReflectiveOperationException e) {
            Log.e("VHLA", "LoadAsync can't be instantiated (make sure class and constructor are public)", e);
        }
        return loadAsync;
    }

    @NonNull
    public LoadAsyncList<T, ?, ?> newLoadAsyncList(@NonNull LoadAsyncData.LoadInBackground<T> loadInBackground) {
        return new LoadAsyncList<>(this, loadInBackground);
    }

    public static class LoadAsyncList<T, VH extends ViewHolderAdapter.ViewHolder<T>, A extends ViewHolderListAdapter<T, VH>> extends LoadAsyncData<T, A> {

        public LoadAsyncList(@NonNull A adapter, @NonNull LoadInBackground<T> loadInBackground) {
            super(adapter, loadInBackground);
        }

        @Override
        protected void onDataLoadFinished(@NonNull A adapter, @NonNull Collection<T> data) {
            adapter.addItems(data);
        }
    }
}
