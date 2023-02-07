package rocks.tbog.tblauncher.widgets;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class WidgetListAdapter extends ViewHolderListAdapter<MenuItem, ItemWidget.InfoViewHolder> {

    public WidgetListAdapter() {
        super(ItemWidget.InfoViewHolder.class, R.layout.popup_list_item_icon, new ArrayList<>());
    }

    public void clearList() {
        mList.clear();
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        return !(getItem(position) instanceof ItemTitle);
    }

    @Override
    protected int getItemViewTypeLayout(int viewType) {
        if (viewType == 1)
            return R.layout.popup_title;
        return super.getItemViewTypeLayout(viewType);
    }

    public int getItemViewType(int position) {
        return getItem(position) instanceof ItemTitle ? 1 : 0;
    }

    public int getViewTypeCount() {
        return 2;
    }
}
