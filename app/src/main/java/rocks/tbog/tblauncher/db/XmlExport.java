package rocks.tbog.tblauncher.db;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.Writer;
import java.util.Set;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;

public class XmlExport {
    public static void tagsXml(@NonNull Context context, @NonNull Writer w) throws IOException {
        w.write("<taglist version=\"1\">\n");
        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        Set<String> tags = tagsHandler.getAllTagsAsSet();
        for (String tagName : tags) {
            w.write("\t<tag name=\"");
            w.write(tagName);
            w.write("\">\n");
            for (String idName : tagsHandler.getIds(tagName)) {
                w.write("\t\t<item>");
                w.write(idName);
                w.write("</item>\n");
            }
            w.write("\t</tag>\n");
        }
        w.write("</taglist>\n");

    }
}
