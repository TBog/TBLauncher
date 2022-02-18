package rocks.tbog.tblauncher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import rocks.tbog.tblauncher.WorkAsync.RunnableTask;
import rocks.tbog.tblauncher.WorkAsync.TaskRunner;
import rocks.tbog.tblauncher.customicon.IconSelectDialog;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.drawable.CodePointDrawable;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.handler.TagsHandler;
import rocks.tbog.tblauncher.result.ResultViewHelper;
import rocks.tbog.tblauncher.utils.DialogHelper;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class TagsManager {
    private static final String TAG = "TagMgr";

    private final ArrayList<TagInfo> mTagList = new ArrayList<>();
    private ListView mListView;
    private TagsAdapter mAdapter;

    public interface OnItemClickListener {
        void onItemClickListener(@NonNull View view, @NonNull TagInfo tagInfo);
    }

    public void applyChanges(@NonNull Context context) {
        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        IconsHandler iconsHandler = TBApplication.iconsHandler(context);
        TBLauncherActivity launcherActivity = TBApplication.launcherActivity(context);
        boolean changesMade = false;
        for (TagInfo tagInfo : mTagList) {
            if (tagInfo.staticEntry instanceof ActionEntry) {
                // can't delete actions (it's the show untagged action)
                if (tagInfo.action == TagInfo.Action.RENAME) {
                    TBApplication.dataHandler(context).renameStaticEntry(tagInfo.staticEntry, tagInfo.name);
                    changesMade = true;
                }
            } else {
                switch (tagInfo.action) {
                    case RENAME:
                        if (tagsHandler.renameTag(tagInfo.tagName, tagInfo.name))
                            changesMade = true;
                        break;
                    case DELETE:
                        if (tagInfo.staticEntry != null)
                            iconsHandler.restoreDefaultIcon(tagInfo.staticEntry);
                        tagInfo.icon = null;
                        if (tagsHandler.removeTag(tagInfo.tagName))
                            changesMade = true;
                        break;
                }
            }
            if (tagInfo.icon != null && tagInfo.staticEntry != null) {
                iconsHandler.changeIcon(tagInfo.staticEntry, tagInfo.icon);
                if (launcherActivity != null) {
                    // force a result refresh to update the icon in the view
                    launcherActivity.behaviour.refreshSearchRecord(tagInfo.staticEntry);
                }
            }
        }
        // make sure we're in sync
        if (changesMade) {
            if (launcherActivity != null)
                launcherActivity.queueDockReload();
            afterChangesMade(context);
        }
    }

    public static void afterChangesMade(@NonNull Context context) {
        TBApplication.drawableCache(context).clearCache();
        DataHandler dataHandler = TBApplication.dataHandler(context);

        dataHandler.reloadProviders(IProvider.LOAD_STEP_2);

        RunnableTask afterProviders = TaskRunner.newTask(task -> {
                TBApplication app = TBApplication.getApplication(context);
                AppsHandler.setTagsForApps(app.appsHandler().getAllApps(), app.tagsHandler());
            },
            task -> {
                Log.d(TAG, "tags and fav providers should have loaded by now");
                TBLauncherActivity activity = TBApplication.launcherActivity(context);
                if (activity != null) {
                    activity.refreshSearchRecords();
                    activity.queueDockReload();
                }
            });
        DataHandler.EXECUTOR_PROVIDERS.execute(afterProviders);
    }

    public void bindView(@NonNull View view, @Nullable OnItemClickListener listener) {
        final Context context = view.getContext();
        mListView = view.findViewById(android.R.id.list);

        if (listener != null) {
            mListView.setOnItemClickListener((parent, v, pos, id) -> {
                Object objItem = parent.getAdapter().getItem(pos);
                if (objItem instanceof TagInfo) {
                    listener.onItemClickListener(v, (TagInfo) objItem);
                }
            });
        }

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
            ArrayList<TagInfo> tags = new ArrayList<>(validTags.size() + 1);
            for (String tagName : validTags) {
                TagEntry tagEntry = tagsProvider != null ? tagsProvider.getTagEntry(tagName) : null;
                TagInfo tagInfo = tagEntry != null ? new TagInfo(tagEntry) : new TagInfo(tagName);
                tagInfo.setInfo(tagName, tagsHandler.getValidEntryIds(tagName).size());
                tags.add(tagInfo);
            }
            Collections.sort(tags, (lhs, rhs) -> lhs.tagName.compareTo(rhs.tagName));

            EntryItem untaggedEntry = TBApplication.dataHandler(context).getPojo(ActionEntry.SCHEME + "show/untagged");
            if (untaggedEntry instanceof ActionEntry) {
                TagInfo tagInfo = new TagInfo((ActionEntry) untaggedEntry);
                tagInfo.setInfo(untaggedEntry.getName(), -1);
                tags.add(0, tagInfo);
            }

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
            .setHint(R.string.hint_rename_tag)
            .show();
    }

    private void launchCustomTagIconDialog(Context ctx, TagInfo info) {
        DataHandler dh = TBApplication.dataHandler(ctx);
        final StaticEntry staticEntry;
        if (info.staticEntry != null) {
            staticEntry = info.staticEntry;
        } else {
            TagsProvider tagsProvider = dh.getTagsProvider();
            if (tagsProvider == null)
                return;
            TagEntry tagEntry = tagsProvider.getTagEntry(info.tagName);
            // add this tag to the provider before launchCustomIconDialog, in case it isn't already
            tagsProvider.addTagEntry(tagEntry);

            staticEntry = tagEntry;
        }

        // make sure we have the tag in the provider or else IconSelectDialog will not find it
        if (staticEntry instanceof TagEntry) {
            TagsProvider tagsProvider = dh.getTagsProvider();
            if (tagsProvider != null)
                tagsProvider.addTagEntry((TagEntry) staticEntry);
        }

        IconSelectDialog dlg = Behaviour.getCustomIconDialog(ctx, false);
        dlg.putArgString("entryId", staticEntry.id);
        dlg.setOnConfirmListener(drawable -> {
            int pos = mTagList.indexOf(info);
            if (pos == -1)
                return;

            // update tag info
            if (staticEntry.hasCustomIcon() && drawable == null)
                info.icon = staticEntry.getDefaultDrawable(ctx);
            else
                info.icon = drawable;

            mAdapter.notifyDataSetChanged();
        });
        Behaviour.showDialog(ctx, dlg, Behaviour.DIALOG_CUSTOM_ICON);
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

            if (content.staticEntry instanceof ActionEntry) {
                // this is the untagged entry
                removeBtnView.setVisibility(View.GONE);
                Context context = text1View.getContext();
                Drawable untagged = AppCompatResources.getDrawable(context, R.drawable.ic_untagged);
                if (untagged != null) {
                    int iconSize = text1View.getHeight();
                    if (iconSize <= 0)
                        iconSize = context.getResources().getDimensionPixelSize(R.dimen.icon_preview_size);
                    untagged.setBounds(0, 0, iconSize, iconSize);
                    int dir = context.getResources().getConfiguration().getLayoutDirection();
                    CharSequence text = Utilities.addDrawableAfterString(content.name, untagged, dir);
                    text1View.setText(text);
                }
            } else {
                removeBtnView.setVisibility(View.VISIBLE);
                removeBtnView.setOnClickListener(v -> {
                    if (tagsAdapter.mOnRemoveListener != null)
                        tagsAdapter.mOnRemoveListener.onClick(tagsAdapter, v, position);
                });
            }

            if (content.action == TagInfo.Action.DELETE) {
                text1View.setPaintFlags(text1View.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                // the rest of the views are null, exit now
                return;
            }

            int count = content.entryCount;
            if (count >= 0) {
                text2View.setVisibility(View.VISIBLE);
                text2View.setText(text2View.getResources().getQuantityString(R.plurals.tag_entry_count, count, count));
            } else {
                // we can't have a negative count
                text2View.setVisibility(View.GONE);
            }

            if (content.icon == null) {
                if (content.staticEntry != null) {
                    int drawFlags = EntryItem.FLAG_DRAW_ICON | EntryItem.FLAG_DRAW_NO_CACHE;
                    ResultViewHelper.setIconAsync(drawFlags, content.staticEntry, iconView, StaticEntry.AsyncSetEntryIcon.class);
                } else {
                    Drawable icon = new CodePointDrawable(content.name);
                    icon = DrawableUtils.applyIconMaskShape(iconView.getContext(), icon, DrawableUtils.SHAPE_SQUIRCLE, false);
                    iconView.setImageDrawable(icon);
                }
            } else {
                iconView.setImageDrawable(content.icon);
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

    public static class TagInfo {
        public final StaticEntry staticEntry;
        public final String tagName;
        private String name;
        private Drawable icon = null;

        private int entryCount;
        private Action action = Action.NONE;

        public enum Action {NONE, DELETE, RENAME}

        public TagInfo(String name) {
            staticEntry = null;
            tagName = name;
        }

        public TagInfo(StaticEntry entry) {
            staticEntry = entry;
            tagName = entry.getName();
        }

        public void setInfo(String name, int count) {
            this.name = name;
            this.entryCount = count;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TagInfo tagInfo = (TagInfo) o;
            return Objects.equals(staticEntry, tagInfo.staticEntry) &&
                Objects.equals(tagName, tagInfo.tagName) &&
                Objects.equals(name, tagInfo.name) &&
                Objects.equals(icon, tagInfo.icon) &&
                action == tagInfo.action;
        }

        @Override
        public int hashCode() {
            return Objects.hash(staticEntry, tagName, name, icon, action);
        }
    }
}
