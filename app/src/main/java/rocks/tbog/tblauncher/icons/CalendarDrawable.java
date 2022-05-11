package rocks.tbog.tblauncher.icons;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;

public class CalendarDrawable extends DrawableInfo {
    private final int[] drawableForDay;

    protected CalendarDrawable(@NonNull String drawableName) {
        super(drawableName);
        drawableForDay = new int[31];
    }

    public void setDayDrawable(int dayOfMonthIdx, @DrawableRes int drawableId) {
        drawableForDay[dayOfMonthIdx] = drawableId;
    }

    @DrawableRes
    public int getDayDrawable(int dayOfMonthIdx) {
        return drawableForDay[dayOfMonthIdx];
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
