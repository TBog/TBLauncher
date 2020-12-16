package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.searcher.TagSearcher;
import rocks.tbog.tblauncher.ui.CodePointDrawable;

public class TagEntry extends StaticEntry {
    public static final String SCHEME = "tag://";

    public TagEntry(@NonNull String id) {
        super(id, 0);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + TagEntry.class.getSimpleName() + " id `" + id + "`");
        }
    }

    @Override
    public void doLaunch(@NonNull View v) {
        Context ctx = v.getContext();
        TBApplication.behaviour(ctx).runSearcher(getName(), TagSearcher.class);
    }

    @Override
    public Drawable getDefaultDrawable(Context context) {
        return new CodePointDrawable(getName());
    }
}
