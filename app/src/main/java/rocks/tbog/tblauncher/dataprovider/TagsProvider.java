package rocks.tbog.tblauncher.dataprovider;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.TagEntry;

public class TagsProvider extends StaticProvider<TagEntry> {


    public TagsProvider() {
        super(new ArrayList<>(0));
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(TagEntry.SCHEME);
    }

    @Override
    public EntryItem findById(@NonNull String id) {
        EntryItem entryItem = super.findById(id);
        if (entryItem == null) {
            entryItem = new TagEntry(id);
            entryItem.setName(id.substring(TagEntry.SCHEME.length()));
            pojos.add((TagEntry) entryItem);
        }
        return entryItem;
    }
}
