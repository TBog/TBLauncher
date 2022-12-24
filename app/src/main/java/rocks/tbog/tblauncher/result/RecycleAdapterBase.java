package rocks.tbog.tblauncher.result;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Collection;
import java.util.List;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.UIColors;

public abstract class RecycleAdapterBase<VH extends RecycleAdapterBase.Holder> extends RecyclerView.Adapter<VH> {

    private static final String TAG = RecycleAdapterBase.class.getSimpleName();

    @NonNull
    protected final List<EntryItem> entryList;
    protected int mDrawFlags;
    @Nullable
    private OnClickListener mOnClickListener = null;
    @Nullable
    private OnLongClickListener mOnLongClickListener = null;

    public RecycleAdapterBase(@NonNull List<EntryItem> list) {
        entryList = list;
    }

    public void setOnClickListener(@Nullable OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        mOnLongClickListener = listener;
    }

    @Override
    public int getItemViewType(int position) {
        final EntryItem entry = getItem(position);
        if (entry == null)
            return -1; // this is invalid and will throw later on
        return ResultHelper.getItemViewType(entry, mDrawFlags);
    }

    @Override
    public long getItemId(int position) {
        final EntryItem entry = getItem(position);
        if (entry == null)
            return -1;
        return entry.id.hashCode();
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        final EntryItem entry = getItem(position);
        if (entry == null)
            return;

        if (mOnClickListener != null)
            holder.setOnClickListener(v -> mOnClickListener.onClick(entry, v));
        else
            holder.setOnClickListener(null);

        if (mOnLongClickListener != null)
            holder.setOnLongClickListener(v -> mOnLongClickListener.onLongClick(entry, v));
        else
            holder.setOnLongClickListener(null);

        entry.displayResult(holder.itemView, mDrawFlags);
    }

    @Override
    public int getItemCount() {
        return entryList.size();
    }

    @Nullable
    public EntryItem getItem(int index) {
        final EntryItem entry;
        try {
            entry = entryList.get(index);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "pos=" + index + " size=" + entryList.size(), e);
            return null;
        }
        return entry;
    }

    public void removeItem(EntryItem result) {
        int position = entryList.indexOf(result);
        entryList.remove(result);
        notifyItemRemoved(position);
    }

    public void addItem(EntryItem item) {
        notifyItemInserted(entryList.size());
        entryList.add(item);
    }

    public void clear() {
        final int itemCount = entryList.size();
        entryList.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    public void refresh() {
        final int itemCount = entryList.size();
        notifyItemRangeChanged(0, itemCount);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateItems(Collection<? extends EntryItem> results) {
        this.entryList.clear();
        this.entryList.addAll(results);
        notifyDataSetChanged();
    }

    public void notifyItemChanged(EntryItem result) {
        int position = entryList.indexOf(result);
        Log.d(TAG, "notifyItemChanged #" + position + " id=" + result.id);
        if (position >= 0)
            notifyItemChanged(position);
    }

    public static class Holder extends RecyclerView.ViewHolder {

        public Holder(@NonNull View itemView) {
            super(itemView);
            itemView.setTag(this);

            // we set background selector here to do it only once
            int touchColor = UIColors.getResultListRipple(itemView.getContext());
            Drawable selectorBackground = CustomizeUI.getSelectorDrawable(itemView, touchColor, false);
            itemView.setBackground(selectorBackground);
        }

        public void setOnClickListener(@Nullable View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
            if (listener == null)
                itemView.setClickable(false);
        }

        public void setOnLongClickListener(@Nullable View.OnLongClickListener listener) {
            itemView.setOnLongClickListener(listener);
            if (listener == null)
                itemView.setLongClickable(false);
        }
    }

    public interface OnClickListener {
        void onClick(EntryItem entryItem, View view);
    }

    public interface OnLongClickListener {
        boolean onLongClick(EntryItem entryItem, View view);
    }
}
