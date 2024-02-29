package rocks.tbog.tblauncher.utils;

import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_GESTURE;
import static rocks.tbog.tblauncher.utils.Constants.KEYBOARD_ANIMATION_DELAY;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.DeviceAdmin;
import rocks.tbog.tblauncher.LauncherState;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.ui.ListPopup;

public class ActionHelper {
    private static final String TAG = ActionHelper.class.getSimpleName();

    @NonNull
    final TBLauncherActivity mTBLauncherActivity;
    private final SharedPreferences mPref;
    public final DesktopHelper mDesktopHelper;

    public ActionHelper(@NonNull TBLauncherActivity launcherActivity) {
        mTBLauncherActivity = launcherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(launcherActivity);
        mDesktopHelper = new DesktopHelper(launcherActivity);
    }

    private Context getContext() {
        return mTBLauncherActivity;
    }

    private boolean launchStaticEntry(@NonNull String entryId) {
        Context ctx = getContext();
        DataHandler dataHandler = TBApplication.dataHandler(ctx);
        EntryItem item = dataHandler.getPojo(entryId);
        if (item == null) {
            item = TagsProvider.newTagEntryCheckId(entryId);
        }
        if (item instanceof StaticEntry) {
            if (TBApplication.state().getDesktop() != LauncherState.Desktop.SEARCH) {
                // TODO: switchToDesktop might show the result list, we may need to prevent this as an optimization
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.SEARCH);
                mTBLauncherActivity.clearAdapter();
            }
            // make sure the QuickList will not toggle off
            TBApplication.quickList(ctx).adapterCleared();
            item.doLaunch(mTBLauncherActivity.getLauncherButton(), LAUNCHED_FROM_GESTURE);
            return true;
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.entry_not_found, entryId), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private boolean launchActionEntry(@NonNull String action) {
        return launchStaticEntry(ActionEntry.SCHEME + action);
    }

    private boolean launchAppEntry(@NonNull String userComponentName) {
        Context ctx = getContext();
        UserHandleCompat user = UserHandleCompat.fromComponentName(ctx, userComponentName);
        ComponentName component = UserHandleCompat.unflattenComponentName(userComponentName);
        String appId = AppEntry.generateAppId(component, user);
        EntryItem item = TBApplication.dataHandler(ctx).getPojo(appId);
        if (item instanceof AppEntry) {
            ResultHelper.launch(mTBLauncherActivity.getLauncherButton(), item, LAUNCHED_FROM_GESTURE);
            return true;
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.application_not_found, appId), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private boolean launchEntryById(@NonNull String entryId) {
        Context ctx = getContext();
        DataHandler dataHandler = TBApplication.dataHandler(ctx);
        EntryItem item = dataHandler.getPojo(entryId);
        if (item == null) {
            Toast.makeText(ctx, ctx.getString(R.string.entry_not_found, entryId), Toast.LENGTH_SHORT).show();
            return false;
        }
        item.doLaunch(mTBLauncherActivity.getLauncherButton(), LAUNCHED_FROM_GESTURE);
        return true;
    }

    public void executeButtonAction(@Nullable String button) {
        if (mPref != null)
            executeAction(mPref.getString(button, null), button);
    }

    private boolean executeGestureAction(@Nullable String gesture) {
        if (mPref != null)
            return executeAction(mPref.getString(gesture, null), gesture);
        return false;
    }

    public void executeResultActionAsync(@Nullable String action, @NonNull String source) {
        mTBLauncherActivity.getLauncherButton().postDelayed(() ->
                TBApplication.dataHandler(getContext()).runAfterLoadOver(() -> {
                    LauncherState state = TBApplication.state();
                    if (!state.isResultListVisible() && state.getDesktop() == LauncherState.Desktop.SEARCH)
                        executeAction(action, source);
                }),
            KEYBOARD_ANIMATION_DELAY);
    }

    public boolean executeAction(@Nullable String action, @Nullable String source) {
        if (action == null)
            return false;
        if (TBApplication.activityInvalid(getContext())) {
            Log.e(TAG, "[activityInvalid] executeAction " + action);
            // only do stuff if we are the current activity
            return false;
        }
        Log.d(TAG, "executeAction( action=" + action + " source=" + source + " )");
        switch (action) {
            case "lockScreen":
                if (DeviceAdmin.isAdminActive(getContext())) {
                    DeviceAdmin.lockScreen(getContext());
                } else {
                    Toast.makeText(getContext(), R.string.device_admin_required, Toast.LENGTH_SHORT).show();
                }
                return true;
            case "expandNotificationsPanel":
                Utilities.expandNotificationsPanel(mTBLauncherActivity);
                return true;
            case "expandSettingsPanel":
                Utilities.expandSettingsPanel(mTBLauncherActivity);
                return true;
            case "showSearchBar":
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            case "showSearchBarAndKeyboard":
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.SEARCH);
                mTBLauncherActivity.showKeyboard();
                return true;
            case "showWidgets":
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.WIDGET);
                return true;
            case "showWidgetsCenter":
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.WIDGET);
                mTBLauncherActivity.liveWallpaper.resetPosition();
                return true;
            case "showEmpty":
                mDesktopHelper.switchToDesktop(LauncherState.Desktop.EMPTY);
                return true;
            case "toggleSearchAndWidget":
                if (TBApplication.state().getDesktop() == LauncherState.Desktop.SEARCH)
                    mDesktopHelper.switchToDesktop(LauncherState.Desktop.WIDGET);
                else
                    mDesktopHelper.switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            case "toggleSearchWidgetEmpty": {
                final LauncherState.Desktop desktop = TBApplication.state().getDesktop();
                if (desktop == LauncherState.Desktop.SEARCH)
                    mDesktopHelper.switchToDesktop(LauncherState.Desktop.WIDGET);
                else if (desktop == LauncherState.Desktop.WIDGET)
                    mDesktopHelper.switchToDesktop(LauncherState.Desktop.EMPTY);
                else
                    mDesktopHelper.switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            }
            case "reloadProviders":
                TBApplication.dataHandler(getContext()).reloadProviders();
                return true;
            case "showAllAppsAZ":
                return launchActionEntry("show/apps/byName");
            case "toggleGrid":
                return launchActionEntry("toggle/grid");
            case "showAllAppsZA":
                return launchActionEntry("show/apps/byNameReversed");
            case "showContactsAZ":
                return launchActionEntry("show/contacts/byName");
            case "showContactsZA":
                return launchActionEntry("show/contacts/byNameReversed");
            case "showShortcutsAZ":
                return launchActionEntry("show/shortcuts/byName");
            case "showShortcutsZA":
                return launchActionEntry("show/shortcuts/byNameReversed");
            case "showFavorites":
                return launchActionEntry("show/favorites/byName");
            case "showHistoryByRecency":
                return launchActionEntry("show/history/recency");
            case "showHistoryByFrequency":
                return launchActionEntry("show/history/frequency");
            case "showHistoryByFrecency":
                return launchActionEntry("show/history/frecency");
            case "showHistoryByAdaptive":
                return launchActionEntry("show/history/adaptive");
            case "showUntagged":
                return launchActionEntry("show/untagged");
            case "showTagsList":
                return launchActionEntry("show/tags/list");
            case "showTagsListReversed":
                return launchActionEntry("show/tags/listReversed");
            case "showTagsMenu": {
                View anchor = null;
                if ("button-launcher".equals(source))
                    anchor = mTBLauncherActivity.getLauncherButton();
                Context ctx = getContext();
                ListPopup menu = TBApplication.tagsHandler(ctx).getTagsMenu(ctx);
                mTBLauncherActivity.registerPopup(menu);
                if (anchor != null)
                    menu.show(anchor);
                else
                    menu.showCenter(mTBLauncherActivity.mDecorView);

            }
            return true;
            case "runApp": {
                String runApp = mPref.getString(source + "-app-to-run", null);
                if (runApp != null)
                    return launchAppEntry(runApp);
                break;
            }
            case "runShortcut": {
                String runApp = mPref.getString(source + "-shortcut-to-run", null);
                if (runApp != null)
                    return launchEntryById(runApp);
                break;
            }
            case "showEntry": {
                String entryToShow = mPref.getString(source + "-entry-to-show", null);
                if (entryToShow != null)
                    return launchStaticEntry(entryToShow);
                break;
            }
            default:
                // do nothing
                break;
        }
        return false;
    }

    public boolean onFlingDownLeft() {
        return executeGestureAction("gesture-fling-down-left");
    }

    public boolean onFlingDownRight() {
        return executeGestureAction("gesture-fling-down-right");
    }

    public boolean onFlingUp() {
        return executeGestureAction("gesture-fling-up");
    }

    public boolean onFlingLeft() {
        return executeGestureAction("gesture-fling-left");
    }

    public boolean onFlingRight() {
        return executeGestureAction("gesture-fling-right");
    }

    public boolean onClick() {
        return executeGestureAction("gesture-click");
    }

    public boolean hasDoubleClick() {
        if (mPref == null)
            return false;
        String action = mPref.getString("gesture-double-click", null);
        return action != null && !action.isEmpty() && !action.equals("none");
    }

    public boolean onDoubleClick() {
        return executeGestureAction("gesture-double-click");
    }

    public void refreshDesktop() {
        mDesktopHelper.showDesktop(TBApplication.state().getDesktop());
    }

    public void showResultList(boolean animate) {
        mTBLauncherActivity.showResultList(animate);
    }

    public void hideResultList(boolean animate) {
        mTBLauncherActivity.hideResultList(animate);
    }
}
