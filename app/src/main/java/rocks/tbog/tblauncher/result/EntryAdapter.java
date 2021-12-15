package rocks.tbog.tblauncher.result;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.List;

import rocks.tbog.tblauncher.entry.EntryItem;

public class EntryAdapter extends BaseAdapter {
    private final List<EntryItem> mItems;
    private final int mDrawFlags;

    public EntryAdapter(@NonNull List<EntryItem> objects) {
        mItems = objects;
        mDrawFlags = EntryItem.FLAG_DRAW_GRID | EntryItem.FLAG_DRAW_NAME | EntryItem.FLAG_DRAW_ICON | EntryItem.FLAG_DRAW_ICON_BADGE;
    }

    public EntryAdapter(@NonNull List<EntryItem> objects, int drawFlags) {
        mItems = objects;
        mDrawFlags = drawFlags;
    }

    public void addAll(Collection<EntryItem> newElements) {
        mItems.addAll(newElements);
        notifyDataSetChanged();
    }

    @Override
    public int getViewTypeCount() {
        return ResultHelper.getItemViewTypeCount();
    }

    @Override
    public int getItemViewType(int position) {
        return ResultHelper.getItemViewType(mItems.get(position), mDrawFlags);
    }

    @Override
    public EntryItem getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        final View view;
        EntryItem content = getItem(position);
        if (convertView == null) {
            view = LayoutInflater.from(parent.getContext()).inflate(content.getResultLayout(mDrawFlags), parent, false);
        } else {
            view = convertView;
        }

        content.displayResult(view, mDrawFlags);

        return view;
    }
}
