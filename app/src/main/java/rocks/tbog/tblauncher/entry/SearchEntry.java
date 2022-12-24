package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.appcompat.content.res.AppCompatResources;

import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.preference.ContentLoadHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class SearchEntry extends EntryItem implements ICustomIconEntry {

    private static final int[] RESULT_LAYOUT = {R.layout.item_builtin, R.layout.item_grid, R.layout.item_dock};

    protected String query;
    private int customIcon;

    public SearchEntry(String id) {
        super(id);
    }

    public void setQuery(@NonNull String query) {
        this.query = query;
    }

    @Override
    public String getHistoryId() {
        // Search POJO should not appear in history
        return "";
    }

    @NonNull
    @Override
    public String getIconCacheId() {
        return id + "/ic" + customIcon;
    }

    @Override
    public void setCustomIcon() {
        customIcon += 1;
    }

    @Override
    public void clearCustomIcon() {
        customIcon = 0;
    }

    @Override
    public boolean hasCustomIcon() {
        return customIcon > 0;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @WorkerThread
    public Drawable getIconDrawable(Context context) {
        if (hasCustomIcon()) {
            IconsHandler iconsHandler = TBApplication.getApplication(context).iconsHandler();
            Drawable drawable = iconsHandler.getCustomIcon(this);
            if (drawable != null)
                return drawable;
            else
                iconsHandler.restoreDefaultIcon(this);
        }
        return getDefaultDrawable(context);
    }

    public Drawable getDefaultDrawable(Context context) {
        return AppCompatResources.getDrawable(context, R.drawable.ic_search);
    }

    public static int[] getResultLayout() {
        return RESULT_LAYOUT;
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? RESULT_LAYOUT[0] :
            (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? RESULT_LAYOUT[1] :
                RESULT_LAYOUT[2]);
    }

    @Override
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        List<ContentLoadHelper.CategoryItem> categoryTitle = PrefCache.getResultPopupOrder(context);
        for (ContentLoadHelper.CategoryItem categoryItem : categoryTitle) {
            int pos = adapter.getCount();
            buildPopupMenuCategory(context, adapter, categoryItem.textId);
            if (pos != adapter.getCount())
                adapter.add(pos, new LinearAdapter.ItemTitle(context, categoryItem.textId));
        }

        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            buildPopupMenuCategory(context, adapter, R.string.menu_popup_title_settings);
        }

        return inflatePopupMenu(context, adapter);
    }

    protected void buildPopupMenuCategory(Context context, @NonNull LinearAdapter adapter, int titleStringId) {
        if (titleStringId == R.string.popup_title_customize) {
            adapter.add(new LinearAdapter.Item(context, R.string.menu_custom_icon));
        } else if (titleStringId == R.string.menu_popup_title_settings) {
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }
    }

    @Override
    protected boolean popupMenuClickHandler(@NonNull final View view, @NonNull LinearAdapter.MenuItem item, int stringId, View parentView) {
        if (stringId == R.string.menu_custom_icon) {
            Context ctx = view.getContext();
            TBApplication.behaviour(ctx).launchCustomIconDialog(this, null);
            return true;
        }
        return super.popupMenuClickHandler(view, item, stringId, parentView);
    }
}
