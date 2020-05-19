package rocks.tbog.tblauncher.entry;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;

public abstract class EntryWithTags extends EntryItem {
    // Tags assigned to this pojo
    private String tags = "";

    EntryWithTags(@NonNull String id) {
        super(id);
    }

//    public StringNormalizer.Result getNormalizedTags() {
//        return normalizedTags;
//    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        if (tags != null) {
            // Set the actual user-friendly name
            this.tags = tags;
//            this.normalizedTags = StringNormalizer.normalizeWithResult(this.tags, false);
        } else {
            this.tags = null;
//            this.normalizedTags = null;
        }
    }

}
