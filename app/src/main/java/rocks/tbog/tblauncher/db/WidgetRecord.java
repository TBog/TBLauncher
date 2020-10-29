package rocks.tbog.tblauncher.db;

import android.appwidget.AppWidgetHostView;
import android.util.Log;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.StringReader;

import rocks.tbog.tblauncher.ui.WidgetLayout;

public class WidgetRecord {

    private static final String TAG = "WRec";
    public int appWidgetId;
    public int width;
    public int height;
    public int left;
    public int top;
    public int screen;
    private String packedProperties = null;

    public void loadProperties(String properties) {
        XmlPullParser xpp = Xml.newPullParser();
        try {
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
                        case "position":
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                switch (attrName) {
                                    case "left":
                                        left = parseInt(xpp.getAttributeValue(attrIdx));
                                        break;
                                    case "top":
                                        top = parseInt(xpp.getAttributeValue(attrIdx));
                                        break;
                                    case "screen":
                                        screen = parseInt(xpp.getAttributeValue(attrIdx));
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
        } catch (Exception e) {
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
                    "\t<position " +
                    " left=\"" + left + "\"" +
                    " top=\"" + top + "\"" +
                    " screen=\"" + screen + "\"" +
                    "/>\n" +
                    "</widget>";
        }
        return packedProperties;
    }

    public void saveProperties(AppWidgetHostView view) {
        packedProperties = null;
        final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
        width = lp.width;
        height = lp.height;
        left = lp.leftMargin;
        top = lp.topMargin;
        screen = lp.screen;
    }
}
