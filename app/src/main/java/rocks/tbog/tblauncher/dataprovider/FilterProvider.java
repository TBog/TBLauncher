package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;

public class FilterProvider extends StaticProvider<FilterEntry> {

    public FilterProvider(Context context) {
        super(new ArrayList<>(3));
        // apps filter
        {
            String id = FilterEntry.SCHEME + "applications";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_apps);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                AppProvider provider = TBApplication.getApplication(ctx).getDataHandler().getAppProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            filter.setName(context.getResources().getString(R.string.filter_apps));
            pojos.add(filter);
        }
        // contacts filter
        {
            String id = FilterEntry.SCHEME + "contacts";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_contacts);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ContactsProvider provider = TBApplication.dataHandler(ctx).getContactsProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            filter.setName(context.getResources().getString(R.string.filter_contacts));
            pojos.add(filter);
        }
        // pinned shortcuts filter
        {
            String id = FilterEntry.SCHEME + "shortcuts";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_shortcuts);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ShortcutsProvider provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            filter.setName(context.getResources().getString(R.string.filter_shortcuts));
            pojos.add(filter);
        }
        // TODO: somehow enable filtering for the favorites, right now we can only show them
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(FilterEntry.SCHEME);
    }
}
