package rocks.tbog.tblauncher.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.ShortcutEntry;

public class DBHelper {
    private static final String TAG = DBHelper.class.getSimpleName();
    private static SQLiteDatabase database = null;
    private static final String[] TABLE_COLUMNS_APPS = new String[]{"_id", "display_name", "component_name", "custom_flags"};//, "custom_icon"};
    private static final String[] TABLE_APPS_CUSTOM_ICON = new String[]{"custom_icon"};
    private static final String[] TABLE_COLUMNS_FAVORITES = new String[]{"record", "position", "custom_flags"};

    private DBHelper() {
    }

    private static SQLiteDatabase getDatabase(Context context) {
        if (database == null) {
            database = new DB(context).getReadableDatabase();
        }
        return database;
    }

    private static ArrayList<ValuedHistoryRecord> readCursor(Cursor cursor) {
        cursor.moveToFirst();

        ArrayList<ValuedHistoryRecord> records = new ArrayList<>(cursor.getCount());
        while (!cursor.isAfterLast()) {
            ValuedHistoryRecord entry = new ValuedHistoryRecord();

            entry.record = cursor.getString(0);
            entry.value = cursor.getInt(1);

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
        db.insert("history", null, values);

        if (Math.random() <= 0.005) {
            // Roughly every 200 inserts, clean up the history of items older than 3 months
            long twoMonthsAgo = 7776000000L; // 1000 * 60 * 60 * 24 * 30 * 3;
            db.delete("history", "timeStamp < ?", new String[]{Long.toString(System.currentTimeMillis() - twoMonthsAgo)});
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
                " DESC " +
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    private static Cursor getHistoryByFrequency(SQLiteDatabase db, int limit) {
        // order history based on frequency
        String sql = "SELECT record, count(*) FROM history" +
                " GROUP BY record " +
                " ORDER BY count(*) DESC " +
                " LIMIT " + limit;
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
                " LIMIT " + limit;
        return db.rawQuery(sql, null);
    }

    /**
     * Retrieve previous query history
     *
     * @param context     android context
     * @param limit       max number of items to retrieve
     * @param sortHistory sort history entries alphabetically
     * @return records with number of use
     */
    public static ArrayList<ValuedHistoryRecord> getHistory(Context context, int limit, String historyMode, boolean sortHistory) {
        ArrayList<ValuedHistoryRecord> records;

        SQLiteDatabase db = getDatabase(context);

        Cursor cursor;
        switch (historyMode) {
            case "frecency":
                cursor = getHistoryByFrecency(db, limit);
                break;
            case "frequency":
                cursor = getHistoryByFrequency(db, limit);
                break;
            case "adaptive":
                cursor = getHistoryByAdaptive(db, 36, limit);
                break;
            default:
                cursor = getHistoryByRecency(db, limit);
                break;
        }

        records = readCursor(cursor);
        cursor.close();

        // sort history entries alphabetically
        if (sortHistory) {
            DataHandler dataHandler = TBApplication.getApplication(context).getDataHandler();

            for (ValuedHistoryRecord entry : records) {
                entry.name = dataHandler.getItemName(entry.record);
            }

            Collections.sort(records, new Comparator<ValuedHistoryRecord>() {
                @Override
                public int compare(ValuedHistoryRecord a, ValuedHistoryRecord b) {
                    return a.name.compareTo(b.name);
                }
            });
        }

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

        // Cursor query (boolean distinct, String table, String[] columns,
        // String selection, String[] selectionArgs, String groupBy, String
        // having, String orderBy, String limit)
        Cursor cursor = db.query(false, "history", new String[]{"COUNT(*)"}, null, null,
                null, null, null, null);

        cursor.moveToFirst();
        int historyLength = cursor.getInt(0);
        cursor.close();
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
        Cursor cursor = db.query("history", new String[]{"record", "COUNT(*) AS count"},
                "query LIKE ?", new String[]{query + "%"}, "record", null, "COUNT(*) DESC", "10");
        records = readCursor(cursor);
        cursor.close();
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
        Cursor cursor = db.query("shortcuts", new String[]{"package", "info_data"},
                "package = ? AND info_data = ?", new String[]{shortcut.packageName, shortcut.infoData}, null, null, null, null);
        // cursor contains duplicates
        if (cursor.getCount() > 0) {
            return false;
        }
        cursor.close();

        ContentValues values = new ContentValues();
        values.put("name", shortcut.displayName);
        values.put("package", shortcut.packageName);
        values.put("info_data", shortcut.infoData);
        values.put("icon_png", shortcut.iconPng);
        values.put("custom_flags", shortcut.getFlagsDB());

        db.insert("shortcuts", null, values);
        return true;
    }

    public static void removeShortcut(@NonNull Context context, @NonNull ShortcutEntry shortcut) {
        SQLiteDatabase db = getDatabase(context);
        db.delete("shortcuts", "package = ? AND info_data = ?", new String[]{shortcut.packageName, shortcut.shortcutData});
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
    public static List<ShortcutRecord> getShortcuts(@NonNull Context context, @NonNull String packageName) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<ShortcutRecord> records;
        try (Cursor cursor = db.query("shortcuts",
                new String[]{"_id", "name", "package", "info_data", "custom_flags"},
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
    public static ArrayList<ShortcutRecord> getShortcuts(@NonNull Context context) {
        SQLiteDatabase db = getDatabase(context);

        ArrayList<ShortcutRecord> records;
        try (Cursor cursor = db.query("shortcuts",
                new String[]{"_id", "name", "package", "info_data", "custom_flags"},
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

        Cursor cursor = db.query("shortcuts", new String[]{"icon_png"},
                "_id = ?", new String[]{Long.toString(dbId)},
                null, null, null);

        byte[] iconBlob = null;
        if (cursor.moveToNext()) {
            iconBlob = cursor.getBlob(0);
        }
        cursor.close();
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
     * @param entry   EntryItem
     */
    public static void removeTag(Context context, String tag, EntryItem entry) {
        SQLiteDatabase db = getDatabase(context);

        db.delete("tags", "tag = ? AND record = ?", new String[]{tag, entry.id});
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
     * @param context android context
     * @param record the id of the app
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
            Log.e(TAG, "Insert or Update custom app name", e);
        }

        return getAppRecord(db, componentName);
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

    public static byte[] getCustomAppIcon(Context context, String componentName) {
        SQLiteDatabase db = getDatabase(context);
        String[] selArgs = new String[]{componentName};
        try (Cursor cursor = db.query("apps", TABLE_APPS_CUSTOM_ICON,
                "component_name=?", selArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getBlob(0);
            }
        }
        return null;
    }

    public static void setFavorite(Context context, FavRecord fav) {
        SQLiteDatabase db = getDatabase(context);

        ContentValues values = new ContentValues();
        values.put("record", fav.record);
        values.put("position", fav.position);
        values.put("custom_flags", fav.flags);

//        int rows = db.update("favorites", values, "record=?", new String[]{fav.record});
//        if (rows == 0)
//            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        try {
            db.replaceOrThrow("favorites", null, values);
        } catch (SQLException e) {
            Log.e(TAG, "setFavorite " + fav.record);
        }
    }

    public static void removeFavorite(Context context, FavRecord fav) {
        SQLiteDatabase db = getDatabase(context);

        if (0 == db.delete("favorites", "record=?", new String[]{fav.record}))
            Log.e(TAG, "removeFavorite " + fav.record);
    }

    @NonNull
    public static ArrayList<FavRecord> getFavorites(@NonNull Context context) {
        ArrayList<FavRecord> list;
        SQLiteDatabase db = getDatabase(context);
        try (Cursor c = db.query("favorites", TABLE_COLUMNS_FAVORITES, null, null, null, null, "position")) {
            list = new ArrayList<>(c.getCount());
            while (c.moveToNext()) {
                FavRecord fav = new FavRecord();
                fav.record = c.getString(0);
                fav.position = c.getString(1);
                fav.flags = c.getInt(2);
                list.add(fav);
            }
        }
        return list;
    }

    public static boolean setQuickListPosition(@NonNull Context context, String record, String position) {
        SQLiteDatabase db = getDatabase(context);
        String sql = "UPDATE \"favorites\" SET \"custom_flags\"=(\"custom_flags\"|?), \"position\"=? WHERE \"record\"=?";
        try {
            SQLiteStatement statement = db.compileStatement(sql);
            statement.bindLong(1, FavRecord.FLAG_SHOW_IN_QUICK_LIST);
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
}
