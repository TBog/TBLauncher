package rocks.tbog.tblauncher.ui;

import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_GESTURE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.searcher.TagSearcher;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagsMenuUtils {
    public static ListPopup createTagsMenu(Context ctx, Iterable<String> tagNames) {
        TagsProvider tagsProvider = TBApplication.dataHandler(ctx).getTagsProvider();
        MenuTagAdapter adapter = new MenuTagAdapter();
        for (String tagName : tagNames) {
            TagEntry tagEntry = tagsProvider != null ? tagsProvider.getTagEntry(tagName) : null;
            MenuTagAdapter.Tag menuItem = tagEntry != null ? new MenuTagAdapter.Tag(tagEntry) : new MenuTagAdapter.Tag(tagName);
            adapter.addItem(menuItem);
        }
        if (PrefCache.showTagsMenuUntagged(ctx)) {
            adapter.addItem(new MenuTagAdapter.Divider());
            adapter.addItem(new MenuTagAdapter.Untagged(ctx.getString(R.string.action_show_untagged)));
        }
        return ListPopup.create(ctx, adapter)
                .setOnItemClickListener((a, v, pos) -> {
                    MenuTagAdapter.Item item = (MenuTagAdapter.Item) a.getItem(pos);
                    if (item instanceof MenuTagAdapter.Tag) {
                        TagEntry tagEntry = ((MenuTagAdapter.Tag) item).tagEntry;
                        if (tagEntry != null) {
                            tagEntry.doLaunch(v, LAUNCHED_FROM_GESTURE);
                            return;
                        }
                    } else if (item instanceof MenuTagAdapter.Untagged) {
                        TBApplication.behaviour(ctx).showUntagged();
                        return;
                    }
                    TBApplication.quickList(ctx).toggleSearch(v, item.toString(), TagSearcher.class);
                });
    }

    private static class MenuTagAdapter extends BaseAdapter {

        private final ArrayList<Item> mList = new ArrayList<>();

        private static class Item {
            final String text;

            public Item(String text) {
                this.text = text;
            }

            public boolean isEnabled() {
                return true;
            }

            @NonNull
            @Override
            public String toString() {
                return text;
            }
        }

        private static class Divider extends Item {

            public Divider() {
                super(null);
            }

            public boolean isEnabled() {
                return false;
            }

            @NonNull
            @Override
            public String toString() {
                return "-";
            }
        }

        private static class Untagged extends Item {

            public Untagged(String text) {
                super(text);
            }
        }

        private static class Tag extends Item {
            final TagEntry tagEntry;

            public Tag(@NonNull String tagName) {
                super(tagName);
                tagEntry = null;
            }

            public Tag(@NonNull TagEntry entry) {
                super(entry.getName());
                tagEntry = entry;
            }

            public void setIcon(TextView textView) {
                if (tagEntry == null) {
                    // this is not likely to happen
                    return;
                }
                Context ctx = textView.getContext();
                if (!PrefCache.showTagsMenuIcons(ctx))
                    return;
                // make sure we have enough space to inline the drawable
                final int size = UISizes.getTagsMenuIconSize(ctx);
                Drawable loadingIcon = PrefCache.getLoadingIconDrawable(ctx);
                loadingIcon.setBounds(0, 0, size, size);
                textView.setCompoundDrawables(loadingIcon, null, null, null);
                textView.setCompoundDrawablePadding(UISizes.sp2px(ctx, 2));
                Utilities.startAnimatable(textView);

                // async load and show the icon
                Utilities.setViewAsync(textView,
                        tagEntry::getIconDrawable,
                        (view, drawable) -> {
                            if (view instanceof TextView) {
                                drawable.setBounds(0, 0, size, size);
                                TextView v = (TextView) view;
                                v.setCompoundDrawables(drawable, null, null, null);
                            }
                        });
            }
        }

        public MenuTagAdapter() {
            super();
        }

        public void addItem(Item item) {
            mList.add(item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public Item getItem(int position) {
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

        @SuppressLint("ViewHolder")
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            view = LayoutInflater.from(parent.getContext()).inflate(getItemViewType(position), parent, false);

            final Item item = getItem(position);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;

                textView.setText(item.toString());
                if (item instanceof Tag)
                    ((Tag) item).setIcon(textView);
            }


            return view;
        }

        @Override
        public int getItemViewType(int position) {
            if (getItem(position) instanceof Divider)
                return R.layout.popup_divider;
            return android.R.layout.simple_list_item_1;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position).isEnabled();
        }
    }
}
