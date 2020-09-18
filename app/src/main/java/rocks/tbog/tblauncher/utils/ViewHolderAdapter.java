package rocks.tbog.tblauncher.utils;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public abstract class ViewHolderAdapter<T, VH extends ViewHolderAdapter.ViewHolder<T>> extends BaseAdapter {
    @NonNull
    final Class<VH> mViewHolderClass;
    @LayoutRes
    final int mListItemLayout;

    protected ViewHolderAdapter(@NonNull Class<VH> viewHolderClass, @LayoutRes int listItemLayout) {
        mViewHolderClass = viewHolderClass;
        mListItemLayout = listItemLayout;
    }

    @LayoutRes
    protected int getListItemLayout(int position) {
        return mListItemLayout;
    }

    @Override
    public abstract T getItem(int position);

    @Nullable
    protected VH getNewViewHolder(View view) {
        VH holder = null;
        try {
            holder = mViewHolderClass.getDeclaredConstructor(View.class).newInstance(view);
        } catch (ReflectiveOperationException e) {
            Log.e("VHA", "ViewHolder can't be instantiated (make sure class and constructor are public)", e);
        }
        return holder;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final View view;
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(getListItemLayout(position), parent, false);
        } else {
            view = convertView;
        }

        Object tag = view.getTag();
        VH holder = mViewHolderClass.isInstance(tag) ? mViewHolderClass.cast(tag) : getNewViewHolder(view);
        if (holder != null) {
            T content = getItem(position);
            holder.setContent(content, position, this);
        }
        return view;

    }

    public static abstract class ViewHolder<T> {
        protected ViewHolder(View view) {
            view.setTag(this);
        }

        protected abstract void setContent(T content, int position, @NonNull ViewHolderAdapter<T, ? extends ViewHolder<T>> adapter);
    }
}
