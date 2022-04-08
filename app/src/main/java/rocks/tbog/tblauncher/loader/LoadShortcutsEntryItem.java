package rocks.tbog.tblauncher.loader;

import android.content.Context;
import android.content.pm.LauncherApps;
import android.content.pm.LauncherApps.ShortcutQuery;
import android.content.pm.ShortcutInfo;
import android.os.Build;
import android.os.Process;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.handler.TagsHandler;

public class LoadShortcutsEntryItem extends LoadEntryItem<ShortcutEntry> {

    private final TagsHandler tagsHandler;
    private final LauncherApps mLauncherApps;

    public LoadShortcutsEntryItem(Context context) {
        super(context);
        tagsHandler = TBApplication.tagsHandler(context);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mLauncherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        } else {
            mLauncherApps = null;
        }
    }

    @NonNull
    @Override
    public String getScheme() {
        return ShortcutEntry.SCHEME;
    }

    @Override
    protected ArrayList<ShortcutEntry> doInBackground(Void arg) {
        Context ctx = context.get();
        if (ctx == null) {
            return new ArrayList<>();
        }

        final HashMap<String, ModRecord> favorites;
        {
            ArrayList<ModRecord> favList = DBHelper.getMods(ctx);
            favorites = new HashMap<>();
            for (ModRecord fav : favList)
                favorites.put(fav.record, fav);
        }

        List<ShortcutRecord> records = DBHelper.getShortcutsNoIcons(ctx);
        ArrayList<ShortcutEntry> pojos = new ArrayList<>(records.size());

        HashMap<String, ShortcutRecord> oreoMap = new HashMap<>();

        for (ShortcutRecord shortcutRecord : records) {
            if (shortcutRecord.isOreo()) {
                oreoMap.put(shortcutRecord.infoData, shortcutRecord);
                continue;
            }

            final String id = ShortcutEntry.generateShortcutId(shortcutRecord);
            final ShortcutEntry pojo = new ShortcutEntry(id, shortcutRecord.dbId, shortcutRecord.packageName, shortcutRecord.infoData);

            pojo.setName(shortcutRecord.displayName);
            if (shortcutRecord.isFlagSet(ShortcutRecord.FLAG_CUSTOM_INTENT))
                pojo.setCustomIntent();

            ModRecord modRecord = favorites.get(pojo.id);
            if (modRecord != null && modRecord.hasCustomIcon())
                pojo.setCustomIcon();

            pojos.add(pojo);
        }

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            List<ShortcutInfo> shortcutInfos = null;

            ShortcutQuery q = new ShortcutQuery();
            if (TBApplication.getApplication(ctx).preferences().getBoolean("shortcut-dynamic-in-results", false)) {
                q.setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED
                    | ShortcutQuery.FLAG_MATCH_MANIFEST
                    | ShortcutQuery.FLAG_MATCH_DYNAMIC
                    | ShortcutQuery.FLAG_MATCH_CACHED);
            } else {
                q.setQueryFlags(ShortcutQuery.FLAG_MATCH_PINNED
                    | ShortcutQuery.FLAG_MATCH_MANIFEST);
            }

            if (mLauncherApps.hasShortcutHostPermission())
                shortcutInfos = mLauncherApps.getShortcuts(q, Process.myUserHandle());
            if (shortcutInfos == null) {
                shortcutInfos = Collections.emptyList();
            }

            for (ShortcutInfo shortcutInfo : shortcutInfos) {
                ShortcutRecord record = oreoMap.remove(shortcutInfo.getId());
                long dbId = 0;
                String name = null;
                if (record != null) {
                    dbId = record.dbId;
                    name = record.displayName;
                }
                // if no name found, try the shortcut text
                if (name == null || name.isEmpty()) {
                    CharSequence label = shortcutInfo.getLongLabel();
                    if (label != null)
                        name = label.toString();
                }
                // if no name found, try the shortcut title
                if (name == null || name.isEmpty()) {
                    CharSequence label = shortcutInfo.getShortLabel();
                    if (label != null)
                        name = label.toString();
                }
                ShortcutEntry pojo = new ShortcutEntry(dbId, shortcutInfo);
                pojo.setName(name);

                ModRecord modRecord = favorites.get(pojo.id);
                if (modRecord != null && modRecord.hasCustomIcon())
                    pojo.setCustomIcon();

                pojos.add(pojo);
            }

            // clear remaining shortcuts
            for (ShortcutRecord record : oreoMap.values()) {
                DBHelper.removeShortcut(ctx, record.dbId);
                //tagsHandler.removeAllTags(ShortcutEntry.SCHEME + record.infoData);
            }
        }

        tagsHandler.runWhenLoaded(() -> {
            for (ShortcutEntry shortcutEntry : pojos)
                shortcutEntry.setTags(tagsHandler.getTags(shortcutEntry.id));
        });

        return pojos;
    }

}
