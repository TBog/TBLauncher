package rocks.tbog.tblauncher.entry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;

import java.util.List;
import java.util.Objects;

import rocks.tbog.tblauncher.normalizer.StringNormalizer;

public abstract class EntryWithTags extends EntryItem {
    // Tags assigned to this pojo
    private final ArraySet<TagDetails> tags = new ArraySet<>(0);

    public static class TagDetails {
        @NonNull
        public final String name;
        public final StringNormalizer.Result normalized;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TagDetails that = (TagDetails) o;
            return name.equals(that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }

        public TagDetails(@NonNull String name) {
            this.name = name;
            normalized = StringNormalizer.normalizeWithResult(name, true);
        }

        public TagDetails(@NonNull String name, StringNormalizer.Result normalized) {
            this.name = name;
            this.normalized = normalized;
        }
    }

    EntryWithTags(@NonNull String id) {
        super(id);
    }

    @NonNull
    public ArraySet<TagDetails> getTags() {
        return tags;
    }

    public void setTags(@Nullable List<String> tags) {
        this.tags.clear();
        if (tags != null) {
            for (String tag : tags)
                this.tags.add(new TagDetails(tag));
        }
    }

}
