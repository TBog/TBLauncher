package rocks.tbog.tblauncher.db;

public final class AppRecord extends FlagsRecord {
    public static final int FLAG_DEFAULT_NAME = 0x000001;
    public static final int FLAG_CUSTOM_NAME = 0x000002;
    public static final int FLAG_CUSTOM_ICON = 0x000004;
    public static final int FLAG_APP_HIDDEN = 0x000008;
    private static final int MASK_SAVE_DB_FLAGS = FLAG_DEFAULT_NAME | FLAG_CUSTOM_NAME | FLAG_CUSTOM_ICON | FLAG_APP_HIDDEN;
    public static final int FLAG_VALIDATED = 0x080000;

    public long dbId = -1;

    public String displayName;

    public String componentName;

    public AppRecord() {
        flags = FLAG_DEFAULT_NAME;
    }

    @Override
    public int getFlagsDB() {
        return flags & MASK_SAVE_DB_FLAGS;
    }

    public boolean hasCustomName() {
        return (flags & FLAG_CUSTOM_NAME) == FLAG_CUSTOM_NAME;
    }

    public boolean hasCustomIcon() {
        return (flags & FLAG_CUSTOM_ICON) == FLAG_CUSTOM_ICON;
    }

    public boolean isHidden() {
        return (flags & FLAG_APP_HIDDEN) == FLAG_APP_HIDDEN;
    }
}
