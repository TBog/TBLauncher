package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.BuildConfig;
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
    public Drawable getDefaultDrawable(Context context) {
        return new CodePointDrawable(getName());
    }
}
