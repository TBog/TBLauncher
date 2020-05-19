package rocks.tbog.tblauncher.entry;

import androidx.annotation.NonNull;

import java.util.Comparator;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;

public abstract class EntryItem {

    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    @NonNull
    public final String id;
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Name for this Entry, e.g. app name
    @NonNull
    private
    String name = "";
    // How relevant is this record? The higher, the most probable it will be displayed
    private int relevance = 0;

    public EntryItem(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        if (name != null) {
            // Set the actual user-friendly name
            this.name = name;
            this.normalizedName = StringNormalizer.normalizeWithResult(this.name, false);
        } else {
            this.name = "null";
            this.normalizedName = null;
        }
    }

    public void setName(String name, boolean generateNormalization) {
        if (generateNormalization) {
            setName(name);
        } else {
            this.name = name;
            this.normalizedName = null;
        }
    }

    public int getRelevance()
    {
        return relevance;
    }

    public void setRelevance(int relevance) {
        this.relevance = relevance;
    }

    /**
     * ID to use in the history
     * (may be different from the one used in the adapter for display)
     */
    public String getHistoryId() {
        return this.id;
    }

    public static class RelevanceComparator implements java.util.Comparator<EntryItem>
    {
        @Override
        public int compare(EntryItem lhs, EntryItem rhs) {
            if (lhs.getRelevance() == rhs.getRelevance()) {
                if (lhs.normalizedName != null && rhs.normalizedName != null)
                    return lhs.normalizedName.compareTo(rhs.normalizedName);
                else
                    return lhs.name.compareTo(rhs.name);
            }
            return lhs.getRelevance() - rhs.getRelevance();
        }
    }
}
