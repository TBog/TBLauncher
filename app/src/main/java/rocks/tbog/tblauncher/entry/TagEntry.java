package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

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
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + TagEntry.class.getSimpleName() + " id `" + id + "`");
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
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        if (stringId == R.string.menu_action_rename) {
            Context ctx = view.getContext();
            launchRenameDialog(ctx);
            return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        Context ctx = v.getContext();

        TBApplication.quickList(ctx).toggleSearch(v, getName(), TagSearcher.class);
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        super.displayResult(view, drawFlags);
        view.setTag(R.id.tag_actionId, id);
    }

    @Override
    public Drawable getDefaultDrawable(Context context) {
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
