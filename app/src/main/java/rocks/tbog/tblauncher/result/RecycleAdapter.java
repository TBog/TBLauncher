package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.ui.ListPopup;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.Holder> {

    /**
     * Array list containing all the results currently displayed
     */
    @NonNull
    private final ArrayList<EntryItem> results;
    @Nullable
    private ArrayList<EntryItem> resultsOriginal = null;

    private Filter mFilter = new RecycleAdapter.FilterById();

    public RecycleAdapter(@NonNull ArrayList<EntryItem> results) {
        this.results = results;
        setHasStableIds(true);
    }

    @Override
    public int getItemViewType(int position) {
        //TODO: cache a Pair<itemType, layoutRes> to be used in `onCreateViewHolder`
        return ResultHelper.getItemViewType(results.get(position));
    }

    @Override
    public long getItemId(int position) {
        return position < results.size() ? results.get(position).id.hashCode() : -1;
    }

    @NonNull
    @NotNull
    @Override
    public Holder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int drawFlags = EntryItem.FLAG_DRAW_NAME | EntryItem.FLAG_DRAW_LIST;
        if (prefs.getBoolean("tags-enabled", true))
            drawFlags |= EntryItem.FLAG_DRAW_TAGS;
        if (prefs.getBoolean("icons-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON;
        if (prefs.getBoolean("shortcut-show-badge", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON_BADGE;

        // TODO: use a cache for the layout res
        @LayoutRes
        int layoutRes = 0;
        for (EntryItem result : results) {
            if (ResultHelper.getItemViewType(result) == viewType) {
                layoutRes = result.getResultLayout(drawFlags);
                break;
            }
        }

        LayoutInflater inflater = LayoutInflater.from(context);
        return new Holder(inflater.inflate(layoutRes, parent, false), drawFlags);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull Holder holder, int position) {
        holder.setOnClickListener(view -> onClick(position, view));
        holder.setOnLongClickListener(view -> onLongClick(position, view));

        results.get(position).displayResult(holder.itemView, holder.drawFlags);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public void onClick(final int position, View v) {
        final EntryItem result;

        try {
            result = results.get(position);
            ResultHelper.launch(v, result);
        } catch (ArrayIndexOutOfBoundsException e) {
//            return;
        }

        // Record the launch after some period,
        // * to ensure the animation runs smoothly
        // * to avoid a flickering -- launchOccurred will refresh the list
        // Thus TOUCH_DELAY * 3
//        Handler handler = new Handler();
//        handler.postDelayed(() -> TBApplication.behaviour(v.getContext()).onLaunchOccurred(), TBApplication.TOUCH_DELAY * 3);
    }

    public boolean onLongClick(final int pos, View v) {
        ListPopup menu;
        try {
            menu = results.get(pos).getPopupMenu(v);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return false;
        }

        // check if menu contains elements and if yes show it
        if (!menu.getAdapter().isEmpty()) {
            TBApplication.behaviour(v.getContext()).registerPopup(menu);
            menu.show(v);
            return true;
        }

        return false;
    }

    public int getCount() {
        return getItemCount();
    }

    public void clear() {
        results.clear();
        notifyDataSetChanged();
    }

    public void updateResults(Collection<? extends EntryItem> results) {
        resultsOriginal = null;
        this.results.clear();
        this.results.addAll(results);
        notifyDataSetChanged();
    }

    public void removeResult(EntryItem result) {
        results.remove(result);
        if (resultsOriginal != null)
            resultsOriginal.remove(result);
        notifyDataSetChanged();
    }

    public Filter getFilter() {
        if (resultsOriginal == null)
            resultsOriginal = new ArrayList<>(results);
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

    public static class Holder extends RecyclerView.ViewHolder {
        int drawFlags;

        public Holder(@NonNull @NotNull View itemView, int drawFlags) {
            super(itemView);
            itemView.setTag(this);
            this.drawFlags = drawFlags;
        }

        public void setOnClickListener(@Nullable View.OnClickListener listener) {
            itemView.setOnClickListener(listener);
        }

        public void setOnLongClickListener(@Nullable View.OnLongClickListener listener) {
            itemView.setOnLongClickListener(listener);
        }
    }
}
