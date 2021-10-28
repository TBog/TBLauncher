package rocks.tbog.tblauncher.db;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArraySet;
import androidx.preference.PreferenceManager;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.SettingsActivity;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.WidgetManager;
import rocks.tbog.tblauncher.utils.Utilities;

public class ExportedData {
    private static final String TAG = "XParse";

    // XTN = xml tag name
    public static final String XTN_TAG_LIST = "taglist";
    public static final String XTN_TAG_LIST_ITEM = "tag";
    public static final String XTN_TAG_LIST_ITEM_ID = "item";
    public static final String XTN_MOD_LIST = "favlist";        // modification list
    public static final String XTN_MOD_LIST_ITEM = "favorite";  // modification entry
    public static final String XTN_MOD_LIST_ITEM_ID = "id";
    public static final String XTN_APP_LIST = "applist";
    public static final String XTN_APP_LIST_ITEM = "app";
    public static final String XTN_APP_LIST_ITEM_ID = "component";
    public static final String XTN_UI_LIST = "interface";
    public static final String XTN_PREF_LIST = "preferences";
    public static final String XTN_PREF_LIST_ITEM = "preference";
    public static final String XTN_WIDGET_LIST = "widgets";
    public static final String XTN_WIDGET_LIST_ITEM = "widget";
    public static final String XTN_HISTORY_LIST = "history";
    public static final String XTN_HISTORY_LIST_ITEM = "item";

    // HashMap with tag name as key and an ArrayList of records for each
    private final HashMap<String, List<String>> mTags = new HashMap<>();
    private final HashMap<String, Object> mPreferences = new HashMap<>();
    private final ArrayList<ModRecord> mMods = new ArrayList<>();
    private final ArrayList<AppRecord> mApplications = new ArrayList<>();
    private final ArrayList<PlaceholderWidgetRecord> mWidgets = new ArrayList<>();
    private final HashMap<FlagsRecord, byte[]> mIcons = new HashMap<>();
    private final ArrayList<ValuedHistoryRecord> mHistory = new ArrayList<>();
    private boolean bTagListLoaded = false;
    private boolean bModListLoaded = false;
    private boolean bAppListLoaded = false;
    private boolean bPrefListLoaded = false;
    private boolean bWidgetListLoaded = false;
    private boolean bHistoryListLoaded = false;

    void parseTagList(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        String currentTag = null;
        boolean bTagItem = false;
        boolean bTagListFinished = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_TAG_LIST_ITEM:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("name".equals(attrName)) {
                                    currentTag = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            break;
                        case XTN_TAG_LIST_ITEM_ID:
                            bTagItem = currentTag != null;
                            break;
                        case XTN_TAG_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String tagListVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "tagList version " + tagListVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_TAG_LIST_ITEM:
                            currentTag = null;
                            // fall-through
                        case XTN_TAG_LIST_ITEM_ID:
                            bTagItem = false;
                            break;
                        case XTN_TAG_LIST:
                            bTagListFinished = true;
                            break;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (bTagItem && currentTag != null) {
                        addRecordTag(xpp.getText(), currentTag);
                    }
                    break;
            }
            if (bTagListFinished)
                break;
            eventType = xpp.next();
        }
        bTagListLoaded = true;
    }

    public void parseFavorites(XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        ModRecord currentFav = null;
        boolean bFavListFinished = false;
        String lastTag = null;
        String iconEncoding = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_MOD_LIST_ITEM:
                            currentFav = new ModRecord();
                            lastTag = null;
                            break;
                        case XTN_MOD_LIST_ITEM_ID:
                        case "flags":
                        case "name":
                        case "quicklist":
                            if (currentFav != null)
                                lastTag = xpp.getName();
                            break;
                        case "icon":
                            if (currentFav != null)
                                lastTag = xpp.getName();
                            iconEncoding = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("encoding".equals(attrName)) {
                                    iconEncoding = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            break;
                        case XTN_MOD_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String tagListVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "favList version " + tagListVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_MOD_LIST_ITEM:
                            if (currentFav != null && currentFav.record != null)
                                mMods.add(currentFav);
                            currentFav = null;
                            // fall-through
                        case XTN_MOD_LIST_ITEM_ID:
                        case "flags":
                        case "name":
                        case "icon":
                        case "quicklist":
                            lastTag = null;
                            break;
                        case XTN_MOD_LIST:
                            bFavListFinished = true;
                            break;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (lastTag != null && currentFav != null) {
                        switch (lastTag) {
                            case XTN_MOD_LIST_ITEM_ID:
                                currentFav.record = xpp.getText();
                                if (currentFav.record.isEmpty())
                                    currentFav.record = null;
                                break;
                            case "flags":
                                try {
                                    currentFav.setFlags(Integer.parseInt(xpp.getText()));
                                } catch (NumberFormatException ignored) {
                                    currentFav.setFlags(0);
                                }
                                break;
                            case "name":
                                currentFav.displayName = xpp.getText();
                                break;
                            case "icon":
                                addIcon(currentFav, xpp.getText(), iconEncoding);
                                break;
                            case "quicklist":
                                currentFav.position = xpp.getText();
                                break;
                        }
                    }
                    break;
            }
            if (bFavListFinished)
                break;
            eventType = xpp.next();
        }
        bModListLoaded = true;
    }

    public void parseApplications(XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        AppRecord currentApp = null;
        boolean bAppListFinished = false;
        String lastTag = null;
        String iconEncoding = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_APP_LIST_ITEM:
                            currentApp = new AppRecord();
                            lastTag = null;
                            break;
                        case XTN_APP_LIST_ITEM_ID:
                        case "flags":
                        case "name":
                            if (currentApp != null)
                                lastTag = xpp.getName();
                            break;
                        case "icon":
                            if (currentApp != null)
                                lastTag = xpp.getName();
                            iconEncoding = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("encoding".equals(attrName)) {
                                    iconEncoding = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            break;
                        case XTN_APP_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String tagListVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "appList version " + tagListVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_APP_LIST_ITEM:
                            if (currentApp != null && currentApp.componentName != null)
                                mApplications.add(currentApp);
                            currentApp = null;
                            // fall-through
                        case XTN_APP_LIST_ITEM_ID:
                        case "flags":
                        case "name":
                        case "icon":
                            lastTag = null;
                            break;
                        case XTN_APP_LIST:
                            bAppListFinished = true;
                            break;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (lastTag != null && currentApp != null) {
                        switch (lastTag) {
                            case XTN_APP_LIST_ITEM_ID:
                                currentApp.componentName = xpp.getText();
                                if (currentApp.componentName.isEmpty())
                                    currentApp.componentName = null;
                                break;
                            case "flags":
                                try {
                                    currentApp.setFlags(Integer.parseInt(xpp.getText()));
                                } catch (NumberFormatException ignored) {
                                    currentApp.setFlags(0);
                                }
                                break;
                            case "name":
                                currentApp.displayName = xpp.getText();
                                break;
                            case "icon":
                                addIcon(currentApp, xpp.getText(), iconEncoding);
                                break;
                        }
                    }
                    break;
            }
            if (bAppListFinished)
                break;
            try {
                eventType = xpp.next();
            } catch (IOException e) {
                if (currentApp != null)
                    Log.e(TAG, "currentApp " + currentApp.componentName + " " + currentApp.displayName);
                throw new IOException("app xpp.next", e);
            }
        }
        bAppListLoaded = true;
    }

    void parsePreferences(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        String prefName = null;
        Object prefValue = null;
        boolean addTextToValueSet = false;
        boolean bPrefListFinished = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_PREF_LIST_ITEM:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                switch (attrName) {
                                    case "key":
                                        prefName = xpp.getAttributeValue(attrIdx);
                                        break;
                                    case "value":
                                        prefValue = xpp.getAttributeValue(attrIdx);
                                        break;
                                    case "bool":
                                        prefValue = Boolean.parseBoolean(xpp.getAttributeValue(attrIdx));
                                        break;
                                    case "int":
                                        try {
                                            prefValue = Integer.parseInt(xpp.getAttributeValue(attrIdx));
                                        } catch (NumberFormatException ignored) {
                                            prefValue = 0;
                                        }
                                        break;
                                    case "color":
                                        try {
                                            String str = xpp.getAttributeValue(attrIdx).substring(1);
                                            int length = str.length();
                                            if (length > 6)
                                                str = str.substring(length - 6);
                                            prefValue = Integer.parseInt(str, 16);
                                        } catch (NumberFormatException ignored) {
                                            prefValue = 0;
                                        }
                                        break;
                                    case "argb":
                                        try {
                                            String str = xpp.getAttributeValue(attrIdx).substring(1);
                                            long parsed = Long.parseLong(str, 16);
                                            prefValue = (int) parsed;
                                        } catch (NumberFormatException ignored) {
                                            prefValue = 0;
                                        }
                                        break;
                                    case "set":
                                        // we get Strings from the XML parser, no need to keep Objects
                                        prefValue = new ArraySet<String>();
                                        addTextToValueSet = false;
                                        break;
                                    default:
                                        Log.d(TAG, "ignored attribute " + xpp.getAttributeValue(attrIdx) + " from tag " + xpp.getName());
                                }
                            }
                            break;
                        case XTN_UI_LIST:
                        case XTN_PREF_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String prefListVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "prefList version " + prefListVersion);
                                }
                            }
                            break;
                        case "item":
                            if (prefValue instanceof ArraySet)
                                addTextToValueSet = true;
                            else
                                Log.d(TAG, "expected Set, found " + prefValue);
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_PREF_LIST_ITEM:
                            if (prefName != null && prefValue != null)
                                mPreferences.put(prefName, prefValue);
                            prefName = null;
                            prefValue = null;
                            break;
                        case XTN_UI_LIST:
                        case XTN_PREF_LIST:
                            bPrefListFinished = true;
                            break;
                        case "item":
                            addTextToValueSet = false;
                            break;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (addTextToValueSet) {
                        @SuppressWarnings("unchecked")
                        ArraySet<String> set = (ArraySet<String>) prefValue;
                        set.add(xpp.getText());
                    } else if (prefName != null)
                        Log.d(TAG, "preference `" + prefName + "` has text `" + xpp.getText() + "`");
                    break;
            }
            if (bPrefListFinished)
                break;
            eventType = xpp.next();
        }
        bPrefListLoaded = true;
    }

    public void parseWidgets_v1(XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        PlaceholderWidgetRecord currentWidget = null;
        boolean bWidgetListFinished = false;
        String lastTag = null;
        String iconEncoding = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_WIDGET_LIST_ITEM:
                            currentWidget = new PlaceholderWidgetRecord();
                            lastTag = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("id".equals(attrName)) {
                                    try {
                                        currentWidget.appWidgetId = Integer.parseInt(xpp.getAttributeValue(attrIdx));
                                    } catch (NumberFormatException ignored) {
                                        currentWidget.appWidgetId = WidgetManager.INVALID_WIDGET_ID;
                                    }
                                }
                            }
                            break;
                        case "name":
                        case "provider":
                            if (currentWidget != null)
                                lastTag = xpp.getName();
                            break;
                        case "preview":
                            if (currentWidget != null)
                                lastTag = xpp.getName();
                            iconEncoding = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("encoding".equals(attrName)) {
                                    iconEncoding = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            break;
                        case "properties":
                            if (currentWidget != null) {
                                WidgetRecord widgetRecord = new WidgetRecord();
                                widgetRecord.appWidgetId = currentWidget.appWidgetId;
                                widgetRecord.parseProperties(xpp, eventType);
                                currentWidget.copyFrom(widgetRecord);
                            }
                            break;
                        case XTN_WIDGET_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String listVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "widgetList version " + listVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_WIDGET_LIST_ITEM:
                            if (currentWidget != null)
                                mWidgets.add(currentWidget);
                            currentWidget = null;
                            // fall-through
                        case "name":
                        case "provider":
                        case "preview":
                            lastTag = null;
                            break;
                        case XTN_WIDGET_LIST:
                            bWidgetListFinished = true;
                            break;
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (lastTag != null && currentWidget != null)
                        switch (lastTag) {
                            case "name":
                                currentWidget.name = xpp.getText();
                                break;
                            case "provider":
                                currentWidget.provider = ComponentName.unflattenFromString(xpp.getText());
                                break;
                            case "preview":
                                currentWidget.preview = Utilities.decodeIcon(xpp.getText(), iconEncoding);
                                break;
                        }
                    break;
            }
            if (bWidgetListFinished)
                break;
            eventType = xpp.next();
        }
        bWidgetListLoaded = true;
    }

    public void parseWidgets_v2(XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        PlaceholderWidgetRecord currentWidget = null;
        boolean bWidgetListFinished = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_WIDGET_LIST_ITEM:
                            currentWidget = new PlaceholderWidgetRecord();
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("id".equals(attrName)) {
                                    try {
                                        currentWidget.appWidgetId = Integer.parseInt(xpp.getAttributeValue(attrIdx));
                                    } catch (NumberFormatException ignored) {
                                        currentWidget.appWidgetId = WidgetManager.INVALID_WIDGET_ID;
                                    }
                                }
                            }
                            break;
                        case "properties":
                            if (currentWidget != null)
                                currentWidget.parseProperties(xpp, eventType);
                            break;
                        case XTN_WIDGET_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String prefListVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "widgetList version " + prefListVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_WIDGET_LIST_ITEM:
                            if (currentWidget != null)
                                mWidgets.add(currentWidget);
                            currentWidget = null;
                            break;
                        case XTN_WIDGET_LIST:
                            bWidgetListFinished = true;
                            break;
                    }
                    break;
            }
            if (bWidgetListFinished)
                break;
            eventType = xpp.next();
        }
        bWidgetListLoaded = true;
    }

    public void parseHistory(XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        ValuedHistoryRecord currentRecord = null;
        boolean bHistoryListFinished = false;
        String lastTag = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case XTN_HISTORY_LIST_ITEM:
                            lastTag = null;
                            currentRecord = new ValuedHistoryRecord();
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("time".equals(attrName)) {
                                    try {
                                        currentRecord.value = Long.parseLong(xpp.getAttributeValue(attrIdx));
                                    } catch (NumberFormatException ignored) {
                                        currentRecord.value = 0;
                                    }
                                }
                            }
                            break;
                        case "id":
                        case "query":
                            if (currentRecord != null)
                                lastTag = xpp.getName();
                            break;
                        case XTN_HISTORY_LIST:
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    String listVersion = xpp.getAttributeValue(attrIdx);
                                    Log.d(TAG, "historyList version " + listVersion);
                                }
                            }
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.TEXT:
                    if (lastTag != null && currentRecord != null)
                        switch (lastTag) {
                            case "id":
                                currentRecord.record = xpp.getText();
                                break;
                            case "query":
                                currentRecord.name = xpp.getText();
                                break;
                        }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case XTN_HISTORY_LIST_ITEM:
                            if (currentRecord != null)
                                mHistory.add(currentRecord);
                            currentRecord = null;
                            // fall-through
                        case "id":
                        case "query":
                            lastTag = null;
                            break;
                        case XTN_HISTORY_LIST:
                            bHistoryListFinished = true;
                            break;
                    }
                    break;
            }
            if (bHistoryListFinished)
                break;
            eventType = xpp.next();
        }
        bHistoryListLoaded = true;
    }

    private void addIcon(@NonNull FlagsRecord rec, String text, @Nullable String encoding) {
        if (text == null) {
            mIcons.remove(rec);
            return;
        }
        byte[] icon = Utilities.decodeIcon(text, encoding);
        if (icon != null)
            mIcons.put(rec, icon);
    }

    private void addRecordTag(@Nullable String record, @NonNull String tagName) {
        if (record == null)
            return;
        record = record.trim();
        if (record.isEmpty())
            return;
        List<String> records = mTags.get(tagName);
        if (records == null)
            mTags.put(tagName, records = new ArrayList<>());
        records.add(record);
    }


    public void saveToDB(@NonNull Context context, @NonNull Method method) {
        saveTags(context, method);
        saveMods(context, method);
        saveApplications(context, method);
        savePreferences(context, method);
        restoreWidgets(context, method);
        saveHistory(context, method);
        TBApplication.dataHandler(context).reloadProviders();
    }

    private void saveTags(@NonNull Context context, Method method) {
        if (!bTagListLoaded)
            return;
        HashMap<String, ArraySet<String>> tags = new HashMap<>();
        if (method == Method.OVERWRITE || method == Method.APPEND) {
            // load from DB first
            Map<String, List<String>> tagsDB = DBHelper.loadTags(context);
            for (Map.Entry<String, List<String>> entry : tagsDB.entrySet()) {
                ArraySet<String> tagSet = tags.get(entry.getKey());
                if (tagSet == null)
                    tags.put(entry.getKey(), tagSet = new ArraySet<>(entry.getValue().size()));
                tagSet.addAll(entry.getValue());
            }
        }
        for (Map.Entry<String, List<String>> entry : mTags.entrySet()) {
            ArraySet<String> tagSet = null;
            if (method == Method.APPEND) {
                tagSet = tags.get(entry.getKey());
            }
            if (tagSet == null)
                tags.put(entry.getKey(), tagSet = new ArraySet<>(entry.getValue().size()));

            tagSet.addAll(entry.getValue());
        }
        DBHelper.setTagsMap(context, tags);

        TBApplication.tagsHandler(context).loadFromDB(true);
    }

    private void saveMods(Context context, Method method) {
        if (!bModListLoaded)
            return;
        HashMap<String, Pair<ModRecord, byte[]>> mods = new HashMap<>();
        if (method == Method.OVERWRITE || method == Method.APPEND) {
            List<ModRecord> modDB = DBHelper.getMods(context);
            for (ModRecord rec : modDB)
                mods.put(rec.record, new Pair<>(rec, mIcons.get(rec)));
        }
        for (ModRecord modRecord : mMods) {
            if (method == Method.APPEND && mods.containsKey(modRecord.record))
                continue;
            mods.put(modRecord.record, new Pair<>(modRecord, mIcons.get(modRecord)));
        }
        DBHelper.setMods(context, mods.values());
    }

    private void saveApplications(Context context, Method method) {
        if (!bAppListLoaded)
            return;

        Map<String, AppRecord> cachedApps = TBApplication.appsHandler(context).getAppRecords(context);
        if (method == Method.OVERWRITE || method == Method.SET) {
            // make sure the validate flag is off
            for (AppRecord rec : cachedApps.values())
                rec.clearFlags(AppRecord.FLAG_VALIDATED);

            for (AppRecord importedRec : mApplications) {
                AppRecord rec = cachedApps.get(importedRec.componentName);
                if (rec == null)
                    continue;
                // validate apps that are found in the imported list
                rec.setFlags(AppRecord.FLAG_VALIDATED);

                // overwrite
                if (rec.isHidden() && !importedRec.isHidden())
                    DBHelper.removeAppHidden(context, rec.componentName);
                if (rec.hasCustomName() && !importedRec.hasCustomName()) {
                    String name = importedRec.displayName != null ? importedRec.displayName : "";
                    DBHelper.removeCustomAppName(context, rec.componentName, name);
                }
                if (rec.hasCustomIcon() && importedRec.hasCustomIcon())
                    DBHelper.removeCustomAppIcon(context, rec.componentName);
            }
            if (method == Method.SET) {
                // clean apps that don't appear in the import
                for (AppRecord rec : cachedApps.values()) {
                    if (rec.isFlagSet(AppRecord.FLAG_VALIDATED))
                        continue;
                    if (rec.isHidden())
                        DBHelper.removeAppHidden(context, rec.componentName);
                    if (rec.hasCustomName()) {
                        String name = rec.displayName != null ? rec.displayName : "";
                        DBHelper.removeCustomAppName(context, rec.componentName, name);
                    }
                    if (rec.hasCustomIcon())
                        DBHelper.removeCustomAppIcon(context, rec.componentName);
                }
            }
        }

        for (AppRecord importedRec : mApplications) {
            AppRecord rec = cachedApps.get(importedRec.componentName);
            // if app not found (on device) there no need to customize it
            if (rec == null)
                continue;
            if (importedRec.isHidden())
                DBHelper.setAppHidden(context, importedRec.componentName);
            if (importedRec.hasCustomName())
                DBHelper.setCustomAppName(context, importedRec.componentName, importedRec.displayName);
            if (importedRec.hasCustomIcon())
                DBHelper.setCustomAppIcon(context, importedRec.componentName, mIcons.get(importedRec));
        }
    }

    @SuppressLint("ApplySharedPref")
    private void savePreferences(Context context, Method method) {
        if (!bPrefListLoaded || method == Method.APPEND)
            return;

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        if (method == Method.SET) {
            editor.clear().commit();

            PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
            PreferenceManager.setDefaultValues(context, R.xml.preference_features, true);

            editor = preferences.edit();
        }

        for (Map.Entry<String, Object> entry : mPreferences.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String)
                editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Integer)
                editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Boolean)
                editor.putBoolean(entry.getKey(), (Boolean) value);
            else if (value instanceof ArraySet) {
                @SuppressWarnings("unchecked")
                ArraySet<String> set = (ArraySet<String>) value;
                editor.putStringSet(entry.getKey(), set);
            }
        }

        editor.commit();

        for (String key : mPreferences.keySet())
            SettingsActivity.onSharedPreferenceChanged(context, preferences, key);
    }

    private void restoreWidgets(Context context, Method method) {
        if (!bWidgetListLoaded)
            return;

        WidgetManager wm = TBApplication.widgetManager(context);
        wm.onBeforeRestoreFromBackup(method != Method.APPEND);

        final boolean append = method == Method.APPEND;
        for (PlaceholderWidgetRecord widget : mWidgets) {
            wm.restoreFromBackup(append, widget);
        }

        wm.onAfterRestoreFromBackup(method == Method.SET);
    }

    private void saveHistory(Context context, Method method) {
        if (!bHistoryListLoaded)
            return;

        List<ValuedHistoryRecord> history;
        if (method == Method.SET) {
            history = mHistory;
        } else {
            // load from DB first
            history = DBHelper.getHistoryRaw(context);
            long time = history.isEmpty() ? 0 : history.get(history.size() - 1).value;
            for (ValuedHistoryRecord rec : mHistory) {
                if (rec.value > time)
                    history.add(rec);
            }
        }

        DBHelper.setHistory(context, history);
    }

    public enum Method {OVERWRITE, APPEND, SET}
}
