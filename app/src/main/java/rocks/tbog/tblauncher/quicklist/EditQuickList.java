package rocks.tbog.tblauncher.quicklist;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.mm2d.color.chooser.ViewPagerAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kotlin.Pair;
import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.DataHandler;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ActionProvider;
import rocks.tbog.tblauncher.dataprovider.FilterProvider;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.db.FavRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.result.EntryAdapter;
import rocks.tbog.tblauncher.result.LoadDataForAdapter;

public class EditQuickList {

    private static final String TAG = "EQL";
    private final ArrayList<EntryItem> mQuickList = new ArrayList<>();
    private LinearLayout mQuickListContainer;
    ViewPager mViewPager;
    private SharedPreferences mPref;
    private final AdapterView.OnItemClickListener mAddToQuickList = (parent, view, pos, id) -> {
        Object item = parent.getAdapter().getItem(pos);
        if (item instanceof EntryItem) {
            mQuickList.add((EntryItem) item);
            populateList();
        }
    };

    public void applyChanges(@NonNull Context context) {
        ArrayList<String> idList = new ArrayList<>(mQuickList.size());
        for (EntryItem entry : mQuickList)
            idList.add(entry.id);
        TBApplication.dataHandler(context).setQuickList(idList);
    }

    public void bindView(@NonNull View view) {
        final Context context = view.getContext();
        // keep the preview the same as the actual thing
        mQuickListContainer = view.findViewById(R.id.preview);
        {
            QuickListProvider provider = TBApplication.dataHandler(context).getQuickListProvider();
            List<? extends EntryItem> list = provider != null ? provider.getPojos() : null;
            if (list != null)
                mQuickList.addAll(list);
        }
        mPref = PreferenceManager.getDefaultSharedPreferences(context);
        QuickList.applyUiPref(mPref, mQuickListContainer);
        populateList();

        mViewPager = view.findViewById(R.id.viewPager);
        {
            TabLayout tabLayout = mViewPager.findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(mViewPager);
        }
        {
            ArrayList<Pair<String, View>> pages = new ArrayList<>();
            LayoutInflater inflater = LayoutInflater.from(context);

            // filters
            {
                GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
                pages.add(new Pair<>(context.getString(R.string.edit_quick_list_tab_filters), gridView));

                ArrayList<EntryItem> list = new ArrayList<>();
                EntryAdapter adapter = new EntryAdapter(list);
                gridView.setAdapter(adapter);
                new LoadDataForAdapter(adapter, () -> {
                    Context ctx = gridView.getContext();
                    ArrayList<EntryItem> data = new ArrayList<>();
                    {
                        FilterProvider provider = TBApplication.dataHandler(ctx).getFilterProvider();
                        if (provider != null) {
                            List<? extends EntryItem> entryItems = provider.getPojos();
                            data.addAll(entryItems);
                        }
                    }
                    return data;
                }).execute();
                gridView.setOnItemClickListener(mAddToQuickList);
            }

            // actions
            {
                GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
                pages.add(new Pair<>(context.getString(R.string.edit_quick_list_tab_actions), gridView));

                ArrayList<EntryItem> list = new ArrayList<>();
                EntryAdapter adapter = new EntryAdapter(list);
                gridView.setAdapter(adapter);
                new LoadDataForAdapter(adapter, () -> {
                    Context ctx = gridView.getContext();
                    ArrayList<EntryItem> data = new ArrayList<>();
                    {
                        ActionProvider provider = TBApplication.dataHandler(ctx).getActionProvider();
                        if (provider != null) {
                            List<? extends EntryItem> entryItems = provider.getPojos();
                            data.addAll(entryItems);
                        }
                    }
                    return data;
                }).execute();
                gridView.setOnItemClickListener(mAddToQuickList);
            }

            // tags
            {
                GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
                pages.add(new Pair<>(context.getString(R.string.edit_quick_list_tab_tags), gridView));

                ArrayList<EntryItem> list = new ArrayList<>();
                EntryAdapter adapter = new EntryAdapter(list);
                gridView.setAdapter(adapter);
                new LoadDataForAdapter(adapter, () -> {
                    Context ctx = gridView.getContext();
                    ArrayList<EntryItem> data = new ArrayList<>();
                    {
                        TagsProvider tagsProvider = TBApplication.dataHandler(ctx).getTagsProvider();
                        if (tagsProvider != null) {
                            List<String> tagNameList = new ArrayList<>(TBApplication.tagsHandler(ctx).getValidTags());
                            Collections.sort(tagNameList);
                            for (String tagName : tagNameList) {
                                TagEntry tagEntry = tagsProvider.getTagEntry(tagName);
                                data.add(tagEntry);
                            }
                        }
                    }
                    return data;
                }).execute();
                gridView.setOnItemClickListener(mAddToQuickList);
            }

            // favorites
            {
                GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
                pages.add(new Pair<>(context.getString(R.string.edit_quick_list_tab_favorites), gridView));

                ArrayList<EntryItem> list = new ArrayList<>();
                EntryAdapter adapter = new EntryAdapter(list);
                gridView.setAdapter(adapter);
                new LoadDataForAdapter(adapter, () -> {
                    Context ctx = gridView.getContext();
                    DataHandler dataHandler = TBApplication.dataHandler(ctx);
                    ArrayList<FavRecord> favRecords = dataHandler.getFavorites();
                    ArrayList<EntryItem> data = new ArrayList<>(favRecords.size());
                    for (FavRecord fav : favRecords) {
                        EntryItem entry = dataHandler.getPojo(fav.record);
                        // we have a separate section for StaticEntry (filters and actions), don't duplicate
                        if (entry != null && !(entry instanceof StaticEntry))
                            data.add(entry);
                    }
                    return data;
                }).execute();
                gridView.setOnItemClickListener(mAddToQuickList);
            }

            pages.trimToSize();
            mViewPager.setAdapter(new ViewPagerAdapter(pages));

            CustomizeUI customizeUI = TBApplication.ui(context);
            for (Pair<String, View> page : pages) {
                customizeUI.setResultListPref(page.getSecond());
            }
        }
    }

    /**
     * Start drag and drop action.
     *
     * @param v         The view we are dragging
     * @param quickList The list we intend to reorder
     * @return if the startDrag method completes successfully
     */
    private static boolean previewStartDrag(@NonNull View v, @NonNull ArrayList<EntryItem> quickList) {
        final DragAndDropInfo dragDropInfo = new DragAndDropInfo(quickList);
        int idx = ((ViewGroup) v.getParent()).indexOfChild(v);
        dragDropInfo.location = idx;
        dragDropInfo.draggedEntry = quickList.get(idx);
        dragDropInfo.draggedView = v;
        ClipData clipData = ClipData.newPlainText(Integer.toString(idx), dragDropInfo.draggedEntry.id);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
        v.setVisibility(View.INVISIBLE);
        return v.startDrag(clipData, shadow, dragDropInfo, 0);
    }

    protected static void repositionViews(@NonNull ViewGroup quickList, int resetUntilIdx, int moveRightUntilIdx, int moveLeftUntilIdx) {
        int idx = 0;
        for (; idx < resetUntilIdx; idx += 1) {
            View child = quickList.getChildAt(idx);
            child.animate().translationX(0f);
            //Log.d(TAG, "child #" + idx + " reset pos");
        }
        for (; idx < moveRightUntilIdx; idx += 1) {
            View child = quickList.getChildAt(idx);
            child.animate().translationX(child.getWidth());
            //Log.d(TAG, "child #" + idx + " move right");
        }
        for (; idx < moveLeftUntilIdx; idx += 1) {
            View child = quickList.getChildAt(idx);
            child.animate().translationX(-child.getWidth());
            //Log.d(TAG, "child #" + idx + " move left");
        }
        final int childCount = quickList.getChildCount();
        for (; idx < childCount; idx += 1) {
            View child = quickList.getChildAt(idx);
            child.animate().translationX(0f);
            //Log.d(TAG, "child #" + idx + " reset pos");
        }
    }

    private static boolean previewDragListener(@Nullable View v, @NonNull DragEvent event) {
        final DragAndDropInfo dragDropInfo;
        final ViewGroup quickList;

        Object local = event.getLocalState();
        if (!(local instanceof DragAndDropInfo)) {
            Log.d(TAG, "drag outside activity?");
            return true;
        }
        if (!(v instanceof ViewGroup)) {
            Log.d(TAG, "only QuickList should listen");
            return true;
        }

        dragDropInfo = (DragAndDropInfo) local;
        quickList = (ViewGroup) v;

        switch (event.getAction()) {
            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DROP:
                return true;
            case DragEvent.ACTION_DRAG_LOCATION: {
                final float x = event.getX();
                // find new location index
                int location = 0;
                final int childCount = quickList.getChildCount();
                for (int idx = 0; idx < childCount; idx += 1) {
                    View child = quickList.getChildAt(idx);
                    int left = child.getLeft();
                    //Log.d(TAG, "child #" + idx + " left = " + left);
                    if (left > x)
                        break;
                    location = idx;
                }

                // check if we already processed this location
                if (dragDropInfo.location == location)
                    return true;

                final int emptyLocation = quickList.indexOfChild(dragDropInfo.draggedView);
                if (location < emptyLocation) {
                    repositionViews(quickList, location, emptyLocation, 0);
                } else {
                    repositionViews(quickList, emptyLocation + 1, 0, location + 1);
                }

                dragDropInfo.location = location;
//                Log.d(TAG, "location = " + location);
                return true;
            }
            case DragEvent.ACTION_DRAG_EXITED:
                // if dragging outside, reset locations
                dragDropInfo.location = quickList.indexOfChild(dragDropInfo.draggedView);
                repositionViews(quickList, dragDropInfo.location, 0, 0);
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
            default: {
                //Log.d(TAG, "drag ended");
                final int childCount = quickList.getChildCount();
                for (int idx = 0; idx < childCount; idx += 1) {
                    View child = quickList.getChildAt(idx);
                    child.animate().cancel();
                    child.setTranslationX(0f);
                }
                int initialLocation = quickList.indexOfChild(dragDropInfo.draggedView);
                // check event.getResult() if dropping outside should matter
                if (initialLocation != dragDropInfo.location) {
                    quickList.removeViewAt(initialLocation);
                    quickList.addView(dragDropInfo.draggedView, dragDropInfo.location);
                    dragDropInfo.list.remove(dragDropInfo.draggedEntry);
                    dragDropInfo.list.add(dragDropInfo.location, dragDropInfo.draggedEntry);
                }
                dragDropInfo.draggedView.setVisibility(View.VISIBLE);
                return false;
            }
        }
    }

    private void populateList() {
        Context context = mQuickListContainer.getContext();

        mQuickListContainer.removeAllViews();
        int drawFlags = QuickList.getDrawFlags(mPref) | EntryItem.FLAG_DRAW_NO_CACHE;
        for (EntryItem entry : mQuickList) {
            View view = LayoutInflater.from(context).inflate(entry.getResultLayout(drawFlags), mQuickListContainer, false);
            entry.displayResult(view, drawFlags);
            mQuickListContainer.addView(view);

            // when user clicks, remove the view and the list item
            view.setOnClickListener((v) -> {
                if (v.getParent() instanceof ViewGroup) {
                    int idx = ((ViewGroup) v.getParent()).indexOfChild(v);
                    ((ViewGroup) v.getParent()).removeViewAt(idx);
                    mQuickList.remove(idx);
                }
            });

            view.setOnLongClickListener(v -> previewStartDrag(v, mQuickList));
        }
        mQuickListContainer.setOnDragListener(EditQuickList::previewDragListener);
        mQuickListContainer.requestLayout();
    }
}
