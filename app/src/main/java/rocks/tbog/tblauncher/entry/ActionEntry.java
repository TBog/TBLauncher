package rocks.tbog.tblauncher.entry;

import android.view.View;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;

public class ActionEntry extends StaticEntry {
    public static final String SCHEME = "action://";
    private DoAction action = null;

    public interface DoAction {
        void doAction(View view);
    }

    public ActionEntry(@NonNull String id, @DrawableRes int icon) {
        super(id, icon);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + ActionEntry.class.getSimpleName() + " id `" + id + "`");
        }
    }

    @Override
    public void displayResult(@NonNull View view, int drawFlags) {
        super.displayResult(view, drawFlags);
        view.setTag(R.id.tag_actionId, id);
    }

    @Override
    public void doLaunch(@NonNull View view) {
        if (action == null) {
            Toast.makeText(view.getContext(), "`" + id + "` not implemented", Toast.LENGTH_LONG).show();
            return;
        }
        action.doAction(view);
    }

    public void setAction(@Nullable DoAction action) {
        this.action = action;
    }
}
