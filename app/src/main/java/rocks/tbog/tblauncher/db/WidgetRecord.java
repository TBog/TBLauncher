package rocks.tbog.tblauncher.db;

import android.appwidget.AppWidgetHostView;
import android.util.Log;
import android.util.Xml;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import rocks.tbog.tblauncher.WidgetManager;
import rocks.tbog.tblauncher.ui.WidgetLayout;
import rocks.tbog.tblauncher.utils.SimpleXmlWriter;

public class WidgetRecord {

    private static final String TAG = "WRec";
    public int appWidgetId;
    public int width;
    public int height;
    public int left;
    public int top;
    public int screen;
    protected String packedProperties = null;

    protected <T extends WidgetRecord> void copyFrom(@NonNull T o) {
        appWidgetId = o.appWidgetId;
        width = o.width;
        height = o.height;
        left = o.left;
        top = o.top;
        screen = o.screen;
        packedProperties = o.packedProperties;
    }

    @NonNull
    public static WidgetRecord loadFromDB(int widgetId, String properties) {
        WidgetRecord rec;
        if (widgetId == WidgetManager.INVALID_WIDGET_ID)
            rec = new PlaceholderWidgetRecord();
        else
            rec = new WidgetRecord();

        XmlPullParser xpp = Xml.newPullParser();
        try {
            xpp.setInput(new StringReader(properties));
            int eventType = xpp.getEventType();
            rec.parseProperties(xpp, eventType);
        } catch (Exception e) {
            Log.e(TAG, "parse XML properties", e);
        }

        rec.appWidgetId = widgetId;
        return rec;
    }

    public void parseProperties(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        boolean bWidgetFinished = false;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
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
                        case "widget":  // reading from DB
                        case "properties": // importing from backup
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case "widget":  // reading from DB
                        case "properties": // importing from backup
                            bWidgetFinished = true;
                    }
            }
            if (bWidgetFinished)
                break;
            eventType = xpp.next();
        }
    }

    public void writeProperties(@NonNull SimpleXmlWriter simpleXmlWriter, boolean addRoot) throws IOException {
        if (addRoot)
            simpleXmlWriter.startTag("widget");

        simpleXmlWriter
                .startTag("size")
                .attribute("width", width)
                .attribute("height", height)
                .endTag("size")
                .startTag("position")
                .attribute("left", left)
                .attribute("top", top)
                .attribute("screen", screen)
                .endTag("position");

        if (addRoot)
            simpleXmlWriter.endTag("widget");
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
            //TODO: use a writer that extends BufferedWriter because that's what KXmlSerializer expects
            StringWriter writer = new StringWriter();
            SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
            try {
                sx.setOutput(writer);
                writeProperties(sx, true);
                sx.endDocument();
            } catch (Exception e) {
                Log.e(TAG, "pack properties", e);
            }
            packedProperties = writer.toString();
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
        screen = lp.screenPage;
    }
}
