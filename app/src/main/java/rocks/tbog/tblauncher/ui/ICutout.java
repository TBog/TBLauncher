package rocks.tbog.tblauncher.ui;

import android.graphics.Rect;

public interface ICutout {
    boolean hasCutout();

    Rect[] getCutout();
    Rect getSafeZone();
}
