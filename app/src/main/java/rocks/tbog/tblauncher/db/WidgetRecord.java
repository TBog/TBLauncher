package rocks.tbog.tblauncher.db;

import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.StringReader;

public class WidgetRecord {

    private static final String TAG = "WRec";
    public int appWidgetId;
    public int width;
    public int height;
    private String packedProperties = null;

    public void loadProperties(String properties) {
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(properties));
            int eventType = xpp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case "size":
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                switch (attrName) {
                                    case "width":
                                        width = parseInt(xpp.getAttributeValue(attrIdx));
                                        break;
                                    case "height":
                                        height = parseInt(xpp.getAttributeValue(attrIdx));
                                        break;
                                }
                            }
                            break;
                        case "widget":
                            // root element
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                }
                eventType = xpp.next();
            }
        } catch (XmlPullParserException | IOException e) {
            Log.e(TAG, "parse XML properties", e);
        }
    }

    static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public String packedProperties() {
        if (packedProperties == null) {
            packedProperties = "<widget>\n" +
                    "\t<size " +
                    " width=\"" + width + "\"" +
                    " height=\"" + height + "\"" +
                    "/>\n" +
                    "</widget>";
        }
        return packedProperties;
    }
}
