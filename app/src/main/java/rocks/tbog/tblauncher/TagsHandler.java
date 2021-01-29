package rocks.tbog.tblauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

public class TagsHandler {
    private final TBApplication mApplication;
    // HashMap with EntryItem id as key and an ArrayList of tags for each
    private final HashMap<String, List<String>> mTagsCache = new HashMap<>();
    private boolean mIsLoaded = false;
    private final ArrayDeque<Runnable> mAfterLoadedTasks = new ArrayDeque<>(2);

    TagsHandler(TBApplication application) {
        mApplication = application;
        loadFromDB(false);
    }

    public void loadFromDB(boolean wait) {
        synchronized (this) {
            mIsLoaded = false;
        }
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
                // run and remove tasks
                Runnable task;
                while (null != (task = mAfterLoadedTasks.poll()))
                    task.run();
            }
        };
        if (wait) {
            load.run();
            apply.run();
        } else
            Utilities.runAsync((t) -> load.run(), (t) -> apply.run());
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

    public boolean removeTag(@Nullable EntryItem entry, String tag) {
        if (entry instanceof EntryWithTags) {
            if (removeTag(entry.id, tag)) {
                ((EntryWithTags) entry).setTags(getTags(entry.id));
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
            return AppEntry.SCHEME + user.getUserComponentName(packageName, className);
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

                return AppEntry.SCHEME + user.getUserComponentName(cn);
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
        return DBHelper.renameTag(getContext(), tagName, newName) > 0;
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
