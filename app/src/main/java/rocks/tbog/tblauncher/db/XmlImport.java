package rocks.tbog.tblauncher.db;

import android.content.Context;
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

public class XmlImport {
    private static final String TAG = "Import";

    public static void settingsXml(@NonNull Context context, @Nullable XmlPullParser xpp) {
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
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing settingsXml", e);
        }
        settings.saveToDB(context, SettingsData.Method.SET);
    }

    private static class SettingsData {
        // XTN = xml tag name
        private static final String XTN_TAG_LIST = "taglist";
        private static final String XTN_TAG_LIST_TAG = "tag";
        private static final String XTN_TAG_LIST_TAG_RECORD = "item";
        private static final String XTN_FAV_LIST = "favlist";
        private static final String XTN_FAV_LIST_RECORD = "id";

        // HashMap with tag name as key and an ArrayList of records for each
        private final HashMap<String, List<String>> mTags = new HashMap<>();
        private final ArrayList<FavRecord> mFavorites = new ArrayList<>();
        private final HashMap<FavRecord, byte[]> mIcons = new HashMap<>();
        private boolean bTagListLoaded = false;
        private boolean bFavListLoaded = false;

        void parseTagList(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
            String currentTag = null;
            boolean bTagItem = false;
            boolean bTagListFinished = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        int attrCount = xpp.getAttributeCount();
                        switch (xpp.getName()) {
                            case XTN_TAG_LIST_TAG:
                                for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                    String attrName = xpp.getAttributeName(attrIdx);
                                    if ("name".equals(attrName)) {
                                        currentTag = xpp.getAttributeValue(attrIdx);
                                    }
                                }
                                break;
                            case XTN_TAG_LIST_TAG_RECORD:
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
                            case XTN_TAG_LIST_TAG:
                                currentTag = null;
                                // fall-through
                            case XTN_TAG_LIST_TAG_RECORD:
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
                            case "favorite":
                                currentFav = new FavRecord();
                                break;
                            case XTN_FAV_LIST_RECORD:
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
                            case "favorite":
                                if (currentFav != null && currentFav.record != null)
                                    mFavorites.add(currentFav);
                                currentFav = null;
                                // fall-through
                            case XTN_FAV_LIST_RECORD:
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
                                case XTN_FAV_LIST_RECORD:
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

        private void addIcon(@NonNull FavRecord fav, String text, @Nullable String encoding) {
            if (text == null) {
                mIcons.remove(fav);
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
                mIcons.put(fav, icon);
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


        public void saveToDB(@NonNull Context context, Method method) {
            saveTags(context, method);
            saveFavorites(context, method);
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

        enum Method {OVERWRITE, APPEND, SET}
    }
}
