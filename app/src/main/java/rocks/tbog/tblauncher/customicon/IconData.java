package rocks.tbog.tblauncher.customicon;

import android.graphics.drawable.Drawable;

import rocks.tbog.tblauncher.icons.DrawableInfo;
import rocks.tbog.tblauncher.icons.IconPackXML;

class IconData {
    final DrawableInfo drawableInfo;
    final IconPackXML iconPack;

    IconData(IconPackXML iconPack, DrawableInfo drawableInfo) {
        this.iconPack = iconPack;
        this.drawableInfo = drawableInfo;
    }

    Drawable getIcon() {
        return iconPack.getDrawable(drawableInfo);
    }
}
