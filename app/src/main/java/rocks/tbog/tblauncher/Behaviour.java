package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.preference.PreferenceManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import rocks.tbog.tblauncher.CustomIcon.IconSelectDialog;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.searcher.QuerySearcher;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.searcher.TagSearcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.AnimatedListView;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.EditQuickListDialog;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.LoadingDrawable;
import rocks.tbog.tblauncher.ui.TagsManagerDialog;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.Utilities;


/**
 * Behaviour of the launcher, when are stuff hidden, animation, user interaction responses
 */
public class Behaviour implements ISearchActivity, KeyboardScrollHider.KeyboardHandler {

    private static final int UI_ANIMATION_DELAY = 300;
    private static final int UI_ANIMATION_DURATION = 200;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;
    public static final int LAUNCH_DELAY = 100;
    private static final String TAG = Behaviour.class.getSimpleName();

    private TBLauncherActivity mTBLauncherActivity = null;
    private DialogFragment<?> mFragmentDialog = null;

    private View mResultLayout;
    private AnimatedListView mResultList;
    private ResultAdapter mResultAdapter;
    private EditText mSearchEditText;
    private View mSearchBarContainer;
    private View mWidgetContainer;
    private View mClearButton;
    private View mMenuButton;
    private ImageView mLauncherButton;
    private View mDecorView;
    private View mNotificationBackground;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            SystemUiVisibility.setFullscreen(mDecorView);
        }
    };

    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = mTBLauncherActivity != null ? mTBLauncherActivity.getSupportActionBar() : null;
            if (actionBar != null) {
                actionBar.show();
            }
            mSearchBarContainer.setVisibility(View.VISIBLE);
        }
    };
    private SharedPreferences mPref;

    private void initResultLayout(ViewGroup resultLayout) {
        mResultLayout = resultLayout;
        mResultAdapter = new ResultAdapter(new ArrayList<>());
        mResultList = resultLayout.findViewById(R.id.resultList);
        mResultList.setAdapter(mResultAdapter);
        mResultList.setOnItemClickListener((parent, view, position, id) -> mResultAdapter.onClick(position, view));
        mResultList.setOnItemLongClickListener((parent, view, position, id) -> mResultAdapter.onLongClick(position, view));
    }

    private void initLauncherButton(ImageView launcherButton) {
        mLauncherButton = launcherButton;
        //mLoaderSpinner = loaderBar;
        mLauncherButton.setImageDrawable(new LoadingDrawable());

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //mLauncherButton.setOnTouchListener(mDelayHideTouchListener);

        mLauncherButton.setOnClickListener((v) -> TBApplication.dataHandler(v.getContext()).reloadProviders());
    }

    private void initLauncherSearchEditText(EditText searchEditText) {
        mSearchEditText = searchEditText;

        searchEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                if (isViewingAllApps()) {
//                    displayKissBar(false, false);
//                }
                String text = s.toString();
                updateSearchRecords(false, text);
                updateClearButton();
            }
        });

        // On validate, launch first record
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // if keyboard closed
                if (actionId == android.R.id.closeButton)
                    return onKeyboardClosed();

                // launch most relevant result
                mResultAdapter.onClick(mResultAdapter.getCount() - 1, v);
                return true;
            }
        });
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
//        int animationDuration = mTBLauncherActivity.getResources().getInteger(android.R.integer.config_longAnimTime);

        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(tbLauncherActivity);

        mSearchBarContainer = findViewById(R.id.searchBarContainer);
        mWidgetContainer = findViewById(R.id.widgetContainer);
        mClearButton = findViewById(R.id.clearButton);
        mMenuButton = findViewById(R.id.menuButton);
        mNotificationBackground = findViewById(R.id.notificationBackground);

        mDecorView = mTBLauncherActivity.getWindow().getDecorView();

        // Set up the user interaction to manually show or hide the system UI.
        //findViewById(R.id.root_layout).setOnClickListener(view -> toggleSearchBar());

        initResultLayout(findViewById(R.id.resultLayout));
        initLauncherButton(findViewById(R.id.launcherButton));
        initLauncherSearchEditText(findViewById(R.id.launcherSearch));

        // menu button / 3 dot button actions
        mMenuButton.setOnClickListener(v -> {
            Context ctx = v.getContext();
            ListPopup menu = getMenuPopup(ctx);
            TBApplication.behaviour(ctx).registerPopup(menu);
            menu.showCenter(v);
        });
        mMenuButton.setOnLongClickListener(v -> {
            Context ctx = v.getContext();
            ListPopup menu = getMenuPopup(ctx);

            // check if menu contains elements and if yes show it
            if (!menu.getAdapter().isEmpty()) {
                TBApplication.behaviour(ctx).registerPopup(menu);
                menu.show(v, 0f);
                return true;
            }

            return false;
        });

        // clear button actions
        mClearButton.setOnClickListener(v -> {
            clearSearch();
        });
        mClearButton.setOnLongClickListener(v -> {
            mSearchEditText.setText("");

            Context ctx = v.getContext();
            ListPopup menu = getMenuPopup(ctx);

            // check if menu contains elements and if yes show it
            if (!menu.getAdapter().isEmpty()) {
                TBApplication.behaviour(ctx).registerPopup(menu);
                menu.show(v);
                return true;
            }

            return false;
        });
    }

    private ListPopup getMenuPopup(Context ctx) {
        LinearAdapter adapter = new LinearAdapter();
        ListPopup menu = ListPopup.create(ctx, adapter);

        adapter.add(new LinearAdapter.ItemTitle(ctx, R.string.menu_popup_title));
        adapter.add(new LinearAdapter.Item(ctx, R.string.change_wallpaper));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_widget_add));
        if (TBApplication.widgetManager(ctx).widgetCount() > 0)
            adapter.add(new LinearAdapter.Item(ctx, R.string.menu_widget_remove));
        adapter.add(new LinearAdapter.ItemTitle(ctx, R.string.menu_popup_title_settings));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_popup_launcher_settings));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_popup_tags_manager));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_popup_android_settings));

        menu.setOnItemClickListener((a, v, pos) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) a).getItem(pos);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            Context c = mTBLauncherActivity;
            Intent intent;
            switch (stringId) {
                case R.string.menu_popup_tags_manager:
                    launchTagsManagerDialog();
                    break;
                case R.string.menu_popup_launcher_settings:
                    intent = new Intent(mClearButton.getContext(), SettingsActivity.class);
                    launchIntent(this, mClearButton, intent);
                    break;
                case R.string.change_wallpaper:
                    intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                    intent = Intent.createChooser(intent, c.getString(R.string.change_wallpaper));
                    launchIntent(this, mClearButton, intent);
                    break;
                case R.string.menu_widget_add:
                    TBApplication.widgetManager(c).showSelectWidget(mTBLauncherActivity);
                    break;
                case R.string.menu_widget_remove:
                    TBApplication.widgetManager(c).showRemoveWidgetPopup();
                    break;
                case R.string.menu_popup_android_settings:
                    intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
                    launchIntent(this, mClearButton, intent);
                    break;
            }
        });

        return menu;
    }

    public void launchIntent(@NonNull View view, @NonNull Intent intent) {
        launchIntent(this, view, intent);
    }

    static void launchIntent(@NonNull Behaviour behaviour, @NonNull View view, @NonNull Intent intent) {
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

    public void onPostCreate() {
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
        hideSearchBar();
        updateClearButton();
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends View> T findViewById(@IdRes int id) {
        return mTBLauncherActivity.findViewById(id);
    }

    private void updateClearButton() {
        if (mSearchEditText.getText().length() > 0 || TBApplication.state().isResultListVisible()) {
            mClearButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
        } else {
            mClearButton.setVisibility(View.INVISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
        }
    }

    public void toggleSearchBar() {
        if (TBApplication.state().isSearchBarVisible()) {
            hideKeyboard();
            hideSearchBar();
        } else {
            showKeyboard();
            showSearchBar();
        }
    }

    private void showSearchBar() {
        SystemUiVisibility.clearFullscreen(mDecorView);

        hideWidgets();

        if (!TBApplication.state().isNotificationBarVisible())
            mNotificationBackground.setTranslationY(-mNotificationBackground.getLayoutParams().height);
        mNotificationBackground.animate()
                .translationY(0f)
                .setStartDelay(0)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new LinearInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.VISIBLE);
                    }
                })
                .start();

        mSearchBarContainer.setVisibility(View.VISIBLE);
        mSearchBarContainer.animate()
                .setListener(null)
                .setStartDelay(0)
                .alpha(1f)
                .translationY(0f)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE);
                    }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.VISIBLE);
                    }
                })
                .start();

        TBApplication.quickList(getContext()).showQuickList();

        // Schedule a runnable to display UI elements after a delay
        mSearchBarContainer.removeCallbacks(mHidePart2Runnable);
        mSearchBarContainer.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void hideWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.HIDDEN);
        mWidgetContainer.setVisibility(View.GONE);
        mResultLayout.setVisibility(View.INVISIBLE);
    }

    private void hideSearchBar() {
        hideSearchBar(UI_ANIMATION_DELAY, true);
    }

    private void hideSearchBar(boolean animate) {
        hideSearchBar(0, animate);
    }

    private void hideSearchBar(int startDelay, boolean animate) {
        // Hide UI first
        ActionBar actionBar = mTBLauncherActivity != null ? mTBLauncherActivity.getSupportActionBar() : null;
        if (actionBar != null) {
            actionBar.hide();
        }

        if (animate)
            mNotificationBackground.animate()
                    .translationY(-mNotificationBackground.getLayoutParams().height)
                    .setStartDelay(startDelay)
                    .setDuration(UI_ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.HIDDEN);
                        }
                    })
                    .start();
        else {
            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.HIDDEN);
            mNotificationBackground.setTranslationY(-mNotificationBackground.getLayoutParams().height);
        }

        //TODO: animate mResultLayout to fill the space freed by mSearchBarContainer
        if (animate)
            mSearchBarContainer.animate()
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mSearchBarContainer.setVisibility(View.INVISIBLE);
                        }
                    })
                    .setStartDelay(startDelay)
                    .alpha(0f)
                    .translationY(mSearchBarContainer.getHeight() * 2f)
                    .setDuration(UI_ANIMATION_DURATION)
                    .setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN);
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.HIDDEN);
                        }
                    })
                    .start();
        else {
            TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.HIDDEN);
            mSearchBarContainer.setAlpha(0f);
            mSearchBarContainer.setTranslationY(mSearchBarContainer.getHeight() * 2f);
            mSearchBarContainer.setVisibility(View.INVISIBLE);
        }
        clearAdapter();

        showWidgets();

        TBApplication.quickList(getContext()).hideQuickList(animate);

        // Schedule a runnable to remove the status and navigation bar after a delay
        mSearchBarContainer.removeCallbacks(mShowPart2Runnable);
        //mHideHandler.post(mHidePart2Runnable);
        mSearchBarContainer.postDelayed(mHidePart2Runnable, startDelay);
    }

    private void showWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.VISIBLE);
        mWidgetContainer.setVisibility(View.VISIBLE);
        mResultLayout.setVisibility(View.GONE);
    }

    public void showKeyboard() {
        Log.i(TAG, "Keyboard - SHOW");
        TBApplication.state().setKeyboard(LauncherState.AnimatedVisibility.VISIBLE);
        mTBLauncherActivity.dismissPopup();

        mSearchEditText.requestFocus();

        InputMethodManager mgr = (InputMethodManager) mTBLauncherActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(mSearchEditText, InputMethodManager.SHOW_IMPLICIT);
        //mTBLauncherActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void hideKeyboard() {
        Log.i(TAG, "Keyboard - HIDE");
        TBApplication.state().setKeyboard(LauncherState.AnimatedVisibility.HIDDEN);
        mTBLauncherActivity.dismissPopup();

        View focus = mTBLauncherActivity.getCurrentFocus();
        mSearchEditText.clearFocus();

        // Check if no view has focus:
        InputMethodManager mgr = (InputMethodManager) mTBLauncherActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;

        if (focus != null) {
            focus.clearFocus();
            mgr.hideSoftInputFromWindow(focus.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } else {
            mgr.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
        }

        //mTBLauncherActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void displayLoader(boolean display) {
        if (mLauncherButton == null)
            return;

        Drawable loadingDrawable = mLauncherButton.getDrawable();
        if (loadingDrawable instanceof Animatable) {
            if (display)
                ((Animatable) loadingDrawable).start();
            else
                ((Animatable) loadingDrawable).stop();
        }
    }

    @NonNull
    @Override
    public Context getContext() {
        return mTBLauncherActivity;
    }

    @Override
    public void resetTask() {
        TBApplication.resetTask(getContext());
    }

    @Override
    public void clearAdapter() {
        mResultAdapter.clear();
        TBApplication.quickList(getContext()).adapterCleared();
        if (!TBApplication.state().isSearchBarVisible())
            return;
        TBApplication.state().setResultList(LauncherState.AnimatedVisibility.HIDDEN);
        mResultLayout.setVisibility(View.INVISIBLE);
        updateClearButton();
    }

    @Override
    public void updateAdapter(List<? extends EntryItem> results, boolean isRefresh) {
        if (isRefresh) {
            // We're refreshing an existing dataset, do not reset scroll!
            temporarilyDisableTranscriptMode();
        }
        if (isFragmentDialogVisible()) {
            mResultAdapter.updateResults(results);
        } else {
            TBApplication.state().setResultList(LauncherState.AnimatedVisibility.VISIBLE);
            mResultLayout.setVisibility(View.VISIBLE);
            mResultList.prepareChangeAnim();
            mResultAdapter.updateResults(results);
            mResultList.animateChange();
        }
        TBApplication.quickList(getContext()).adapterUpdated();
        mClearButton.setVisibility(View.VISIBLE);
        mMenuButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void removeResult(EntryItem result) {
        mResultAdapter.removeResult(result);
        // Do not reset scroll, we want the remaining items to still be in view
        temporarilyDisableTranscriptMode();
    }

    @Override
    public void filterResults(String text) {
        mResultList.prepareChangeAnim();
        mResultAdapter.getFilter().filter(text, count -> mResultList.animateChange());
    }

    /**
     * transcriptMode on the listView decides when to scroll back to the first item.
     * The value we have by default, TRANSCRIPT_MODE_ALWAYS_SCROLL, means that on every new search,
     * (actually, on any change to the listview's adapter items)
     * scroll is reset to the bottom, which makes sense as we want the most relevant search results
     * to be visible first (searching for "ab" after "a" should reset the scroll).
     * However, when updating an existing result set (for instance to remove a record, add a tag,
     * etc.), we don't want the scroll to be reset. When this happens, we temporarily disable
     * the scroll mode.
     * However, we need to be careful here: the PullView system we use actually relies on
     * TRANSCRIPT_MODE_ALWAYS_SCROLL being active. So we add a new message in the queue to change
     * back the transcript mode once we've rendered the change.
     * <p>
     * (why is PullView dependent on this? When you show the keyboard, no event is being dispatched
     * to our application, but if we don't reset the scroll when the keyboard appears then you
     * could be looking at an element that isn't the latest one as you start scrolling down
     * [which will hide the keyboard] and start a very ugly animation revealing items currently
     * hidden. Fairly easy to test, remove the transcript mode from the XML and the .post() here,
     * then scroll in your history, display the keyboard and scroll again on your history)
     */
    private void temporarilyDisableTranscriptMode() {
        mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_DISABLED);
        // Add a message to be processed after all current messages, to reset transcript mode to default
        mResultList.post(() -> mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL));
    }

    public void runTagSearch(@NonNull String tagName) {
        if (mSearchEditText == null)
            return;
        if (TBApplication.state().isResultListVisible() && mSearchEditText.getText().length() == 0) {
            mSearchEditText.setText("");
        } else {
            mSearchEditText.setText("");
            updateSearchRecords(false, new TagSearcher(this, tagName));
        }
    }

    public void runSearcher(@NonNull String query, Class<? extends Searcher> searcherClass) {
        if (mSearchEditText == null)
            return;
        if (TBApplication.state().isResultListVisible() && mSearchEditText.getText().length() == 0) {
            mSearchEditText.setText("");
        } else {
            mSearchEditText.setText("");
            Searcher searcher = null;
            try {
                Constructor<? extends Searcher> constructor = searcherClass.getConstructor(ISearchActivity.class, String.class);
                searcher = constructor.newInstance(this, query);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                Log.e(TAG, "new <? extends Searcher>", e);
            }
            if (searcher != null)
                updateSearchRecords(false, searcher);
        }
    }

    public void clearSearch() {
        if (mSearchEditText == null)
            return;
        mSearchEditText.setText("");
        showKeyboard();
    }

    public void refreshSearchRecords() {
        mResultList.post(() -> mResultList.refreshViews());
    }

    public void updateSearchRecords() {
        if (mSearchEditText != null)
            updateSearchRecords(true, mSearchEditText.getText().toString());
    }

    /**
     * This function gets called on query changes.
     * It will ask all the providers for data
     * This function is not called for non search-related changes! Have a look at onDataSetChanged() if that's what you're looking for :)
     *
     * @param isRefresh whether the query is refreshing the existing result, or is a completely new query
     * @param query     the query on which to search
     */
    private void updateSearchRecords(boolean isRefresh, @NonNull String query) {
//        if (isRefresh && isViewingAllApps()) {
//            // Refreshing while viewing all apps (for instance app installed or uninstalled in the background)
//            Searcher searcher = new ApplicationsSearcher(this);
//            searcher.setRefresh(isRefresh);
//            runTask(searcher);
//            return;
//        }

        updateSearchRecords(isRefresh, new QuerySearcher(this, query));
    }

    private void updateSearchRecords(boolean isRefresh, @NonNull Searcher searcher) {
        resetTask();
        mTBLauncherActivity.dismissPopup();

        if (searcher.getQuery().isEmpty()) {
            clearAdapter();
//            systemUiVisibilityHelper.resetScroll();
        } else {
            searcher.setRefresh(isRefresh);
            TBApplication.runTask(getContext(), searcher);
        }
    }

    public void beforeLaunchOccurred() {
        hideKeyboard();
    }

    public void afterLaunchOccurred() {
        mSearchEditText.postDelayed(() -> {
            Context ctx = getContext();
            if (PrefCache.clearSearchAfterLaunch(ctx)) {
                // We selected an item on the list, now we can cleanup the filter:
                if (mSearchEditText.getText().length() > 0) {
                    mSearchEditText.setText("");
                }
            }
            if (PrefCache.showWidgetScreenAfterLaunch(ctx)) {
                // show widgets when we return to the launcher
                hideSearchBar(0, true);
            }
        }, UI_ANIMATION_DELAY);
    }

    public void showContextMenu() {
        mMenuButton.performClick();
    }

    /**
     * Handle the back button press. Returns true if action handled.
     *
     * @return returns true if action handled
     */
    public boolean onBackPressed() {
        // Empty the search bar
        // (this will trigger a new event if the search bar was already empty)
        // (which means pressing back in minimalistic mode with history displayed
        // will hide history again)
        mSearchEditText.setText("");
        hideSearchBar();

        if (closeFragmentDialog())
            return true;

        // Calling super.onBackPressed() will quit the launcher, only do this if KISS is not the user's default home.
        // Action not handled (return false) if not the default launcher.
        return TBApplication.isDefaultLauncher(mTBLauncherActivity);
    }

    boolean onKeyboardClosed() {
        if (mTBLauncherActivity.dismissPopup())
            return true;
        //mHider.fixScroll();
        mSearchEditText.setText("");
        hideSearchBar(0, true);
        return false;
    }

    public void launchCustomIconDialog(AppEntry appEntry) {
        IconSelectDialog dialog = new IconSelectDialog();
        openFragmentDialog(dialog);

        // If mResultLayout is visible
//        boolean bResultListVisible = TBApplication.state().isResultListVisible();
//        if (bResultListVisible)
//            mResultLayout.setVisibility(View.INVISIBLE);

        // set args
        {
            Bundle args = new Bundle();
            args.putString("componentName", appEntry.getUserComponentName());
            args.putLong("customIcon", appEntry.getCustomIcon());
            args.putString("entryName", appEntry.getName());
            dialog.setArguments(args);
        }
        // OnDismiss: We restore mResultLayout visibility
//        if (bResultListVisible)
//            dialog.setOnDismissListener(dlg -> mResultLayout.setVisibility(View.VISIBLE));

        dialog.setOnConfirmListener(drawable -> {
            if (drawable == null)
                TBApplication.getApplication(mTBLauncherActivity).iconsHandler().restoreDefaultIcon(appEntry);
            else
                TBApplication.getApplication(mTBLauncherActivity).iconsHandler().changeIcon(appEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecords();
            TBApplication.quickList(mTBLauncherActivity).onFavoritesChanged();
        });
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "custom_icon_dialog");
    }

    public void launchCustomIconDialog(StaticEntry staticEntry) {
        IconSelectDialog dialog = new IconSelectDialog();
        openFragmentDialog(dialog);

        // If mResultLayout is visible
        boolean bResultListVisible = TBApplication.state().isResultListVisible();
        if (bResultListVisible)
            mResultLayout.setVisibility(View.INVISIBLE);

        // set args
        {
            Bundle args = new Bundle();
            args.putString("entryId", staticEntry.id);
            dialog.setArguments(args);
        }
        // OnDismiss: We restore mResultLayout visibility
        if (bResultListVisible)
            dialog.setOnDismissListener(dlg -> mResultLayout.setVisibility(View.VISIBLE));

        dialog.setOnConfirmListener(drawable -> {
            if (drawable == null)
                TBApplication.getApplication(mTBLauncherActivity).iconsHandler().restoreDefaultIcon(staticEntry);
            else
                TBApplication.getApplication(mTBLauncherActivity).iconsHandler().changeIcon(staticEntry, drawable);
            // force a result refresh to update the icon in the view
            refreshSearchRecords();
            TBApplication.quickList(mTBLauncherActivity).onFavoritesChanged();
        });
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "custom_icon_dialog");
    }

//    public void launchCustomIconDialog(TagEntry tagEntry) {
//        CustomIconDialog dialog = new CustomIconDialog();
//        openFragmentDialog(dialog);
//
//        // If mResultLayout is visible
//        boolean bResultListVisible = TBApplication.state().isResultListVisible();
//        if (bResultListVisible)
//            mResultLayout.setVisibility(View.INVISIBLE);
//
//        // set args
//        {
//            Bundle args = new Bundle();
//            args.putString("entryId", staticEntry.id);
//            dialog.setArguments(args);
//        }
//        // OnDismiss: We restore mResultLayout visibility
//        if (bResultListVisible)
//            dialog.setOnDismissListener(dlg -> mResultLayout.setVisibility(View.VISIBLE));
//
//        dialog.setOnConfirmListener(drawable -> {
//            if (drawable == null)
//                TBApplication.getApplication(mTBLauncherActivity).getIconsHandler().restoreDefaultIcon(staticEntry);
//            else
//                TBApplication.getApplication(mTBLauncherActivity).getIconsHandler().changeIcon(staticEntry, drawable);
//            // force a result refresh to update the icon in the view
//            refreshSearchRecords();
//            TBApplication.quickList(mTBLauncherActivity).onFavoritesChanged();
//        });
//        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "custom_icon_dialog");
//    }

    public void launchEditTagsDialog(EntryWithTags entry) {
        EditTagsDialog dialog = new EditTagsDialog();
        openFragmentDialog(dialog);

        // set args
        {
            Bundle args = new Bundle();
            args.putString("entryId", entry.id);
            args.putString("entryName", entry.getName());
            dialog.setArguments(args);
        }

        dialog.setOnConfirmListener(newTags -> {
            TBApplication.tagsHandler(mTBLauncherActivity).setTags(entry, newTags);
            refreshSearchRecords();
        });

        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "dialog_edit_tags");
    }

    public void launchEditQuickListDialog() {
        EditQuickListDialog dialog = new EditQuickListDialog();
        openFragmentDialog(dialog);
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "dialog_edit_quick_list");
    }

    public void launchTagsManagerDialog() {
        TagsManagerDialog dialog = new TagsManagerDialog();
        openFragmentDialog(dialog);
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), "dialog_tags_manager");
    }

    private boolean isFragmentDialogVisible() {
        return mFragmentDialog != null && mFragmentDialog.isVisible();
    }

    private void openFragmentDialog(DialogFragment<?> dialog) {
        closeFragmentDialog();
        mFragmentDialog = dialog;
    }

    private boolean closeFragmentDialog() {
        if (mFragmentDialog != null && mFragmentDialog.isVisible()) {
            mFragmentDialog.dismiss();
            return true;
        }
        return false;
    }

    public void registerPopup(ListPopup menu) {
        mTBLauncherActivity.registerPopup(menu);
    }

    public void onResume() {
        Log.i(TAG, "onResume");

        // set activity orientation
        {
            final Activity act = mTBLauncherActivity;
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

        TBApplication.dataHandler(getContext()).checkServices();
        LauncherState state = TBApplication.state();
        if (state.isWidgetScreenVisible()) {
            if (state.isSearchBarVisible())
                hideSearchBar(0, false);
        } else {
            // can't show keyboard if we don't have focus, wait for onWindowFocusChanged
            mSearchEditText.requestFocus();
            mSearchEditText.postDelayed(this::showKeyboard, UI_ANIMATION_DURATION);
            showSearchBar();
        }
    }

    public void onNewIntent() {
        Log.i(TAG, "onNewIntent");
        if (mSearchEditText.getText().length() > 0) {
            mSearchEditText.setText("");
            hideKeyboard();
            hideSearchBar(false);
        } else if (!TBApplication.state().isResultListVisible()) {
            toggleSearchBar();
        }

        Intent intent = mTBLauncherActivity.getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                // Save single shortcut via a pin request
                ShortcutUtil.addShortcut(mTBLauncherActivity, intent);
            }
        }

        closeFragmentDialog();
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        Log.i(TAG, "onWindowFocusChanged " + hasFocus);
        LauncherState state = TBApplication.state();
        if (hasFocus && state.isSearchBarVisible()) {
            if (!state.isKeyboardVisible() && !state.isResultListVisible()) {
                mSearchEditText.requestFocus();
                // UI_ANIMATION_DURATION should be the exact time the full-screen animation ends
                mSearchEditText.postDelayed(this::showKeyboard, UI_ANIMATION_DURATION);
            }
            if (mSearchEditText.getText().length() != 0 || !mResultAdapter.isEmpty()) {
                //updateSearchRecords();
                mResultLayout.setVisibility(View.VISIBLE);
            }
        } else {
            if (state.isWidgetScreenVisible()) {
                hideKeyboard();
                hideSearchBar(0, false);
            }
        }
    }

    private boolean executeAction(@Nullable String action) {
        if (action == null)
            return false;
        switch (action) {
            case "expandNotificationsPanel":
                Utilities.expandNotificationsPanel(mTBLauncherActivity);
                return true;
            case "expandSettingsPanel":
                Utilities.expandSettingsPanel(mTBLauncherActivity);
                return true;
            case "showSearchBar":
                showKeyboard();
                showSearchBar();
                return true;
            case "showWidgets":
                hideKeyboard();
                hideSearchBar();
                return true;
            case "toggleSearchAndWidget":
                toggleSearchBar();
                return true;
        }
        return false;
    }

    public boolean onFlingDownLeft() {
        return executeAction(mPref.getString("gesture-fling-down-left", null));
    }

    public boolean onFlingDownRight() {
        return executeAction(mPref.getString("gesture-fling-down-right", null));
    }

    public boolean onFlingUp() {
        return executeAction(mPref.getString("gesture-fling-up", null));
    }

    public boolean onFlingLeft() {
        return executeAction(mPref.getString("gesture-fling-left", null));
    }

    public boolean onFlingRight() {
        return executeAction(mPref.getString("gesture-fling-right", null));
    }

    public boolean onClick() {
        return executeAction(mPref.getString("gesture-click", null));
    }
}
