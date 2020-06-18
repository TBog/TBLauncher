package rocks.tbog.tblauncher.db;

public class FavRecord {
    public static final int FLAG_SHOW_IN_QUICK_LIST = 1;
    private static final int MASK_SAVE_DB_FLAGS = FLAG_SHOW_IN_QUICK_LIST;
    public static final int FLAG_VALIDATED = 1 << 3;

    public String record;
    public String position;
    public int flags = 0;

    public int getFlagsDB() {
        return flags & MASK_SAVE_DB_FLAGS;
    }

    public boolean isInQuickList() {
        return (flags & FLAG_SHOW_IN_QUICK_LIST) == FLAG_SHOW_IN_QUICK_LIST;
    }
}
