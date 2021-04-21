package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.ActionBar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import rocks.tbog.tblauncher.customicon.IconSelectDialog;
import rocks.tbog.tblauncher.dataprovider.FavProvider;
import rocks.tbog.tblauncher.drawable.LoadingDrawable;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.quicklist.EditQuickListDialog;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.searcher.QuerySearcher;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.AnimatedListView;
import rocks.tbog.tblauncher.ui.BlockableListView;
import rocks.tbog.tblauncher.ui.BottomPullEffectView;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.TagsManagerDialog;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;

import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_GESTURE;


/**
 * Behaviour of the launcher, when are stuff hidden, animation, user interaction responses
 */
public class Behaviour implements ISearchActivity {

    private static final int UI_ANIMATION_DELAY = 300;
    // time to wait for the keyboard to show up
    private static final int KEYBOARD_ANIMATION_DELAY = 100;
    private static final int UI_ANIMATION_DURATION = 200;
    public static final int LAUNCH_DELAY = 100;
    private static final String TAG = Behaviour.class.getSimpleName();

    static final String DIALOG_CUSTOM_ICON = "custom_icon_dialog";

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
    private KeyboardScrollHider mHider;

    private final KeyboardScrollHider.KeyboardHandler mKeyboardHandler = new KeyboardScrollHider.KeyboardHandler() {
        @Override
        public void showKeyboard() {
            LauncherState state = TBApplication.state();
//            if (state.isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref))
//                disableFullscreen();

            Log.i(TAG, "Keyboard - SHOW");
            state.setKeyboard(LauncherState.AnimatedVisibility.VISIBLE);
            mTBLauncherActivity.dismissPopup();

            mSearchEditText.requestFocus();
            mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);

            InputMethodManager mgr = (InputMethodManager) mTBLauncherActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            assert mgr != null;
            mgr.showSoftInput(mSearchEditText, InputMethodManager.SHOW_IMPLICIT);
        }

        @Override
        public void hideKeyboard() {
            Log.i(TAG, "Keyboard - HIDE");

            if (TBApplication.state().isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref))
                enableFullscreen(0);

            TBApplication.state().setKeyboard(LauncherState.AnimatedVisibility.HIDDEN);
            mTBLauncherActivity.dismissPopup();

            mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
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
        }
    };

    private final Runnable mHidePart2Runnable = new Runnable() {
        @Override
        public void run() {
//            if (TBApplication.state().isKeyboardVisible()) {
//                // if keyboard is visible, the notification bar is also visible
//                return;
//            }

            // Delayed hide UI elements
            ActionBar actionBar = mTBLauncherActivity != null ? mTBLauncherActivity.getSupportActionBar() : null;
            if (actionBar != null) {
                actionBar.hide();
            }
            SystemUiVisibility.setFullscreen(mDecorView);
            //mTBLauncherActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
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
            SystemUiVisibility.clearFullscreen(mDecorView);
            //mTBLauncherActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        }
    };

    private final TextWatcher mSearchTextWatcher = new TextWatcher() {
        @NonNull
        String lastText = "";

        public void afterTextChanged(Editable s) {
            //Log.i(TAG, "afterTextChanged `" + s + "`");
            // left-trim text.
            final int length = s.length();
            int spaceEnd = 0;
            while (spaceEnd < length && s.charAt(spaceEnd) == ' ')
                spaceEnd += 1;
            if (spaceEnd > 0) {
                // delete and wait for the next call to afterTextChanged generated by the delete
                s.delete(0, spaceEnd);
            } else {
                String text = s.toString();
                if (lastText.equals(text))
                    return;
                if (text == null || text.isEmpty())
                    clearAdapter();
                else
                    updateSearchRecords(false, text);
                updateClearButton();
            }
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            lastText = (s != null) ? s.toString() : "";
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // do nothing
        }
    };

    private SharedPreferences mPref;

    private void initResultLayout() {
        mResultAdapter = new ResultAdapter(new ArrayList<>());
        mResultList = mResultLayout.findViewById(R.id.resultList);
        mResultList.setAdapter(mResultAdapter);
        mResultList.setOnItemClickListener((parent, view, position, id) -> mResultAdapter.onClick(position, view));
        mResultList.setOnItemLongClickListener((parent, view, position, id) -> mResultAdapter.onLongClick(position, view));
    }

    private void initSearchBarContainer() {
        if (PrefCache.searchBarAtBottom(mPref))
            moveSearchBarAtBottom();
        else
            moveSearchBarAtTop();
    }

    private void moveSearchBarAtBottom() {
        // move search bar
        ViewGroup.LayoutParams layoutParams = mSearchBarContainer.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            mSearchBarContainer.setLayoutParams(params);
        }

        // update quick list constraints
        ViewGroup quickList = findViewById(R.id.quickList);
        layoutParams = quickList.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.bottomToTop = mSearchBarContainer.getId();
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            quickList.setLayoutParams(params);
        }

        // update result list constraints
        layoutParams = mResultLayout.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.topToBottom = ConstraintLayout.LayoutParams.UNSET;
            params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            mResultLayout.setLayoutParams(params);
        }
    }

    private void moveSearchBarAtTop() {
        // move search bar
        ViewGroup.LayoutParams layoutParams = mSearchBarContainer.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.topToBottom = mNotificationBackground.getId();
            params.bottomToBottom = ConstraintLayout.LayoutParams.UNSET;
            mSearchBarContainer.setLayoutParams(params);
        }

        // update quick list constraints
        ViewGroup quickList = findViewById(R.id.quickList);
        layoutParams = quickList.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.bottomToTop = ConstraintLayout.LayoutParams.UNSET;
            params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            quickList.setLayoutParams(params);
        }

        // update result list constraints
        layoutParams = mResultLayout.getLayoutParams();
        if (layoutParams instanceof ConstraintLayout.LayoutParams) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) layoutParams;
            params.topToBottom = mSearchBarContainer.getId();
            params.topToTop = ConstraintLayout.LayoutParams.UNSET;
            mResultLayout.setLayoutParams(params);
        }
    }

    private void initLauncherButton() {
        //mLoaderSpinner = loaderBar;
        mLauncherButton.setImageDrawable(new LoadingDrawable());

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //mLauncherButton.setOnTouchListener(mDelayHideTouchListener);

        //mLauncherButton.setOnClickListener((v) -> TBApplication.dataHandler(v.getContext()).reloadProviders());
        mLauncherButton.setOnClickListener((v) -> executeButtonAction("button-launcher"));
    }

    private void setSearchHint() {
        Set<String> selectedHints = mPref.getStringSet("selected-search-hints", null);
        if (selectedHints != null && !selectedHints.isEmpty()) {
            int random = new Random().nextInt(selectedHints.size());
            for (String selectedHint : selectedHints) {
                if (--random < 0) {
                    mSearchEditText.setHint(selectedHint);
                    break;
                }
            }
        }
    }

    private void initLauncherSearchEditText() {
        setSearchHint();

        mSearchEditText.setTextIsSelectable(false);
        mSearchEditText.addTextChangedListener(mSearchTextWatcher);

        // On validate, launch first record
        mSearchEditText.setOnEditorActionListener((view, actionId, event) -> {
            // if keyboard closed
            if (actionId == android.R.id.closeButton)
                return onKeyboardClosed();

            // launch most relevant result
            mResultAdapter.onClick(mResultAdapter.getCount() - 1, view);
            return true;
        });
    }

    private void initKeyboardScrollHider(BottomPullEffectView listEdgeEffect, BlockableListView resultList) {
        mHider = new KeyboardScrollHider(mKeyboardHandler, resultList, listEdgeEffect);
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(tbLauncherActivity);

        mNotificationBackground = findViewById(R.id.notificationBackground);
        mResultLayout = findViewById(R.id.resultLayout);
        mWidgetContainer = findViewById(R.id.widgetContainer);
        mSearchBarContainer = findViewById(R.id.searchBarContainer);
        mLauncherButton = findViewById(R.id.launcherButton);
        mSearchEditText = findViewById(R.id.launcherSearch);
        mClearButton = findViewById(R.id.clearButton);
        mMenuButton = findViewById(R.id.menuButton);

        mDecorView = mTBLauncherActivity.getWindow().getDecorView();

        initKeyboardScrollHider(findViewById(R.id.listEdgeEffect), findViewById(R.id.resultList));
        initResultLayout();
        initSearchBarContainer();
        initLauncherButton();
        initLauncherSearchEditText();

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
        mClearButton.setOnClickListener(v -> clearSearch());
        mClearButton.setOnLongClickListener(v -> {
            clearSearch();

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

    public void onPostCreate() {
        String initialDesktop = mPref.getString("initial-desktop", null);
        if (executeAction(initialDesktop, null))
            return;
        switchToDesktop(LauncherState.Desktop.EMPTY);
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
            if (stringId == R.string.menu_popup_tags_manager) {
                launchTagsManagerDialog();
            } else if (stringId == R.string.menu_popup_launcher_settings) {
                Intent intent = new Intent(mClearButton.getContext(), SettingsActivity.class);
                launchIntent(this, mClearButton, intent);
            } else if (stringId == R.string.change_wallpaper) {
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                intent = Intent.createChooser(intent, c.getString(R.string.change_wallpaper));
                launchIntent(this, mClearButton, intent);
            } else if (stringId == R.string.menu_widget_add) {
                TBApplication.widgetManager(c).showSelectWidget(mTBLauncherActivity);
            } else if (stringId == R.string.menu_widget_remove) {
                TBApplication.widgetManager(c).showRemoveWidgetPopup();
            } else if (stringId == R.string.menu_popup_android_settings) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                launchIntent(this, mClearButton, intent);
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

    public void switchToDesktop(@NonNull LauncherState.Desktop mode) {
        // get current mode
        @Nullable
        LauncherState.Desktop currentMode = TBApplication.state().getDesktop();
        Log.d(TAG, "desktop changed " + currentMode + " -> " + mode);
        if (mode.equals(currentMode)) {
            // no change, maybe refresh?
            //showDesktop(mode);
            return;
        }

        // hide current mode
        if (currentMode != null) {
            switch (currentMode) {
                case SEARCH:
                    hideSearchBar();
                    break;
                case WIDGET:
                    hideWidgets();
                    break;
                case EMPTY:
                default:
                    break;
            }
        }

        // show next mode
        showDesktop(mode);
    }

    private void showDesktop(LauncherState.Desktop mode) {
        TBApplication.state().setDesktop(mode);
        switch (mode) {
            case SEARCH:
                // show the SearchBar
                showSearchBar();
                // hide/show the QuickList
                if (PrefCache.modeSearchQuickListVisible(mPref))
                    TBApplication.quickList(getContext()).showQuickList();
                else
                    TBApplication.quickList(getContext()).hideQuickList(false);
                // enable/disable fullscreen (status and navigation bar)
                if (!TBApplication.state().isKeyboardVisible()
                        && PrefCache.modeSearchFullscreen(mPref))
                    enableFullscreen(UI_ANIMATION_DELAY);
                else
                    disableFullscreen();

                final String openResult = PrefCache.modeSearchOpenResult(mPref);
                if ("none".equals(openResult)) {
                    // hide result
                    hideResultList(false);
                } else {
                    // try to execute the action
                    TBApplication.dataHandler(getContext()).runAfterLoadOver(() -> {
                        mLauncherButton.postDelayed(() -> executeAction(openResult, null), KEYBOARD_ANIMATION_DELAY);
                    });
                }
                break;
            case WIDGET:
                // show widgets
                showWidgets();
                // hide/show the QuickList
                if (PrefCache.modeWidgetQuickListVisible(mPref))
                    TBApplication.quickList(getContext()).showQuickList();
                else
                    TBApplication.quickList(getContext()).hideQuickList(false);
                // enable/disable fullscreen (status and navigation bar)
                if (PrefCache.modeWidgetFullscreen(mPref))
                    enableFullscreen(UI_ANIMATION_DELAY);
                else
                    disableFullscreen();
                break;
            case EMPTY:
            default:
                // hide/show the QuickList
                if (PrefCache.modeEmptyQuickListVisible(mPref))
                    TBApplication.quickList(getContext()).showQuickList();
                else
                    TBApplication.quickList(getContext()).hideQuickList(false);
                // enable/disable fullscreen (status and navigation bar)
                if (PrefCache.modeEmptyFullscreen(mPref))
                    enableFullscreen(UI_ANIMATION_DELAY);
                else
                    disableFullscreen();
                break;
        }
    }

    /**
     * Hide status and notification bar
     *
     * @param startDelay milliseconds of delay
     */
    private void enableFullscreen(int startDelay) {
        boolean animate = !SystemUiVisibility.isFullscreenSet(mDecorView) || TBApplication.state().isNotificationBarVisible();

        // Schedule a runnable to remove the status and navigation bar after a delay
        mDecorView.removeCallbacks(mShowPart2Runnable);
        mDecorView.postDelayed(mHidePart2Runnable, startDelay);

        // hide notification background
        final int statusHeight = UISizes.getStatusBarSize(getContext());
        if (TBApplication.state().getNotificationBarVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN)
            mNotificationBackground.animate().cancel();
        if (animate) {
            mNotificationBackground.animate()
                    .translationY(-statusHeight)
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
        } else {
            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.HIDDEN);
            mNotificationBackground.setTranslationY(-statusHeight);
        }
    }

    /**
     * Show status and notification bar
     */
    private void disableFullscreen() {
        boolean animate = SystemUiVisibility.isFullscreenSet(mDecorView) || !TBApplication.state().isNotificationBarVisible();

        // Schedule a runnable to display UI elements after a delay
        mDecorView.removeCallbacks(mHidePart2Runnable);
        mDecorView.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);

        // show notification background
        final int statusHeight = UISizes.getStatusBarSize(getContext());
        if (!TBApplication.state().isNotificationBarVisible())
            mNotificationBackground.setTranslationY(-statusHeight);
        if (TBApplication.state().getNotificationBarVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE)
            mNotificationBackground.animate().cancel();
        if (animate) {
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
        } else {
            TBApplication.state().setNotificationBar(LauncherState.AnimatedVisibility.VISIBLE);
            mNotificationBackground.setTranslationY(0f);
        }
    }

    private void showSearchBar() {
        setSearchHint();

        mSearchBarContainer.animate().cancel();
        mSearchBarContainer.setVisibility(View.VISIBLE);
        mSearchBarContainer.animate()
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

        if (PrefCache.linkKeyboardAndSearchBar(mPref))
            showKeyboard();
    }

    private void hideWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.HIDDEN);
        mWidgetContainer.setVisibility(View.GONE);

//        hideResultList(false);
    }

    private void hideSearchBar() {
        hideSearchBar(UI_ANIMATION_DELAY, true);
    }

    private void hideSearchBar(int startDelay, boolean animate) {
        clearSearchText();
        clearAdapter();

        final float translationY;
        if (PrefCache.searchBarAtBottom(mPref))
            translationY = mSearchBarContainer.getHeight() * 2f;
        else
            translationY = mSearchBarContainer.getHeight() * -2f;

        mSearchBarContainer.animate().cancel();
        if (animate) {
            mSearchBarContainer.animate()
                    .setStartDelay(startDelay)
                    .alpha(0f)
                    .translationY(translationY)
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
                            mSearchBarContainer.setVisibility(View.GONE);
                        }
                    })
                    .start();
        } else {
            TBApplication.state().setSearchBar(LauncherState.AnimatedVisibility.HIDDEN);
            mSearchBarContainer.setAlpha(0f);
            mSearchBarContainer.setTranslationY(translationY);
            mSearchBarContainer.setVisibility(View.GONE);
        }

        if (PrefCache.linkKeyboardAndSearchBar(mPref))
            hideKeyboard();
    }

    private void showWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.VISIBLE);
        mWidgetContainer.setVisibility(View.VISIBLE);

        hideResultList(false);
    }

    public void showKeyboard() {
        mKeyboardHandler.showKeyboard();

        mHider.start();
        //mTBLauncherActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void hideKeyboard() {
        mKeyboardHandler.hideKeyboard();

        mHider.stop();
        //mTBLauncherActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
    }

    @Override
    public void displayLoader(boolean running) {
        if (mLauncherButton == null)
            return;

        Drawable loadingDrawable = mLauncherButton.getDrawable();
        if (loadingDrawable instanceof Animatable) {
            if (running)
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

        if (TBApplication.state().isResultListVisible())
            hideResultList(true);
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
            if (!TBApplication.state().isResultListVisible())
                showResultList(false);
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

    public void runSearcher(@NonNull String query, @NonNull Class<? extends Searcher> searcherClass) {
        if (mSearchEditText == null)
            return;
        /*if (TBApplication.state().isResultListVisible() && mSearchEditText.getText().length() == 0) {
            mSearchEditText.setText("");
        } else*/
        {
            clearSearchText();
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

    public void clearSearchText() {
        if (mSearchEditText == null)
            return;

        mSearchEditText.removeTextChangedListener(mSearchTextWatcher);
        mSearchEditText.setText("");
        mSearchEditText.addTextChangedListener(mSearchTextWatcher);
    }

    public void clearSearch() {
        if (mSearchEditText == null)
            return;

        mSearchEditText.setText("");
        clearAdapter();
    }

    public void refreshSearchRecords() {
        if (mResultList == null)
            return;
        mResultList.post(() -> mResultList.refreshViews());
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

    private void showResultList(boolean animate) {
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
        Log.d(TAG, "hideResultList (anim " + animate + ")");
        if (TBApplication.state().getResultListVisibility() != LauncherState.AnimatedVisibility.ANIM_TO_HIDDEN)
            mResultLayout.animate().cancel();
        if (mResultLayout.getVisibility() != View.VISIBLE)
            return;
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

    private void updateSearchRecords(boolean isRefresh, @NonNull Searcher searcher) {
        if (isRefresh) {
            //mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_NORMAL);
            temporarilyDisableTranscriptMode();
        } else {
            mResultList.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        }

        resetTask();
        mTBLauncherActivity.dismissPopup();

        searcher.setRefresh(isRefresh);
        TBApplication.runTask(getContext(), searcher);
        showResultList(true);
    }

    public void beforeLaunchOccurred() {
        hideKeyboard();
    }

    public void afterLaunchOccurred() {
        mSearchEditText.postDelayed(() -> {
            if (PrefCache.clearSearchAfterLaunch(mPref)) {
                // We selected an item on the list, now we can cleanup the filter:
                if (mSearchEditText.getText().length() > 0) {
                    mSearchEditText.setText("");
                } else if (TBApplication.state().isResultListVisible()) {
                    clearAdapter();
                }
            }
            if (PrefCache.showWidgetScreenAfterLaunch(mPref)) {
                // show widgets when we return to the launcher
                switchToDesktop(LauncherState.Desktop.WIDGET);
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
        if (closeFragmentDialog())
            return true;

        mSearchEditText.setText("");

        switch (TBApplication.state().getDesktop()) {
            case SEARCH:
                executeButtonAction("dm-search-back");
                break;
            case WIDGET:
                executeButtonAction("dm-widget-back");
                break;
            case EMPTY:
            default:
                executeButtonAction("dm-empty-back");
                break;
        }

        // Calling super.onBackPressed() will quit the launcher, only do this if this is not the user's default home.
        // Action not handled (return false) if not the default launcher.
        return TBApplication.isDefaultLauncher(mTBLauncherActivity);
    }

    boolean onKeyboardClosed() {
        if (mTBLauncherActivity.dismissPopup())
            return true;
        LauncherState state = TBApplication.state();
        state.setKeyboard(LauncherState.AnimatedVisibility.HIDDEN);
        if (state.isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref))
            enableFullscreen(0);
        if (PrefCache.linkCloseKeyboardToBackButton(mPref))
            onBackPressed();
        // check if we should hide the keyboard
        return state.isSearchBarVisible() && PrefCache.linkKeyboardAndSearchBar(mPref);
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
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_CUSTOM_ICON);
    }

    public void launchCustomIconDialog(ShortcutEntry shortcutEntry) {
        IconSelectDialog dialog = new IconSelectDialog();
        openFragmentDialog(dialog);

        // set args
        {
            Bundle args = new Bundle();
            args.putString("packageName", shortcutEntry.packageName);
            args.putString("shortcutData", shortcutEntry.shortcutData);
            args.putString("shortcutId", shortcutEntry.id);
            dialog.setArguments(args);
        }

        dialog.setOnConfirmListener(drawable -> {
            if (drawable == null)
                TBApplication.iconsHandler(mTBLauncherActivity).restoreDefaultIcon(shortcutEntry);
            else {
                final TBApplication app = TBApplication.getApplication(mTBLauncherActivity);
                final DataHandler dh = app.getDataHandler();
                EntryItem favItem = null;
                // if the shortcut is not in the favorites, add it before changing the icon
                {
                    FavProvider favProvider = dh.getFavProvider();
                    if (favProvider != null)
                        favItem = favProvider.findById(shortcutEntry.id);
                    if (favItem == null)
                        dh.addToFavorites(shortcutEntry);
                }
                app.iconsHandler().changeIcon(shortcutEntry, drawable);
            }
            // force a result refresh to update the icon in the view
            refreshSearchRecords();
            TBApplication.quickList(mTBLauncherActivity).onFavoritesChanged();
        });
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_CUSTOM_ICON);
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
        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_CUSTOM_ICON);
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
//        dialog.show(mTBLauncherActivity.getSupportFragmentManager(), DIALOG_CUSTOM_ICON);
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

        LauncherState.Desktop desktop = TBApplication.state().getDesktop();
        showDesktop(desktop);
        if (desktop != null) {
            switch (desktop) {
                case SEARCH:
                    // hide/show the QuickList
                    if (PrefCache.modeSearchQuickListVisible(mPref))
                        TBApplication.quickList(getContext()).showQuickList();
                    else
                        TBApplication.quickList(getContext()).hideQuickList(false);
                    break;
                case WIDGET:
                    // hide/show the QuickList
                    if (PrefCache.modeWidgetQuickListVisible(mPref))
                        TBApplication.quickList(getContext()).showQuickList();
                    else
                        TBApplication.quickList(getContext()).hideQuickList(false);
                    break;
                case EMPTY:
                default:
                    // hide/show the QuickList
                    if (PrefCache.modeEmptyQuickListVisible(mPref))
                        TBApplication.quickList(getContext()).showQuickList();
                    else
                        TBApplication.quickList(getContext()).hideQuickList(false);
                    break;
            }
        }
    }

    public void onNewIntent() {
        LauncherState state = TBApplication.state();
        Log.i(TAG, "onNewIntent desktop=" + state.getDesktop());

        executeButtonAction("button-home");
//        if (mSearchEditText.getText().length() > 0) {
//            mSearchEditText.setText("");
//            hideKeyboard();
//            hideSearchBar(false);
//        } else if (!TBApplication.state().isResultListVisible()) {
//            toggleSearchBar();
//        }

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
        if (hasFocus) {
            if (state.getDesktop() == LauncherState.Desktop.SEARCH) {
                if (state.isSearchBarVisible() && PrefCache.linkKeyboardAndSearchBar(mPref)) {
                    mSearchEditText.requestFocus();
                    mSearchEditText.post(this::showKeyboard);
                    // UI_ANIMATION_DURATION should be the exact time the full-screen animation ends
                    mSearchEditText.postDelayed(this::showKeyboard, UI_ANIMATION_DURATION);
                }
                //if (mSearchEditText.getText().length() != 0 || !mResultAdapter.isEmpty())
                if (TBApplication.state().isResultListVisible()) {
                    //updateSearchRecords();
                    showResultList(false);
                }
            } else {
                if (state.getDesktop() == LauncherState.Desktop.WIDGET) {
                    hideKeyboard();
                }
            }
        }
    }

    private boolean launchStaticEntry(@NonNull String entryId) {
        Context ctx = getContext();
        EntryItem item = TBApplication.dataHandler(ctx).getPojo(entryId);
        if (item instanceof StaticEntry) {
            if (TBApplication.state().getDesktop() != LauncherState.Desktop.SEARCH) {
                // TODO: switchToDesktop might show the result list, we may need to prevent this as an optimization
                switchToDesktop(LauncherState.Desktop.SEARCH);
                clearAdapter();
            }
            // make sure the QuickList will not toggle off
            TBApplication.quickList(ctx).adapterCleared();
            item.doLaunch(mLauncherButton, LAUNCHED_FROM_GESTURE);
            return true;
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.entry_not_found, entryId), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private boolean launchActionEntry(@NonNull String action) {
        return launchStaticEntry(ActionEntry.SCHEME + action);
    }

    private boolean launchAppEntry(@NonNull String appId) {
        Context ctx = getContext();
        EntryItem item = TBApplication.dataHandler(ctx).getPojo(AppEntry.SCHEME + appId);
        if (item instanceof AppEntry) {
            ResultHelper.launch(mLauncherButton, item, LAUNCHED_FROM_GESTURE);
            return true;
        } else {
            Toast.makeText(ctx, ctx.getString(R.string.application_not_found, appId), Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    private void executeButtonAction(@Nullable String button) {
        executeAction(mPref.getString(button, null), button);
    }

    private boolean executeGestureAction(@Nullable String gesture) {
        return executeAction(mPref.getString(gesture, null), gesture);
    }

    private boolean executeAction(@Nullable String action, @Nullable String source) {
        if (action == null)
            return false;
        switch (action) {
            case "lockScreen":
                if (DeviceAdmin.isAdminActive(mTBLauncherActivity)) {
                    DeviceAdmin.lockScreen(mTBLauncherActivity);
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
                switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            case "showWidgets":
                switchToDesktop(LauncherState.Desktop.WIDGET);
                return true;
            case "showEmpty":
                switchToDesktop(LauncherState.Desktop.EMPTY);
                return true;
            case "toggleSearchAndWidget":
                if (TBApplication.state().getDesktop() == LauncherState.Desktop.SEARCH)
                    switchToDesktop(LauncherState.Desktop.WIDGET);
                else
                    switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            case "toggleSearchWidgetEmpty": {
                final LauncherState.Desktop desktop = TBApplication.state().getDesktop();
                if (desktop == LauncherState.Desktop.SEARCH)
                    switchToDesktop(LauncherState.Desktop.WIDGET);
                else if (desktop == LauncherState.Desktop.WIDGET)
                    switchToDesktop(LauncherState.Desktop.EMPTY);
                else
                    switchToDesktop(LauncherState.Desktop.SEARCH);
                return true;
            }
            case "reloadProviders":
                TBApplication.dataHandler(getContext()).reloadProviders();
                return true;
            case "showAllAppsAZ":
                return launchActionEntry("show/apps/byName");
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
            case "runApp": {
                String runApp = mPref.getString(source + "-app-to-run", null);
                if (runApp != null)
                    return launchAppEntry(runApp);
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
        String action = mPref.getString("gesture-double-click", null);
        return action != null && !action.isEmpty() && !action.equals("none");
    }

    public boolean onDoubleClick() {
        return executeGestureAction("gesture-double-click");
    }

    public void fixScroll() {
        mHider.fixScroll();
    }
}
