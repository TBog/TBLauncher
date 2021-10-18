package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.TagEntry;

public class TagsProvider extends DBProvider<TagEntry> {

    public TagsProvider(Context context) {
        super(context);
    }

    @Override
    protected DBLoader<TagEntry> newLoadTask() {
        return new FavLoader(this);
    }

    @Nullable
    private static TagEntry newTagEntryCheckId(String id) {
        if (id.startsWith(TagEntry.SCHEME)) {
            TagEntry tagEntry = new TagEntry(id);
            String tagName = id.substring(TagEntry.SCHEME.length());
            tagEntry.setName(tagName);
            return tagEntry;
        }
        return null;
    }

    @NonNull
    private static TagEntry newTagEntry(@Nullable String id, @NonNull String tagName) {
        if (id == null)
            id = TagEntry.SCHEME + tagName;
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
        String id = TagEntry.SCHEME + tagName;
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
            List<FavRecord> favorites = dataHandler.getFavorites();
            // get TagEntry from FavRecord
            for (FavRecord fav : favorites) {
                TagEntry entry = newTagEntryCheckId(fav.record);
                if (entry == null)
                    continue;
                if (fav.hasCustomIcon())
                    entry.setCustomIcon();
                tagList.add(entry);
            }

            return tagList;
        }

        @Override
        protected void onPostExecute(List<TagEntry> entryItems) {
            super.onPostExecute(entryItems);
            DBProvider<TagEntry> provider = weakProvider.get();
            if (provider != null)
                TBApplication.quickList(provider.context).reload();
        }
    }
}

//public class TagsProvider extends StaticProvider<TagEntry> {
//    final Context context;
//
//    public TagsProvider(Context context) {
//        super(new ArrayList<>(0));
//        this.context = context;
//    }
//
//    @Override
//    public boolean mayFindById(@NonNull String id) {
//        return id.startsWith(TagEntry.SCHEME);
//    }
//
//    @Override
//    public EntryItem findById(@NonNull String id) {
//        EntryItem entryItem = super.findById(id);
//        if (entryItem == null) {
//            Set<String> cachedTags = TBApplication.tagsHandler(context).getAllTagsAsSet();
//            String tagName = id.substring(TagEntry.SCHEME.length());
//            if (cachedTags.contains(tagName)) {
//                entryItem = new TagEntry(id);
//                entryItem.setName(tagName);
//                pojos.add((TagEntry) entryItem);
//            }
//        }
//        return entryItem;
//    }
//
//    private static TagEntry newTagEntry(String tagName)
//    {
//        TagEntry tagEntry = new TagEntry(TagEntry.SCHEME + tagName);
//        tagEntry.setName(tagName);
//    }
//
//    @Override
//    public List<? extends EntryItem> getPojos() {
//        Set<String> cachedTags = TBApplication.tagsHandler(context).getAllTagsAsSet();
//        ArrayList<? extends EntryItem> list = new ArrayList<>(pojos.size());
//        for(String tag : cachedTags) {
//            String id = TagEntry.SCHEME + tag;
//            EntryItem entryItem = super.findById(id);
//            if (entryItem == null)
//        }
//
//        if (BuildConfig.DEBUG)
//            return Collections.unmodifiableList(pojos);
//        return pojos;
//    }
//
//}
