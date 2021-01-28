package rocks.tbog.tblauncher.preference;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.GridView;

import androidx.preference.PreferenceViewHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.result.EntryAdapter;
import rocks.tbog.tblauncher.result.LoadDataForAdapter;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public class PreviewImagePreference extends androidx.preference.DialogPreference {

    public PreviewImagePreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public PreviewImagePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public PreviewImagePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PreviewImagePreference(Context context) {
        super(context);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        View view = holder.findViewById(R.id.grid);
        if (!(view instanceof GridView))
            return;
        GridView gridView = (GridView) view;
        // disable touch
        gridView.setOnTouchListener((v, event) -> true);
        Utilities.setVerticalScrollbarThumbDrawable(gridView, null);
        Context ctx = gridView.getContext();

        int background = UIColors.getResultListBackground(getSharedPreferences());
        gridView.setBackgroundColor(background);

        ArrayList<EntryItem> list = new ArrayList<>();
        int drawFlags = EntryItem.FLAG_DRAW_GRID | EntryItem.FLAG_DRAW_ICON | EntryItem.FLAG_DRAW_ICON_BADGE | EntryItem.FLAG_DRAW_NO_CACHE;
        EntryAdapter adapter = new EntryAdapter(list, drawFlags);
        gridView.setAdapter(adapter);
        new LoadDataForAdapter(adapter, () -> {
            Activity activity = Utilities.getActivity(ctx);
            if (activity == null)
                return null;
            DataHandler dataHandler = TBApplication.dataHandler(activity);
            List<EntryItem> data = dataHandler.getHistory(9, DBHelper.HistoryMode.RECENCY, false, Collections.emptySet());
            if (data instanceof ArrayList)
                return (ArrayList<EntryItem>) data;
            return new ArrayList<>(data);
        }).execute();

    }
}
