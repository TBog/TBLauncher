package rocks.tbog.tblauncher.quicklist;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class PagedScrollListener extends RecyclerView.OnScrollListener {

    private static final String TAG = "PagedSL";

    public PagedScrollListener() {
    }

    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            snapToPage(recyclerView);
        }
    }

    public static void snapToPage(@NonNull RecyclerView recyclerView) {
        if (recyclerView.getLayoutManager() instanceof DockRecycleLayoutManager) {
            DockRecycleLayoutManager dockRecycleLayoutManager = (DockRecycleLayoutManager) recyclerView.getLayoutManager();
            final float scroll = dockRecycleLayoutManager.getPageScroll();
            final int page = (int) (scroll + .5f);
            final float delta = scroll - page;
            Log.d(TAG, "snapToPage: pageScroll=" + scroll + " delta=" + delta);
            final int pos;
            if (delta > .01f) {
                pos = dockRecycleLayoutManager.getPageAdapterPosition(page);
            } else if (delta < -0.01f) {
                pos = dockRecycleLayoutManager.getPageAdapterPosition(page + 1) - 1;
            } else {
                return;
            }
            Log.d(TAG, "smoothScrollToPosition " + pos + " page=" + page);
            recyclerView.smoothScrollToPosition(pos);
        }
    }
}
