package rocks.tbog.tblauncher.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UIColors;

/**
 * Adapter used to inflate views in a LinearLayout
 * WARNING: don't use this adapter for long lists, it does not recycle views
 */
public class LinearAdapter extends BaseAdapter {
    final ArrayList<MenuItem> list = new ArrayList<>(0);

    public interface MenuItem {
        @LayoutRes
        int getLayoutResource();

        boolean isEnabled();
    }

    public static class ItemDivider implements MenuItem {
        @Override
        public int getLayoutResource() {
            return R.layout.popup_divider;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    public static class ItemTitle implements MenuItem {
        @NonNull
        final String name;

        public ItemTitle(Context context, @StringRes int nameRes) {
            this.name = context.getString(nameRes);
        }

        public ItemTitle(@NonNull String string) {
            this.name = string;
        }

        @NonNull
        @Override
        public String toString() {
            return name;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_title;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    public static class ItemString implements MenuItem {
        @NonNull
        final String string;

        public ItemString(@NonNull String string) {
            this.string = string;
        }

        @NonNull
        @Override
        public String toString() {
            return string;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }
    }

    public static class ItemText extends ItemString {
        public ItemText(@NonNull String string) {
            super(string);
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_text;
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    public static class Item extends ItemString {
        @StringRes
        public final int stringId;

        public Item(Context context, @StringRes int stringId) {
            super(context.getResources().getString(stringId));
            this.stringId = stringId;
        }

//        public Item(@NonNull String string) {
//            super(string);
//            this.stringId = 0;
//        }
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public MenuItem getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @SuppressLint("ViewHolder")
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        MenuItem item = getItem(position);
        convertView = LayoutInflater.from(parent.getContext()).inflate(item.getLayoutResource(), parent, false);

        // set color of the divider
        View divider = convertView.findViewById(R.id.title_divider);
        if (divider != null) {
            Context ctx = divider.getContext();
            int color = UIColors.getPopupBorderColor(ctx);
            int background = UIColors.getPopupBackgroundColor(ctx);
            int separator = UIColors.isColorLight(background) ? R.drawable.list_separator_dark : R.drawable.list_separator_light;
            Drawable drawable = AppCompatResources.getDrawable(ctx, separator);
            if (drawable == null)
                drawable = divider.getBackground();
            if (drawable != null) {
                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.MULTIPLY));
                divider.setBackground(drawable);
            }
        }
        if (item instanceof ItemDivider) {
            return convertView;
        }

        bindView(convertView, item);

        return convertView;
    }

    protected void bindView(View convertView, MenuItem item) {
        String text = item.toString();
        TextView textView = convertView.findViewById(android.R.id.text1);
        textView.setText(text);
    }

    public void add(MenuItem item) {
        list.add(item);
        notifyDataSetChanged();
    }

    public void add(int index, MenuItem item) {
        list.add(index, item);
        notifyDataSetChanged();
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        MenuItem item = list.get(position);
        return item.isEnabled();
    }

    public void remove(MenuItem item) {
        list.remove(item);
        notifyDataSetChanged();
    }
}
