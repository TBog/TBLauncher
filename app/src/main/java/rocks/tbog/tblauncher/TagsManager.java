package rocks.tbog.tblauncher;

import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.ui.CodePointDrawable;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;

public class TagsManager {

    private final ArrayList<TagInfo> mTagList = new ArrayList<>();
    private ListView mListView;
    private TagsAdapter mAdapter;

    public void applyChanges(@NonNull Context context) {
        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        boolean changesMade = false;
        for (TagInfo tagInfo : mTagList) {
            switch (tagInfo.action) {
                case RENAME:
                    if (tagsHandler.renameTag(tagInfo.tagName, tagInfo.name))
                        changesMade = true;
                    break;
                case DELETE:
                    DataHandler dataHandler = TBApplication.dataHandler(context);
                    for (String entryId : tagInfo.entryList)
                        if (tagsHandler.removeTag(dataHandler.getPojo(entryId), tagInfo.tagName))
                            changesMade = true;
                    break;
            }
        }
//        // make sure we're in sync
//        if (changesMade)
//            tagsHandler.loadFromDB();
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

        // refresh tags
        {
            TagsHandler tagsHandler = TBApplication.tagsHandler(context);
            for (String tagName : tagsHandler.getAllTagsAsSet()) {
                TagInfo tagInfo = new TagInfo(tagName);
                tagInfo.name = tagName;
                tagInfo.entryList = tagsHandler.getIds(tagName);
                mTagList.add(tagInfo);
            }
            Collections.sort(mTagList, (lhs, rhs) -> lhs.tagName.compareTo(rhs.tagName));
        }
    }

    private void launchRenameDialog(Context ctx, TagInfo info) {
        ContextThemeWrapper context = new ContextThemeWrapper(ctx, R.style.NoTitleDialogTheme);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(context.getResources().getString(R.string.title_rename_tag));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setView(R.layout.dialog_rename);
        } else {
            builder.setView(View.inflate(context, R.layout.dialog_rename, null));
        }

        builder.setPositiveButton(R.string.custom_name_rename, (dialog, which) -> {
            EditText input = ((AlertDialog) dialog).findViewById(R.id.rename);
            if (input == null)
                return;
            String newName = input.getText().toString().trim();
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

            dialog.dismiss();
        });
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            dialog.cancel();
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        // call after dialog got inflated (show call)
        TextView nameView = dialog.findViewById(R.id.rename);
        nameView.setText(info.name);
        nameView.requestFocus();
    }

    private void launchCustomTagIconDialog(Context ctx, TagInfo info) {
        TagsProvider tagsProvider = TBApplication.dataHandler(ctx).getTagsProvider();
        if (tagsProvider == null)
            return;
        TagEntry tagEntry = tagsProvider.getTagEntry(info.tagName);
        // add this tag to the provider before launchCustomIconDialog, in case it isn't already
        tagsProvider.addTagEntry(tagEntry);
        TBApplication.behaviour(ctx).launchCustomIconDialog(tagEntry);
    }

    public void onStart() {
        // Set list adapter after the view inflated
        // This is a workaround to fix listview items not having the correct width
        mListView.post(() -> mListView.setAdapter(mAdapter));
    }

    static class TagsAdapter extends ViewHolderAdapter<TagInfo, TagViewHolder> {
        private final ArrayList<TagInfo> mTags;
        private OnItemClickListener mOnRemoveListener = null;
        private OnItemClickListener mOnRenameListener = null;
        private OnItemClickListener mOnEditIconListener = null;

        public interface OnItemClickListener {
            void onClick(TagsAdapter adapter, View view, int position);
        }

        TagsAdapter(@NonNull ArrayList<TagInfo> tags) {
            super(TagViewHolder.class, R.layout.tags_manager_item);
            mTags = tags;
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
        protected int getListItemLayout(int position) {
            if (getItemViewType(position) == 1)
                return R.layout.tags_manager_item_deleted;
            return super.getListItemLayout(position);
        }

        @Override
        public int getCount() {
            return mTags.size();
        }

        @Override
        public TagInfo getItem(int position) {
            return mTags.get(position);
        }

        @Override
        public long getItemId(int position) {
            return getItem(position).hashCode();
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

            Drawable icon = new CodePointDrawable(content.name);
            icon = DrawableUtils.applyIconMaskShape(iconView.getContext(), icon, DrawableUtils.SHAPE_SQUIRCLE, false);
            iconView.setImageDrawable(icon);

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
        final String tagName;
        String name;
        List<String> entryList;
        Action action = Action.NONE;

        enum Action {NONE, DELETE, RENAME}

        public TagInfo(String tagName) {
            this.tagName = tagName;
        }
    }
}
