package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.drawable.CodePointDrawable;
import rocks.tbog.tblauncher.searcher.TagSearcher;

public class TagEntry extends StaticEntry {
    public static final String SCHEME = "tag://";

    public TagEntry(@NonNull String id) {
        super(id, 0);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + TagEntry.class.getSimpleName() + " id `" + id + "`");
        }
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
}
