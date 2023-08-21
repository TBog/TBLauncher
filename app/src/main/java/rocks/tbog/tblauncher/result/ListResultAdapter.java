package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.entry.EntryItem;

public class ListResultAdapter extends BaseAdapter {
    final ArrayList<EntryItem> mList = new ArrayList<>(0);
    final int mDrawFlags = EntryItem.FLAG_DRAW_GRID | EntryItem.FLAG_DRAW_ICON;

    @Override
    public int getCount() {
        return mList.size();
    }

    @Override
    public EntryItem getItem(int position) {
        return mList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).hashCode();
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final EntryItem item = getItem(position);

        Context context = parent.getContext();
        View itemView = convertView;
        if (itemView == null) {
            final int viewType = getItemViewType(position);
            final int layoutRes = ResultHelper.getItemViewLayout(viewType);

            LayoutInflater inflater = LayoutInflater.from(context);
            itemView = inflater.inflate(layoutRes, parent, false);
        }

        item.displayResult(itemView, mDrawFlags);
        return itemView;
    }

    @Override
    public int getItemViewType(int position) {
        final EntryItem item = getItem(position);
        return ResultHelper.getItemViewType(item, mDrawFlags);
    }

    @Override
    public int getViewTypeCount() {
        return ResultHelper.getItemViewTypeCount();
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    public void setItems(Collection<EntryItem> list) {
        mList.clear();
        mList.addAll(list);
        notifyDataSetChanged();
    }
}
