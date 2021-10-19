package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;

public class FilterProvider extends DBProvider<FilterEntry> {

    private static final FilterEntry[] s_entries = new FilterEntry[3];
    @StringRes
    private static final int[] s_names = new int[3];

    static {
        int cnt = 0;
        // apps filter
        {
            String id = FilterEntry.SCHEME + "applications";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_apps, AppEntry.SCHEME);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                AppProvider provider = TBApplication.getApplication(ctx).getDataHandler().getAppProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            s_names[cnt] = R.string.filter_apps;
            s_entries[cnt++] = filter;
        }
        // contacts filter
        {
            String id = FilterEntry.SCHEME + "contacts";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_contacts, ContactEntry.SCHEME);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ContactsProvider provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            s_names[cnt] = R.string.filter_contacts;
            s_entries[cnt++] = filter;
        }
        // pinned shortcuts filter
        {
            String id = FilterEntry.SCHEME + "shortcuts";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_shortcuts, ShortcutEntry.SCHEME);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ShortcutsProvider provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            s_names[cnt] = R.string.filter_shortcuts;
            s_entries[cnt++] = filter;
        }

        //noinspection ConstantConditions
        if (cnt != s_entries.length || cnt != s_names.length)
            throw new IllegalStateException("FilterEntry static list size");
    }

    public FilterProvider(Context context) {
        super(context);
    }

    @Override
    protected DBLoader<FilterEntry> newLoadTask() {
        return new UpdateFromModsLoader<>(this, s_entries, s_names);
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(FilterEntry.SCHEME);
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
