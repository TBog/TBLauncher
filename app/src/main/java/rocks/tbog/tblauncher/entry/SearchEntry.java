package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class SearchEntry extends EntryItem {
    protected String query;

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

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////
    private static final int[] RESULT_LAYOUT = {R.layout.item_builtin, R.layout.item_grid, R.layout.item_quick_list};

    public static int[] getResultLayout() {
        return RESULT_LAYOUT;
    }

    @Override
    public int getResultLayout(int drawFlags) {
        return Utilities.checkFlag(drawFlags, FLAG_DRAW_LIST) ? RESULT_LAYOUT[0] :
            (Utilities.checkFlag(drawFlags, FLAG_DRAW_GRID) ? RESULT_LAYOUT[1] :
                RESULT_LAYOUT[2]);
    }

    private static final ArrayList<Pair<String, String>> APP4URL;

    static {
        APP4URL = new ArrayList<>(5);
        APP4URL.add(new Pair<>("https://encrypted.google.com", "com.google.android.googlequicksearchbox"));
        APP4URL.add(new Pair<>("https://play.google.com/store", "com.android.vending"));
        APP4URL.add(new Pair<>("https://start.duckduckgo.com", "com.duckduckgo.mobile.android"));
        APP4URL.add(new Pair<>("https://www.google.com/maps", "com.google.android.apps.maps"));
        APP4URL.add(new Pair<>("https://www.youtube.com", "com.google.android.youtube"));
    }

    @Nullable
    protected static Drawable getApplicationIconForUrl(@NonNull Context context, @Nullable String url) {
        if (url == null || url.isEmpty())
            return null;
        for (Pair<String, String> pair : APP4URL) {
            if (url.startsWith(pair.first)) {
                try {
                    return context.getPackageManager().getApplicationIcon(pair.second);
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }

        }
        return null;
    }

    protected static boolean isGoogleSearch(String url) {
        return url.startsWith("https://encrypted.google.com");
    }
}
