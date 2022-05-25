package rocks.tbog.tblauncher.quicklist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collection;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.utils.UIColors;

public class RecycleAdapter extends RecyclerView.Adapter<RecycleAdapter.Holder> {

    private static final String TAG = "DockAdapter";
    /**
     * Array list containing all the results currently displayed
     */
    @NonNull
    private final ArrayList<EntryItem> results;
    @Nullable
    private OnClickListener mOnClickListener = null;
    private OnLongClickListener mOnLongClickListener = null;

    private int mDrawFlags;

    public RecycleAdapter(@NonNull Context context, @NonNull ArrayList<EntryItem> results) {
        this.results = results;
        setHasStableIds(true);
        setGridLayout(context, false);
    }

    public void setOnClickListener(@Nullable OnClickListener listener) {
        mOnClickListener = listener;
    }

    public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
        mOnLongClickListener = listener;
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

        ViewGroup.LayoutParams layoutParams = itemView.getLayoutParams();
        layoutParams.width = parent.getWidth() / 6;
        itemView.setLayoutParams(layoutParams);

        return new Holder(itemView);
    }

    public void setGridLayout(@NonNull Context context, boolean bGridLayout) {
        final int oldFlags = mDrawFlags;
        // get new flags
        mDrawFlags = getDrawFlags(context);
        mDrawFlags |= bGridLayout ? EntryItem.FLAG_DRAW_GRID : EntryItem.FLAG_DRAW_QUICK_LIST;
        // refresh items if flags changed
        if (oldFlags != mDrawFlags)
            refresh();
    }

    public static int getDrawFlags(@NonNull Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int drawFlags = EntryItem.FLAG_DRAW_NO_CACHE | EntryItem.FLAG_RELOAD;
        if (prefs.getBoolean("quick-list-text-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_NAME;
        if (prefs.getBoolean("quick-list-icons-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON;
        if (prefs.getBoolean("quick-list-show-badge", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON_BADGE;
        if (UIColors.isColorLight(UIColors.getColor(prefs, "quick-list-argb"))) {
            drawFlags |= EntryItem.FLAG_DRAW_WHITE_BG;
        }
        return drawFlags;
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        final EntryItem entry;
        try {
            entry = results.get(position);
        } catch (ArrayIndexOutOfBoundsException e) {
            Log.e(TAG, "pos=" + position + " size=" + results.size(), e);
            return;
        }

        if (mOnClickListener != null)
            holder.setOnClickListener(v -> mOnClickListener.onClick(entry, v));
        else
            holder.setOnClickListener(null);

        if (mOnLongClickListener != null)
            holder.setOnLongClickListener(v -> mOnLongClickListener.onLongClick(entry, v));
        else
            holder.setOnLongClickListener(null);

        results.get(position).displayResult(holder.itemView, mDrawFlags);

        Context context = holder.itemView.getContext();
        final int color;
        if (entry instanceof FilterEntry)
            color = UIColors.getQuickListToggleColor(context);
        else
            color = UIColors.getQuickListRipple(context);
        Drawable selector = CustomizeUI.getSelectorDrawable(holder.itemView, color, true);
        holder.itemView.setBackground(selector);
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    public EntryItem getItem(int index) {
        return results.get(index);
    }

    public void clear() {
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
        this.results.clear();
        this.results.addAll(results);
        notifyDataSetChanged();
    }

    public void removeResult(EntryItem result) {
        int position = results.indexOf(result);
        results.remove(result);
        notifyItemRemoved(position);
    }

    public void moveResult(int sourceIdx, int destIdx) {
        notifyItemMoved(sourceIdx, destIdx);
        EntryItem entryItem = results.remove(sourceIdx);
        results.add(destIdx, entryItem);
    }

    public void notifyItemChanged(EntryItem result) {
        int position = results.indexOf(result);
        Log.d(TAG, "notifyItemChanged #" + position + " id=" + result.id);
        if (position >= 0)
            notifyItemChanged(position);
    }

    public void addResult(EntryItem item) {
        notifyItemInserted(results.size());
        results.add(item);
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
