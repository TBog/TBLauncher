package rocks.tbog.tblauncher.db;

public class FavRecord extends FlagsRecord {
    public static final int FLAG_SHOW_IN_QUICK_LIST = 1;
    public static final int FLAG_CUSTOM_NAME = 1 << 1;
    public static final int FLAG_CUSTOM_ICON = 1 << 2;
    private static final int MASK_SAVE_DB_FLAGS = FLAG_SHOW_IN_QUICK_LIST | FLAG_CUSTOM_NAME | FLAG_CUSTOM_ICON;
    public static final int FLAG_VALIDATED = 1 << 3;

    public String record;
    public String position;
    public String displayName;

    @Override
    public int getFlagsDB() {
        return flags & MASK_SAVE_DB_FLAGS;
    }

    public boolean isInQuickList() {
        return (flags & FLAG_SHOW_IN_QUICK_LIST) == FLAG_SHOW_IN_QUICK_LIST;
    }

    public boolean hasCustomName() {
        return (flags & FLAG_CUSTOM_NAME) == FLAG_CUSTOM_NAME;
    }

    public boolean hasCustomIcon() {
        return (flags & FLAG_CUSTOM_ICON) == FLAG_CUSTOM_ICON;
    }
}
