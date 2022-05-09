package rocks.tbog.tblauncher.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class SimpleDrawable extends DrawableInfo {
    @DrawableRes
    private final int drawableId;

    public SimpleDrawable(@NonNull String drawableName, @DrawableRes int drawableId) {
        super(drawableName);
        this.drawableId = drawableId;
    }

    @DrawableRes
    public int getResourceId() {
        return drawableId;
    }
}
