package rocks.tbog.tblauncher.db;

import android.content.ComponentName;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import rocks.tbog.tblauncher.utils.SimpleXmlWriter;
import rocks.tbog.tblauncher.utils.Utilities;

public class PlaceholderWidgetRecord extends WidgetRecord {

    private static final String TAG = "PWRec";
    public String name;
    public ComponentName provider;
    public byte[] preview;

    @Override
    protected <T extends WidgetRecord> void copyFrom(@NonNull T o) {
        super.copyFrom(o);
        if (o instanceof PlaceholderWidgetRecord) {
            PlaceholderWidgetRecord p = (PlaceholderWidgetRecord) o;
            name = p.name;
            provider = p.provider;
            preview = p.preview;
        }
    }

    @Override
    public void parseProperties(@NonNull XmlPullParser xpp, int eventType) throws IOException, XmlPullParserException {
        boolean bPlaceholderFinished = false;
        String lastTag = null;
        String iconEncoding = null;
        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    int attrCount = xpp.getAttributeCount();
                    switch (xpp.getName()) {
                        case "widget":
                            super.parseProperties(xpp, eventType);
                            break;
                        case "preview":
                            iconEncoding = null;
                            for (int attrIdx = 0; attrIdx < attrCount; attrIdx += 1) {
                                String attrName = xpp.getAttributeName(attrIdx);
                                if ("encoding".equals(attrName)) {
                                    iconEncoding = xpp.getAttributeValue(attrIdx);
                                }
                            }
                            //fall-through
                        case "name":
                        case "provider":
                            lastTag = xpp.getName();
                            break;
                        default:
                            Log.d(TAG, "ignored " + xpp.getName());
                    }
                    break;
                case XmlPullParser.END_TAG:
                    switch (xpp.getName()) {
                        case "name":
                        case "provider":
                        case "preview":
                            lastTag = null;
                            break;
                        case "placeholder": // importing from DB
                        case "properties": // importing from backup
                            bPlaceholderFinished = true;
                            break;
                    }
                case XmlPullParser.TEXT:
                    if (lastTag != null)
                        switch (lastTag) {
                            case "name":
                                name = xpp.getText();
                                break;
                            case "provider":
                                provider = ComponentName.unflattenFromString(xpp.getText());
                                break;
                            case "preview":
                                preview = Utilities.decodeIcon(xpp.getText(), iconEncoding);
                                break;
                        }
                    break;
            }
            if (bPlaceholderFinished)
                break;
            eventType = xpp.next();
        }
    }

    @Override
    public void writeProperties(@NonNull SimpleXmlWriter simpleXmlWriter, boolean addRoot) throws IOException {
        if (addRoot)
            simpleXmlWriter.startTag("placeholder");

        super.writeProperties(simpleXmlWriter, true);
        simpleXmlWriter
                .startTag("name")
                .content(name)
                .endTag("name")
                .startTag("provider")
                .content(provider != null ? provider.flattenToString() : "")
                .endTag("provider");
        if (preview != null) {
            byte[] base64enc = Base64.encode(preview, Base64.NO_WRAP);
            simpleXmlWriter.startTag("preview")
                    .attribute("encoding", "base64")
                    .content(base64enc)
                    .endTag("preview");
        }

        if (addRoot)
            simpleXmlWriter.endTag("placeholder");
    }
}
