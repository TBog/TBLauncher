package rocks.tbog.tblauncher.quicklist;

import android.content.ClipData;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.CustomizeUI;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.ActionProvider;
import rocks.tbog.tblauncher.dataprovider.FilterProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.db.ModRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.TagEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.result.EntryAdapter;
import rocks.tbog.tblauncher.result.LoadDataForAdapter;
import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.utils.DebugInfo;

public class EditQuickList {

    private static final String TAG = "EQL";
    private RecycleAdapter mAdapter;
    ViewPager mViewPager;
    private final AdapterView.OnItemClickListener mAddToQuickList = (parent, view, pos, id) -> {
        Object item = parent.getAdapter().getItem(pos);
        if (item instanceof EntryItem) {
            mAdapter.addResult((EntryItem) item);
        }
    };

    public void applyChanges(@NonNull Context context) {
        final int itemCount = mAdapter.getItemCount();
        ArrayList<String> idList = new ArrayList<>(itemCount);
        for (int i = 0; i < itemCount; i++) {
            EntryItem entry = mAdapter.getItem(i);
            idList.add(entry.id);
        }
        TBApplication.dataHandler(context).setQuickList(idList);
    }

    private void addFilters(@NonNull LayoutInflater inflater, @NonNull ArrayList<ViewPagerAdapter.PageInfo> pages) {
        GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
        pages.add(new ViewPagerAdapter.PageInfo(inflater.getContext().getString(R.string.edit_quick_list_tab_filters), gridView));

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

    private void addActions(@NonNull LayoutInflater inflater, @NonNull ArrayList<ViewPagerAdapter.PageInfo> pages) {
        GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
        pages.add(new ViewPagerAdapter.PageInfo(inflater.getContext().getString(R.string.edit_quick_list_tab_actions), gridView));

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

    private void addTags(@NonNull LayoutInflater inflater, @NonNull ArrayList<ViewPagerAdapter.PageInfo> pages) {
        GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
        pages.add(new ViewPagerAdapter.PageInfo(inflater.getContext().getString(R.string.edit_quick_list_tab_tags), gridView));

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

    private void addFavorites(@NonNull LayoutInflater inflater, @NonNull ArrayList<ViewPagerAdapter.PageInfo> pages) {
        GridView gridView = (GridView) inflater.inflate(R.layout.quick_list_editor_page, mViewPager, false);
        pages.add(new ViewPagerAdapter.PageInfo(inflater.getContext().getString(R.string.edit_quick_list_tab_favorites), gridView));

        ArrayList<EntryItem> list = new ArrayList<>();
        EntryAdapter adapter = new EntryAdapter(list);
        gridView.setAdapter(adapter);
        new LoadDataForAdapter(adapter, () -> {
            Context ctx = gridView.getContext();
            DataHandler dataHandler = TBApplication.dataHandler(ctx);
            List<ModRecord> modRecords = dataHandler.getMods();
            ArrayList<EntryItem> data = new ArrayList<>(modRecords.size());
            for (ModRecord fav : modRecords) {
                EntryItem entry = dataHandler.getPojo(fav.record);
                if (entry != null)
                    data.add(entry);
            }
            return data;
        }).execute();
        gridView.setOnItemClickListener(mAddToQuickList);
    }

    public void bindView(@NonNull View view) {
        final Context context = view.getContext();

        mAdapter = new RecycleAdapter(context, new ArrayList<>());
        // the correct grid size will be set later
        DockRecycleLayoutManager layoutManager = new DockRecycleLayoutManager(4, 1);

        // keep the preview the same as the actual thing
        RecyclerList quickListPreview = view.findViewById(R.id.dockPreview);
        quickListPreview.setAdapter(mAdapter);
        quickListPreview.setHasFixedSize(true);
        // the default item animator will mess up when drag and dropping
        quickListPreview.setItemAnimator(null);
        quickListPreview.setLayoutManager(layoutManager);
        // don't snap to pages or else we can't move items between them
        //quickListPreview.addOnScrollListener(new PagedScrollListener());
        quickListPreview.setOnDragListener(EditQuickList::previewDragListener);
        quickListPreview.requestLayout();

        // when user clicks, remove the view and the list item
        mAdapter.setOnClickListener((entry, v) -> mAdapter.removeResult(entry));
        mAdapter.setOnLongClickListener(EditQuickList::previewStartDrag);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        QuickList.applyUiPref(pref, quickListPreview);
        if (!QuickList.populateList(context, mAdapter)) {
            TBApplication.behaviour(context).closeFragmentDialog();
            Toast.makeText(context, "Failed!", Toast.LENGTH_SHORT).show();
        }
        //TODO: implement drag and drop for multiple rows
        layoutManager.setRowCount(1);
        //TODO: implement drag and drop for right to left layout
        layoutManager.setRightToLeft(false);

        mViewPager = view.findViewById(R.id.viewPager);
        {
            TabLayout tabLayout = mViewPager.findViewById(R.id.tabLayout);
            tabLayout.setupWithViewPager(mViewPager);
        }
        {
            ArrayList<ViewPagerAdapter.PageInfo> pages = new ArrayList<>(3);
            LayoutInflater inflater = LayoutInflater.from(context);

            // actions
            addActions(inflater, pages);

            // filters
            addFilters(inflater, pages);

            // tags
            addTags(inflater, pages);

            if (DebugInfo.enableFavorites(context)) {
                // favorites
                addFavorites(inflater, pages);
            }

            pages.trimToSize();
            mViewPager.setAdapter(new ViewPagerAdapter(pages));

            for (ViewPagerAdapter.PageInfo page : pages) {
                CustomizeUI.setResultListPref(page.getView());
            }
        }
    }

    /**
     * Start drag and drop action.
     *
     * @param entry the EntryItem we are moving
     * @param v     The view we are dragging
     * @return if the startDrag method completes successfully
     */
    private static boolean previewStartDrag(@NonNull EntryItem entry, @NonNull View v) {
        final DragAndDropInfo dragDropInfo = new DragAndDropInfo();
        int idx = ((ViewGroup) v.getParent()).indexOfChild(v);
        dragDropInfo.overChildIdx = idx;
        dragDropInfo.draggedView = v;
        ClipData clipData = ClipData.newPlainText(Integer.toString(idx), entry.id);
        View.DragShadowBuilder shadow = new View.DragShadowBuilder(v);
        v.setVisibility(View.INVISIBLE);
        return startDragAndDrop(v, clipData, shadow, dragDropInfo);
    }

    @SuppressWarnings("deprecation")
    private static boolean startDragAndDrop(@NonNull View v, ClipData clipData, View.DragShadowBuilder shadow, DragAndDropInfo dragDropInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return v.startDragAndDrop(clipData, shadow, dragDropInfo, 0);
        } else {
            return v.startDrag(clipData, shadow, dragDropInfo, 0);
        }
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
                if (dragDropInfo.overChildIdx == location)
                    return true;

                final int emptyLocation = quickList.indexOfChild(dragDropInfo.draggedView);
                if (location < emptyLocation) {
                    repositionViews(quickList, location, emptyLocation, 0);
                } else {
                    repositionViews(quickList, emptyLocation + 1, 0, location + 1);
                }

                dragDropInfo.overChildIdx = location;
//                Log.d(TAG, "location = " + location);
                return true;
            }
            case DragEvent.ACTION_DRAG_EXITED:
                // if dragging outside, reset locations
                dragDropInfo.overChildIdx = quickList.indexOfChild(dragDropInfo.draggedView);
                repositionViews(quickList, dragDropInfo.overChildIdx, 0, 0);
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
                RecyclerView.Adapter<?> adapter = quickList instanceof RecyclerList ? ((RecyclerList) quickList).getAdapter() : null;
                if (adapter instanceof RecycleAdapter) {
                    int initialPosition = ((RecyclerView.LayoutParams) dragDropInfo.draggedView.getLayoutParams()).getViewAdapterPosition();
                    View overChild = quickList.getChildAt(dragDropInfo.overChildIdx);
                    int newPosition = ((RecyclerView.LayoutParams) overChild.getLayoutParams()).getViewAdapterPosition();
                    if (initialPosition != newPosition) {
                        ((RecycleAdapter) adapter).moveResult(initialPosition, newPosition);
                        ((RecyclerList) quickList).scrollToPosition(newPosition);
                    }
                }
                // check event.getResult() if dropping outside should matter
                dragDropInfo.draggedView.setVisibility(View.VISIBLE);
                return false;
            }
        }
    }
}
