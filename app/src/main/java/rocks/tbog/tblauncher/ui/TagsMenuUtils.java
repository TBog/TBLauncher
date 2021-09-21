package rocks.tbog.tblauncher.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
        MenuTagAdapter adapter = new MenuTagAdapter(ctx, android.R.layout.simple_list_item_1);
        for (String tagName : tagNames) {
            TagEntry tagEntry = tagsProvider != null ? tagsProvider.getTagEntry(tagName) : null;
            MenuTagItem menuItem = tagEntry != null ? new MenuTagItem(tagEntry) : new MenuTagItem(tagName);
            adapter.add(menuItem);
        }
        return ListPopup.create(ctx, adapter)
                .setOnItemClickListener((a, v, pos) -> {
                    ListPopup.Item item = (ListPopup.Item) a.getItem(pos);
                    TBApplication.quickList(ctx).toggleSearch(v, item.toString(), TagSearcher.class);
                });
    }

    private static class MenuTagAdapter extends ArrayAdapter<ListPopup.Item> {
        public MenuTagAdapter(@NonNull Context context, int resource) {
            super(context, resource);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);
            ListPopup.Item item = getItem(position);
            if (item instanceof MenuTagItem && view instanceof TextView)
                ((MenuTagItem) item).setIcon((TextView) view);
            return view;
        }
    }

    private static class MenuTagItem extends ListPopup.Item {
        final TagEntry tagEntry;

        public MenuTagItem(@NonNull String tagName) {
            super(tagName);
            tagEntry = null;
        }

        public MenuTagItem(@NonNull TagEntry entry) {
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
}
