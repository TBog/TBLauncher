package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.DataHandler;

public class TagsProvider extends DBProvider<TagEntry> {

    public TagsProvider(Context context) {
        super(context);
    }

    @Override
    protected DBLoader<TagEntry> newLoadTask() {
        return new FavLoader(this);
    }

    @Nullable
    public static TagEntry newTagEntryCheckId(String id) {
        if (id.startsWith(TagEntry.SCHEME)) {
            TagEntry tagEntry = new TagEntry(id);
            String tagName = id.substring(TagEntry.SCHEME.length());
            tagEntry.setName(tagName);
            return tagEntry;
        }
        return null;
    }

    @NonNull
    public static String getTagId(@NonNull String tagName) {
        return TagEntry.SCHEME + tagName;
    }

    @NonNull
    private static TagEntry newTagEntry(@NonNull String id, @NonNull String tagName) {
        TagEntry tagEntry = new TagEntry(id);
        tagEntry.setName(tagName);
        return tagEntry;
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(TagEntry.SCHEME);
    }

    @NonNull
    public TagEntry getTagEntry(String tagName) {
        String id = getTagId(tagName);
        TagEntry entryItem = findById(id);
        if (entryItem == null)
            return newTagEntry(id, tagName);
        return entryItem;
    }

    public void addTagEntry(TagEntry tagEntry) {
        if (null == findById(tagEntry.id))
            entryList.add(tagEntry);
    }

    private static class FavLoader extends DBProvider.DBLoader<TagEntry> {

        public FavLoader(DBProvider<TagEntry> provider) {
            super(provider);
        }

        @Override
        List<TagEntry> getEntryItems(DataHandler dataHandler) {
            ArrayList<TagEntry> tagList = new ArrayList<>();
            List<ModRecord> mods = dataHandler.getMods();
            // get TagEntry from ModRecord
            for (ModRecord mod : mods) {
                TagEntry entry = newTagEntryCheckId(mod.record);
                if (entry == null)
                    continue;
                if (mod.hasCustomIcon())
                    entry.setCustomIcon();
                tagList.add(entry);
            }

            return tagList;
        }
    }
}