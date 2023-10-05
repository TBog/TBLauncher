package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsManager;
import rocks.tbog.tblauncher.drawable.CodePointDrawable;
import rocks.tbog.tblauncher.searcher.TagSearcher;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.utils.DialogHelper;

public class TagEntry extends StaticEntry {
    public static final String SCHEME = "tag://";

    public TagEntry(@NonNull String id) {
        super(id, 0);
        if (BuildConfig.DEBUG) {
            if (!id.startsWith(SCHEME))
                throw new IllegalStateException("Invalid " + getClass().getSimpleName() + " id `" + id + "`");
            if ((!(this instanceof TagSortEntry) && TagSortEntry.isTagSort(id))
                || ((this instanceof TagSortEntry) && !TagSortEntry.isTagSort(id)))
                throw new IllegalStateException(getClass().getSimpleName() + " instantiated with id `" + id + "`");
        }
    }

    @Override
    public void setName(String name) {
        if (name != null) {
            if (!id.endsWith(name))
                throw new IllegalStateException("tags can't have the display name different from the tag name");
            super.setName(name);
        } else {
            super.setName(id.substring(SCHEME.length()));
        }
    }

    @Override
    protected void buildPopupMenuCategory(Context context, @NonNull LinearAdapter adapter, int titleStringId) {
        if (titleStringId == R.string.popup_title_hist_fav) {
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_az));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_za));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_rec));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_freq));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_frec));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_adaptive));
        }
        super.buildPopupMenuCategory(context, adapter, titleStringId);
    }

    @Override
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        Context ctx = view.getContext();
        boolean changesMade = false;
        CharSequence toastText = null;
        if (stringId == R.string.menu_action_rename) {
            launchRenameDialog(ctx);
            return true;
        } else if (stringId == R.string.menu_tag_sort_az) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.SORT_AZ + getName());
            toastText = ctx.getText(stringId);
        } else if (stringId == R.string.menu_tag_sort_za) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.SORT_ZA + getName());
            toastText = ctx.getText(stringId);
        } else if (stringId == R.string.menu_tag_sort_hist_rec) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.HISTORY_REC + getName());
            toastText = ctx.getText(stringId);
        } else if (stringId == R.string.menu_tag_sort_hist_freq) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.HISTORY_FREQ + getName());
            toastText = ctx.getText(stringId);
        } else if (stringId == R.string.menu_tag_sort_hist_frec) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.HISTORY_FREC + getName());
            toastText = ctx.getText(stringId);
        } else if (stringId == R.string.menu_tag_sort_hist_adaptive) {
            changesMade = TBApplication.tagsHandler(ctx).changeTagSort(id, SCHEME + TagSortEntry.HISTORY_ADAPTIVE + getName());
            toastText = ctx.getText(stringId);
        }
        if (toastText != null) {
            if (!changesMade) {
                toastText = ctx.getString(R.string.error, toastText);
            }
            Toast.makeText(ctx, toastText, Toast.LENGTH_SHORT).show();
        }
        if (changesMade) {
            // update providers, we're expecting the tag to be in the Dock
            TBApplication.dataHandler(ctx).afterQuickListChanged();
            return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        if (TBApplication.activityInvalid(v))
            return;
        Context ctx = v.getContext();
        TBApplication.quickList(ctx).toggleSearch(v, getName(), TagSearcher.class);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        super.displayResult(view, drawFlags);
        view.setTag(R.id.tag_actionId, id);
    }

    @Override
    public Drawable getDefaultDrawable(@NonNull Context context) {
        return new CodePointDrawable(getName());
    }

    private void launchRenameDialog(@NonNull Context c) {
        DialogHelper.makeRenameDialog(c, getName(), (dialog, newName) -> {
                Context ctx = dialog.getContext();

                String oldName = getName();

                TBApplication app = TBApplication.getApplication(ctx);
                app.tagsHandler().renameTag(oldName, newName);
                app.behaviour().refreshSearchRecord(TagEntry.this);

                // update providers and refresh views
                TagsManager.afterChangesMade(ctx);
            })
            .setTitle(R.string.title_rename_tag)
            .setHint(R.string.hint_rename_tag)
            .show();
    }
}
