package rocks.tbog.tblauncher.icons;

import androidx.annotation.NonNull;

import java.util.Objects;

public class DrawableInfo {
    @NonNull
    private final String drawableName;

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        DrawableInfo that = (DrawableInfo) o;
        return drawableName.equals(that.drawableName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(drawableName);
    }

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
}
