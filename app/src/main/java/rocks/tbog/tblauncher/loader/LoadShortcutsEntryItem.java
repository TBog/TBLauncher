package rocks.tbog.tblauncher.loader;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;

public class LoadShortcutsEntryItem extends LoadEntryItem<ShortcutEntry> {

    //private final TagsHandler tagsHandler;

    public LoadShortcutsEntryItem(Context context) {
        super(context, ShortcutEntry.SCHEME);
        //tagsHandler = TBApplication.getApplication(context).getDataHandler().getTagsHandler();
    }

    @Override
    protected ArrayList<ShortcutEntry> doInBackground(Void... arg0) {
        if (context.get() == null) {
            return new ArrayList<>();
        }

        List<ShortcutRecord> records = DBHelper.getShortcuts(context.get());
        ArrayList<ShortcutEntry> pojos = new ArrayList<>(records.size());

        for (ShortcutRecord shortcutRecord : records) {
            String id = ShortcutUtil.generateShortcutId(shortcutRecord.name);

            ShortcutEntry pojo = new ShortcutEntry(id, shortcutRecord.dbId, shortcutRecord.packageName, shortcutRecord.intentUri);

            pojo.setName(shortcutRecord.name);
            //pojo.setTags(tagsHandler.getTags(pojo.id));

            pojos.add(pojo);
        }

        return pojos;
    }

}
