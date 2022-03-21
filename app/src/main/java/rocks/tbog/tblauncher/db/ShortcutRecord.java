package rocks.tbog.tblauncher.db;

public class ShortcutRecord extends FlagsRecord {
    public static final int FLAG_HIDE_BADGE = 1;
    public static final int FLAG_OREO = 1 << 1;
    public static final int FLAG_CUSTOM_INTENT = 1 << 4;
    private static final int MASK_SAVE_DB_FLAGS = FLAG_HIDE_BADGE | FLAG_OREO | FLAG_CUSTOM_INTENT;
    public static final int FLAG_VALIDATED = 1 << 3;

    public long dbId = -1;

    public String displayName;

    public String packageName;

    public String infoData;

    public byte[] iconPng;

    @Override
    public int getFlagsDB() {
        return flags & MASK_SAVE_DB_FLAGS;
    }

    public boolean isOreo() {
        return (flags & FLAG_OREO) == FLAG_OREO;
    }
}
