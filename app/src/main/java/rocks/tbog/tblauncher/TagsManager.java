package rocks.tbog.tblauncher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.drawable.CodePointDrawable;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class TagsManager {

    private final ArrayList<TagInfo> mTagList = new ArrayList<>();
    private ListView mListView;
    private TagsAdapter mAdapter;

    public void applyChanges(@NonNull Context context) {
        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        DataHandler dataHandler = TBApplication.dataHandler(context);
        boolean changesMade = false;
        for (TagInfo tagInfo : mTagList) {
            switch (tagInfo.action) {
                case RENAME:
                    if (tagsHandler.renameTag(tagInfo.tagName, tagInfo.name))
                        changesMade = true;
                    break;
                case DELETE:
                    for (String entryId : tagInfo.entryList)
                        if (tagsHandler.removeTag(dataHandler.getPojo(entryId), tagInfo.tagName))
                            changesMade = true;
                    break;
            }
        }
        // make sure we're in sync
        if (changesMade) {
            afterChangesMade(context);
        }
    }

    public static void afterChangesMade(@NonNull Context context) {
        TBApplication.drawableCache(context).clearCache();
        DataHandler dataHandler = TBApplication.dataHandler(context);

        // reload tags to regenerate the tag entries
        TagsProvider tagsProvider = dataHandler.getTagsProvider();
        if (tagsProvider != null)
            tagsProvider.reload(true);

        // reload FavProvider to refresh the QuickList
        FavProvider favProvider = dataHandler.getFavProvider();
        if (favProvider != null)
            favProvider.reload(true);

        DataHandler.EXECUTOR_PROVIDERS.submit(() -> {
            TBApplication.behaviour(context).refreshSearchRecords();
            TBApplication.quickList(context).onFavoritesChanged();
        });
    }

    public void bindView(@NonNull View view) {
        final Context context = view.getContext();
        mListView = view.findViewById(android.R.id.list);

        // prepare the grid with all the tags
        mAdapter = new TagsAdapter(mTagList);

        mAdapter.setOnRemoveListener((adapter, v, position) -> {
            TagInfo info = adapter.getItem(position);
            if (info.action == TagInfo.Action.DELETE) {
                info.action = info.tagName.equals(info.name) ? TagInfo.Action.NONE : TagInfo.Action.RENAME;
            } else {
                info.action = TagInfo.Action.DELETE;
            }
            mAdapter.notifyDataSetChanged();
        });

        mAdapter.setOnRenameListener((adapter, v, position) -> {
            TagInfo info = adapter.getItem(position);
            launchRenameDialog(v.getContext(), info);
        });

        mAdapter.setOnEditIconListener((adapter, v, position) -> {
            TagInfo info = adapter.getItem(position);
            launchCustomTagIconDialog(v.getContext(), info);
        });

        mAdapter.newLoadAsyncList(() -> {
            Activity activity = Utilities.getActivity(context);
            if (activity == null)
                return null;

            TagsHandler tagsHandler = TBApplication.tagsHandler(activity);
            TagsProvider tagsProvider = TBApplication.dataHandler(activity).getTagsProvider();
            Collection<String> validTags = tagsHandler.getValidTags();
            ArrayList<TagInfo> tags = new ArrayList<>(validTags.size());
            for (String tagName : validTags) {
                TagEntry tagEntry = tagsProvider != null ? tagsProvider.getTagEntry(tagName) : null;
                TagInfo tagInfo = tagEntry != null ? new TagInfo(tagEntry) : new TagInfo(tagName);
                tagInfo.name = tagName;
                tagInfo.entryList = tagsHandler.getValidEntryIds(tagName);
                tags.add(tagInfo);
            }
            Collections.sort(tags, (lhs, rhs) -> lhs.tagName.compareTo(rhs.tagName));

            return tags;
        }).execute();
    }

    private void launchRenameDialog(Context ctx, TagInfo info) {
        DialogHelper.makeRenameDialog(ctx, info.name, (dialog, newName) -> {
            boolean isValid = true;
            for (TagInfo tagInfo : mTagList) {
                if (tagInfo == info)
                    continue;
                if (tagInfo.tagName.equals(newName) || tagInfo.name.equals(newName)) {
                    isValid = false;
                    break;
                }
            }
            if (!isValid) {
                Toast.makeText(ctx, ctx.getString(R.string.invalid_rename_tag, newName), Toast.LENGTH_LONG).show();
                return;
            }

            // Set new name
            info.name = newName;
            info.action = info.tagName.equals(info.name) ? TagInfo.Action.NONE : TagInfo.Action.RENAME;

            mAdapter.notifyDataSetChanged();
        })
                .setTitle(R.string.title_rename_tag)
                .show();
    }

    private void launchCustomTagIconDialog(Context ctx, TagInfo info) {
        TBApplication app = TBApplication.getApplication(ctx);
        DataHandler dh = app.getDataHandler();
        TagsProvider tagsProvider = dh.getTagsProvider();
        if (tagsProvider == null)
            return;
        TagEntry tagEntry = tagsProvider.getTagEntry(info.tagName);
        // add this tag to the provider before launchCustomIconDialog, in case it isn't already
        tagsProvider.addTagEntry(tagEntry);

        FavProvider favProvider = dh.getFavProvider();
        if (favProvider != null) {
            EntryItem item = favProvider.findById(tagEntry.id);
            if (item == null)
                dh.addToFavorites(tagEntry);
        }
        TBApplication.behaviour(ctx).launchCustomIconDialog(tagEntry);
    }

    public void onStart() {
        // Set list adapter after the view inflated
        // This is a workaround to fix listview items not having the correct width
        mListView.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                mListView.getViewTreeObserver().removeOnPreDrawListener(this);
                mListView.setAdapter(mAdapter);
                return false;
            }
        });
        //mListView.post(() -> mListView.setAdapter(mAdapter));
    }

    static class TagsAdapter extends ViewHolderListAdapter<TagInfo, TagViewHolder> {
        private OnItemClickListener mOnRemoveListener = null;
        private OnItemClickListener mOnRenameListener = null;
        private OnItemClickListener mOnEditIconListener = null;

        public interface OnItemClickListener {
            void onClick(TagsAdapter adapter, View view, int position);
        }

        TagsAdapter(@NonNull ArrayList<TagInfo> tags) {
            super(TagViewHolder.class, R.layout.tags_manager_item, tags);
        }

        void setOnRemoveListener(OnItemClickListener listener) {
            mOnRemoveListener = listener;
        }

        void setOnRenameListener(OnItemClickListener listener) {
            mOnRenameListener = listener;
        }

        void setOnEditIconListener(OnItemClickListener listener) {
            mOnEditIconListener = listener;
        }

        @Override
        protected int getItemViewTypeLayout(int viewType) {
            if (viewType == 1)
                return R.layout.tags_manager_item_deleted;
            return super.getItemViewTypeLayout(viewType);
        }

        public int getItemViewType(int position) {
            return getItem(position).action == TagInfo.Action.DELETE ? 1 : 0;
        }

        public int getViewTypeCount() {
            return 2;
        }
    }

    public static class TagViewHolder extends ViewHolderAdapter.ViewHolder<TagInfo> {
        ImageView iconView;
        TextView text1View;
        TextView text2View;
        View removeBtnView;
        View renameBtnView;
        View changeIconBtnView;

        public TagViewHolder(View itemView) {
            super(itemView);
            iconView = itemView.findViewById(android.R.id.icon);
            text1View = itemView.findViewById(android.R.id.text1);
            text2View = itemView.findViewById(android.R.id.text2);
            removeBtnView = itemView.findViewById(android.R.id.button1);
            renameBtnView = itemView.findViewById(android.R.id.button2);
            changeIconBtnView = itemView.findViewById(android.R.id.button3);
        }

        @Override
        protected void setContent(TagInfo content, int position, @NonNull ViewHolderAdapter<TagInfo, ? extends ViewHolderAdapter.ViewHolder<TagInfo>> adapter) {
            TagsAdapter tagsAdapter = (TagsAdapter) adapter;

            text1View.setText(content.name);
            text1View.setTypeface(null, content.action == TagInfo.Action.RENAME ? Typeface.BOLD : Typeface.NORMAL);

            removeBtnView.setOnClickListener(v -> {
                if (tagsAdapter.mOnRemoveListener != null)
                    tagsAdapter.mOnRemoveListener.onClick(tagsAdapter, v, position);
            });

            if (content.action == TagInfo.Action.DELETE) {
                text1View.setPaintFlags(text1View.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                // the rest of the views are null, exit now
                return;
            }

            int count = content.entryList.size();
            text2View.setText(text2View.getResources().getQuantityString(R.plurals.tag_entry_count, count, count));

            if (content.tagEntry != null) {
                int drawFlags = EntryItem.FLAG_DRAW_ICON | EntryItem.FLAG_DRAW_NO_CACHE;
                ResultViewHelper.setIconAsync(drawFlags, content.tagEntry, iconView, StaticEntry.AsyncSetEntryIcon.class);
            } else {
                Drawable icon = new CodePointDrawable(content.name);
                icon = DrawableUtils.applyIconMaskShape(iconView.getContext(), icon, DrawableUtils.SHAPE_SQUIRCLE, false);
                iconView.setImageDrawable(icon);
            }

            renameBtnView.setOnClickListener(v -> {
                if (tagsAdapter.mOnRenameListener != null)
                    tagsAdapter.mOnRenameListener.onClick(tagsAdapter, v, position);
            });

            changeIconBtnView.setOnClickListener(v -> {
                if (tagsAdapter.mOnEditIconListener != null)
                    tagsAdapter.mOnEditIconListener.onClick(tagsAdapter, v, position);
            });
        }
    }

    static class TagInfo {
        final TagEntry tagEntry;
        final String tagName;
        String name;

        List<String> entryList;
        Action action = Action.NONE;

        enum Action {NONE, DELETE, RENAME}

        public TagInfo(String name) {
            tagEntry = null;
            tagName = name;
        }

        public TagInfo(TagEntry entry) {
            tagEntry = entry;
            tagName = entry.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TagInfo tagInfo = (TagInfo) o;
            return Objects.equals(tagEntry, tagInfo.tagEntry) &&
                    Objects.equals(tagName, tagInfo.tagName) &&
                    Objects.equals(name, tagInfo.name) &&
                    action == tagInfo.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(tagEntry, tagName, name, action);
        }
    }
}
