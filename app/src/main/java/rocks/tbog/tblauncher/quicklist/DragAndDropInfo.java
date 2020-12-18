package rocks.tbog.tblauncher.quicklist;

import android.view.View;

import java.util.ArrayList;

import rocks.tbog.tblauncher.entry.EntryItem;

class DragAndDropInfo {
    public final ArrayList<EntryItem> list;
    public View draggedView;
    public EntryItem draggedEntry;
    public int location;

    DragAndDropInfo(ArrayList<EntryItem> quickList) {
        list = quickList;
    }
}
