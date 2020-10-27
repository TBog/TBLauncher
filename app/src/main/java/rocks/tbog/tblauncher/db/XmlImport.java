package rocks.tbog.tblauncher.db;

import android.content.Context;
import android.util.Log;

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
                        case "taglist":
                            settings.parseTagList(xpp, eventType);
                            break;
                        case "favorites":
                            settings.parseFavorites(xpp, eventType);
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing drawable.xml", e);
        }
        settings.saveToDB(context, SettingsData.Method.APPEND);
    }

    private static class SettingsData {
        // HashMap with record as key and an ArrayList of tags for each
        private final HashMap<String, List<String>> mRecTags = new HashMap<>();
        private boolean bTagListLoaded = false;

        void parseTagList(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
            String currentTag = null;
            boolean bTagItem = false;
            boolean bTagListFinished = false;
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        int attrCount = xpp.getAttributeCount();
                        switch (xpp.getName()) {
                            case "tag":
                                for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                    String attrName = xpp.getAttributeName(attrIdx);
                                    if ("name".equals(attrName)) {
                                        currentTag = xpp.getAttributeValue(attrIdx);
                                    }
                                }
                                break;
                            case "item":
                                bTagItem = currentTag != null;
                                break;
                            case "taglist":
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
                            case "tag":
                                currentTag = null;
                                // fall-through
                            case "item":
                                bTagItem = false;
                                break;
                            case "taglist":
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
            //TODO: implement favorites parsing
        }

        private void addRecordTag(@Nullable String record, @NonNull String tagName) {
            if (record == null)
                return;
            record = record.trim();
            if (record.isEmpty())
                return;
            List<String> tags = mRecTags.get(record);
            if (tags == null) {
                tags = new ArrayList<>(1);
                mRecTags.put(record, tags);
            }
            tags.add(tagName);
        }


        public void saveToDB(@NonNull Context context, Method method) {
            saveTags(context, method);
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
            for (Map.Entry<String, List<String>> entry : mRecTags.entrySet()) {
                HashSet<String> tagSet = null;
                if (method == Method.APPEND) {
                    tagSet = tags.get(entry.getKey());
                }
                if (tagSet == null)
                    tags.put(entry.getKey(), tagSet = new HashSet<>(entry.getValue().size()));

                tagSet.addAll(entry.getValue());
            }
            DBHelper.setTags(context, tags);

//            TBApplication app = TBApplication.getApplication(context);
//            TagsHandler tagsHandler = app.tagsHandler();
            TBApplication.tagsHandler(context).loadFromDB();
        }

        enum Method {OVERWRITE, APPEND, SET}
    }
}
