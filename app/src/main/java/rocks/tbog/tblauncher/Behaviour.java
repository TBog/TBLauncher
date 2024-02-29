package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.customicon.IconSelectDialog;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.DialContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.quicklist.EditQuickListDialog;
import rocks.tbog.tblauncher.result.RecycleScrollListener;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.dialog.TagsManagerDialog;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.Utilities;

/**
 * Behaviour of the launcher, when are stuff hidden, animation, user interaction responses
 */
public class Behaviour {

    public static final int LAUNCH_DELAY = 100;
    static final String DIALOG_CUSTOM_ICON = "custom_icon_dialog";
    static final String DIALOG_EDIT_TAGS = "edit_tags_dialog";
    static final String DIALOG_EDIT_QUICK_LIST = "edit_quick_list_dialog";
    static final String DIALOG_TAGS_MANAGER = "tags_manager_dialog";
    private static final int UI_ANIMATION_DELAY = 300;
    // time to wait for the keyboard to show up
    private static final int KEYBOARD_ANIMATION_DELAY = 100;
    private static final int UI_ANIMATION_DURATION = 200;
    private static final String TAG = Behaviour.class.getSimpleName();
    private TBLauncherActivity mTBLauncherActivity = null;
    private DialogFragment<?> mFragmentDialog = null;

    //    private View mResultLayout;
//    private RecyclerList mResultList;
//    private RecycleAdapter mResultAdapter;
//    private EditText mSearchEditText;
//    private View mSearchBarContainer;
//    private View mClearButton;
//    private View mMenuButton;
//    private TextView mLauncherTime = null;
//    private final Runnable mUpdateTime = new Runnable() {
//        @Override
//        public void run() {
//            if (mLauncherTime == null ||
//                !mLauncherTime.isAttachedToWindow() ||
//                !mTBLauncherActivity.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.STARTED))
//                return;
//            Date date = new Date();
//            mLauncherTime.setText(DateFormat.getDateTimeInstance().format(date));
//            long delay = 1000 - date.getTime() % 1000;
//            mLauncherTime.postDelayed(mUpdateTime, delay);
//        }
//    };
    private SharedPreferences mPref;

    private static void launchIntent(@NonNull Behaviour behaviour, @NonNull View view, @NonNull Intent intent) {
        behaviour.beforeLaunchOccurred();
        view.postDelayed(() -> {
            Activity activity = Utilities.getActivity(view);
            if (activity == null)
                return;
            Utilities.setIntentSourceBounds(intent, view);
            Bundle startActivityOptions = Utilities.makeStartActivityOptions(view);
            try {
                activity.startActivity(intent, startActivityOptions);
            } catch (ActivityNotFoundException ignored) {
                return;
            }
            behaviour.afterLaunchOccurred();
        }, LAUNCH_DELAY);
    }

//    private void initResultLayout() {
//        mResultLayout = inflateViewStub(R.id.resultLayout);
//
//        mResultList = mResultLayout.findViewById(R.id.resultList);
//        if (mResultList == null)
//            throw new IllegalStateException("mResultList==null");
//
//        mResultAdapter = new RecycleAdapter(getContext(), new ArrayList<>());
//
//        mResultList.setHasFixedSize(true);
//        mResultList.setAdapter(mResultAdapter);
//        mResultList.addOnScrollListener(mRecycleScrollListener);
////        mResultList.addOnLayoutChangeListener(recycleScrollListener);
//
//        int vertical = getContext().getResources().getDimensionPixelSize(R.dimen.result_margin_vertical);
//        mResultList.addItemDecoration(new ResultItemDecoration(0, vertical, true));
//
//        setListLayout();
//    }

//    private void initSearchBarContainer() {
//        int layout = PrefCache.getSearchBarLayout(mPref);
//        if (PrefCache.searchBarAtBottom(mPref)) {
//            mSearchBarContainer = inflateViewStub(R.id.stubSearchBottom, layout);
//        } else {
//            mSearchBarContainer = inflateViewStub(R.id.stubSearchTop, layout);
//        }
//        if (mSearchBarContainer == null)
//            throw new IllegalStateException("mSearchBarContainer==null");
//
//        mLauncherButton = mSearchBarContainer.findViewById(R.id.launcherButton);
//        mSearchEditText = mSearchBarContainer.findViewById(R.id.launcherSearch);
//        mClearButton = mSearchBarContainer.findViewById(R.id.clearButton);
//        mMenuButton = mSearchBarContainer.findViewById(R.id.menuButton);
//    }

//    private void initLauncherButtons() {
//        final ListPopup buttonMenu;
//        if (PrefCache.getSearchBarLayout(mPref) == R.layout.search_pill)
//            buttonMenu = getButtonPopup(getContext(), ButtonHelper.BTN_ID_LAUNCHER_PILL, R.drawable.launcher_pill);
//        else
//            buttonMenu = getButtonPopup(getContext(), ButtonHelper.BTN_ID_LAUNCHER_WHITE, R.drawable.launcher_white);
//
//        mLauncherButton.setOnLongClickListener((v) -> ButtonHelper.showButtonPopup(v, buttonMenu));
//
//        // menu button / 3 dot button actions
//        mMenuButton.setOnClickListener(v -> {
//            Context ctx = v.getContext();
//            ListPopup menu = getMenuPopup(ctx);
//            registerPopup(menu);
//            menu.showCenter(v);
//        });
//        mMenuButton.setOnLongClickListener(v -> {
//            Context ctx = v.getContext();
//            ListPopup menu = getMenuPopup(ctx);
//
//            // check if menu contains elements and if yes show it
//            if (!menu.getAdapter().isEmpty()) {
//                registerPopup(menu);
//                menu.show(v, 0f);
//                return true;
//            }
//
//            return false;
//        });
//
//        // clear button actions
//        mClearButton.setOnClickListener(v -> clearSearch());
//        mClearButton.setOnLongClickListener(v -> {
//            clearSearch();
//
//            Context ctx = v.getContext();
//            ListPopup menu = getMenuPopup(ctx);
//
//            // check if menu contains elements and if yes show it
//            if (!menu.getAdapter().isEmpty()) {
//                registerPopup(menu);
//                menu.show(v);
//                return true;
//            }
//
//            return false;
//        });
//    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(mTBLauncherActivity);

//        initResultLayout();
//        initSearchBarContainer();

//        initLauncherButtons();
//        initLauncherSearchEditText();
    }

    private Context getContext() {
        return mTBLauncherActivity;
    }

    public void launchIntent(@NonNull View view, @NonNull Intent intent) {
        launchIntent(this, view, intent);
    }

    public boolean showProviderEntries(@Nullable IProvider<?> provider) {
        return showProviderEntries(provider, null);
    }

    public boolean showProviderEntries(@Nullable IProvider<?> provider, @Nullable java.util.Comparator<? super EntryItem> comparator) {
        if (TBApplication.state().getDesktop() != LauncherState.Desktop.SEARCH) {
            // TODO: switchToDesktop might show the result list, we may need to prevent this as an optimization
            mTBLauncherActivity.switchToDesktop(LauncherState.Desktop.SEARCH);
            mTBLauncherActivity.clearAdapter();
        }

        List<? extends EntryItem> entries = provider != null ? provider.getPojos() : null;
        if (entries != null && entries.size() > 0) {
            // reset relevance. This is normally done by a Searcher.
            for (EntryItem entry : entries)
                entry.resetResultInfo();

//            // copy list in order to change it
//            entries = new ArrayList<>(entries);
//            // remove actions and filters from the result list
//            for (Iterator<? extends EntryItem> iterator = entries.iterator(); iterator.hasNext(); ) {
//                EntryItem entry = iterator.next();
//                if (entry instanceof FilterEntry)
//                    iterator.remove();
//            }

            if (comparator != null) {
                // copy list in order to change it
                entries = new ArrayList<>(entries);
                //TODO: do we need this on another thread?
                Collections.sort(entries, comparator);
            }

            mTBLauncherActivity.getSearchHelper().updateAdapter(entries, false);
            return true;
        }

        return false;
    }

    public void handleRemoveApp(String packageName) {
        final var resultAdapter = mTBLauncherActivity.getResultAdapter();
        final int count = resultAdapter.getItemCount();
        for (int idx = count - 1; idx >= 0; idx -= 1) {
            EntryItem entryItem = resultAdapter.getItem(idx);
            if (entryItem.id.contains(packageName))
                mTBLauncherActivity.getSearchHelper().removeResult(entryItem);
        }
    }

    public void refreshSearchRecord(EntryItem entry) {
        mTBLauncherActivity.getResultAdapter().notifyItemChanged(entry);
    }

    private void showResultList(boolean animate) {
        final var mResultLayout = mTBLauncherActivity.getResultLayout();
        Log.d(TAG, "showResultList (anim " + animate + ")");
        if (TBApplication.state().getResultListVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE)
            mResultLayout.animate().cancel();
        if (mResultLayout.getVisibility() == View.VISIBLE)
            return;
        mResultLayout.setVisibility(View.VISIBLE);
        Log.d(TAG, "mResultLayout set VISIBLE (anim " + animate + ")");
        if (animate) {
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
            mResultLayout.setAlpha(0f);
            mResultLayout.animate()
                .alpha(1f)
                .setDuration(UI_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setResultList(LauncherState.AnimatedVisibility.VISIBLE);
                    }
                })
                .start();
        } else {
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.VISIBLE);
            mResultLayout.setAlpha(1f);
        }
    }

    private void hideResultList(boolean animate) {
        final var mResultLayout = mTBLauncherActivity.getResultLayout();
        Log.d(TAG, "hideResultList (anim " + animate + ")");
        if (TBApplication.state().getResultListVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN)
            mResultLayout.animate().cancel();
        if (mResultLayout.getVisibility() != View.VISIBLE) {
            Log.d(TAG, "mResultLayout not VISIBLE, setting state to HIDDEN");
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.HIDDEN);
            return;
        }
        if (animate) {
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN);
            mResultLayout.animate()
                .alpha(0f)
                .setDuration(UI_ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setResultList(LauncherState.AnimatedVisibility.HIDDEN);
                        Log.d(TAG, "mResultLayout set INVISIBLE");
                        mResultLayout.setVisibility(View.INVISIBLE);
                    }
                })
                .start();
        } else {
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.HIDDEN);
            Log.d(TAG, "mResultLayout set INVISIBLE");
            mResultLayout.setVisibility(View.INVISIBLE);
        }
    }

    public void beforeLaunchOccurred() {
        final var mResultList = mTBLauncherActivity.getResultList();
        RecycleScrollListener.setListLayoutHeight(mResultList, mResultList.getHeight());
        mTBLauncherActivity.hideKeyboard();
    }

    public void afterLaunchOccurred() {
        final var mSearchEditText = mTBLauncherActivity.getSearchEditText();
        mSearchEditText.postDelayed(() -> {
            final var mResultList = mTBLauncherActivity.getResultList();
            RecycleScrollListener.setListLayoutHeight(mResultList, ViewGroup.LayoutParams.MATCH_PARENT);
            if (PrefCache.clearSearchAfterLaunch(mPref)) {
                // We selected an item on the list, now we can cleanup the filter:
                if (mSearchEditText.getText().length() > 0) {
                    mSearchEditText.setText("");
                } else if (TBApplication.state().isResultListVisible()) {
                    mTBLauncherActivity.getSearchHelper().clearAdapter();
                }
            }
            if (PrefCache.showWidgetScreenAfterLaunch(mPref)) {
                // show widgets when we return to the launcher
                mTBLauncherActivity.switchToDesktop(LauncherState.Desktop.WIDGET);
            }
        }, UI_ANIMATION_DELAY);
    }

    @NonNull
    public static ListPopup getButtonPopup(@NonNull Context ctx, @NonNull String buttonId, @DrawableRes int defaultButtonIcon) {
        LinearAdapter adapter = new LinearAdapter();
        adapter.add(new LinearAdapter.ItemTitle(ctx, R.string.popup_title_customize));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_custom_icon));

        return ListPopup.create(ctx, adapter).setOnItemClickListener((a, view, pos) -> {
            LinearAdapter.MenuItem menuItem = ((LinearAdapter) a).getItem(pos);
            @StringRes int id = 0;
            if (menuItem instanceof LinearAdapter.Item) {
                id = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            if (id == R.string.menu_custom_icon) {
                TBApplication.behaviour(ctx).launchCustomIconDialog(buttonId, defaultButtonIcon, () -> {
                    // refresh search bar preferences to reload the icon
                    TBApplication.ui(ctx).refreshSearchBar();
                });
            }
        });
    }

    @NonNull
    public static IconSelectDialog getCustomIconDialog(@NonNull Context ctx, boolean hideResultList) {
        IconSelectDialog dialog = new IconSelectDialog();
        //openFragmentDialog(dialog, DIALOG_CUSTOM_ICON);
        if (hideResultList) {
            // If results are visible
            if (TBApplication.state().isResultListVisible()) {
                final TBLauncherActivity app = TBApplication.launcherActivity(ctx);
                final View resultLayout = app != null ? app.getResultLayout() : null;
                if (resultLayout != null) {
                    resultLayout.setVisibility(View.INVISIBLE);
                    // OnDismiss: We restore mResultLayout visibility
                    dialog.setOnDismissListener(dlg -> resultLayout.setVisibility(View.VISIBLE));
                }
            }
        }

        //dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_CUSTOM_ICON);
        return dialog;
    }

    public void launchCustomIconDialog(AppEntry appEntry) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), false);
        dialog
            .putArgString("componentName", appEntry.getUserComponentName())
            .putArgLong("customIcon", appEntry.getCustomIcon())
            .putArgString("entryName", appEntry.getName());

        dialog.setOnConfirmListener(drawable -> {
            TBApplication app = TBApplication.getApplication(getContext());
            if (drawable == null)
                app.iconsHandler().restoreDefaultIcon(appEntry);
            else
                app.iconsHandler().changeIcon(appEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecord(appEntry);
            mTBLauncherActivity.queueDockReload();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }

    public void launchCustomIconDialog(ShortcutEntry shortcutEntry) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), true);
        dialog
            .putArgString("packageName", shortcutEntry.packageName)
            .putArgString("shortcutData", shortcutEntry.shortcutData)
            .putArgString("shortcutId", shortcutEntry.id);

        dialog.setOnConfirmListener(drawable -> {
            final TBApplication app = TBApplication.getApplication(mTBLauncherActivity);
            if (drawable == null)
                app.iconsHandler().restoreDefaultIcon(shortcutEntry);
            else
                app.iconsHandler().changeIcon(shortcutEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecord(shortcutEntry);
            mTBLauncherActivity.queueDockReload();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }

    public void launchCustomIconDialog(@NonNull StaticEntry staticEntry) {
        launchCustomIconDialog(staticEntry, null);
    }

    public void launchCustomIconDialog(@NonNull StaticEntry staticEntry, @Nullable Runnable afterConfirmation) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), true);
        dialog.putArgString("entryId", staticEntry.id);

        dialog.setOnConfirmListener(drawable -> {
            final TBApplication app = TBApplication.getApplication(mTBLauncherActivity);
            if (drawable == null)
                app.iconsHandler().restoreDefaultIcon(staticEntry);
            else
                app.iconsHandler().changeIcon(staticEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecord(staticEntry);
            mTBLauncherActivity.queueDockReload();
            if (afterConfirmation != null)
                afterConfirmation.run();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }

    public void launchCustomIconDialog(@NonNull SearchEntry searchEntry, @Nullable Runnable afterConfirmation) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), true);
        dialog
            .putArgString("searchEntryId", searchEntry.id)
            .putArgString("searchName", searchEntry.getName());

        dialog.setOnConfirmListener(drawable -> {
            final TBApplication app = TBApplication.getApplication(mTBLauncherActivity);
            if (drawable == null)
                app.iconsHandler().restoreDefaultIcon(searchEntry);
            else
                app.iconsHandler().changeIcon(searchEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecord(searchEntry);
            mTBLauncherActivity.queueDockReload();
            if (afterConfirmation != null)
                afterConfirmation.run();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }

    /**
     * Change the icon for the "Dial" contact
     *
     * @param dialEntry entry that currently holds the "Dial" icon
     */
    public void launchCustomIconDialog(@NonNull DialContactEntry dialEntry) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), true);
        dialog
            .putArgString("contactEntryId", dialEntry.id)
            .putArgString("contactName", dialEntry.getName());

        dialog.setOnConfirmListener(drawable -> {
            final TBApplication app = TBApplication.getApplication(getContext());
            if (drawable == null)
                app.iconsHandler().restoreDefaultIcon(dialEntry);
            else
                app.iconsHandler().changeIcon(dialEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecord(dialEntry);
            mTBLauncherActivity.queueDockReload();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }


    public void launchCustomIconDialog(@NonNull String buttonId, int defaultButtonIcon, @Nullable Runnable afterConfirmation) {
        IconSelectDialog dialog = getCustomIconDialog(getContext(), true);
        dialog
            .putArgString("buttonId", buttonId)
            .putArgInt("defaultIcon", defaultButtonIcon);

        dialog.setOnConfirmListener(drawable -> {
            var iconsHandler = TBApplication.iconsHandler(getContext());
            if (drawable == null)
                iconsHandler.restoreDefaultIcon(buttonId);
            else
                iconsHandler.changeIcon(buttonId, drawable);
            if (afterConfirmation != null)
                afterConfirmation.run();
        });
        showDialog(dialog, DIALOG_CUSTOM_ICON);
    }

    public void launchEditTagsDialog(EntryWithTags entry) {
        EditTagsDialog dialog = new EditTagsDialog();
        openFragmentDialog(dialog, DIALOG_EDIT_TAGS);

        // set args
        {
            Bundle args = new Bundle();
            args.putString("entryId", entry.id);
            args.putString("entryName", entry.getName());
            dialog.setArguments(args);
        }

        dialog.setOnConfirmListener(newTags -> {
            TBApplication.tagsHandler(getContext()).setTags(entry, newTags);
            refreshSearchRecord(entry);
        });

        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_EDIT_TAGS);
    }

    public void launchEditQuickListDialog(Context context) {
        showDialog(context, new EditQuickListDialog(), DIALOG_EDIT_QUICK_LIST);
    }

    public void launchTagsManagerDialog(Context context) {
        showDialog(context, new TagsManagerDialog(), DIALOG_TAGS_MANAGER);
    }

    public boolean isFragmentDialogVisible() {
        return mFragmentDialog != null && mFragmentDialog.isVisible();
    }

    /**
     * Keep track of the last dialog. Use context to find a SupportFragmentManager
     *
     * @param context to get the FragmentActivity from
     * @param dialog  to open
     * @param tag     name to keep track of
     */
    public static void showDialog(Context context, DialogFragment<?> dialog, String tag) {
        if (TBApplication.activityInvalid(context)) {
            Log.e(TAG, "[activityInvalid] showDialog " + tag);
            return;
        }
        TBApplication.behaviour(context).showDialog(dialog, tag);
    }

    private void showDialog(@NonNull DialogFragment<?> dialog, @Nullable String tag) {
        openFragmentDialog(dialog, tag);
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), tag);
    }

    private void openFragmentDialog(DialogFragment<?> dialog, @Nullable String tag) {
        closeFragmentDialog(tag);
        mFragmentDialog = dialog;
    }

    public boolean closeFragmentDialog() {
        return closeFragmentDialog(null);
    }

    private boolean closeFragmentDialog(@Nullable String tag) {
        if (mFragmentDialog != null && mFragmentDialog.isVisible()) {
            if (tag != null && tag.equals(mFragmentDialog.getTag())) {
                mFragmentDialog.dismiss();
                return true;
            } else if (tag == null) {
                mFragmentDialog.dismiss();
                mFragmentDialog = null;
                return true;
            }
        }
        mFragmentDialog = null;
        return false;
    }

    public void onResume() {
        Log.i(TAG, "onResume");

        //LauncherState.Desktop desktop = TBApplication.state().getDesktop();
        //mTBLauncherActivity.showDesktop(desktop);

//        mLauncherTime = null;
//        if (PrefCache.searchBarHasTimer(mPref)) {
//            mLauncherTime = mSearchBarContainer.findViewById(R.id.launcherTime);
//            UITheme.applySearchBarTextShadow(mLauncherTime);
//            mLauncherTime.post(mUpdateTime);
//        }
    }

    public void setActivityOrientation(@NonNull Activity act) {
        if (mPref.getBoolean("lock-portrait", true)) {
            if (mPref.getBoolean("sensor-orientation", true))
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
            else
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
        } else {
            if (mPref.getBoolean("sensor-orientation", true))
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
            else
                act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
        }
    }
}
