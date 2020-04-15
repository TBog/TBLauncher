package rocks.tbog.tblauncher.db;

public class AppRecord {
    public static final int FLAG_DEFAULT_NAME = 1;
    public static final int FLAG_CUSTOM_NAME = 1 << 1;
    public static final int FLAG_CUSTOM_ICON = 1 << 2;
    private static final int MASK_SAVE_DB_FLAGS = FLAG_DEFAULT_NAME | FLAG_CUSTOM_NAME | FLAG_CUSTOM_ICON;
    public static final int FLAG_VALIDATED = 1 << 3;

    public long dbId = -1;

    public String displayName;

    public String componentName;

    public int flags = FLAG_DEFAULT_NAME;

    public boolean hasCustomName() {
        return (flags & FLAG_CUSTOM_NAME) == FLAG_CUSTOM_NAME;
    }

    public boolean hasCustomIcon() {
        return (flags & FLAG_CUSTOM_ICON) == FLAG_CUSTOM_ICON;
    }

    public int getFlagsDB() {
        return flags & MASK_SAVE_DB_FLAGS;
    }

//    private AppRecord() {
//        super();
//    }
//
//    public static class Builder
//    {
//        public static
//    }
}
