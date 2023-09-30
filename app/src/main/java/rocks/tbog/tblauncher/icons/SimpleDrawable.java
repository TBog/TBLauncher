package rocks.tbog.tblauncher.icons;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class SimpleDrawable extends DrawableInfo {
    @DrawableRes
    private final int drawableId;

    public SimpleDrawable(@NonNull String drawableName, @DrawableRes int drawableId) {
        super(drawableName);
        this.drawableId = drawableId;
    }

    @Override
    @DrawableRes
    public int getDrawableResId(@NonNull IconPackXML iconPack) {
        return drawableId;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull IconPackXML iconPack, @Nullable Resources.Theme theme) {
        Resources res = iconPack.getResources();
        if (res == null)
            return null;
        try {
            return ResourcesCompat.getDrawable(res, drawableId, theme);
        } catch (Resources.NotFoundException ignored) {
            return null;
        }
    }
}
