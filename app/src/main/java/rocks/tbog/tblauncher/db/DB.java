package rocks.tbog.tblauncher.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import rocks.tbog.tblauncher.entry.FilterEntry;

class DB extends SQLiteOpenHelper {

    private final static String DB_NAME = "kiss.s3db";
    private final static int DB_VERSION = 13;

    DB(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        createHistory(database);
        createTags(database);
        addAppsTable(database);
        createShortcutsTable(database);
        createFavoritesTable(database, true);
        createWidgetsTable(database);
    }

    void createHistory(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE history ( _id INTEGER PRIMARY KEY AUTOINCREMENT, \"query\" TEXT, \"record\" TEXT NOT NULL, \"timeStamp\" INTEGER DEFAULT 0 NOT NULL)");
    }

    void createTags(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE \"tags\" (\"tag\" TEXT NOT NULL, \"record\" TEXT NOT NULL)");
        database.execSQL("CREATE INDEX idx_tags_record ON \"tags\"(\"record\");");
    }

    private void addTimeStamps(SQLiteDatabase database) {
        database.execSQL("ALTER TABLE \"history\" ADD COLUMN \"timeStamp\" INTEGER DEFAULT 0 NOT NULL");
    }

    private void addAppsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE \"apps\" ( _id INTEGER PRIMARY KEY AUTOINCREMENT, display_name TEXT NOT NULL DEFAULT '', component_name TEXT NOT NULL UNIQUE, custom_flags INTEGER DEFAULT 0, custom_icon BLOB DEFAULT NULL, cached_icon BLOB DEFAULT NULL)");
        db.execSQL("CREATE INDEX \"index_component\" ON \"apps\"(component_name);");
    }

    private void createShortcutsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE \"shortcuts\" ( _id INTEGER PRIMARY KEY AUTOINCREMENT, \"name\" TEXT NOT NULL, \"package\" TEXT, \"info_data\" TEXT, \"icon_png\" BLOB, \"custom_flags\" INTEGER DEFAULT 0)");
    }

    void createFavoritesTable(SQLiteDatabase db, boolean generateDefaults) {
        db.execSQL("CREATE TABLE \"favorites\" ( \"record\" TEXT NOT NULL UNIQUE, \"position\" TEXT NOT NULL, \"custom_flags\" INTEGER DEFAULT 0, \"name\" TEXT DEFAULT NULL, \"custom_icon\" BLOB DEFAULT NULL )");

        if (!generateDefaults)
            return;

        // generate default values
        ContentValues values = new ContentValues();
        {
            values.put("record", FilterEntry.SCHEME + "applications");
            values.put("position", "0");
            values.put("custom_flags", ModRecord.FLAG_SHOW_IN_QUICK_LIST);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
        {
            values.put("record", FilterEntry.SCHEME + "contacts");
            values.put("position", "1");
            values.put("custom_flags", ModRecord.FLAG_SHOW_IN_QUICK_LIST);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
        {
            values.put("record", FilterEntry.SCHEME + "shortcuts");
            values.put("position", "2");
            values.put("custom_flags", ModRecord.FLAG_SHOW_IN_QUICK_LIST);
            db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void createWidgetsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE \"widgets\" (_id INTEGER PRIMARY KEY AUTOINCREMENT, \"appWidgetId\" INTEGER NOT NULL, \"properties\" TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion) {
        Log.d("onUpgrade", "Updating database from version " + oldVersion + " to version " + newVersion);
        // See
        // http://www.drdobbs.com/database/using-sqlite-on-android/232900584
        if (oldVersion < newVersion) {
            switch (oldVersion) {
                case 1:
                case 2:
                case 3:
                    database.execSQL("CREATE TABLE shortcuts ( _id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, package TEXT,"
                            + "icon TEXT, intent_uri TEXT NOT NULL, icon_blob BLOB)");
                    // fall through
                case 4:
                    createTags(database);
                    // fall through
                case 5:
                    addTimeStamps(database);
                    addAppsTable(database);
                    // fall through
                case 6:
                    database.execSQL("DROP TABLE \"shortcuts\"");
                    createShortcutsTable(database);
                    database.execSQL("DROP TABLE \"tags\"");
                    createTags(database);
                    // fall through
                case 7:
                    createFavoritesTable(database, true);
                    // fall through
                case 8:
                    database.execSQL("ALTER TABLE \"apps\" ADD COLUMN \"custom_icon\" BLOB DEFAULT NULL");
                    // fall through
                case 9:
                    createWidgetsTable(database);
                    // fall through
                case 10:
                    database.execSQL("ALTER TABLE \"favorites\" ADD COLUMN \"name\" TEXT DEFAULT NULL");
                    database.execSQL("ALTER TABLE \"favorites\" ADD COLUMN \"custom_icon\" BLOB DEFAULT NULL");
                    // fall through
                case 11:
                    database.execSQL("PRAGMA foreign_keys=off");
                    database.beginTransaction();
                    try {
                        database.execSQL("DROP TABLE IF EXISTS \"widgets_old\"");
                        database.execSQL("ALTER TABLE \"widgets\" RENAME TO \"widgets_old\"");
                        createWidgetsTable(database);
                        database.execSQL("INSERT INTO \"widgets\" SELECT * FROM \"widgets_old\"");
                        database.execSQL("DROP TABLE \"widgets_old\"");

                        database.setTransactionSuccessful();
                    } finally {
                        database.endTransaction();
                        database.execSQL("PRAGMA foreign_keys=on");
                    }
                    // fall through
                case 12:
                    database.execSQL("ALTER TABLE \"apps\" ADD COLUMN \"cached_icon\" BLOB DEFAULT NULL");
                    // fall through
                default:
                    break;
            }
        }
    }
}
