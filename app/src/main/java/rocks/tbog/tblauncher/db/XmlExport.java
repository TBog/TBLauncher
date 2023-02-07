package rocks.tbog.tblauncher.db;

import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.TagsHandler;
import rocks.tbog.tblauncher.widgets.WidgetManager;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.utils.SimpleXmlWriter;

public class XmlExport {

    private static final String TAG = "XExport";

    public static void tagsXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        tagsXml(context, sx);

        sx.endDocument();
    }

    public static void tagsXml(@NonNull Context context, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_TAG_LIST).attribute("version", "1");

        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        Set<String> tags = tagsHandler.getAllTags();
        for (String tagName : tags) {
            sx.startTag(ExportedData.XTN_TAG_LIST_ITEM).attribute("name", tagName);
            for (String idName : tagsHandler.getAllEntryIds(tagName)) {
                sx.startTag(ExportedData.XTN_TAG_LIST_ITEM_ID).content(idName).endTag(ExportedData.XTN_TAG_LIST_ITEM_ID);
            }
            sx.endTag(ExportedData.XTN_TAG_LIST_ITEM);
        }

        sx.endTag(ExportedData.XTN_TAG_LIST);
    }

    public static void modificationsXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        modificationsXml(context, sx);

        sx.endDocument();
    }

    public static void modificationsXml(@NonNull Context context, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_MOD_LIST).attribute("version", "1");

        List<ModRecord> modRecords = TBApplication.dataHandler(context).getMods();
        for (ModRecord fav : modRecords) {
            sx.startTag(ExportedData.XTN_MOD_LIST_ITEM)
                    .startTag(ExportedData.XTN_MOD_LIST_ITEM_ID).content(fav.record).endTag(ExportedData.XTN_MOD_LIST_ITEM_ID)
                    .startTag("flags").content(fav.getFlagsDB()).endTag("flags");

            if (fav.hasCustomName() && fav.displayName != null) {
                sx.startTag("name")
                        .content(fav.displayName)
                        .endTag("name");
            }

            if (fav.hasCustomIcon()) {
                byte[] favIcon = DBHelper.getCustomFavIcon(context, fav.record);
                if (favIcon != null) {
                    byte[] base64enc = Base64.encode(favIcon, Base64.NO_WRAP);
                    sx.startTag("icon")
                            .attribute("encoding", "base64")
                            .content(base64enc)
                            .endTag("icon");
                }
            }

            if (fav.isInQuickList()) {
                sx.startTag("quicklist")
                        .content(fav.position)
                        .endTag("quicklist");
            }

            sx.endTag(ExportedData.XTN_MOD_LIST_ITEM);
        }

        sx.endTag(ExportedData.XTN_MOD_LIST);
    }

    public static void applicationsXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        applicationsXml(context, sx);

        sx.endDocument();
    }

    public static void applicationsXml(@NonNull Context context, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_APP_LIST).attribute("version", "1");

        Map<String, AppRecord> cachedApps = TBApplication.appsHandler(context).getAppRecords(context);
        for (AppRecord app : cachedApps.values()) {
            // if there is no custom settings, skip this app
            if (app.getFlagsDB() == AppRecord.FLAG_DEFAULT_NAME)
                continue;

            sx.startTag(ExportedData.XTN_APP_LIST_ITEM)
                    .startTag(ExportedData.XTN_APP_LIST_ITEM_ID).content(app.componentName).endTag(ExportedData.XTN_APP_LIST_ITEM_ID)
                    .startTag("flags").content(app.getFlagsDB()).endTag("flags");

            if (app.displayName != null && !app.displayName.isEmpty()) {
                sx.startTag("name")
                        .content(app.displayName)
                        .endTag("name");
            }

            if (app.hasCustomIcon()) {
                byte[] appIcon = DBHelper.getCustomAppIcon(context, app.componentName);
                if (appIcon != null) {
                    byte[] base64enc = Base64.encode(appIcon, Base64.NO_WRAP);
                    sx.startTag("icon")
                            .attribute("encoding", "base64")
                            .content(base64enc)
                            .endTag("icon");
                }
            }

            sx.endTag(ExportedData.XTN_APP_LIST_ITEM);
        }

        sx.endTag(ExportedData.XTN_APP_LIST);
    }

    public static void interfaceXml(@NonNull PreferenceGroup rootPref, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        interfaceXml(rootPref, sx);

        sx.endDocument();
    }

    public static void interfaceXml(@NonNull PreferenceGroup rootPref, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_UI_LIST).attribute("version", "1");

        // we remove the key from the map after it's exported to avoid duplicates
        Map<String, ?> prefMap = new HashMap<>(rootPref.getSharedPreferences().getAll());
        Preference pref;

        // do not export the following
        prefMap.remove("pin-auto-confirm");

        pref = rootPref.findPreference("ui-holder");
        if ((pref instanceof PreferenceGroup))
            recursiveWritePreferences(sx, (PreferenceGroup) pref, prefMap);

        pref = rootPref.findPreference("quick-list-section");
        if ((pref instanceof PreferenceGroup))
            recursiveWritePreferences(sx, (PreferenceGroup) pref, prefMap);

        pref = rootPref.findPreference("shortcut-section");
        if ((pref instanceof PreferenceGroup))
            recursiveWritePreferences(sx, (PreferenceGroup) pref, prefMap);

        pref = rootPref.findPreference("tags-section");
        if ((pref instanceof PreferenceGroup))
            recursiveWritePreferences(sx, (PreferenceGroup) pref, prefMap);

        sx.endTag(ExportedData.XTN_UI_LIST);
    }

    public static void preferencesXml(@NonNull PreferenceGroup rootPref, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        preferencesXml(rootPref, sx);

        sx.endDocument();
    }

    public static void preferencesXml(@NonNull PreferenceGroup rootPref, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_PREF_LIST).attribute("version", "1");

        // we remove the key from the map after it's exported to avoid duplicates
        Map<String, ?> prefMap = new HashMap<>(rootPref.getSharedPreferences().getAll());

        recursiveWritePreferences(sx, rootPref, prefMap);

        sx.endTag(ExportedData.XTN_PREF_LIST);

        for (Map.Entry<String, ?> entry : prefMap.entrySet()) {
            Log.w(TAG, "not saved pref `" + entry.getKey() + "` with value " + entry.getValue());
        }
    }

    public static void widgetsXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        widgetsXml(context, sx);

        sx.endDocument();
    }

    public static void widgetsXml(@NonNull Context context, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_WIDGET_LIST).attribute("version", "2");

        //TBApplication.widgetManager(context).

        List<WidgetRecord> widgets = DBHelper.getWidgets(context);
        for (WidgetRecord widget : widgets) {
            AppWidgetProviderInfo appWidgetProviderInfo = WidgetManager.getWidgetProviderInfo(context, widget.appWidgetId);
            sx.startTag(ExportedData.XTN_WIDGET_LIST_ITEM).attribute("id", widget.appWidgetId);
            // we use PlaceholderWidgetRecord because it has the info we need to restore
            PlaceholderWidgetRecord widgetRecord = new PlaceholderWidgetRecord();
            widgetRecord.copyFrom(widget);
            if (appWidgetProviderInfo != null) {
                widgetRecord.name = WidgetManager.getWidgetName(context, appWidgetProviderInfo);
                widgetRecord.provider = appWidgetProviderInfo.provider;
                Drawable preview = WidgetManager.getWidgetPreview(context, appWidgetProviderInfo);
                widgetRecord.preview = ShortcutUtil.getIconBlob(preview);
            }
            {
                sx.startTag("properties");
                widgetRecord.writeProperties(sx, false);
                sx.endTag("properties");
            }
            sx.endTag(ExportedData.XTN_WIDGET_LIST_ITEM);
        }

        sx.endTag(ExportedData.XTN_WIDGET_LIST);
    }

    public static void historyXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        historyXml(context, sx);

        sx.endDocument();
    }

    public static void historyXml(@NonNull Context context, @NonNull SimpleXmlWriter sx) throws IOException {
        sx.startTag(ExportedData.XTN_HISTORY_LIST).attribute("version", "1");

        List<ValuedHistoryRecord> history = DBHelper.getHistoryRaw(context);
        for (ValuedHistoryRecord historyRecord : history) {
            String query = historyRecord.name;
            sx.startTag(ExportedData.XTN_HISTORY_LIST_ITEM).attribute("time", historyRecord.value);

            sx.startTag("id").content(historyRecord.record).endTag("id");

            if (query != null)
                sx.startTag("query").content(historyRecord.name).endTag("query");

            sx.endTag(ExportedData.XTN_HISTORY_LIST_ITEM);
        }

        sx.endTag(ExportedData.XTN_HISTORY_LIST);
    }

    public static void backupXml(@NonNull PreferenceGroup rootPref, @NonNull Writer writer) throws IOException {
        Context context = rootPref.getContext().getApplicationContext();
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();

        sx.startTag("backup");

        tagsXml(context, sx);
        modificationsXml(context, sx);
        applicationsXml(context, sx);
        preferencesXml(rootPref, sx);
        widgetsXml(context, sx);
        historyXml(context, sx);

        sx.endTag("backup");

        sx.endDocument();
    }

    private static void recursiveWritePreferences(@NonNull SimpleXmlWriter sx, @NonNull PreferenceGroup prefGroup, @NonNull Map<String, ?> prefMap) throws IOException {
        int prefCount = prefGroup.getPreferenceCount();
        for (int prefIdx = 0; prefIdx < prefCount; prefIdx += 1) {
            Preference pref = prefGroup.getPreference(prefIdx);
            if (pref instanceof PreferenceGroup) {
                //Log.d(TAG, "recursiveWritePreferences " + pref.getKey());
                recursiveWritePreferences(sx, (PreferenceGroup) pref, prefMap);
                continue;
            }
            final String key = pref.getKey();
            // write preference and remove the key to prevent duplicates
            writePreference(sx, key, prefMap.remove(key));
        }
    }

    private static void writePreference(@NonNull SimpleXmlWriter sx, @NonNull String key, @Nullable Object value) throws IOException {
        if (value == null) {
            // skip this as we don't have a value
        } else if (value instanceof String)
            sx.startTag(ExportedData.XTN_PREF_LIST_ITEM)
                    .attribute("key", key)
                    .attribute("value", (String) value)
                    .endTag(ExportedData.XTN_PREF_LIST_ITEM);
        else if (value instanceof Integer) {
            sx.startTag(ExportedData.XTN_PREF_LIST_ITEM)
                    .attribute("key", key);
            if (key.contains("-color"))
                sx.attribute("color", String.format("#%06x", ((Integer) value) & 0xffffff));
            else if (key.contains("-argb"))
                sx.attribute("argb", String.format("#%08x", (Integer) value));
            else
                sx.attribute("int", ((Integer) value).toString());
            sx.endTag(ExportedData.XTN_PREF_LIST_ITEM);
        } else if (value instanceof Boolean)
            sx.startTag(ExportedData.XTN_PREF_LIST_ITEM)
                    .attribute("key", key)
                    .attribute("bool", ((Boolean) value).toString())
                    .endTag(ExportedData.XTN_PREF_LIST_ITEM);
        else if (value instanceof Set) {
            Set<?> set = (Set<?>) value;
            // find contained object type
            String type = "object";
            {
                Iterator<?> iterator = set.iterator();
                if (iterator.hasNext()) {
                    Object item = iterator.next();
                    if (item instanceof String)
                        type = "string";
                }
            }
            sx.startTag(ExportedData.XTN_PREF_LIST_ITEM)
                    .attribute("key", key)
                    .attribute("set", type);
            for (Object item : set) {
                sx.startTag("item")
                        .content(item.toString())
                        .endTag("item");
            }
            sx.endTag(ExportedData.XTN_PREF_LIST_ITEM);
        } else {
            Log.d(TAG, "skipped pref `" + key + "` with value " + value);
        }
    }
}
