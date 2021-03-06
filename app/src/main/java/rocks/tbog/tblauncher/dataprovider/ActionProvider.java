package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Collections;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.searcher.HistorySearcher;
import rocks.tbog.tblauncher.searcher.TagSearcher;

public class ActionProvider extends DBProvider<ActionEntry> {

    private static final ActionEntry[] s_entries = new ActionEntry[12];
    @StringRes
    private static final int[] s_names = new int[12];

    static {
        int cnt = 0;
        {
            String id = ActionEntry.SCHEME + "show/apps/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_apps_az);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getAppProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            s_names[cnt] = R.string.action_show_apps;
            s_entries[cnt++] = actionEntry;
        }
        // show apps sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/apps/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_apps_za);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getAppProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            s_names[cnt] = R.string.action_show_apps_reversed;
            s_entries[cnt++] = actionEntry;
        }
        // show contacts sorted by name
        {
            String id = ActionEntry.SCHEME + "show/contacts/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_contacts_az);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            s_names[cnt] = R.string.action_show_contacts;
            s_entries[cnt++] = actionEntry;
        }
        // show contacts sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/contacts/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_contacts_za);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            s_names[cnt] = R.string.action_show_contacts_reversed;
            s_entries[cnt++] = actionEntry;
        }
        // show shortcuts sorted by name
        {
            String id = ActionEntry.SCHEME + "show/shortcuts/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_shortcuts_az);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            s_names[cnt] = R.string.action_show_shortcuts;
            s_entries[cnt++] = actionEntry;
        }
        // show shortcuts sorted by name in reverse order
        {
            String id = ActionEntry.SCHEME + "show/shortcuts/byNameReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_shortcuts_za);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Provider<? extends EntryItem> provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, Collections.reverseOrder(EntryItem.NAME_COMPARATOR));
            });
            s_names[cnt] = R.string.action_show_shortcuts_reversed;
            s_entries[cnt++] = actionEntry;
        }
        // show favorites sorted by name
        {
            String id = ActionEntry.SCHEME + "show/favorites/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_favorites);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                FavProvider provider = TBApplication.dataHandler(ctx).getFavProvider();
                TBApplication.quickList(ctx).toggleProvider(v, provider, EntryItem.NAME_COMPARATOR);
            });
            s_names[cnt] = R.string.action_show_favorites;
            s_entries[cnt++] = actionEntry;
        }
        // show history sorted by how recent it was accessed
        {
            String id = ActionEntry.SCHEME + "show/history/recency";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_history);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "recency", HistorySearcher.class);
            });
            s_names[cnt] = R.string.action_show_history_recency;
            s_entries[cnt++] = actionEntry;
        }
        // show history sorted by how frequent it was accessed
        {
            String id = ActionEntry.SCHEME + "show/history/frequency";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_history);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "frequency", HistorySearcher.class);
            });
            s_names[cnt] = R.string.action_show_history_frequency;
            s_entries[cnt++] = actionEntry;
        }
        // show history sorted based on frequency * recency
        // frequency = #launches_for_app / #all_launches
        // recency = 1 / position_of_app_in_normal_history
        {
            String id = ActionEntry.SCHEME + "show/history/frecency";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_history);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "frecency", HistorySearcher.class);
            });
            s_names[cnt] = R.string.action_show_history_frecency;
            s_entries[cnt++] = actionEntry;
        }
        // show history sorted by how frequent it was accessed in the last 36 hours
        {
            String id = ActionEntry.SCHEME + "show/history/adaptive";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_history);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "adaptive", HistorySearcher.class);
            });
            s_names[cnt] = R.string.action_show_history_adaptive;
            s_entries[cnt++] = actionEntry;
        }
        {
            String id = ActionEntry.SCHEME + "show/untagged";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_tags);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "", TagSearcher.class);
            });
            s_names[cnt] = R.string.action_show_untagged;
            s_entries[cnt++] = actionEntry;
        }
        //noinspection ConstantConditions
        if (cnt != s_entries.length || cnt != s_names.length)
            throw new IllegalStateException("ActionEntry static list size");
    }

    public ActionProvider(@NonNull Context context) {
        super(context);
    }

    @Override
    protected DBLoader<ActionEntry> newLoadTask() {
        return new UpdateFromFavoritesLoader<>(this, s_entries, s_names);
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(ActionEntry.SCHEME);
    }

}
