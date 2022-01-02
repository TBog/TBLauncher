package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.searcher.HistorySearcher;
import rocks.tbog.tblauncher.searcher.TagList;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DebugInfo;

public class ActionProvider extends DBProvider<ActionEntry> {

    private static final ActionEntry[] s_entries = new ActionEntry[17];
    @StringRes
    private static final int[] s_names = new int[17];

    static {
        int cnt = 0;
        {
            String id = ActionEntry.SCHEME + "toggle/grid";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_grid);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                Behaviour behaviour = TBApplication.behaviour(ctx);

                // toggle grid/list layout
                if (behaviour.isGridLayout())
                    behaviour.setListLayout();
                else
                    behaviour.setGridLayout();
            });
            s_names[cnt] = R.string.action_toggle_grid;
            s_entries[cnt++] = actionEntry;
        }
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
        // show favorites sorted by name (removed by load task if not enabled)
        {
            String id = ActionEntry.SCHEME + "show/favorites/byName";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_favorites);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                ModProvider provider = TBApplication.dataHandler(ctx).getModProvider();
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
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_untagged);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "untagged", TagList.class);
            });
            s_names[cnt] = R.string.action_show_untagged;
            s_entries[cnt++] = actionEntry;
        }
        {
            String id = ActionEntry.SCHEME + "show/tags/menu";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_tags);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication app = TBApplication.getApplication(ctx);
                ListPopup menu = app.tagsHandler().getTagsMenu(ctx);
                app.registerPopup(menu);
                menu.show(v);
            });
            s_names[cnt] = R.string.show_tags_menu;
            s_entries[cnt++] = actionEntry;
        }
        {
            String id = ActionEntry.SCHEME + "show/tags/list";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_tags);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "list", TagList.class);
            });
            s_names[cnt] = R.string.show_tags_list;
            s_entries[cnt++] = actionEntry;
        }
        {
            String id = ActionEntry.SCHEME + "show/tags/listReversed";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_tags);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.quickList(ctx).toggleSearch(v, "listReversed", TagList.class);
            });
            s_names[cnt] = R.string.show_tags_list_reversed;
            s_entries[cnt++] = actionEntry;
        }
        {
            String id = ActionEntry.SCHEME + "reload/providers";
            ActionEntry actionEntry = new ActionEntry(id, R.drawable.ic_refresh);
            actionEntry.setAction((v, flags) -> {
                Context ctx = v.getContext();
                TBApplication.dataHandler(ctx).reloadProviders();
            });
            s_names[cnt] = R.string.action_reload;
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
        return new UpdateFromModsLoader<ActionEntry>(this, s_entries, s_names) {
            @Override
            public List<ActionEntry> getEntryItems(DataHandler dataHandler) {
                List<ActionEntry> entries = super.getEntryItems(dataHandler);
                Context context = dataHandler.getContext();
                if (context == null || !DebugInfo.enableFavorites(context)) {
                    // remove debug entry
                    for (Iterator<ActionEntry> iterator = entries.iterator(); iterator.hasNext(); ) {
                        ActionEntry entry = iterator.next();
                        if (entry.id.endsWith("show/favorites/byName"))
                            iterator.remove();
                    }
                }
                return entries;
            }
        };
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(ActionEntry.SCHEME);
    }

    @NonNull
    public String getDefaultName(@NonNull String id) {
        for (int idx = 0; idx < s_entries.length; idx += 1) {
            if (id.equals(s_entries[idx].id))
                return context.getString(s_names[idx]);
        }
        return "null";
    }
}
