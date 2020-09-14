package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;

public class ActionProvider extends StaticProvider<ActionEntry> {

    public ActionProvider(@NonNull Context context) {
        super(new ArrayList<>(7));
        // show apps sorted by name
        {
            String id = ActionEntry.SCHEME + "show/apps/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_apps_az);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getAppProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_apps));
            pojos.add(actionEntry);
        }
        // show apps sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/apps/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_apps_za);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getAppProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_apps_reversed));
            pojos.add(actionEntry);
        }
        // show contacts sorted by name
        {
            String id = ActionEntry.SCHEME + "show/contacts/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_contacts_az);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_contacts));
            pojos.add(actionEntry);
        }
        // show contacts sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/contacts/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_contacts_za);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_contacts_reversed));
            pojos.add(actionEntry);
        }
        // show shortcuts sorted by name
        {
            String id = ActionEntry.SCHEME + "show/shortcuts/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_shortcuts_az);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_shortcuts));
            pojos.add(actionEntry);
        }
        // show shortcuts sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/shortcuts/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_shortcuts_za);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_shortcuts));
            pojos.add(actionEntry);
        }
        // show favorites sorted by name
        {
            String id = ActionEntry.SCHEME + "show/favorites/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_favorites);
            actionEntry.setAction(v -> {
                Context ctx = v.getContext();
                FavProvider provider = TBApplication.dataHandler(ctx).getFavProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            actionEntry.setName(context.getResources().getString(R.string.action_show_favorites));
            pojos.add(actionEntry);
        }
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(ActionEntry.SCHEME);
    }
}
