package rocks.tbog.tblauncher.widgets;

import androidx.annotation.NonNull;

class ItemTitle implements MenuItem {
    @NonNull
    private final String name;

    ItemTitle(@NonNull String string) {
        this.name = string;
    }

    @NonNull
    @Override
    public String getName() {
        return name;
    }
}
