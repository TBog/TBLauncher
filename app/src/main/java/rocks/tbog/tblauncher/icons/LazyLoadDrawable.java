package rocks.tbog.tblauncher.icons;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class LazyLoadDrawable extends DrawableInfo {

    @DrawableRes
    private int drawableId = 0;
    private boolean drawableIdCached = false;

    protected LazyLoadDrawable(@NonNull String drawableName) {
        super(drawableName);
    }

    @SuppressLint("DiscouragedApi")
    @Nullable
    @Override
    public Drawable getDrawable(@NonNull IconPackXML iconPack, @Nullable Resources.Theme theme) {
        Resources res = iconPack.getResources();
        if (res == null)
            return null;
        if (!drawableIdCached) {
            drawableId = res.getIdentifier(getDrawableName(), "drawable", iconPack.getPackPackageName());
            drawableIdCached = true;
        }
        try {
            return ResourcesCompat.getDrawable(res, drawableId, theme);
        } catch (Resources.NotFoundException ignored) {
            return null;
        }
    }
}
