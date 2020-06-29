package rocks.tbog.tblauncher.dataprovider;

import android.content.Context;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.searcher.Searcher;

public class FilterProvider implements IProvider {

    final List<FilterEntry> pojos;

    public FilterProvider(Context context) {
        pojos = new ArrayList<>(3);
        // apps filter
        {
            String id = FilterEntry.SCHEME + "applications";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_android, AppEntry.SCHEME);
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
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_contact, ContactEntry.SCHEME);
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
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_send, ShortcutEntry.SCHEME);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                ShortcutsProvider provider = TBApplication.dataHandler(ctx).getShortcutsProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider);
            });
            filter.setName(context.getResources().getString(R.string.filter_shortcuts));
            pojos.add(filter);
        }
        // favorites filter
        {
            String id = FilterEntry.SCHEME + "favorites";
            String filterScheme = "(fav)";
            FilterEntry filter = new FilterEntry(id, R.drawable.ic_favorite, filterScheme);
            filter.setOnClickListener(v -> {
                Context ctx = v.getContext();
                FavProvider provider = TBApplication.dataHandler(ctx).getFavProvider();
                TBApplication.quickList(ctx).toggleFilter(v, provider, filterScheme);
            });
            filter.setName(context.getResources().getString(R.string.filter_favorites));
            pojos.add(filter);
        }
    }

    @Override
    public void requestResults(String s, Searcher searcher) {

    }

    public void reload(boolean cancelCurrentLoadTask) {
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void setDirty() {
        // do nothing, we already have the full list of items
    }

    @Override
    public int getLoadStep() {
        return LOAD_STEP_1;
    }

    @Override
    public boolean mayFindById(@NonNull String id) {
        return id.startsWith(FilterEntry.SCHEME);
    }

    @Override
    public EntryItem findById(@NonNull String id) {
        for (EntryItem pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }

    @Override
    public List<? extends EntryItem> getPojos() {
        if (BuildConfig.DEBUG)
            return Collections.unmodifiableList(pojos);
        return pojos;
    }
}
