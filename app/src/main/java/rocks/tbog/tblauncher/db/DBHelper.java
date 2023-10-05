package rocks.tbog.tblauncher.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.utils.PrefCache;

public class DBHelper {
    private static final String TAG = DBHelper.class.getSimpleName();
    private static DB database = null;
    private static final String[] TABLE_COLUMNS_APPS = new String[]{"_id", "display_name", "component_name", "custom_flags"};//, "custom_icon", "cached_icon"};
    private static final String[] TABLE_APPS_CUSTOM_ICON = new String[]{"custom_icon"};
    private static final String[] TABLE_APPS_CACHED_ICON = new String[]{"cached_icon"};
    private static final String[] TABLE_MODS_CUSTOM_ICON = new String[]{"custom_icon"};
    private static final String[] TABLE_COLUMNS_MODS = new String[]{"record", "position", "custom_flags", "name"};//, "custom_icon"};
    private static final String[] TABLE_COLUMNS_SHORTCUTS = new String[]{"_id", "name", "package", "info_data", "icon_png", "custom_flags"};
    private static final String[] TABLE_COLUMNS_SHORTCUTS_NO_ICON = new String[]{"_id", "name", "package", "info_data", "custom_flags"};

    private DBHelper() {
    }

    private static SQLiteDatabase getDatabase(Context context) {
        if (database == null) {
            database = new DB(context);
        }
        return database.getReadableDatabase();
    }

    private static ArrayList<ValuedHistoryRecord> readCursor(Cursor cursor) {
        cursor.moveToFirst();

        ArrayList<ValuedHistoryRecord> records = new ArrayList<>(cursor.getCount());
        while (!cursor.isAfterLast()) {
            ValuedHistoryRecord entry = new ValuedHistoryRecord();

            entry.record = cursor.getString(0);
            entry.value = cursor.getLong(1);

            records.add(entry);
            cursor.moveToNext();
        }
        cursor.close();

        return records;
    }

    /**
     * Insert new item into history
     *
     * @param context android context
     * @param query   query to insert
     * @param record  record to insert
     */
    public static void insertHistory(Context context, String query, String record) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("query", query);
        values.put("record", record);
        values.put("timeStamp", System.currentTimeMillis());
        long rowId = db.insert("history", null, values);
        Log.d(TAG, "insertHistory rowId " + rowId);

        if (Math.random() <= 0.005) {
            // Roughly every 200 inserts, clean up the history of items older than 3 months
            long monthsAgo = 7776000000L; // 1000 * 60 * 60 * 24 * 30 * 3;
            db.delete("history", "timeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() - monthsAgo)});
            // And vacuum the DB for speed
            db.execSQL("VACUUM");
        }
    }

    public static void removeFromHistory(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "record = ?", new String[]{record});
    }

    public static void clearHistory(Context context) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("history", "", null);
    }

    public static void setHistory(Context context, Collection<ValuedHistoryRecord> history) {
        SQLiteDatabase db = getDatabase(context);
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS \"history\"");
            database.createHistory(db);
            ContentValues values = new ContentValues(3);
            for (ValuedHistoryRecord rec : history) {
                values.put("record", rec.record);
                values.put("query", rec.name);
                values.put("timeStamp", rec.value);
                db.insert("history", null, values);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private static Cursor getHistoryByFrecency(SQLiteDatabase db, int limit) {
        // Since smart history sql uses a group by we don't use the whole history but a limit of recent apps
        int historyWindowSize = limit * 30;

        // order history based on frequency * recency
        // frequency = #launches_for_app / #all_launches
        // recency = 1 / position_of_app_in_normal_history
        String sql = "SELECT record, count(*) FROM " +
            " (" +
            "   SELECT * FROM history ORDER BY _id DESC " +
            "   LIMIT " + historyWindowSize + "" +
            " ) small_history " +
            " GROUP BY record " +
            " ORDER BY " +
            "   count(*) * 1.0 / (select count(*) from history LIMIT " + historyWindowSize + ") / ((SELECT _id FROM history ORDER BY _id DESC LIMIT 1) - max(_id) + 0.001) " +
            " DESC " + ((limit > 0 && limit < Integer.MAX_VALUE) ? (" LIMIT " + limit) : "");
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryByFrequency(SQLiteDatabase db, int limit) {
        // order history based on frequency
        String sql = "SELECT record, count(*) FROM history" +
            " GROUP BY record " +
            " ORDER BY count(*) DESC " +
            ((limit > 0 && limit < Integer.MAX_VALUE) ? (" LIMIT " + limit) : "");
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryByRecency(SQLiteDatabase db, int limit) {
        return db.query(true, "history", new String[]{"record", "1"}, null, null,
            null, null, "_id DESC", Integer.toString(limit));
    }

    /**
     * Get the most used history items adaptively based on a set period of time
     *
     * @param db    The SQL db
     * @param hours How many hours back we want to test frequency against
     * @param limit Maximum result size
     * @return Cursor
     */
    private static Cursor getHistoryByAdaptive(SQLiteDatabase db, int hours, int limit) {
        // order history based on frequency
        String sql = "SELECT record, count(*) FROM history " +
            "WHERE timeStamp >= 0 " +
            "AND timeStamp >" + (System.currentTimeMillis() - (hours * 3600000)) +
            " GROUP BY record " +
            " ORDER BY count(*) DESC " +
            ((limit > 0 && limit < Integer.MAX_VALUE) ? (" LIMIT " + limit) : "");
        return db.rawQuery(sql, null);
    }

    @NonNull
    static ArrayList<ValuedHistoryRecord> getHistoryRaw(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<ValuedHistoryRecord> records;
        try (Cursor cursor = db.query("history", new String[]{"record", "query", "timeStamp"}, null, null, null, null, "\"_id\" ASC")) {

            records = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                ValuedHistoryRecord entry = new ValuedHistoryRecord();

                entry.record = cursor.getString(0);
                entry.name = cursor.getString(1);
                entry.value = cursor.getLong(2);

                records.add(entry);
            }
        }
        return records;
    }

    public enum HistoryMode {
        RECENCY,
        FRECENCY,
        FREQUENCY,
        ADAPTIVE,
    }

    /**
     * Retrieve previous query history
     *
     * @param context android context
     * @param limit   max number of items to retrieve
     * @return records with number of use
     */
    @NonNull
    public static List<ValuedHistoryRecord> getHistory(Context context, int limit, HistoryMode historyMode) {
        List<ValuedHistoryRecord> records = null;

        SQLiteDatabase db = getDatabase(context);

        Cursor cursor = null;
        switch (historyMode) {
            case FRECENCY:
                cursor = getHistoryByFrecency(db, limit);
                break;
            case FREQUENCY:
                cursor = getHistoryByFrequency(db, limit);
                break;
            case ADAPTIVE:
                cursor = getHistoryByAdaptive(db, PrefCache.getHistoryAdaptive(context), limit);
                break;
            case RECENCY:
                cursor = getHistoryByRecency(db, limit);
                break;
        }
        if (cursor != null) {
            records = readCursor(cursor);
            cursor.close();
        }
        if (records == null)
            records = Collections.emptyList();

        return records;
    }


    /**
     * Retrieve history size
     *
     * @param context android context
     * @return total number of use for the application
     */
    public static int getHistoryLength(Context context) {
        SQLiteDatabase db = getDatabase(context);

        int historyLength = 0;
        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        try (Cursor cursor = db.query(false, "history", new String[]{"COUNT(*)"},
            null, null, null, null, null, null)) {

            cursor.moveToFirst();
            historyLength = cursor.getInt(0);
        }
        return historyLength;
    }

    /**
     * Retrieve previously selected items for the query
     *
     * @param context android context
     * @param query   query to run
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getPreviousResultsForQuery(Context context,
                                                                            String query) {
        ArrayList<ValuedHistoryRecord> records;
        SQLiteDatabase db = getDatabase(context);

        // Cursor query (String table, String[] columns, String selection,
        // String[] selectionArgs, String groupBy, String having, String
        // orderBy)
        try (Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
            "query LIKE ?", new String[]{query + "%"}, "record", null, "COUNT(*) DESC", "10")) {
            records = readCursor(cursor);
        }
        return records;
    }

    public static boolean insertApp(Context context, AppEntry entry) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("name", entry.getName());
        values.put("component_name", entry.getUserComponentName());

        return -1 != db.insert("apps", null, values);
    }

    public static boolean insertShortcut(@NonNull Context context, @NonNull ShortcutRecord shortcut) {
        SQLiteDatabase db = getDatabase(context);
        // Do not add duplicate shortcuts
        try (Cursor cursor = db.query("shortcuts", new String[]{"package", "info_data"},
            "package = ? AND info_data = ?", new String[]{shortcut.packageName, shortcut.infoData}, null, null, null, null)) {
            // cursor contains duplicates
            if (cursor.getCount() > 0) {
                return false;
            }
        }

        ContentValues values = new ContentValues();
        values.put("name", shortcut.displayName);
        values.put("package", shortcut.packageName);
        values.put("info_data", shortcut.infoData);
        values.put("icon_png", shortcut.iconPng);
        values.put("custom_flags", shortcut.getFlagsDB());

        return -1 != db.insert("shortcuts", null, values);
    }

    public static void removeShortcut(@NonNull Context context, @NonNull ShortcutEntry shortcut) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("shortcuts", "package = ? AND info_data = ?", new String[]{shortcut.packageName, shortcut.shortcutData});
    }

    public static void removeShortcut(@NonNull Context context, long dbId) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("shortcuts", "_id=?", new String[]{String.valueOf(dbId)});
    }

    public static void renameShortcut(@NonNull Context context, @NonNull ShortcutEntry shortcut, String newName) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE \"shortcuts\" SET \"name\"=? WHERE \"package\"=? AND \"info_data\"=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, newName);
            statement.bindString(2, shortcut.packageName);
            statement.bindString(3, shortcut.shortcutData);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Update name count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "rename shortcut", e);
        }
    }

    /**
     * Retrieve a list of all shortcuts for current package name, without icons.
     * Useful when we remove an app and need to also remove the shortcuts for it.
     */
    @NonNull
    public static List<ShortcutRecord> getShortcutsNoIcons(@NonNull Context context, @NonNull String packageName) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<ShortcutRecord> records;
        try (Cursor cursor = db.query("shortcuts",
            TABLE_COLUMNS_SHORTCUTS_NO_ICON,
            "package = ?", new String[]{packageName},
            null, null, null)) {

            records = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                ShortcutRecord entry = new ShortcutRecord();

                entry.dbId = cursor.getLong(0);
                entry.displayName = cursor.getString(1);
                entry.packageName = cursor.getString(2);
                entry.infoData = cursor.getString(3);
                entry.flags = cursor.getInt(4);

                records.add(entry);
            }
        }

        return records;
    }

    /**
     * Retrieve a list of all shortcuts, without icons.
     */
    @NonNull
    public static ArrayList<ShortcutRecord> getShortcutsNoIcons(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<ShortcutRecord> records;
        try (Cursor cursor = db.query("shortcuts",
            TABLE_COLUMNS_SHORTCUTS_NO_ICON,
            null, null, null, null, null)) {

            records = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                ShortcutRecord entry = new ShortcutRecord();

                entry.dbId = cursor.getLong(0);
                entry.displayName = cursor.getString(1);
                entry.packageName = cursor.getString(2);
                entry.infoData = cursor.getString(3);
                entry.flags = cursor.getInt(4);

                records.add(entry);
            }
        }

        return records;
    }

    @Nullable
    public static byte[] getShortcutIcon(@NonNull Context context, long dbId) {
        SQLiteDatabase db = getDatabase(context);

        byte[] iconBlob = null;
        try (Cursor cursor = db.query("shortcuts", new String[]{"icon_png"},
            "_id = ?", new String[]{Long.toString(dbId)},
            null, null, null)) {

            if (cursor.moveToNext()) {
                iconBlob = cursor.getBlob(0);
            }
        }
        return iconBlob;
    }

    /**
     * Remove shortcuts for a given package name
     */
    public static void removeShortcuts(Context context, String packageName) {
        SQLiteDatabase db = getDatabase(context);

        // remove shortcuts
        db.delete("shortcuts", "package = ?", new String[]{packageName});
    }

    public static void removeAllShortcuts(Context context) {
        SQLiteDatabase db = getDatabase(context);
        // delete whole table
        db.delete("shortcuts", null, null);
        //db.execSQL("vacuum"); //https://www.sqlitetutorial.net/sqlite-vacuum/
    }

    /**
     * Insert new tag for given {@link rocks.tbog.tblauncher.entry.EntryItem}
     *
     * @param context android context
     * @param tag     tag name to insert
     * @param entry   EntryItem
     */
    public static void addTag(Context context, String tag, EntryItem entry) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("tag", tag);
        values.put("record", entry.id);
        db.insert("tags", null, values);
    }


    /**
     * Delete a tag from a given {@link rocks.tbog.tblauncher.entry.EntryItem}
     *
     * @param context android context
     * @param tag     tag name to remove
     * @param entryId EntryItem.id
     * @return number of records affected
     */
    public static int removeTag(Context context, String tag, String entryId) {
        SQLiteDatabase db = getDatabase(context);

        return db.delete("tags", "tag = ? AND record = ?", new String[]{tag, entryId});
    }

    /**
     * @param context android context
     * @param tagName what tag to rename
     * @param newName the new name of the tag
     * @return number of records affected
     */
    public static int renameTag(Context context, String tagName, String newName, @Nullable TagEntry tagEntry, @Nullable TagEntry newEntry) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();

        if (tagEntry != null && newEntry != null) {
            values.put("record", newEntry.id);
            int count = db.updateWithOnConflict("favorites", values, "record = ?", new String[]{tagEntry.id}, SQLiteDatabase.CONFLICT_REPLACE);
            if (count != 1) {
                Log.e(TAG, "Update favorites in rename tag; count = " + count);
            }
        }

        values.clear();
        values.put("tag", newName);

        return db.update("tags", values, "tag = ?", new String[]{tagName});
    }

    /**
     * @param context android context
     * @param tagEntry what tag to modify
     * @param newEntry the new tag sort
     * @return number of records affected
     */
    public static int changeTagSort(Context context, @NonNull TagEntry tagEntry, @NonNull TagEntry newEntry) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("record", newEntry.id);
        int count = db.updateWithOnConflict("favorites", values, "record = ?", new String[]{tagEntry.id}, SQLiteDatabase.CONFLICT_IGNORE);
        if (count != 1) {
            Log.e(TAG, "Update favorites in rename tag; count = " + count);
        }
        return count;
    }

    /**
     * @param context android context
     * @return HashMap with EntryItem id as key and an ArrayList of tags for each
     */
    @NonNull
    public static Map<String, List<String>> loadTags(Context context) {
        Map<String, List<String>> records;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("tags", new String[]{"record", "tag"},
            null, null, null, null, null)) {
            records = new HashMap<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String tag = cursor.getString(1);
                List<String> tagList = records.get(id);
                if (tagList == null)
                    records.put(id, tagList = new ArrayList<>());
                tagList.add(tag);
            }
        }
        return records;
    }

    /**
     * Drop all previous tags and set the ones provided in the map
     *
     * @param context android context
     * @param tagsMap map with tag names as key and a list of ids as value
     */
    public static void setTagsMap(Context context, Map<String, ? extends Collection<String>> tagsMap) {
        SQLiteDatabase db = getDatabase(context);
        db.beginTransaction();
        try {
            //db.delete("tags", null, null);
            db.execSQL("DROP TABLE IF EXISTS \"tags\"");
            database.createTags(db);
            ContentValues values = new ContentValues(2);
            for (Map.Entry<String, ? extends Collection<String>> entry : tagsMap.entrySet()) {
                values.put("tag", entry.getKey());
                for (String record : entry.getValue()) {
                    values.put("record", record);
                    db.insert("tags", null, values);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Add all tags from the provided map values
     *
     * @param context android context
     * @param tags    map with record names as key and a list of tags as value
     */
    public static void addTags(Context context, Map<String, ? extends Collection<String>> tags) {
        SQLiteDatabase db = getDatabase(context);
        db.beginTransaction();
        try {
            ContentValues values = new ContentValues(2);
            for (Map.Entry<String, ? extends Collection<String>> entry : tags.entrySet()) {
                values.put("record", entry.getKey());
                for (String tag : entry.getValue()) {
                    values.put("tag", tag);
                    db.insert("tags", null, values);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * @param context android context
     * @return List of all tags
     */
    @NonNull
    public static List<String> loadTagList(Context context) {
        List<String> tags;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("tags", new String[]{"tag"},
            null, null, "tag", null, null)) {
            tags = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String tag = cursor.getString(0);
                tags.add(tag);
            }
        }
        return tags;
    }

    /**
     * @param context android context
     * @param record  the id of the app
     * @return HashMap with EntryItem id as key and an ArrayList of tags for each
     */
    @NonNull
    public static List<String> loadTags(Context context, String record) {
        List<String> tagList;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("tags", new String[]{"tag"},
            "record=?", new String[]{record}, null, null, null)) {
            tagList = new ArrayList<>(cursor.getCount());
            while (cursor.moveToNext()) {
                String tag = cursor.getString(0);
                tagList.add(tag);
            }
        }
        return tagList;
    }

    @NonNull
    public static HashMap<String, AppRecord> getAppsData(Context context) {
        HashMap<String, AppRecord> records;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor cursor = db.query("apps", TABLE_COLUMNS_APPS,
            null, null, null, null, null)) {
            records = new HashMap<>(cursor.getCount());
            while (cursor.moveToNext()) {
                AppRecord entry = new AppRecord();

                entry.dbId = cursor.getLong(0);
                entry.displayName = cursor.getString(1);
                entry.componentName = cursor.getString(2);
                entry.flags = cursor.getInt(3);

                records.put(entry.componentName, entry);
            }
        }

        return records;
    }

    public static void insertOrUpdateApps(Context context, ArrayList<AppRecord> appRecords) {
        SQLiteDatabase db = getDatabase(context);
        db.beginTransaction();
        ContentValues values = new ContentValues();
        try {
            for (AppRecord app : appRecords) {
                values.put("display_name", app.displayName);
                values.put("component_name", app.componentName);
                values.put("custom_flags", app.getFlagsDB());
                if (app.dbId == -1) {
                    // insert
                    db.insertWithOnConflict("apps", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                } else {
                    // update
                    db.updateWithOnConflict("apps", values, "_id=" + app.dbId, null, SQLiteDatabase.CONFLICT_IGNORE);
                }
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static void deleteApps(Context context, ArrayList<AppRecord> appRecords) {
        SQLiteDatabase db = getDatabase(context);
        String[] list = new String[appRecords.size()];
        for (int i = 0; i < appRecords.size(); i++) {
            AppRecord rec = appRecords.get(i);
            list[i] = String.valueOf(rec.dbId);
        }
        String whereClause = String.format("_id IN (%s)", TextUtils.join(",", Collections.nCopies(list.length, "?")));
        db.delete("apps", whereClause, list);
    }

    public static void setCustomAppName(Context context, String componentName, String newName) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE apps SET display_name=?,custom_flags=custom_flags|? WHERE component_name=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, newName);
            statement.bindLong(2, AppRecord.FLAG_CUSTOM_NAME);
            statement.bindString(3, componentName);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Update name; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app name", e);
        }
    }

    public static boolean setAppHidden(Context context, String componentName) {
        boolean ret = false;
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE \"apps\" SET \"custom_flags\"=\"custom_flags\"|? WHERE \"component_name\"=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, AppRecord.FLAG_APP_HIDDEN);
            statement.bindString(2, componentName);
            int count = statement.executeUpdateDelete();
            if (count == 1) {
                ret = true;
            } else {
                Log.e(TAG, "Update name; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app name", e);
        }
        return ret;
    }

    public static boolean removeAppHidden(Context context, String componentName) {
        boolean ret = false;
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE \"apps\" SET \"custom_flags\"=\"custom_flags\"&~? WHERE \"component_name\"=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, AppRecord.FLAG_APP_HIDDEN);
            statement.bindString(2, componentName);
            int count = statement.executeUpdateDelete();
            if (count == 1) {
                ret = true;
            } else {
                Log.e(TAG, "Update name; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app name", e);
        }
        return ret;
    }

    public static void setCustomStaticEntryName(Context context, String entryId, String newName) {
        SQLiteDatabase db = getDatabase(context);
        int count;
        String sql = "UPDATE favorites SET name=?,custom_flags=custom_flags|? WHERE record=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, newName);
            statement.bindLong(2, ModRecord.FLAG_CUSTOM_NAME);
            statement.bindString(3, entryId);
            count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Update name for `" + entryId + "`; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Update custom static entry name for `" + entryId + "`", e);
            count = -1;
        }
        if (count < 1) {
            ContentValues values = new ContentValues();
            values.put("record", entryId);
            values.put("position", "");
            values.put("name", newName);
            values.put("custom_flags", ModRecord.FLAG_CUSTOM_NAME);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public static void removeCustomAppName(Context context, String componentName, String defaultName) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE apps SET display_name=?,custom_flags=custom_flags&~? WHERE component_name=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindString(1, defaultName);
            statement.bindLong(2, AppRecord.FLAG_CUSTOM_NAME);
            statement.bindString(3, componentName);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Reset name; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app name", e);
        }
    }

    @Nullable
    private static AppRecord getAppRecord(SQLiteDatabase db, String componentName) {
        String[] selArgs = new String[]{componentName};
        try (Cursor cursor = db.query("apps", TABLE_COLUMNS_APPS,
            "component_name=?", selArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                AppRecord entry = new AppRecord();

                entry.dbId = cursor.getLong(0);
                entry.displayName = cursor.getString(1);
                entry.componentName = cursor.getString(2);
                entry.flags = cursor.getInt(3);

                return entry;
            }
        }
        return null;
    }

    public static boolean setCachedAppIcon(Context context, String componentName, byte[] icon) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE apps SET cached_icon=? WHERE component_name=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindBlob(1, icon);
            statement.bindString(2, componentName);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "setCachedAppIcon; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update cached app icon `" + componentName + "`", e);
            return false;
        }
        return true;
    }

    public static AppRecord setCustomAppIcon(Context context, String componentName, byte[] icon) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE apps SET custom_flags=custom_flags|?, custom_icon=? WHERE component_name=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, AppRecord.FLAG_CUSTOM_ICON);
            statement.bindBlob(2, icon);
            statement.bindString(3, componentName);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Update icon; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app icon `" + componentName + "`", e);
        }

        return getAppRecord(db, componentName);
    }

    public static void setCustomStaticEntryIcon(Context context, String entryId, byte[] icon) {
        SQLiteDatabase db = getDatabase(context);
        int count;
        String sql = "UPDATE favorites SET custom_flags=custom_flags|?, custom_icon=? WHERE record=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, ModRecord.FLAG_CUSTOM_ICON);
            statement.bindBlob(2, icon);
            statement.bindString(3, entryId);
            count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.w(TAG, "Update icon for `" + entryId + "`; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Update custom fav icon `" + entryId + "`", e);
            count = -1;
        }

        if (count < 1) {
            ContentValues values = new ContentValues();
            values.put("record", entryId);
            values.put("position", "");
            values.put("custom_icon", icon);
            values.put("custom_flags", ModRecord.FLAG_CUSTOM_ICON);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        }
    }

    public static AppRecord removeCustomAppIcon(Context context, String componentName) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE apps SET custom_flags=custom_flags&~?, custom_icon=NULL WHERE component_name=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, AppRecord.FLAG_CUSTOM_ICON);
            statement.bindString(2, componentName);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Reset icon; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Insert or Update custom app name", e);
        }

        return getAppRecord(db, componentName);
    }

    public static void removeCustomStaticEntryIcon(Context context, String entryId) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE favorites SET custom_flags=custom_flags&~?, custom_icon=NULL WHERE record=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, ModRecord.FLAG_CUSTOM_ICON);
            statement.bindString(2, entryId);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Reset `" + entryId + "` icon; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Reset custom entry `" + entryId + "` icon", e);
        }
    }

    public static void removeCustomStaticEntryName(Context context, String entryId) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE favorites SET custom_flags=custom_flags&~?, name=NULL WHERE record=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, ModRecord.FLAG_CUSTOM_NAME);
            statement.bindString(2, entryId);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Reset `" + entryId + "` name; count = " + count);
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "Reset custom entry `" + entryId + "` name", e);
        }
    }

    @Nullable
    private static byte[] getAppIcon(Context context, String componentName, String[] dbColumn) {
        SQLiteDatabase db = getDatabase(context);
        String[] selArgs = new String[]{componentName};
        try (Cursor cursor = db.query("apps", dbColumn,
            "component_name=?", selArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getBlob(0);
            }
        }
        return null;
    }

    @Nullable
    public static byte[] getCachedAppIcon(Context context, String componentName) {
        return getAppIcon(context, componentName, TABLE_APPS_CACHED_ICON);
    }

    @Nullable
    public static byte[] getCustomAppIcon(Context context, String componentName) {
        return getAppIcon(context, componentName, TABLE_APPS_CUSTOM_ICON);
    }

    @Nullable
    public static byte[] getCustomFavIcon(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);
        String[] selArgs = new String[]{record};
        try (Cursor cursor = db.query("favorites", TABLE_MODS_CUSTOM_ICON,
            "record=?", selArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getBlob(0);
            }
        }
        return null;
    }

    public static void setMod(Context context, ModRecord fav) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("record", fav.record);
        values.put("position", fav.position);
        values.put("custom_flags", fav.getFlagsDB());

        int rows = db.update("favorites", values, "record=?", new String[]{fav.record});
        if (rows == 0)
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
//        try {
//            db.replaceOrThrow("favorites", null, values);
//        } catch (SQLException e) {
//            Log.e(TAG, "setFavorite " + fav.record);
//        }
    }

    public static void setMods(Context context, Collection<Pair<ModRecord, byte[]>> favRecords) {
        SQLiteDatabase db = getDatabase(context);
        db.beginTransaction();
        try {
            db.execSQL("DROP TABLE IF EXISTS \"favorites\"");
            database.createFavoritesTable(db, false);

            ContentValues values = new ContentValues();
            for (Pair<ModRecord, byte[]> pair : favRecords) {
                ModRecord fav = pair.first;
                byte[] icon = pair.second;
                values.put("record", fav.record);
                values.put("position", fav.position == null ? "" : fav.position);
                values.put("custom_flags", fav.getFlagsDB());
                values.put(TABLE_MODS_CUSTOM_ICON[0], icon);
                db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public static boolean removeMod(Context context, String record) {
        SQLiteDatabase db = getDatabase(context);

        if (0 == db.delete("favorites", "record=?", new String[]{record})) {
            Log.e(TAG, "removeFavorite " + record);
            return false;
        }
        return true;
    }

    @NonNull
    public static ArrayList<ModRecord> getMods(@NonNull Context context) {
        ArrayList<ModRecord> list;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor c = db.query("favorites", TABLE_COLUMNS_MODS, null, null, null, null, "position")) {
            list = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                ModRecord fav = new ModRecord();
                fav.record = c.getString(0);
                fav.position = c.getString(1);
                fav.setFlags(c.getInt(2));
                fav.displayName = c.getString(3);
                list.add(fav);
            }
        }
        return list;
    }

    public static boolean updateQuickListPosition(@NonNull Context context, String record, String position) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE \"favorites\" SET \"custom_flags\"=(\"custom_flags\"|?), \"position\"=? WHERE \"record\"=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, ModRecord.FLAG_SHOW_IN_QUICK_LIST);
            statement.bindString(2, position);
            statement.bindString(3, record);
            int count = statement.executeUpdateDelete();
            if (count != 1) {
                Log.e(TAG, "Update position; count = " + count);
                return false;
            }
            statement.close();
        } catch (Exception e) {
            Log.e(TAG, "set flag and position", e);
            return false;
        }
        return true;
    }

    public static ArrayList<WidgetRecord> getWidgets(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);
        ArrayList<WidgetRecord> list;
        try (Cursor c = db.query("widgets", new String[]{"appWidgetId", "properties"}, null, null, null, null, null)) {
            list = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                int appWidgetId = c.getInt(0);
                String properties = c.getString(1);
                WidgetRecord rec = WidgetRecord.loadFromDB(appWidgetId, properties);
                list.add(rec);
            }
        }
        return list;
    }

    public static void addWidget(@NonNull Context context, WidgetRecord rec) {
        SQLiteDatabase db = getDatabase(context);
        ContentValues values = new ContentValues();
        values.put("appWidgetId", rec.appWidgetId);
        values.put("properties", rec.packedProperties());
        db.insert("widgets", null, values);
    }

    public static void removeWidget(@NonNull Context context, int appWidgetId) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("widgets", "appWidgetId=?", new String[]{String.valueOf(appWidgetId)});
    }

    public static void removeWidgetPlaceholder(@NonNull Context context, int appWidgetId, String provider) {
        SQLiteDatabase db = getDatabase(context);
        //TODO: escape the provider
        String[] whereArgs = new String[]{String.valueOf(appWidgetId), "<provider>" + provider + "</provider>"};
        db.delete("widgets", "appWidgetId=? AND properties LIKE '%' || ? || '%'", whereArgs);
    }

    public static void setWidgetProperties(@NonNull Context context, WidgetRecord rec) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("properties", rec.packedProperties());

        int affectedRows = db.update("widgets", values, "appWidgetId=?", new String[]{String.valueOf(rec.appWidgetId)});
        if (affectedRows == 0)
            addWidget(context, rec);
    }
}
