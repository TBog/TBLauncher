package rocks.tbog.tblauncher.result;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SectionIndexer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.ui.ListPopup;

public class ResultAdapter extends BaseAdapter implements SectionIndexer, Filterable {

    /**
     * Array list containing all the results currently displayed
     */
    @NonNull
    private final List<EntryItem> results;
    @Nullable
    private List<EntryItem> resultsOriginal = null;

    // Mapping from letter to a position (only used for fast scroll, when viewing app list)
    private HashMap<String, Integer> alphaIndexer = new HashMap<>();
    // List of available sections (only used for fast scroll)
    private String[] sections = new String[0];
    private Filter mFilter = new FilterById();

    public ResultAdapter(ArrayList<EntryItem> results) {
        this.results = results;
    }

    @Override
    public int getViewTypeCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        if (results.get(position) instanceof AppEntry)
            return 1;
        if (results.get(position) instanceof ContactEntry)
            return 2;
        if (results.get(position) instanceof FilterEntry)
            return 3;
        if (results.get(position) instanceof ShortcutEntry)
            return 4;
        return super.getItemViewType(position); // return 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Object getItem(int position) {
        return results.get(position);
    }

    @Override
    public long getItemId(int position) {
        // In some situation, Android tries to display an item that does not exist (e.g. item 24 in a list containing 22 items)
        // See https://github.com/Neamar/KISS/issues/890
        return position < results.size() ? results.get(position).id.hashCode() : -1;
    }

    @Override
    public @NonNull
    View getView(int position, View convertView, @NonNull ViewGroup parent) {
        Context context = parent.getContext();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int drawFlags = EntryItem.FLAG_DRAW_NAME | EntryItem.FLAG_DRAW_LIST;
        if ( prefs.getBoolean("tags-enabled", true) )
            drawFlags |= EntryItem.FLAG_DRAW_TAGS;
        if (prefs.getBoolean("icons-visible", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON;
        if (prefs.getBoolean("shortcut-show-badge", true))
            drawFlags |= EntryItem.FLAG_DRAW_ICON_BADGE;

        EntryItem entryItem = results.get(position);
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            convertView = inflater.inflate(entryItem.getResultLayout(drawFlags), parent, false);
        }
        entryItem.displayResult(convertView, drawFlags);
        return convertView;
    }

    public void onClick(final int position, View v) {
        final EntryItem result;

        try {
            result = results.get(position);
            ResultHelper.launch(result, v);
        } catch (ArrayIndexOutOfBoundsException e) {
//            return;
        }

        // Record the launch after some period,
        // * to ensure the animation runs smoothly
        // * to avoid a flickering -- launchOccurred will refresh the list
        // Thus TOUCH_DELAY * 3
//        Handler handler = new Handler();
//        handler.postDelayed(() -> TBApplication.behaviour(v.getContext()).onLaunchOccurred(), TBApplication.TOUCH_DELAY * 3);
    }

    public boolean onLongClick(final int pos, View v) {
        ListPopup menu;
        try {
            menu = results.get(pos).getPopupMenu(v.getContext(), this, v);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            return false;
        }

        // check if menu contains elements and if yes show it
        if (!menu.getAdapter().isEmpty()) {
            TBApplication.behaviour(v.getContext()).registerPopup(menu);
            menu.show(v);
            return true;
        }

        return false;
    }

    /**
     * Should be called only from Behaviour.
     *
     * @param result what to remove
     */
    public void removeResult(EntryItem result) {
        results.remove(result);
        if (resultsOriginal != null)
            resultsOriginal.remove(result);
        notifyDataSetChanged();
    }

    /**
     * Should be called only from Behaviour.
     *
     * @param results new list of results
     */
    public void updateResults(List<? extends EntryItem> results) {
        resultsOriginal = null;
        this.results.clear();
        this.results.addAll(results);
        notifyDataSetChanged();
    }

//    /**
//     * Force set transcript mode on the list.
//     * Prefer to use `parent.temporarilyDisableTranscriptMode();`
//     */
//    public void updateTranscriptMode(int transcriptMode) {
//        parent.updateTranscriptMode(transcriptMode);
//    }


    public void clear() {
        this.results.clear();
        notifyDataSetChanged();
    }

    /**
     * When using fast scroll, generate a mapping to know where a given letter starts in the list
     * (can only be used with a sorted result set!)
     */
//    public void buildSections() {
//        alphaIndexer.clear();
//        int size = results.size();
//
//        // Generate the mapping letter => number
//        for (int x = 0; x < size; x++) {
//            String s = results.get(x).getSection();
//
//            // Put the first one
//            if (!alphaIndexer.containsKey(s)) {
//                alphaIndexer.put(s, x);
//            }
//        }
//
//        // Generate section list
//        List<Map.Entry<String, Integer>> entries = new ArrayList<>(alphaIndexer.entrySet());
//        Collections.sort(entries, (o1, o2) -> {
//            if (o2.getValue().equals(o1.getValue())) {
//                return 0;
//            }
//            // We're displaying from A to Z, everything needs to be reversed
//            return o2.getValue() > o1.getValue() ? -1 : 1;
//        });
//        sections = new String[entries.size()];
//        for (int i = 0; i < entries.size(); i++) {
//            sections[i] = entries.get(i).getKey();
//        }
//    }
    @Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sections.length == 0) {
            return 0;
        }

        // In some rare situations, the system will ask for a section
        // that does not exist anymore.
        // It's likely there is a threading issue in our code somewhere,
        // But I was unable to find where, so the following line is a quick and dirty fix.
        sectionIndex = Math.max(0, Math.min(sections.length - 1, sectionIndex));
        return alphaIndexer.get(sections[sectionIndex]);
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < sections.length; i++) {
            if (alphaIndexer.get(sections[i]) > position) {
                return i - 1;
            }
        }

        // If apps starting with the letter "A" cover more than a full screen,
        // we will never get > position
        // so we just return the before-last section
        // See #1005
        return sections.length - 2;
    }

    @Override
    public Filter getFilter() {
        if (resultsOriginal == null)
            resultsOriginal = new ArrayList<>(results);
        return mFilter;
    }

    private class FilterById extends Filter {

        //Invoked in a worker thread to filter the data according to the constraint.
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null || constraint.length() == 0 || resultsOriginal == null)
                return null;
            String schema = constraint.toString();
            ArrayList<EntryItem> filterList = new ArrayList<>();
            for (EntryItem entryItem : resultsOriginal) {
                if (entryItem.id.startsWith(schema))
                    filterList.add(entryItem);
            }
            FilterResults filterResults = new FilterResults();
            filterResults.values = filterList;
            filterResults.count = filterList.size();
            return filterResults;
        }

        //Invoked in the UI thread to publish the filtering results in the user interface.
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults filterResults) {
            if (filterResults != null) {
                results.clear();
                results.addAll((List<EntryItem>) filterResults.values);
                notifyDataSetChanged();
            } else if (resultsOriginal != null) {
                results.clear();
                results.addAll(resultsOriginal);
                resultsOriginal = null;
                notifyDataSetChanged();
            }
        }
    }
}
