package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.Collections;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.searcher.SortedTagSearcher;
import rocks.tbog.tblauncher.ui.LinearAdapter;

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
        }
        super.setName(name);
    }

    @Override
    public void doLaunch(@NonNull View v, int flags) {
        if (TBApplication.activityInvalid(v))
            return;
        Context ctx = v.getContext();
        TBApplication.quickList(ctx).toggleSearch(v, id, SortedTagSearcher.class);
    }

    @Override
    protected void buildPopupMenuCategory(Context context, @NonNull LinearAdapter adapter, int titleStringId) {
        if (titleStringId == R.string.popup_title_hist_fav) {
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_az));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_za));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_rec));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_freq));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_frec));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_tag_sort_hist_adaptive));
        }
    }

    @Override
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        if (stringId == R.string.menu_tag_sort_az) {
            Toast.makeText(view.getContext(), "WIP: sort AZ", Toast.LENGTH_SHORT).show();
            return true;
        } else if (stringId == R.string.menu_tag_sort_za) {
            Toast.makeText(view.getContext(), "WIP: sort ZA", Toast.LENGTH_SHORT).show();
            return true;
        }else if (stringId == R.string.menu_tag_sort_hist_rec) {
            Toast.makeText(view.getContext(), "WIP: sort hist rec", Toast.LENGTH_SHORT).show();
            return true;
        }else if (stringId == R.string.menu_tag_sort_hist_freq) {
            Toast.makeText(view.getContext(), "WIP: sort hist freq", Toast.LENGTH_SHORT).show();
            return true;
        }else if (stringId == R.string.menu_tag_sort_hist_frec) {
            Toast.makeText(view.getContext(), "WIP: sort hist frec", Toast.LENGTH_SHORT).show();
            return true;
        }else if (stringId == R.string.menu_tag_sort_hist_adaptive) {
            Toast.makeText(view.getContext(), "WIP: sort hist adaptive", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
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
        return !getTagSortOrder(id).isEmpty();
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
