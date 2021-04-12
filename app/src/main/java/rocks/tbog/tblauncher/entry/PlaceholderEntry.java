package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

public class PlaceholderEntry extends StaticEntry {

    public PlaceholderEntry(@NonNull String id) {
        super(id, R.drawable.ic_loading_arrows);
    }

    @Override
    ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }
        return inflatePopupMenu(context, adapter);
    }

    @Override
    public Drawable getDefaultDrawable(Context context) {
        int loadingIconRes = PrefCache.getLoadingIconRes(context);
        return AppCompatResources.getDrawable(context, loadingIconRes);
    }

    @Override
    public void doLaunch(@NonNull View view, int flags) {
        // do nothing
    }
}
