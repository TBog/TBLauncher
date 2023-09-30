package rocks.tbog.tblauncher.icons;

import android.annotation.SuppressLint;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import java.util.Arrays;
import java.util.Calendar;

public class CalendarDrawable extends DrawableInfo {
    private final int[] drawableForDay;
    private final boolean[] drawableIdCached;

    protected CalendarDrawable(@NonNull String drawableName) {
        super(drawableName);
        drawableForDay = new int[31];
        drawableIdCached = new boolean[31];
        Arrays.fill(drawableIdCached, false);
    }

    @SuppressLint("DiscouragedApi")
    @Override
    @DrawableRes
    public int getDrawableResId(@NonNull IconPackXML iconPack) {
        int dayOfMonthIdx = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
        return getDayDrawableId(iconPack, dayOfMonthIdx);
    }

    @SuppressLint("DiscouragedApi")
    @DrawableRes
    private int getDayDrawableId(@NonNull IconPackXML iconPack, int dayOfMonthIdx) {
        Resources res = iconPack.getResources();
        if (res == null)
            return drawableForDay[dayOfMonthIdx];
        if (!drawableIdCached[dayOfMonthIdx]) {
            String drawableName = getDrawableName() + (1 + dayOfMonthIdx);
            drawableForDay[dayOfMonthIdx] = res.getIdentifier(drawableName, "drawable", iconPack.getPackPackageName());
            drawableIdCached[dayOfMonthIdx] = true;
        }

        return drawableForDay[dayOfMonthIdx];
    }

    @Override
    public boolean isDynamic() {
        return true;
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull IconPackXML iconPack, @Nullable Resources.Theme theme) {
        Resources res = iconPack.getResources();
        if (res == null)
            return null;
        int dayOfMonthIdx = Calendar.getInstance().get(Calendar.DAY_OF_MONTH) - 1;
        int drawableId = getDayDrawableId(iconPack, dayOfMonthIdx);
        try {
            return ResourcesCompat.getDrawable(res, drawableId, theme);
        } catch (Resources.NotFoundException ignored) {
            return null;
        }
    }
}
