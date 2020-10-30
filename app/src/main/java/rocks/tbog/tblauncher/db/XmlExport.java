package rocks.tbog.tblauncher.db;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TagsHandler;
import rocks.tbog.tblauncher.utils.SimpleXmlWriter;

public class XmlExport {

    public static void tagsXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();
        sx.startTag("taglist").attribute("version", "1");

        TagsHandler tagsHandler = TBApplication.tagsHandler(context);
        Set<String> tags = tagsHandler.getAllTagsAsSet();
        for (String tagName : tags) {
            sx.startTag("tag").attribute("name", tagName);
            for (String idName : tagsHandler.getIds(tagName)) {
                sx.startTag("item").content(idName).endTag("item");
            }
            sx.endTag("tag");
        }

        sx.endTag("taglist");

        sx.endDocument();
    }

    public static void favoritesXml(@NonNull Context context, @NonNull Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();
        sx.startTag("favlist").attribute("version", "1");

        List<FavRecord> favRecords = TBApplication.dataHandler(context).getFavorites();
        for (FavRecord fav : favRecords) {
            sx.startTag("favorite")
                    .startTag("id").content(fav.record).endTag("id")
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

            sx.endTag("favorite");
        }

        sx.endTag("favlist");

        sx.endDocument();
    }

    public static void applicationsXml(Context context, Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();
        sx.startTag("applist").attribute("version", "1");

        Map<String, AppRecord> cachedApps = TBApplication.dataHandler(context).getCachedApps();
        for (AppRecord app : cachedApps.values()) {
            // if there is no custom settings, skip this app
            if (app.getFlagsDB() == AppRecord.FLAG_DEFAULT_NAME)
                continue;

            sx.startTag("app")
                    .startTag("component").content(app.componentName).endTag("component")
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

            sx.endTag("app");
        }

        sx.endTag("applist");

        sx.endDocument();
    }

    public static void interfaceXml(Context context, Writer writer) throws IOException {
        SimpleXmlWriter sx = SimpleXmlWriter.getNewInstance();
        sx.setOutput(writer);

        sx.setIndentation(true);
        sx.startDocument();
        sx.startTag("interface").attribute("version", "1");

        //PreferenceManager prefMgr = new PreferenceManager(context);
        //SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        //Map<String, ?> prefMap = pref.getAll();

        sx.endTag("interface");

        sx.endDocument();
    }
}
