package rocks.tbog.tblauncher.utils;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import java.util.List;

public abstract class ViewHolderListAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends ViewHolderAdapter<T, VH> {
    @NonNull
    final List<T> mList;

    protected ViewHolderListAdapter(@NonNull Class<VH> viewHolderClass, int listItemLayout, @NonNull List<T> list) {
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
}
