package rocks.tbog.tblauncher.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.Holder> {

    private static final String TAG = RecycleAdapter.class.getSimpleName();
    /**
     * Array list containing all the results currently displayed
     */
    @NonNull
    private final ArrayList<EntryItem> results;
    @Nullable
    private ArrayList<EntryItem> resultsOriginal = null;

    private int mDrawFlags;

    private Filter mFilter = new RecycleAdapter.FilterById();

    public RecycleAdapter(@NonNull Context context, @NonNull ArrayList<EntryItem> results) {
        this.results = results;
        setHasStableIds(true);
        setGridLayout(context, false);
    }

    @Override
    public int getItemViewType(int position) {
        return ResultHelper.getItemViewType(results.get(position), mDrawFlags);
    }

    @Override
    public long getItemId(int position) {
        return position < results.size() ? results.get(position).id.hashCode() : -1;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final Context context = parent.getContext();
        final int layoutRes = ResultHelper.getItemViewLayout(viewType);

        LayoutInflater inflater = LayoutInflater.from(context);
        View itemView = inflater.inflate(layoutRes, parent, false);

        return new Holder(itemView);
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

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final EntryItem result;
        try {
            result = results.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "pos=" + position + " size=" + results.size(), e);
            return;
        }

        holder.setOnClickListener(view -> onClick(result, view));
        holder.setOnLongClickListener(view -> onLongClick(result, view));

        results.get(position).displayResult(holder.itemView, mDrawFlags);

        // result list item height
        if ((mDrawFlags & EntryItem.FLAG_DRAW_LIST) == EntryItem.FLAG_DRAW_LIST) {
            Context ctx = holder.itemView.getContext();
            int rowHeight = UISizes.getResultListRowHeight(ctx);
            ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
            if (params.height != rowHeight) {
                params.height = rowHeight;
                holder.itemView.setLayoutParams(params);
            }
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public EntryItem getItem(int index) {
        return results.get(index);
    }

    public void onClick(int index, View anyView) {
        final EntryItem result;
        try {
            result = results.get(index);
        } catch (IndexOutOfBoundsException e) {
            Log.e(TAG, "pos=" + index + " size=" + results.size(), e);
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

    public void clear() {
        resultsOriginal = null;
        final int itemCount = results.size();
        results.clear();
        notifyItemRangeRemoved(0, itemCount);
    }

    public void refresh() {
        final int itemCount = results.size();
        notifyItemRangeChanged(0, itemCount);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateResults(Collection<? extends EntryItem> results) {
        resultsOriginal = null;
        this.results.clear();
        this.results.addAll(results);
        notifyDataSetChanged();
    }

    public void removeResult(EntryItem result) {
        int position = results.indexOf(result);
        results.remove(result);
        if (resultsOriginal != null)
            resultsOriginal.remove(result);
        notifyItemRemoved(position);
    }

    public void notifyItemChanged(EntryItem result) {
        int position = results.indexOf(result);
        Log.d(TAG, "notifyItemChanged #" + position + " id=" + result.id);
        if (position >= 0)
            notifyItemChanged(position);
    }

    public Filter getFilter() {
        if (resultsOriginal == null)
            resultsOriginal = new ArrayList<>(results);
        return mFilter;
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
        }

        public void setOnLongClickListener(@Nullable View.OnLongClickListener listener) {
            itemView.setOnLongClickListener(listener);
        }
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
                results.clear();
                results.addAll((ArrayList<EntryItem>) filterResults.values);
                notifyDataSetChanged();
            } else if (resultsOriginal != null) {
                results.clear();
                results.addAll(resultsOriginal);
                resultsOriginal = null;
                notifyDataSetChanged();
            }
        }
    }
}
