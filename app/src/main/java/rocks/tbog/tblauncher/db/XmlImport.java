package rocks.tbog.tblauncher.db;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.FileUtils;

public class XmlImport {
    private static final String TAG = "XImport";

    public static boolean settingsXml(@NonNull Context context, @NonNull File file, @NonNull ExportedData.Method method) {
        boolean ok = false;
        try (InputStream inputStream = new FileInputStream(file)) {
            ok = settingsXml(context, FileUtils.getXmlParser(context, inputStream), method);
        } catch (Exception e) {
            Log.e(TAG, "new FileInputStream " + file.toString(), e);
        }
        return ok;
    }

    public static boolean settingsXml(@NonNull Context context, @Nullable XmlPullParser xpp, @NonNull ExportedData.Method method) {
        if (xpp == null)
            return false;
        ExportedData settings = new ExportedData();
        try {
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                int attrCount = xpp.getAttributeCount();
                if (eventType == XmlPullParser.START_TAG) {
                    switch (xpp.getName()) {
                        case ExportedData.XTN_TAG_LIST:
                            settings.parseTagList(xpp, eventType);
                            break;
                        case ExportedData.XTN_MOD_LIST:
                            settings.parseFavorites(xpp, eventType);
                            break;
                        case ExportedData.XTN_APP_LIST:
                            settings.parseApplications(xpp, eventType);
                            break;
                        case ExportedData.XTN_UI_LIST:
                        case ExportedData.XTN_PREF_LIST:
                            settings.parsePreferences(xpp, eventType);
                            break;
                        case ExportedData.XTN_WIDGET_LIST:
                            String widgetListVersion = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("version".equals(attrName)) {
                                    widgetListVersion = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            if ("1".equals(widgetListVersion)) {
                                settings.parseWidgets_v1(xpp, eventType);
                            } else {
                                settings.parseWidgets_v2(xpp, eventType);
                            }
                            break;
                        case ExportedData.XTN_HISTORY_LIST:
                            settings.parseHistory(xpp, eventType);
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parsing settingsXml", e);
            Toast.makeText(context, R.string.error_fail_import, Toast.LENGTH_LONG).show();
            return false;
        }
        settings.saveToDB(context, method);
        return true;
    }
}
