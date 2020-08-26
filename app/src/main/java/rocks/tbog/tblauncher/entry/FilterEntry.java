package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.Utilities;

public class FilterEntry extends EntryItem {
    public static final String SCHEME = "filter://";
    private View.OnClickListener listener = null;
    @DrawableRes
    private int iconResource;
    private final String filterScheme;

    public FilterEntry(@NonNull String id, @DrawableRes int icon, String filterScheme) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + FilterEntry.class.getSimpleName() + " id `" + id + "`");
        }
        iconResource = icon;
        this.filterScheme = filterScheme;
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? R.layout.item_filter :
                (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? R.layout.item_grid :
                        R.layout.item_quick_list);
    }

    @WorkerThread
    private Drawable getIconDrawable(Context context) {
        return ContextCompat.getDrawable(context, iconResource);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        TextView nameView = view.findViewById(android.R.id.text1);
        nameView.setTextColor(UIColors.getResultTextColor(view.getContext()));
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_NAME))
            nameView.setText(getName());
        else
            nameView.setVisibility(View.GONE);

        view.setTag(R.id.tag_filterName, filterScheme);

        ImageView appIcon = view.findViewById(android.R.id.icon);
        if (Utilities.checkFlag(drawFlags, FLAG_DRAW_ICON)) {
            appIcon.setVisibility(View.VISIBLE);
            ResultViewHelper.setIconAsync(drawFlags, this, appIcon, AsyncSetEntryIcon.class);
        } else {
            appIcon.setImageDrawable(null);
            appIcon.setVisibility(View.GONE);
        }
    }

    @Override
    public void doLaunch(@NonNull View view) {
        listener.onClick(view);
    }

    public void setOnClickListener(@Nullable View.OnClickListener listener) {
        this.listener = listener;
    }

    public static class AsyncSetEntryIcon extends ResultViewHelper.AsyncSetEntryDrawable {
        public AsyncSetEntryIcon(@NonNull ImageView image, int drawFlags, @NonNull EntryItem entryItem) {
            super(image, drawFlags, entryItem);
        }

        @Override
        public Drawable getDrawable(Context context) {
            FilterEntry filterEntry = (FilterEntry) entryItem;
            return filterEntry.getIconDrawable(context);
        }
    }
}
