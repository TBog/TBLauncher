package rocks.tbog.tblauncher.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.ui.ListPopup;

public class RecycleAdapter extends RecycleAdapterBase<RecycleAdapterBase.Holder> {

    @Nullable
    private ArrayList<EntryItem> resultsOriginal = null;

    private final Filter mFilter = new RecycleAdapter.FilterById();

    public RecycleAdapter(@NonNull Context context, @NonNull ArrayList<EntryItem> results) {
        super(results);
        setHasStableIds(true);
        setGridLayout(context, false);
        setOnClickListener(RecycleAdapter::onClick);
        setOnLongClickListener(RecycleAdapter::onLongClick);
    }

    @NonNull
    @Override
    public RecycleAdapterBase.Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final int layoutRes = ResultHelper.getItemViewLayout(viewType);

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(layoutRes, parent, false);

        return new RecycleAdapterBase.Holder(itemView);
    }

    public void setGridLayout(@NonNull Context context, boolean bGridLayout) {
        final int oldFlags = mDrawFlags;
        // get new flags
        mDrawFlags = getDrawFlags(context);
        mDrawFlags |= bGridLayout ? EntryItem.FLAG_DRAW_GRID : EntryItem.FLAG_DRAW_LIST;
        // refresh items if flags changed
        if (oldFlags != mDrawFlags)
            refresh();
    }

    private int getDrawFlags(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int drawFlags = EntryItem.FLAG_DRAW_NAME;
        if (prefs.getBoolean("tags-enabled", true))
            drawFlags |= EntryItem.FLAG_DRAW_TAGS;
        if (prefs.getBoolean("icons-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON;
        if (prefs.getBoolean("shortcut-show-badge", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON_BADGE;
        return drawFlags;
    }

    public void onClick(int index, View anyView) {
        final EntryItem result = getItem(index);
        if (result == null) {
            return;
        }

        onClick(result, anyView);
    }

    public static void onClick(final EntryItem result, View v) {
        ResultHelper.launch(v, result);
    }

    public static boolean onLongClick(final EntryItem result, View v) {
        ListPopup menu = result.getPopupMenu(v);

        // check if menu contains elements and if yes show it
        if (!menu.getAdapter().isEmpty()) {
            TBApplication.getApplication(v.getContext()).registerPopup(menu);
            menu.show(v);
            return true;
        }

        return false;
    }

    @Override
    public void clear() {
        resultsOriginal = null;
        super.clear();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateItems(Collection<? extends EntryItem> results) {
        resultsOriginal = null;
        super.updateItems(results);
    }

    public void removeItem(EntryItem result) {
        if (resultsOriginal != null)
            resultsOriginal.remove(result);
        super.removeItem(result);
    }

    public Filter getFilter() {
        if (resultsOriginal == null)
            resultsOriginal = new ArrayList<>(entryList);
        return mFilter;
    }

    private class FilterById extends Filter {

        //Invoked in a worker thread to filter the data according to the constraint.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null || constraint.length() == 0 || resultsOriginal == null)
                return null;
            String schema = constraint.toString();
            ArrayList<EntryItem> filterList = new ArrayList<>();
            for (EntryItem entryItem : resultsOriginal) {
                if (entryItem.id.startsWith(schema))
                    filterList.add(entryItem);
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filterList;
            filterResults.count = filterList.size();
            return filterResults;
        }

        //Invoked in the UI thread to publish the filtering results in the user interface.
        @SuppressWarnings("unchecked")
        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            if (filterResults != null) {
                entryList.clear();
                entryList.addAll((ArrayList<EntryItem>) filterResults.values);
                notifyDataSetChanged();
            } else if (resultsOriginal != null) {
                entryList.clear();
                entryList.addAll(resultsOriginal);
                resultsOriginal = null;
                notifyDataSetChanged();
            }
        }
    }
}
