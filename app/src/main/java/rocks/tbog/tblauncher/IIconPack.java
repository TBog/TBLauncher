package rocks.tbog.tblauncher;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.Nullable;

interface IIconPack {
    String getPackPackageName();
    void load(PackageManager packageManager);
    @Nullable
    Drawable getComponentDrawable(String componentName);
}
