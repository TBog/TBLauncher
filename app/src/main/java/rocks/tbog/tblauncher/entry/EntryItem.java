package rocks.tbog.tblauncher.entry;

import android.content.Context;
import android.view.View;

import androidx.annotation.CallSuper;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DebugInfo;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.Utilities;

public abstract class EntryItem {

    public static final RelevanceComparator RELEVANCE_COMPARATOR = new RelevanceComparator();
    public static final NameComparator NAME_COMPARATOR = new NameComparator();

    /**
     * the layout will be used in a ListView
     */
    public static final int FLAG_DRAW_LIST = 0x0001; // 1 << 0

    /**
     * the layout will be used in a GridView
     */
    public static final int FLAG_DRAW_GRID = 0x0002; // 1 << 1

    /**
     * the layout will be used in a horizontal LinearLayout
     */
    public static final int FLAG_DRAW_QUICK_LIST = 0x0004; // 1 << 2

    /**
     * layout should display an icon
     */
    public static final int FLAG_DRAW_ICON = 0x0008; // 1 << 3

    /**
     * layout may display a badge (shortcut sub-icon) if appropriate
     */
    public static final int FLAG_DRAW_ICON_BADGE = 0x0010; // 1 << 4

    /**
     * layout should display a text/name
     */
    public static final int FLAG_DRAW_NAME = 0x0020; // 1 << 5

    /**
     * layout should display tags
     */
    public static final int FLAG_DRAW_TAGS = 0x0040; // 1 << 6

    /**
     * do not use cache, generate new drawable
     */
    public static final int FLAG_DRAW_NO_CACHE = 0x0080; // 1 << 7

    /**
     * the item will be drawn on a while background
     */
    public static final int FLAG_DRAW_WHITE_BG = 0x0100; // 1 << 8

    /**
     * use cache but also run the load task
     * Note: used for shortcuts as we don't have a way to cache multiple icons for the same entry id
     */
    public static final int FLAG_RELOAD = 0x0200; // 1 << 9

    // Used when generating Popup menu and calling doLaunch
    public static final int LAUNCHED_FROM_RESULT_LIST = 0x01;
    public static final int LAUNCHED_FROM_QUICK_LIST = 0x02;
    public static final int LAUNCHED_FROM_GESTURE = 0x04;

    // Globally unique ID.
    // Usually starts with provider scheme, e.g. "app://" or "contact://" to
    // ensure unique constraint
    @NonNull
    public final String id;
    // normalized name, for faster search
    public StringNormalizer.Result normalizedName = null;
    // Name for this Entry, e.g. app name
    @NonNull
    private
    String name = "";

    // How relevant is this record? The higher, the most probable it will be displayed
    protected FuzzyScore.MatchInfo relevance = null;
    // Pointer to the normalizedName that the above relevance was calculated, used for highlighting
    protected StringNormalizer.Result relevanceSource = null;

    public EntryItem(@NonNull String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof EntryItem))
            return false;
        EntryItem entryItem = (EntryItem) o;
        return id.equals(entryItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Set the user-displayable name of this container
     * <p/>
     * When this method a searchable version of the name will be generated for the name and stored
     * as `nameNormalized`. Additionally a mapping from the positions in the searchable name
     * to the positions in the displayable name will be stored (as `namePositionMap`).
     *
     * @param name User-friendly name of this container
     */
    public void setName(String name) {
        if (name != null) {
            // Set the actual user-friendly name
            this.name = name;
            this.normalizedName = StringNormalizer.normalizeWithResult(this.name, false);
        } else {
            this.name = "null";
            this.normalizedName = null;
        }
    }

    public void setName(String name, boolean generateNormalization) {
        if (generateNormalization) {
            setName(name);
        } else {
            this.name = name;
            this.normalizedName = null;
        }
    }

    public int getRelevance() {
        return relevance == null ? 0 : relevance.score;
    }

    public void setRelevance(StringNormalizer.Result normalizedName, @Nullable FuzzyScore.MatchInfo matchInfo) {
        relevanceSource = normalizedName;
        relevance = matchInfo != null ? new FuzzyScore.MatchInfo(matchInfo) : new FuzzyScore.MatchInfo();
    }

    public void boostRelevance(int boost) {
        if (relevance != null)
            relevance.score += boost;
    }

    public void resetRelevance() {
        this.relevanceSource = null;
        this.relevance = null;
    }

    /**
     * ID to use in the history
     * (may be different from the one used in the adapter for display)
     */
    public String getHistoryId() {
        return this.id;
    }

    public boolean isExcludedFromHistory() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    // Result methods
    ///////////////////////////////////////////////////////////////////////////////////////////////

    @LayoutRes
    public abstract int getResultLayout(int drawFlags);

    public abstract void displayResult(@NonNull View view, int drawFlags);

    @NonNull
    public String getIconCacheId() {
        return id;
    }

    public static class RelevanceComparator implements java.util.Comparator<EntryItem> {
        @Override
        public int compare(EntryItem lhs, EntryItem rhs) {
            if (lhs.getRelevance() == rhs.getRelevance()) {
                if (lhs.relevanceSource != null && rhs.relevanceSource != null)
                    return rhs.relevanceSource.compareTo(lhs.relevanceSource);
                else
                    return rhs.name.compareTo(lhs.name);
            }
            return rhs.getRelevance() - lhs.getRelevance();
        }

    }

    public static class NameComparator implements java.util.Comparator<EntryItem> {
        @Override
        public int compare(EntryItem lhs, EntryItem rhs) {
            if (lhs.normalizedName != null && rhs.normalizedName != null)
                return rhs.normalizedName.compareTo(lhs.normalizedName);
            return rhs.name.compareTo(lhs.name);
        }

    }

    /**
     * Default popup menu implementation, can be overridden by children class to display a more specific menu
     *
     * @return an inflated, listener-free PopupMenu
     */
    protected ListPopup buildPopupMenu(Context context, LinearAdapter adapter, View parentView, int flags) {
        adapter.add(new LinearAdapter.ItemTitle(context, R.string.popup_title_hist_fav));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_remove_history));
        adapter.add(new LinearAdapter.Item(context, R.string.menu_quick_list_add));
        if (Utilities.checkFlag(flags, LAUNCHED_FROM_QUICK_LIST)) {
            adapter.add(new LinearAdapter.ItemTitle(context, R.string.menu_popup_title_settings));
            adapter.add(new LinearAdapter.Item(context, R.string.menu_popup_quick_list_customize));
        }
        return inflatePopupMenu(context, adapter);
    }

    ListPopup inflatePopupMenu(@NonNull Context context, @NonNull LinearAdapter adapter) {
        ListPopup menu = ListPopup.create(context, adapter);

//        boolean foundInQuickList = false;
//        ArrayList<ModRecord> favRecords = TBApplication.dataHandler(context).getFavorites();
//        for (ModRecord fav : favRecords) {
//            if (id.equals(fav.record) && fav.isInQuickList()) {
//                foundInQuickList = true;
//                break;
//            }
//        }
        QuickListProvider provider = TBApplication.dataHandler(context).getQuickListProvider();

        // get current Quick List content
        List<? extends EntryItem> list = provider != null ? provider.getPojos() : Collections.emptyList();
        boolean foundInQuickList = list.contains(this);

        if (foundInQuickList) {
            // if already in quick list, remove the "Add to QuickList" option
            for (int i = 0; i < adapter.getCount(); i += 1) {
                LinearAdapter.MenuItem item = adapter.getItem(i);
                if (item instanceof LinearAdapter.Item) {
                    if (((LinearAdapter.Item) item).stringId == R.string.menu_quick_list_add)
                        adapter.remove(item);
                }
            }
        } else {
            // if not in quick list, remove the "Remove from QuickList" option
            for (int i = 0; i < adapter.getCount(); i += 1) {
                LinearAdapter.MenuItem item = adapter.getItem(i);
                if (item instanceof LinearAdapter.Item) {
                    if (((LinearAdapter.Item) item).stringId == R.string.menu_quick_list_remove)
                        adapter.remove(item);
                }
            }
        }

        if (DebugInfo.itemRelevance(context)) {
            String debugTitle = context.getString(R.string.popup_title_debug);
            int pos = -1;
            // find title
            for (int i = 0; i < adapter.getCount(); i += 1) {
                if (debugTitle.equals(adapter.getItem(i).toString())) {
                    pos = i + 1;
                    break;
                }
            }
            // if title not found, add title
            if (pos == -1) {
                adapter.add(new LinearAdapter.ItemTitle(debugTitle));
                pos = adapter.getCount();
            }
            // add debug data after title
            adapter.add(pos, new LinearAdapter.ItemString("Relevance: " + getRelevance()));
        }

        return menu;
    }

    /**
     * How to display the popup menu
     *
     * @return a PopupMenu object
     */
    @NonNull
    public ListPopup getPopupMenu(final View parentView, int flags) {
        final Context context = parentView.getContext();
        LinearAdapter menuAdapter = new LinearAdapter();
        ListPopup menu = buildPopupMenu(context, menuAdapter, parentView, flags);

        menu.setOnItemClickListener((adapter, view, position) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) adapter).getItem(position);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) adapter.getItem(position)).stringId;
            }
            popupMenuClickHandler(view, item, stringId, parentView);
        });

        return menu;
    }

    @NonNull
    public ListPopup getPopupMenu(final View parentView) {
        return getPopupMenu(parentView, LAUNCHED_FROM_RESULT_LIST);
    }

    /**
     * Handler for popup menu action.
     * Default implementation only handle remove from history action.
     *
     * @return Works in the same way as onOptionsItemSelected, return true if the action has been handled, false otherwise
     */
    @CallSuper
    boolean popupMenuClickHandler(@NonNull View view, @NonNull LinearAdapter.MenuItem item, @StringRes int stringId, View parentView) {
        Context context = parentView.getContext();
        if (R.string.menu_remove_history == stringId) {
            ResultHelper.removeFromResultsAndHistory(this, context);
            return true;
        } else if (R.string.menu_quick_list_add == stringId) {
            ResultHelper.launchAddToQuickList(context, this);
            return true;
        } else if (R.string.menu_quick_list_remove == stringId) {
            ResultHelper.launchRemoveFromQuickList(context, this);
            return true;
        } else if (R.string.menu_popup_quick_list_customize == stringId) {
            TBApplication.behaviour(context).launchEditQuickListDialog(context);
            return true;
        }

//        FullscreenActivity mainActivity = (FullscreenActivity) context;
//        // Update favorite bar
//        mainActivity.onFavoriteChange();
//        mainActivity.launchOccurred();
//        // Update Search to reflect favorite add, if the "exclude favorites" option is active
//        if (mainActivity.prefs.getBoolean("exclude-favorites", false) && mainActivity.isViewingSearchResults()) {
//            mainActivity.updateSearchRecords(true);
//        }

        return false;
    }

    public void doLaunch(@NonNull View view, int flags) {
        throw new IllegalStateException("No launch action defined for " + getClass().getSimpleName());
    }
}
