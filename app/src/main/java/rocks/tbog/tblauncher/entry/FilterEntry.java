package rocks.tbog.tblauncher.entry;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;

public class FilterEntry extends StaticEntry {
    public static final String SCHEME = "filter://";
    private View.OnClickListener listener = null;

    public FilterEntry(@NonNull String id, @DrawableRes int icon) {
        super(id, icon);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + FilterEntry.class.getSimpleName() + " id `" + id + "`");
        }
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        super.displayResult(view, drawFlags);
        // this is used for the toggle animation
        view.setTag(R.id.tag_filterName, id);
    }

    @Override
    public void doLaunch(@NonNull View view, int flags) {
        listener.onClick(view);
    }

    public void setOnClickListener(@Nullable View.OnClickListener listener) {
        this.listener = listener;
    }
}
