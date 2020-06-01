package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public final class ShortcutEntry extends EntryWithTags {

    public static final String SCHEME = "shortcut://";
    public static final String OREO_PREFIX = "oreo-shortcut/";

    private final int dbId;
    public final String packageName;
    public final String intentUri;// TODO: 15/10/18 Use boolean instead of prefix for Oreo shortcuts

    public ShortcutEntry(String id, int dbId, String packageName, String intentUri) {
        super(id);

        this.dbId = dbId;
        this.packageName = packageName;
        this.intentUri = intentUri;
    }

    /**
     * Oreo shortcuts do not have a real intentUri, instead they have a shortcut id
     * and the Android system is responsible for safekeeping the Intent
     */
    public boolean isOreoShortcut() {
        return intentUri.contains(ShortcutEntry.OREO_PREFIX);
    }

    public String getOreoId() {
        // Oreo shortcuts encode their id in the unused intentUri field
        return intentUri.replace(ShortcutEntry.OREO_PREFIX, "");
    }

    public Bitmap getIcon(Context context) {
        byte[] iconBlob = DBHelper.getShortcutIcon(context, this.dbId);

        if(iconBlob == null) {
            return null;
        }

        return BitmapFactory.decodeByteArray(iconBlob, 0, iconBlob.length);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public int getResultLayout() {
        return R.layout.item_app;
    }

    @Override
    public void displayResult(@NonNull View view) {
        Context context = view.getContext();
        throw new RuntimeException("Not implemented");
    }
}
