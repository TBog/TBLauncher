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
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
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
            MenuTagAdapter.MenuItem menuItem = tagEntry != null ? new MenuTagAdapter.MenuItem(tagEntry) : new MenuTagAdapter.MenuItem(tagName);
            adapter.addItem(menuItem);
        }
        EntryItem untaggedEntry;
        boolean bAddUntagged = PrefCache.showTagsMenuUntagged(ctx);
        if (bAddUntagged) {
            untaggedEntry = TBApplication.dataHandler(ctx).getPojo(ActionEntry.SCHEME + "show/untagged");
            if (untaggedEntry instanceof ActionEntry) {
                int idx = PrefCache.getTagsMenuUntaggedIndex(ctx);
                if (idx > adapter.getCount())
                    idx = adapter.getCount();
                adapter.addItem(idx, new MenuTagAdapter.MenuItem((ActionEntry) untaggedEntry));
            }
        }
        return ListPopup.create(ctx, adapter)
            .setOnItemClickListener((a, v, pos) -> {
                MenuTagAdapter.MenuItem item = (MenuTagAdapter.MenuItem) a.getItem(pos);
                if (item == null)
                    return;
                if (item.staticEntry != null) {
                    item.staticEntry.doLaunch(v, LAUNCHED_FROM_GESTURE);
                    return;
                }
                TBApplication.quickList(ctx).toggleSearch(v, item.toString(), TagSearcher.class);
            });
    }

    private static class MenuTagAdapter extends BaseAdapter {

        private final ArrayList<MenuItem> mList = new ArrayList<>();

        private static class MenuItem {
            final String text;
            final private StaticEntry staticEntry;

            public MenuItem(@NonNull String tagName) {
                text = tagName;
                staticEntry = null;
            }

            public MenuItem(@NonNull StaticEntry entry) {
                text = entry.getName();
                staticEntry = entry;
            }

            @NonNull
            @Override
            public String toString() {
                return text;
            }

            public void setIcon(TextView textView) {
                if (staticEntry == null) {
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
                    staticEntry::getIconDrawable,
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

        public void addItem(MenuItem item) {
            mList.add(item);
            notifyDataSetChanged();
        }

        public void addItem(int index, MenuItem item) {
            mList.add(index, item);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public MenuItem getItem(int position) {
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

            final MenuItem item = getItem(position);
            if (view instanceof TextView) {
                TextView textView = (TextView) view;

                textView.setText(item.toString());
                item.setIcon(textView);
            }


            return view;
        }

        @Override
        public int getItemViewType(int position) {
            return R.layout.popup_list_item;
        }
    }
}
