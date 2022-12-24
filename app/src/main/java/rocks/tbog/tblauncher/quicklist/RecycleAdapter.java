package rocks.tbog.tblauncher.quicklist;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.result.RecycleAdapterBase;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.utils.UIColors;

public class RecycleAdapter extends RecycleAdapterBase<RecycleAdapter.Holder> {

    public RecycleAdapter(@NonNull Context context, @NonNull ArrayList<EntryItem> results) {
        super(results);
        setHasStableIds(true);
        setGridLayout(context, false);
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
        super.onBindViewHolder(holder, position);

        Context context = holder.itemView.getContext();
        final int color;
        final EntryItem entry = getItem(position);
        if (entry instanceof FilterEntry)
            color = UIColors.getQuickListToggleColor(context);
        else
            color = UIColors.getQuickListRipple(context);
        Drawable selector = CustomizeUI.getSelectorDrawable(holder.itemView, color, true);
        holder.itemView.setBackground(selector);
    }

    public void moveResult(int sourceIdx, int destIdx) {
        notifyItemMoved(sourceIdx, destIdx);
        EntryItem entryItem = entryList.remove(sourceIdx);
        entryList.add(destIdx, entryItem);
    }
}
