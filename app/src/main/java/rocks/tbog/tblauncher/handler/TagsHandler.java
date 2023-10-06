package rocks.tbog.tblauncher.handler;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ModProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.entry.TagSortEntry;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.TagsMenuUtils;
import rocks.tbog.tblauncher.utils.PrefOrderedListHelper;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagsHandler {
    private static final String TAG = TagsHandler.class.getSimpleName();
    private final TBApplication mApplication;
    // HashMap with EntryItem id as key and an ArrayList of tags for each
    private final HashMap<String, List<String>> mTagsCache = new HashMap<>();
    private boolean mIsLoaded = false;
    private final ArrayDeque<Runnable> mAfterLoadedTasks = new ArrayDeque<>(2);

    public TagsHandler(TBApplication application) {
        mApplication = application;
        loadFromDB(false);
    }

    @Nullable
    public void loadFromDB(boolean wait) {
        Log.d(TAG, "loadFromDB(wait= " + wait + " )");

        synchronized (this) {
            mIsLoaded = false;
        }
        final Timer timer = Timer.startMilli();
        final HashMap<String, List<String>> tags = new HashMap<>();
        final Runnable load = () -> {
            Map<String, List<String>> dbTags = DBHelper.loadTags(getContext());
            tags.clear();
            tags.putAll(dbTags);
        };
        final Runnable apply = () -> {
            if (tags.isEmpty()) {
                mTagsCache.clear();
                addDefaultAliases();
                mTagsCache.put(".", Collections.singletonList(""));
                DBHelper.addTags(getContext(), mTagsCache);
                tags.putAll(mTagsCache);
            }
            synchronized (TagsHandler.this) {
                mTagsCache.clear();
                mTagsCache.putAll(tags);
                mIsLoaded = true;

                timer.stop();
                Log.d("time", "Time to load all tags: " + timer);

                // run and remove tasks
                Runnable task;
                while (null != (task = mAfterLoadedTasks.poll()))
                    task.run();
            }
        };
        if (wait) {
            load.run();
            apply.run();
        } else {
            Utilities.runAsync(
                (t) -> load.run(),
                (t) -> apply.run());
        }
    }

    public void runWhenLoaded(@NonNull Runnable task) {
        synchronized (this) {
            if (mIsLoaded)
                task.run();
            else
                mAfterLoadedTasks.add(task);
        }
    }

    private Context getContext() {
        return mApplication;
    }

    public void addTag(EntryItem entry, String tag) {
        // add to db
        DBHelper.addTag(getContext(), tag, entry);
        // add to cache
        List<String> tags = mTagsCache.get(entry.id);
        if (tags == null)
            mTagsCache.put(entry.id, tags = new ArrayList<>());
        tags.add(tag);
    }

    private boolean removeTag(String entryId, String tag) {
        boolean changesMade = false;
        // remove from DB
        if (DBHelper.removeTag(getContext(), tag, entryId) > 0)
            changesMade = true;
        // remove from cache
        List<String> tags = mTagsCache.get(entryId);
        if (tags != null) {
            tags.remove(tag);
            changesMade = true;
        }
        return changesMade;
    }

    public boolean removeTag(String tag) {
        List<EntryWithTags> entries = getEntries(tag);
        for (EntryWithTags entry : entries) {
            if (removeTag(entry.id, tag)) {
                entry.setTags(getTags(entry.id));
                return true;
            }
        }
        return false;
    }

    @NonNull
    public List<String> getTags(String entryId) {
        List<String> tags = mTagsCache.get(entryId);
        if (tags == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(tags);
    }

    /**
     * Get tags currently used
     *
     * @return a set of tags
     */
    @NonNull
    public Set<String> getValidTags() {
        Set<String> tags = new HashSet<>();
        DataHandler dataHandler = TBApplication.dataHandler(getContext());
        for (Map.Entry<String, List<String>> entry : mTagsCache.entrySet()) {
            EntryItem entryItem = dataHandler.getPojo(entry.getKey());
            if (entryItem != null)
                tags.addAll(entry.getValue());
        }
        tags.remove("");
        return tags;
    }

    /**
     * Filter out entry ids not found from each set. Remove unused tags from the map.
     *
     * @param context used for getting DataHandler to check if entryIds are found.
     * @param tags    map with tag names as keys.
     */
    public static void validateTags(@NonNull Context context, Map<String, Set<String>> tags) {
        tags.remove("");
        DataHandler dataHandler = TBApplication.dataHandler(context);
        for (Iterator<Map.Entry<String, Set<String>>> iteratorTagsMap = tags.entrySet().iterator(); iteratorTagsMap.hasNext(); ) {
            Map.Entry<String, Set<String>> tagsMapEntry = iteratorTagsMap.next();
            String tagName = tagsMapEntry.getKey();
            Set<String> entryIdSet = tagsMapEntry.getValue();
            for (Iterator<String> iteratorEntryId = entryIdSet.iterator(); iteratorEntryId.hasNext(); ) {
                String entryId = iteratorEntryId.next();
                EntryItem entryItem = dataHandler.getPojo(entryId);
                if (entryItem == null)
                    iteratorEntryId.remove();
            }
            if (tagsMapEntry.getValue().isEmpty()) {
                Log.i(TAG, "Dropped tag `" + tagName + "`");
                iteratorTagsMap.remove();
            }
        }
    }

    /**
     * Get all tags from DB, even if not used
     *
     * @return a set of tags
     */
    @NonNull
    public Set<String> getAllTags() {
        Set<String> allTags = new HashSet<>();
        for (List<String> tags : mTagsCache.values()) {
            allTags.addAll(tags);
        }
        allTags.remove("");
        return allTags;
    }

    @NonNull
    public List<String> getValidEntryIds(String tagName) {
        ArrayList<String> ids = new ArrayList<>();
        DataHandler dataHandler = TBApplication.dataHandler(getContext());
        for (Map.Entry<String, List<String>> entry : mTagsCache.entrySet()) {
            if (entry.getValue().contains(tagName)) {
                EntryItem entryItem = dataHandler.getPojo(entry.getKey());
                if (entryItem != null)
                    ids.add(entryItem.id);
            }
        }
        return ids;
    }

    @NonNull
    public List<String> getAllEntryIds(String tagName) {
        ArrayList<String> ids = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : mTagsCache.entrySet()) {
            if (entry.getValue().contains(tagName))
                ids.add(entry.getKey());
        }
        return ids;
    }

    @NonNull
    public List<EntryWithTags> getEntries(String tagName) {
        ArrayList<EntryWithTags> entries = new ArrayList<>();
        DataHandler dataHandler = TBApplication.dataHandler(getContext());
        for (Map.Entry<String, List<String>> mapEntry : mTagsCache.entrySet()) {
            if (mapEntry.getValue().contains(tagName)) {
                EntryItem entryItem = dataHandler.getPojo(mapEntry.getKey());
                if (entryItem instanceof EntryWithTags)
                    entries.add((EntryWithTags) entryItem);
            }
        }
        return entries;
    }

    @NonNull
    public ListPopup getTagsMenu(Context ctx) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
        ArrayList<String> tagList;
        List<String> tagOrder = PrefOrderedListHelper.getOrderedList(pref, "tags-menu-list", "tags-menu-order");
        if (tagOrder.isEmpty()) {
            tagList = new ArrayList<>(5);
            TagsHandler tagsHandler = TBApplication.tagsHandler(ctx);
            Set<String> validTags = tagsHandler.getValidTags();
            for (String tagName : validTags) {
                if (tagList.size() >= 5)
                    break;
                tagList.add(tagName);
            }
        } else {
            tagList = new ArrayList<>(tagOrder.size());
            for (String orderValue : tagOrder)
                tagList.add(PrefOrderedListHelper.getOrderedValueName(orderValue));
        }
        return TagsMenuUtils.createTagsMenu(ctx, tagList);
    }

    @NonNull
    public Collection<String> getAllEntryIds() {
        return Collections.unmodifiableSet(mTagsCache.keySet());
    }

    private void addDefaultAliases() {
        Context context = getContext();
        final PackageManager pm = context.getPackageManager();
        final Resources res = context.getResources();

        // keep all changes here and apply them after we do all the checks
        Map<String, List<String>> pendingTags = new HashMap<>();

        String phoneApp = getApp(pm, Intent.ACTION_DIAL);
        if (phoneApp != null) {
            String phoneAlias = res.getString(R.string.alias_phone);
            addAliasesToEntry(phoneAlias, phoneApp, pendingTags);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            String contactApp = getAppByCategory(pm, Intent.CATEGORY_APP_CONTACTS);
            if (contactApp != null) {
                String contactAlias = res.getString(R.string.alias_contacts);
                addAliasesToEntry(contactAlias, contactApp, pendingTags);
            }

            String browserApp = getAppByCategory(pm, Intent.CATEGORY_APP_BROWSER);
            if (browserApp != null) {
                String webAlias = res.getString(R.string.alias_web);
                addAliasesToEntry(webAlias, browserApp, pendingTags);
            }

            String mailApp = getAppByCategory(pm, Intent.CATEGORY_APP_EMAIL);
            if (mailApp != null) {
                String mailAlias = res.getString(R.string.alias_mail);
                addAliasesToEntry(mailAlias, mailApp, pendingTags);
            }

            String marketApp = getAppByCategory(pm, Intent.CATEGORY_APP_MARKET);
            if (marketApp != null) {
                String marketAlias = res.getString(R.string.alias_market);
                addAliasesToEntry(marketAlias, marketApp, pendingTags);
            }

            String messagingApp = getAppByCategory(pm, Intent.CATEGORY_APP_MESSAGING);
            if (messagingApp != null) {
                String messagingAlias = res.getString(R.string.alias_messaging);
                addAliasesToEntry(messagingAlias, messagingApp, pendingTags);
            }

            String clockApp = getClockApp(pm);
            if (clockApp != null) {
                String clockAlias = res.getString(R.string.alias_clock);
                addAliasesToEntry(clockAlias, clockApp, pendingTags);
            }
        }

        // apply all pending changes in the cache
        for (Map.Entry<String, List<String>> entry : pendingTags.entrySet()) {
            String entryId = entry.getKey();
            List<String> tags = mTagsCache.get(entryId);
            if (tags == null)
                mTagsCache.put(entryId, tags = new ArrayList<>());
            tags.addAll(entry.getValue());
        }
    }

    private void addAliasesToEntry(String aliases, String entryId, Map<String, List<String>> pendingTags) {
        //add aliases only if they haven't overridden by the user (not in db)
        if (!mTagsCache.containsKey(entryId)) {
            //aliases.replace(",", " ")
            String[] arr = aliases.split(",");
            List<String> tags = pendingTags.get(entryId);
            if (tags == null)
                pendingTags.put(entryId, tags = new ArrayList<>());
            tags.addAll(Arrays.asList(arr));
        }
    }

    private String getApp(PackageManager pm, String action) {
        Intent lookingFor = new Intent(action, null);
        return getApp(pm, lookingFor);
    }

    private String getAppByCategory(PackageManager pm, String category) {
        Intent lookingFor = new Intent(Intent.ACTION_MAIN, null);
        lookingFor.addCategory(category);
        return getApp(pm, lookingFor);
    }

    private String getApp(PackageManager pm, Intent lookingFor) {
        List<ResolveInfo> list = pm.queryIntentActivities(lookingFor, 0);
        if (list.size() == 0) {
            return null;
        } else {
            String packageName = list.get(0).activityInfo.applicationInfo.packageName;
            String className = list.get(0).activityInfo.name;

            UserHandleCompat user = UserHandleCompat.CURRENT_USER;
            return AppEntry.generateAppId(packageName, className, user);
        }
    }

    private String getClockApp(PackageManager pm) {
        Intent alarmClockIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);

        // Known clock implementations
        // See http://stackoverflow.com/questions/3590955/intent-to-launch-the-clock-application-on-android
        String[][] clockImpls = {
            // Nexus
            {"com.android.deskclock", "com.android.deskclock.DeskClock"},
            // Samsung
            {"com.sec.android.app.clockpackage", "com.sec.android.app.clockpackage.ClockPackage"},
            // HTC
            {"com.htc.android.worldclock", "com.htc.android.worldclock.WorldClockTabControl"},
            // Standard Android
            {"com.android.deskclock", "com.android.deskclock.AlarmClock"},
            // New Android versions
            {"com.google.android.deskclock", "com.android.deskclock.AlarmClock"},
            // Froyo
            {"com.google.android.deskclock", "com.android.deskclock.DeskClock"},
            // Motorola
            {"com.motorola.blur.alarmclock", "com.motorola.blur.alarmclock.AlarmClock"},
            // Sony
            {"com.sonyericsson.organizer", "com.sonyericsson.organizer.Organizer_WorldClock"},
            // ASUS Tablets
            {"com.asus.deskclock", "com.asus.deskclock.DeskClock"}
        };

        UserHandleCompat user = UserHandleCompat.CURRENT_USER;
        for (String[] clockImpl : clockImpls) {
            String packageName = clockImpl[0];
            String className = clockImpl[1];
            try {
                ComponentName cn = new ComponentName(packageName, className);

                pm.getActivityInfo(cn, PackageManager.GET_META_DATA);
                alarmClockIntent.setComponent(cn);

                return AppEntry.generateAppId(cn, user);
            } catch (PackageManager.NameNotFoundException ignored) {
                // Try next suggestion, this one does not exists on the phone.
            }
        }

        return null;
    }

    public void setTags(EntryWithTags entry, Set<String> tags) {
        if (tags == null || tags.isEmpty()) {
            ArrayList<String> tagsToRemove = new ArrayList<>(getTags(entry.id));
            for (String tag : tagsToRemove)
                removeTag(entry.id, tag);
        } else {
            List<String> oldTags = DBHelper.loadTags(getContext(), entry.id);

            // tags that need to be removed
            {
                ArrayList<String> tagsToRemove = new ArrayList<>();
                for (String tag : oldTags)
                    if (!tags.contains(tag))
                        tagsToRemove.add(tag);
                for (String tag : tagsToRemove)
                    removeTag(entry.id, tag);
            }

            // add new tags
            for (String tag : tags) {
                if (oldTags.contains(tag))
                    continue;
                addTag(entry, tag);
            }
        }
        entry.setTags(getTags(entry.id));
    }

    public boolean renameTag(String tagName, String newName) {
        // rename tags from mTagsCache
        DataHandler dataHandler = mApplication.getDataHandler();
        for (Map.Entry<String, List<String>> entry : mTagsCache.entrySet()) {
            int pos = entry.getValue().indexOf(tagName);
            if (pos >= 0) {
                entry.getValue().set(pos, newName);
                EntryItem entryItem = dataHandler.getPojo(entry.getKey());
                if (entryItem instanceof EntryWithTags)
                    ((EntryWithTags) entryItem).setTags(entry.getValue());
            }
        }

        // rename tags from tags menu
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        SharedPreferences.Editor editor = pref.edit();
        HashSet<String> tagsMenuSet = new HashSet<>(pref.getStringSet("tags-menu-list", Collections.emptySet()));
        if (tagsMenuSet.remove(tagName)) {
            tagsMenuSet.add(newName);
            editor.putStringSet("tags-menu-list", tagsMenuSet);
        }
        int order = -1;
        HashSet<String> tagsMenuOrderSet = new HashSet<>(pref.getStringSet("tags-menu-order", Collections.emptySet()));
        for (Iterator<String> iterator = tagsMenuOrderSet.iterator(); iterator.hasNext(); ) {
            String orderedValue = iterator.next();
            String value = PrefOrderedListHelper.getOrderedValueName(orderedValue);
            if (value.equals(tagName)) {
                order = PrefOrderedListHelper.getOrderedValueIndex(orderedValue);
                iterator.remove();
                break;
            }
        }
        if (order >= 0) {
            tagsMenuOrderSet.add(PrefOrderedListHelper.makeOrderedValue(newName, order));
            editor.putStringSet("tags-menu-order", tagsMenuOrderSet);
        }

        editor.apply();

        boolean changeMade = false;
        // rename sorted tags from favorites
        TagsProvider tagsProvider = dataHandler.getTagsProvider();
        ModProvider modProvider = dataHandler.getModProvider();
        List<EntryItem> favList = modProvider != null ? modProvider.getPojos() : null;
        if (favList != null) {
            for (EntryItem item : favList) {
                if (item instanceof TagSortEntry && item.getName().equals(tagName)) {
                    TagSortEntry tagSortEntry = (TagSortEntry) item;
                    String newTagId = TagEntry.SCHEME + TagSortEntry.getTagSortOrder(tagSortEntry.id) + newName;
                    TagEntry newEntry = tagsProvider != null ? tagsProvider.findById(newTagId) : null;
                    if (newEntry == null) {
                        newEntry = TagsProvider.newTagEntryCheckId(newTagId);
                    }
                    if (newEntry == null) {
                        Log.e(TAG, "Can't change sort order from `" + tagSortEntry.id + "` to invalid tag id `" + newTagId + "`");
                        continue;
                    }
                    if (DBHelper.changeTagSort(getContext(), tagSortEntry, newEntry) > 0) {
                        changeMade = true;
                    }
                }
            }
        }

        // rename tag from favorites
        TagEntry tagEntry = null;
        TagEntry newEntry = null;
        if (tagsProvider != null) {
            tagEntry = tagsProvider.getTagEntry(tagName);
            if (tagEntry.hasCustomIcon()) {
                newEntry = tagsProvider.getTagEntry(newName);
            }
        }

        // rename tags from database
        if (DBHelper.renameTag(getContext(), tagName, newName, tagEntry, newEntry) > 0) {
            changeMade = true;
        }

        if (changeMade) {
            if (tagsProvider != null)
                tagsProvider.setDirty();
            if (modProvider != null)
                modProvider.setDirty();
        }

        return changeMade;
    }

    /**
     * Replace the id of the current TagEntry with the new one. The tag name should remain
     *
     * @param tagId    id of the TagEntry (or TagSortEntry) you want to sort
     * @param newTagId id on the TagSortEntry
     * @return true if at least one entry was renamed (sort order and id are linked)
     */
    public boolean changeTagSort(String tagId, String newTagId) {
        DataHandler dataHandler = mApplication.getDataHandler();
        // change tag id from favorites
        TagEntry tagEntry = null;
        TagEntry newEntry = null;
        TagsProvider tagsProvider = dataHandler.getTagsProvider();
        if (tagsProvider != null) {
            tagEntry = tagsProvider.findById(tagId);
            newEntry = tagsProvider.findById(newTagId);
        }
        if (tagEntry == null)
            tagEntry = TagsProvider.newTagEntryCheckId(tagId);
        if (tagEntry == null) {
            Log.e(TAG, "Can't change sort order of invalid tag id `" + tagId + "`");
            return false;
        }
        if (newEntry == null)
            newEntry = TagsProvider.newTagEntryCheckId(newTagId);
        if (newEntry == null) {
            Log.e(TAG, "Can't change sort order from `" + tagId + "` to invalid tag id `" + newTagId + "`");
            return false;
        }

        // rename tags from database
        return DBHelper.changeTagSort(getContext(), tagEntry, newEntry) > 0;
    }

    /**
     * Remove all tags from the Entry.
     * We keep the DB as is, maybe later we'll reinstall the app.
     *
     * @param entryId what Entry
     */
    public void removeAllTags(String entryId) {
        // remove from cache
        List<String> tags = mTagsCache.remove(entryId);
        // remove from DB
//        if (tags != null) {
//            for (String tag : tags)
//                DBHelper.removeTag(getContext(), tag, entryId);
//        }
    }
}
