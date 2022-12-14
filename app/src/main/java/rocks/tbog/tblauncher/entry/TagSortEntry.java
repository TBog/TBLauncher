package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;

import java.util.Collections;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.searcher.SortedTagSearcher;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;

public class TagSortEntry extends TagEntry {
    public static final String SORT_AZ = "sort/byName/";
    public static final String SORT_ZA = "sort/byNameReversed/";
    public static final String HISTORY_REC = "sort/history/recency/";
    public static final String HISTORY_FREQ = "sort/history/frequency/";
    public static final String HISTORY_FREC = "sort/history/frecency/";
    public static final String HISTORY_ADAPTIVE = "sort/history/adaptive/";

    public TagSortEntry(@NonNull String id) {
        super(id);
        if (BuildConfig.DEBUG) {
            String action = getTagSortOrder(id);
            if (action.isEmpty())
                throw new IllegalStateException("Invalid " + TagSortEntry.class.getSimpleName() + " id `" + id + "`");
        }
    }

    @Override
    public void setName(String name) {
        if (name == null) {
            // find the default name from the id
            String action = getTagSortOrder(id);
            super.setName(id.substring(SCHEME.length() + action.length()));
        } else {
            super.setName(name);
        }
    }

    @Override
    ListPopup inflatePopupMenu(@NonNull Context context, @NonNull LinearAdapter adapter) {
        // this tag is already sorted, remove the option to sort it the same
        var sortOrder = getTagSortOrder(id);
        for (int i = 0; i < adapter.getCount(); i += 1) {
            LinearAdapter.MenuItem item = adapter.getItem(i);
            if (item instanceof LinearAdapter.Item) {
                var itemStringId = ((LinearAdapter.Item) item).stringId;
                if ((itemStringId == R.string.menu_tag_sort_az && SORT_AZ.equals(sortOrder))
                    || (itemStringId == R.string.menu_tag_sort_za && SORT_ZA.equals(sortOrder))
                    || (itemStringId == R.string.menu_tag_sort_hist_rec && HISTORY_REC.equals(sortOrder))
                    || (itemStringId == R.string.menu_tag_sort_hist_freq && HISTORY_FREQ.equals(sortOrder))
                    || (itemStringId == R.string.menu_tag_sort_hist_frec && HISTORY_FREC.equals(sortOrder))
                    || (itemStringId == R.string.menu_tag_sort_hist_adaptive && HISTORY_ADAPTIVE.equals(sortOrder))) {
                    adapter.remove(item);
                    break;
                }
            }
        }
        return super.inflatePopupMenu(context, adapter);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        if (TBApplication.activityInvalid(v))
            return;
        Context ctx = v.getContext();
        TBApplication.quickList(ctx).toggleSearch(v, id, SortedTagSearcher.class);
    }

    @NonNull
    public static String getTagSortOrder(String id) {
        String idWithoutScheme = id.substring(SCHEME.length());
        if (idWithoutScheme.startsWith(SORT_AZ))
            return SORT_AZ;
        if (idWithoutScheme.startsWith(SORT_ZA))
            return SORT_ZA;
        if (idWithoutScheme.startsWith(HISTORY_REC))
            return HISTORY_REC;
        if (idWithoutScheme.startsWith(HISTORY_FREQ))
            return HISTORY_FREQ;
        if (idWithoutScheme.startsWith(HISTORY_FREC))
            return HISTORY_FREC;
        if (idWithoutScheme.startsWith(HISTORY_ADAPTIVE))
            return HISTORY_ADAPTIVE;
        return "";
    }

    public static boolean isTagSort(String id) {
        return id.startsWith(SCHEME) && !getTagSortOrder(id).isEmpty();
    }

    public static class TagDetails {
        public final String name;
        public final String order;

        public TagDetails(@NonNull String tagSortId) {
            order = getTagSortOrder(tagSortId);
            name = tagSortId.substring(SCHEME.length() + order.length());
        }

        public java.util.Comparator<EntryItem> getComparator() {
            switch (order) {
                case SORT_AZ:
                    return NAME_COMPARATOR;
                case SORT_ZA:
                    return Collections.reverseOrder(NAME_COMPARATOR);
                default:
                    return RELEVANCE_COMPARATOR;
            }
        }
    }
}
