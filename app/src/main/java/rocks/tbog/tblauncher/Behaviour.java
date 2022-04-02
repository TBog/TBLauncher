package rocks.tbog.tblauncher;

import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_GESTURE;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
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
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
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
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import rocks.tbog.tblauncher.customicon.IconSelectDialog;
import rocks.tbog.tblauncher.dataprovider.IProvider;
import rocks.tbog.tblauncher.dataprovider.TagsProvider;
import rocks.tbog.tblauncher.drawable.LoadingDrawable;
import rocks.tbog.tblauncher.entry.ActionEntry;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.DialContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.quicklist.EditQuickListDialog;
import rocks.tbog.tblauncher.result.CustomRecycleLayoutManager;
import rocks.tbog.tblauncher.result.RecycleAdapter;
import rocks.tbog.tblauncher.result.RecycleScrollListener;
import rocks.tbog.tblauncher.result.ResultHelper;
import rocks.tbog.tblauncher.result.ResultItemDecoration;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.searcher.QuerySearcher;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.DialogFragment;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.ui.WindowInsetsHelper;
import rocks.tbog.tblauncher.ui.dialog.TagsManagerDialog;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.SystemUiVisibility;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

/**
 * Behaviour of the launcher, when are stuff hidden, animation, user interaction responses
 */
public class Behaviour implements ISearchActivity {

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

    private View mResultLayout;
    private RecyclerList mResultList;
    private RecycleAdapter mResultAdapter;
    private EditText mSearchEditText;
    private View mSearchBarContainer;
    private View mWidgetContainer;
    private View mClearButton;
    private View mMenuButton;
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
    private ImageView mLauncherButton;
    private View mDecorView;
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
    private View mNotificationBackground;
    private WindowInsetsHelper mKeyboardHandler = null;
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

    private void initResultLayout() {
        mResultLayout = inflateViewStub(R.id.resultLayout);

        mResultList = mResultLayout.findViewById(R.id.resultList);
        if (mResultList == null)
            throw new IllegalStateException("mResultList==null");

        RecycleScrollListener recycleScrollListener;
        recycleScrollListener = new RecycleScrollListener(mKeyboardHandler);

        mResultAdapter = new RecycleAdapter(getContext(), new ArrayList<>());

        mResultList.setHasFixedSize(true);
        mResultList.setAdapter(mResultAdapter);
        mResultList.addOnScrollListener(recycleScrollListener);
//        mResultList.addOnLayoutChangeListener(recycleScrollListener);

        int vertical = getContext().getResources().getDimensionPixelSize(R.dimen.result_margin_vertical);
        mResultList.addItemDecoration(new ResultItemDecoration(0, vertical, true));

        setListLayout();
    }

    private void initSearchBarContainer() {
        if (PrefCache.searchBarAtBottom(mPref)) {
            mSearchBarContainer = inflateViewStub(R.id.stubSearchBottom);
            //moveSearchBarAtBottom();
        } else {
            mSearchBarContainer = inflateViewStub(R.id.stubSearchTop);
            //moveSearchBarAtTop();
        }
        mLauncherButton = mSearchBarContainer.findViewById(R.id.launcherButton);
        mSearchEditText = mSearchBarContainer.findViewById(R.id.launcherSearch);
        mClearButton = mSearchBarContainer.findViewById(R.id.clearButton);
        mMenuButton = mSearchBarContainer.findViewById(R.id.menuButton);
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
            // Return true if you have consumed the action, else false.

            // if keyboard close action issued
            if (actionId == android.R.id.closeButton) {
                // Fix for #238
                TBApplication.state().syncKeyboardVisibility(view);
                if (TBApplication.state().isKeyboardHidden())
                    return false;
                return onKeyboardClosed();
            }

            // launch most relevant result
            final int mostRelevantIdx = mResultList.getAdapterFirstItemIdx();
            if (mostRelevantIdx >= 0 && mostRelevantIdx < mResultAdapter.getItemCount()) {
                RecyclerView.ViewHolder holder = mResultList.findViewHolderForAdapterPosition(mostRelevantIdx);
                mResultAdapter.onClick(mostRelevantIdx, holder != null ? holder.itemView : view);
                return true;
            }
            return false;
        });
    }

    private void initKeyboardScrollHider() {
        mKeyboardHandler = new WindowInsetsHelper(findViewById(R.id.root_layout)) {
            @Override
            public void showKeyboard() {
                LauncherState state = TBApplication.state();
                if (TBApplication.activityInvalid(mTBLauncherActivity))
                    return;

                if (state.isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref)) {
                    showSystemBars();
                    disableFullscreen();
                }

                Log.i(TAG, "Keyboard - SHOW");
                state.setKeyboard(LauncherState.AnimatedVisibility.VISIBLE);
                dismissPopup();

                if (mSearchEditText != null)
                    mSearchEditText.requestFocus();

                super.showKeyboard();
            }

            @Override
            public void hideKeyboard() {
                if (TBApplication.activityInvalid(mTBLauncherActivity))
                    return;
                if (TBApplication.state().isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref)) {
                    //hideSystemBars();
                    enableFullscreen(0);
                }

                Log.i(TAG, "Keyboard - HIDE");
                TBApplication.state().setKeyboard(LauncherState.AnimatedVisibility.HIDDEN);
                dismissPopup();

                View focus = mTBLauncherActivity.getCurrentFocus();
                if (focus != null)
                    focus.clearFocus();
                if (mSearchEditText != null)
                    mSearchEditText.clearFocus();

                super.hideKeyboard();
            }
        };
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;
        mPref = PreferenceManager.getDefaultSharedPreferences(tbLauncherActivity);

        initKeyboardScrollHider();
        initResultLayout();
        initSearchBarContainer();

        mNotificationBackground = findViewById(R.id.notificationBackground);
        mWidgetContainer = findViewById(R.id.widgetContainer);
//        mSearchBarContainer = findViewById(R.id.searchBarContainer);
//        mLauncherButton = mSearchBarContainer.findViewById(R.id.launcherButton);
//        mSearchEditText = mSearchBarContainer.findViewById(R.id.launcherSearch);
//        mClearButton = mSearchBarContainer.findViewById(R.id.clearButton);
//        mMenuButton = mSearchBarContainer.findViewById(R.id.menuButton);

        Window window = mTBLauncherActivity.getWindow();
//        WindowCompat.setDecorFitsSystemWindows(window, false);
//        ViewCompat.setOnApplyWindowInsetsListener(window.getDecorView(), (v, insets) -> {
//            int left = v.getPaddingLeft();
//            int top = v.getPaddingTop();
//            int right = v.getPaddingRight();
//            int bottom = v.getPaddingBottom();
//            @WindowInsetsCompat.Type.InsetsType
//            int type = WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime();
//            v.setPadding(left, top, right, insets.getInsets(type).bottom);
//            return insets;
//        });

        mDecorView = window.getDecorView();

        initLauncherButton();
        initLauncherSearchEditText();

        // menu button / 3 dot button actions
        mMenuButton.setOnClickListener(v -> {
            Context ctx = v.getContext();
            ListPopup menu = getMenuPopup(ctx);
            registerPopup(menu);
            menu.showCenter(v);
        });
        mMenuButton.setOnLongClickListener(v -> {
            Context ctx = v.getContext();
            ListPopup menu = getMenuPopup(ctx);

            // check if menu contains elements and if yes show it
            if (!menu.getAdapter().isEmpty()) {
                registerPopup(menu);
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
                registerPopup(menu);
                menu.show(v);
                return true;
            }

            return false;
        });
    }

    public void onStart() {
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
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_popup_tags_menu));
        adapter.add(new LinearAdapter.Item(ctx, R.string.menu_popup_android_settings));

        menu.setOnItemClickListener((a, v, pos) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) a).getItem(pos);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            Context c = mTBLauncherActivity;
            if (stringId == R.string.menu_popup_tags_manager) {
                launchTagsManagerDialog(mTBLauncherActivity);
            } else if (stringId == R.string.menu_popup_tags_menu) {
                executeAction("showTagsMenu", "button-menu");
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

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends View> T findViewById(@IdRes int id) {
        return mTBLauncherActivity.findViewById(id);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T inflateViewStub(@IdRes int id) {
        View stub = mTBLauncherActivity.findViewById(id);
        return (T) Utilities.inflateViewStub(stub);
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
            if (TBApplication.state().isResultListVisible() && mResultAdapter.getItemCount() == 0)
                showDesktop(mode);
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
        if (TBApplication.activityInvalid(mTBLauncherActivity))
            return;
        TBApplication.state().setDesktop(mode);
        switch (mode) {
            case SEARCH:
                // show the SearchBar
                showSearchBar();

                // hide/show result list
                final String openResult = PrefCache.modeSearchOpenResult(mPref);
                if ("none".equals(openResult)) {
                    // hide result
                    hideResultList(false);
                } else {
                    // try to execute the action
                    mLauncherButton.postDelayed(() ->
                            TBApplication.dataHandler(getContext()).runAfterLoadOver(() -> {
                                if (!TBApplication.state().isResultListVisible())
                                    executeAction(openResult, "dm-search-open-result");
                            }),
                        KEYBOARD_ANIMATION_DELAY);
                }

                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();

                // enable/disable fullscreen (status and navigation bar)
                if (TBApplication.state().isKeyboardHidden()
                    && PrefCache.modeSearchFullscreen(mPref))
                    enableFullscreen(UI_ANIMATION_DELAY);
                else
                    disableFullscreen();
                break;
            case WIDGET:
                // show widgets
                showWidgets();
                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();
                // enable/disable fullscreen (status and navigation bar)
                if (PrefCache.modeWidgetFullscreen(mPref))
                    enableFullscreen(UI_ANIMATION_DELAY);
                else
                    disableFullscreen();
                break;
            case EMPTY:
            default:
                // hide/show the QuickList
                TBApplication.quickList(getContext()).updateVisibility();
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
        mSearchEditText.setEnabled(true);
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
                    LauncherState state = TBApplication.state();
                    state.setSearchBar(LauncherState.AnimatedVisibility.VISIBLE);
                    if (PrefCache.linkKeyboardAndSearchBar(mPref))
                        showKeyboard();
                    else {
                        // sync keyboard state
                        state.syncKeyboardVisibility(mSearchEditText);
                    }
                }
            })
            .start();

        mSearchEditText.requestFocus();
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
        // disabling mSearchEditText will most probably also close the keyboard
        mSearchEditText.setEnabled(false);
    }

    private void showWidgets() {
        TBApplication.state().setWidgetScreen(LauncherState.AnimatedVisibility.VISIBLE);
        mWidgetContainer.setVisibility(View.VISIBLE);

        hideResultList(false);
    }

    public void showKeyboard() {
        mKeyboardHandler.showKeyboard();
        //mTBLauncherActivity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    public void hideKeyboard() {
        mKeyboardHandler.hideKeyboard();
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

    public boolean showProviderEntries(@Nullable IProvider<?> provider) {
        return showProviderEntries(provider, null);
    }

    public boolean showProviderEntries(@Nullable IProvider<?> provider, @Nullable java.util.Comparator<? super EntryItem> comparator) {
        if (TBApplication.state().getDesktop() != LauncherState.Desktop.SEARCH) {
            // TODO: switchToDesktop might show the result list, we may need to prevent this as an optimization
            switchToDesktop(LauncherState.Desktop.SEARCH);
            clearAdapter();
        }

        List<? extends EntryItem> entries = provider != null ? provider.getPojos() : null;
        if (entries != null && entries.size() > 0) {
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

            updateAdapter(entries, false);
            return true;
        }

        return false;
    }

    @Override
    public void updateAdapter(@NonNull List<? extends EntryItem> results, boolean isRefresh) {
        Log.d(TAG, "updateAdapter " + results.size() + " result(s); isRefresh=" + isRefresh);

        if (!isFragmentDialogVisible()) {
            if (!TBApplication.state().isResultListVisible())
                showResultList(false);
        }
        mResultAdapter.updateResults(results);

        if (!isRefresh) {
            // Make sure the first item is visible when we search
            mResultList.scrollToFirstItem();
        }

        TBApplication.quickList(getContext()).adapterUpdated();
        mClearButton.setVisibility(View.VISIBLE);
        mMenuButton.setVisibility(View.INVISIBLE);
    }

    @Override
    public void removeResult(@NonNull EntryItem result) {
        // Do not reset scroll, we want the remaining items to still be in view
        mResultAdapter.removeResult(result);
    }

    @Override
    public void filterResults(String text) {
        mResultAdapter.getFilter().filter(text);
    }

    public void handleRemoveApp(String packageName) {
        int count = mResultAdapter.getItemCount();
        for (int idx = count - 1; idx >= 0; idx -= 1) {
            EntryItem entryItem = mResultAdapter.getItem(idx);
            if (entryItem.id.contains(packageName))
                removeResult(entryItem);
        }
    }

    public void runSearcher(@NonNull String query, @NonNull Class<? extends Searcher> searcherClass) {
        if (TBApplication.state().getDesktop() != LauncherState.Desktop.SEARCH) {
            // TODO: switchToDesktop might show the result list, we may need to prevent this as an optimization
            switchToDesktop(LauncherState.Desktop.SEARCH);
            clearAdapter();
        }

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
        if (mResultList != null) {
            mResultList.getRecycledViewPool().clear();
        }
        if (mResultAdapter != null) {
            mResultAdapter.setGridLayout(getContext(), isGridLayout());
            mResultAdapter.refresh();
        }
    }

    public void refreshSearchRecord(EntryItem entry) {
        mResultAdapter.notifyItemChanged(entry);
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

    public void updateSearchRecords() {
        Editable searchText = mSearchEditText.getText();
        if (searchText.length() > 0) {
            String text = searchText.toString();
            updateSearchRecords(true, text);
        } else {
            refreshSearchRecords();
        }
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
        updateSearchRecords(isRefresh, new QuerySearcher(this, query));
    }

    private void updateSearchRecords(boolean isRefresh, @NonNull Searcher searcher) {
        searcher.setRefresh(isRefresh);

        resetTask();
        dismissPopup();

        TBApplication.runTask(getContext(), searcher);
        showResultList(true);
    }

    public void beforeLaunchOccurred() {
        RecycleScrollListener.setListLayoutHeight(mResultList, mResultList.getHeight());
        hideKeyboard();
    }

    public void afterLaunchOccurred() {
        mSearchEditText.postDelayed(() -> {
            RecycleScrollListener.setListLayoutHeight(mResultList, ViewGroup.LayoutParams.MATCH_PARENT);
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

    public void setListLayout() {
        // update adapter draw flags
        mResultAdapter.setGridLayout(getContext(), false);

        // get layout manager
        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
        if (!(layoutManager instanceof CustomRecycleLayoutManager))
            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());

        CustomRecycleLayoutManager lm = (CustomRecycleLayoutManager) layoutManager;
        lm.setBottomToTop(PrefCache.firstAtBottom(mPref));
        lm.setColumns(1, false);
    }

    public void setGridLayout() {
        setGridLayout(3);
    }

    public void setGridLayout(int columnCount) {
        // update adapter draw flags
        mResultAdapter.setGridLayout(getContext(), true);

        // get layout manager
        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
        if (!(layoutManager instanceof CustomRecycleLayoutManager))
            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());

        CustomRecycleLayoutManager lm = (CustomRecycleLayoutManager) layoutManager;
        lm.setBottomToTop(PrefCache.firstAtBottom(mPref));
        lm.setRightToLeft(PrefCache.rightToLeft(mPref));
        lm.setColumns(columnCount, false);
    }

    public boolean isGridLayout() {
        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
        if (layoutManager instanceof CustomRecycleLayoutManager)
            return ((CustomRecycleLayoutManager) layoutManager).getColumnCount() > 1;
        return false;
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
        if (dismissPopup())
            return true;
        LauncherState state = TBApplication.state();

        if (state.isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref))
            enableFullscreen(0);
        if (PrefCache.linkCloseKeyboardToBackButton(mPref))
            onBackPressed();

        // check if we should hide the keyboard
        boolean closeKeyboard = state.isSearchBarVisible() && PrefCache.linkKeyboardAndSearchBar(mPref);
        if (closeKeyboard)
            state.setKeyboard(LauncherState.AnimatedVisibility.HIDDEN);
        return closeKeyboard;
    }

    @NonNull
    public static IconSelectDialog getCustomIconDialog(@NonNull Context ctx, boolean hideResultList) {
        IconSelectDialog dialog = new IconSelectDialog();
        //openFragmentDialog(dialog, DIALOG_CUSTOM_ICON);
        if (hideResultList) {
            // If results are visible
            if (TBApplication.state().isResultListVisible()) {
                final Behaviour behaviour = TBApplication.behaviour(ctx);
                behaviour.mResultLayout.setVisibility(View.INVISIBLE);
                // OnDismiss: We restore mResultLayout visibility
                dialog.setOnDismissListener(dlg -> behaviour.mResultLayout.setVisibility(View.VISIBLE));
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

    private boolean isFragmentDialogVisible() {
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
        if (TBApplication.activityInvalid(context))
            return;
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

    private void registerPopup(ListPopup menu) {
        TBApplication.getApplication(getContext()).registerPopup(menu);
    }

    private boolean dismissPopup() {
        return TBApplication.getApplication(getContext()).dismissPopup();
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
    }

    public void onNewIntent() {
        if (mTBLauncherActivity == null)
            return;
        LauncherState state = TBApplication.state();
        Log.i(TAG, "onNewIntent desktop=" + state.getDesktop());

        closeFragmentDialog();

        Intent intent = mTBLauncherActivity.getIntent();
        if (intent != null) {
            final String action = intent.getAction();
            if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                // Save single shortcut via a pin request
                ShortcutUtil.addShortcut(mTBLauncherActivity, intent);
                return;
            }
        }

        executeButtonAction("button-home");
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
                if (TBApplication.state().isResultListVisible() && mResultAdapter.getItemCount() == 0)
                    showDesktop(TBApplication.state().getDesktop());
                else if (TBApplication.state().isResultListVisible()) {
                    showResultList(false);
                } else
                    hideResultList(true);
            } else {
                if (state.getDesktop() == LauncherState.Desktop.WIDGET) {
                    hideKeyboard();
                }
            }
        }
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

    private boolean launchAppEntry(@NonNull String userComponentName) {
        Context ctx = getContext();
        UserHandleCompat user = UserHandleCompat.fromComponentName(ctx, userComponentName);
        ComponentName component = UserHandleCompat.unflattenComponentName(userComponentName);
        String appId = AppEntry.generateAppId(component, user);
        EntryItem item = TBApplication.dataHandler(ctx).getPojo(appId);
        if (item instanceof AppEntry) {
            ResultHelper.launch(mLauncherButton, item, LAUNCHED_FROM_GESTURE);
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
        item.doLaunch(mLauncherButton, LAUNCHED_FROM_GESTURE);
        return true;
    }

    private void executeButtonAction(@Nullable String button) {
        if (mPref != null)
            executeAction(mPref.getString(button, null), button);
    }

    private boolean executeGestureAction(@Nullable String gesture) {
        if (mPref != null)
            return executeAction(mPref.getString(gesture, null), gesture);
        return false;
    }

    private boolean executeAction(@Nullable String action, @Nullable String source) {
        if (action == null)
            return false;
        if (TBApplication.activityInvalid(mTBLauncherActivity)) {
            // only do stuff if we are the current activity
            return false;
        }
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
            case "showSearchBarAndKeyboard":
                showKeyboard();
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
                    anchor = mLauncherButton;
                Context ctx = mLauncherButton.getContext();
                ListPopup menu = TBApplication.tagsHandler(ctx).getTagsMenu(ctx);
                registerPopup(menu);
                if (anchor != null)
                    menu.show(anchor);
                else
                    menu.showCenter(mLauncherButton);

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
}
