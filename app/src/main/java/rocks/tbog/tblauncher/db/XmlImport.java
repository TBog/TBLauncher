package rocks.tbog.tblauncher.db;

import android.content.Context;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.utils.FileUtils;

public class XmlImport {
    private static final String TAG = "Import";

    public static void settingsXml(@NonNull Context context, @Nullable Uri uri, @NonNull SettingsData.Method method) {
        settingsXml(context, FileUtils.getXmlParser(context, uri), method);
    }

    public static void settingsXml(@NonNull Context context, @Nullable XmlPullParser xpp, @NonNull SettingsData.Method method) {
        if (xpp == null)
            return;
        SettingsData settings = new SettingsData();
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xpp.getName()) {
                        case SettingsData.XTN_TAG_LIST:
                            settings.parseTagList(xpp, eventType);
                            break;
                        case SettingsData.XTN_FAV_LIST:
                            settings.parseFavorites(xpp, eventType);
                            break;
                        case SettingsData.XTN_APP_LIST:
                            settings.parseApplications(xpp, eventType);
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing settingsXml", e);
        }
        settings.saveToDB(context, method);
    }

    public static class SettingsData {
        // XTN = xml tag name
        private static final String XTN_TAG_LIST = "taglist";
        private static final String XTN_TAG_LIST_ITEM = "tag";
        private static final String XTN_TAG_LIST_ITEM_ID = "item";
        private static final String XTN_FAV_LIST = "favlist";
        private static final String XTN_FAV_LIST_ITEM = "favorite";
        private static final String XTN_FAV_LIST_ITEM_ID = "id";
        private static final String XTN_APP_LIST = "applist";
        private static final String XTN_APP_LIST_ITEM = "app";
        private static final String XTN_APP_LIST_ITEM_ID = "component";

        // HashMap with tag name as key and an ArrayList of records for each
        private final HashMap<String, List<String>> mTags = new HashMap<>();
        private final ArrayList<FavRecord> mFavorites = new ArrayList<>();
        private final ArrayList<AppRecord> mApplications = new ArrayList<>();
        private final HashMap<FlagsRecord, byte[]> mIcons = new HashMap<>();
        private boolean bTagListLoaded = false;
        private boolean bFavListLoaded = false;
        private boolean bAppListLoaded = false;

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
            FavRecord currentFav = null;
            boolean bFavListFinished = false;
            String lastTag = null;
            String iconEncoding = null;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        int attrCount = xpp.getAttributeCount();
                        switch (xpp.getName()) {
                            case XTN_FAV_LIST_ITEM:
                                currentFav = new FavRecord();
                                break;
                            case XTN_FAV_LIST_ITEM_ID:
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
                            case XTN_FAV_LIST:
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
                            case XTN_FAV_LIST_ITEM:
                                if (currentFav != null && currentFav.record != null)
                                    mFavorites.add(currentFav);
                                currentFav = null;
                                // fall-through
                            case XTN_FAV_LIST_ITEM_ID:
                            case "flags":
                            case "name":
                            case "icon":
                            case "quicklist":
                                lastTag = null;
                                break;
                            case XTN_FAV_LIST:
                                bFavListFinished = true;
                                break;
                        }
                        break;
                    case XmlPullParser.TEXT:
                        if (lastTag != null && currentFav != null) {
                            switch (lastTag) {
                                case XTN_FAV_LIST_ITEM_ID:
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
            bFavListLoaded = true;
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
                eventType = xpp.next();
            }
            bAppListLoaded = true;
        }

        private void addIcon(@NonNull FlagsRecord rec, String text, @Nullable String encoding) {
            if (text == null) {
                mIcons.remove(rec);
                return;
            }
            text = text.trim();
            int size = text.length();
            if (encoding == null || "base64".equals(encoding)) {
                byte[] base64enc = new byte[size];
                for (int i = 0; i < size; i += 1) {
                    char c = text.charAt(i);
                    base64enc[i] = (byte) (c & 0xff);
                }
                byte[] icon = Base64.decode(base64enc, Base64.NO_WRAP);
                mIcons.put(rec, icon);
            }
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
            saveFavorites(context, method);
            saveApplications(context, method);
            TBApplication.dataHandler(context).reloadProviders();
        }

        public void saveTags(@NonNull Context context, Method method) {
            if (!bTagListLoaded)
                return;
            HashMap<String, HashSet<String>> tags = new HashMap<>();
            if (method == Method.OVERWRITE || method == Method.APPEND) {
                // load from DB first
                Map<String, List<String>> tagsDB = DBHelper.loadTags(context);
                for (Map.Entry<String, List<String>> entry : tagsDB.entrySet()) {
                    HashSet<String> tagSet = tags.get(entry.getKey());
                    if (tagSet == null)
                        tags.put(entry.getKey(), tagSet = new HashSet<>(entry.getValue().size()));
                    tagSet.addAll(entry.getValue());
                }
            }
            for (Map.Entry<String, List<String>> entry : mTags.entrySet()) {
                HashSet<String> tagSet = null;
                if (method == Method.APPEND) {
                    tagSet = tags.get(entry.getKey());
                }
                if (tagSet == null)
                    tags.put(entry.getKey(), tagSet = new HashSet<>(entry.getValue().size()));

                tagSet.addAll(entry.getValue());
            }
            DBHelper.setTags(context, tags);

            TBApplication.tagsHandler(context).loadFromDB();
        }

        private void saveFavorites(Context context, Method method) {
            if (!bFavListLoaded)
                return;
            HashMap<String, Pair<FavRecord, byte[]>> favs = new HashMap<>();
            if (method == Method.OVERWRITE || method == Method.APPEND) {
                List<FavRecord> favDB = DBHelper.getFavorites(context);
                for (FavRecord rec : favDB)
                    favs.put(rec.record, new Pair<>(rec, mIcons.get(rec)));
            }
            for (FavRecord fav : mFavorites) {
                if (method == Method.APPEND && favs.containsKey(fav.record))
                    continue;
                favs.put(fav.record, new Pair<>(fav, mIcons.get(fav)));
            }
            DBHelper.setFavorites(context, favs.values());
        }

        private void saveApplications(Context context, Method method) {
            if (!bAppListLoaded)
                return;

            Map<String, AppRecord> cachedApps = TBApplication.dataHandler(context).getCachedApps();
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
                if (method == Method.SET)
                {
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

        public enum Method {OVERWRITE, APPEND, SET}
    }
}
