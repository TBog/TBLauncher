package rocks.tbog.tblauncher.icons;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

public abstract class DrawableInfo {
    @NonNull
    private final String drawableName;

    protected DrawableInfo(@NonNull String drawableName) {
        this.drawableName = drawableName;
    }

    @NonNull
    public String getDrawableName() {
        return drawableName;
    }

    public boolean isDynamic() {
        return false;
    }

    @DrawableRes
    public abstract int getDrawableResId(@NonNull IconPackXML iconPack);

    @Nullable
    public abstract Drawable getDrawable(@NonNull IconPackXML iconPack, @Nullable Resources.Theme theme);

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof DrawableInfo))
            return false;
        DrawableInfo that = (DrawableInfo) o;
        return drawableName.equals(that.drawableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(drawableName);
    }
}
