package rocks.tbog.tblauncher.db;

public abstract class FlagsRecord {
    protected int flags = 0;

    public abstract int getFlagsDB();

    public void setFlags(int flags) {
        this.flags = flags;
    }

    public void addFlags(int flags) {
        this.flags |= flags;
    }

    public void clearFlags(int flags) {
        this.flags &= ~flags;
    }

    public boolean isFlagSet(int flag) {
        return (flags & flag) == flag;
    }
}
