package rocks.tbog.tblauncher;

import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DELAY;
import static rocks.tbog.tblauncher.utils.Constants.UI_ANIMATION_DURATION;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.LauncherApps;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import java.lang.reflect.Constructor;
import java.util.ArrayList;

import rocks.tbog.tblauncher.customicon.ButtonHelper;
import rocks.tbog.tblauncher.quicklist.QuickList;
import rocks.tbog.tblauncher.result.CustomRecycleLayoutManager;
import rocks.tbog.tblauncher.result.RecycleAdapter;
import rocks.tbog.tblauncher.result.RecycleScrollListener;
import rocks.tbog.tblauncher.result.ResultItemDecoration;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.searcher.QuerySearcher;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.ui.KeyboardHandler;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.RecyclerList;
import rocks.tbog.tblauncher.ui.ViewStubPreview;
import rocks.tbog.tblauncher.ui.WindowInsetsHelper;
import rocks.tbog.tblauncher.utils.ActionHelper;
import rocks.tbog.tblauncher.utils.DebugInfo;
import rocks.tbog.tblauncher.utils.DeviceUtils;
import rocks.tbog.tblauncher.utils.FullscreenHelper;
import rocks.tbog.tblauncher.utils.KeyboardToggleHelper;
import rocks.tbog.tblauncher.utils.KeyboardTriggerBehaviour;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.widgets.WidgetManager;

public class TBLauncherActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "TBLA";
    public static final String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static final String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static final String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";

    /**
     * Receive events from providers
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (START_LOAD.equalsIgnoreCase(intent.getAction())) {
                mSearchHelper.displayLoader(true);
            } else if (LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                updateSearchRecords();
            } else if (FULL_LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                Log.v(TAG, "All providers are done loading.");

                TBApplication app = TBApplication.getApplication(TBLauncherActivity.this);
                app.getDataHandler().executeAfterLoadOverTasks();
                mSearchHelper.displayLoader(false);

                SharedPreferences prefs = app.preferences();
                // we need to set drawable cache preferences after we load all the apps
                app.drawableCache().onPrefChanged(TBLauncherActivity.this, prefs);
                // make sure we load the icon pack as early as possible
                app.iconsHandler().onPrefChanged(prefs);

                // Run GC once to free all the garbage accumulated during provider initialization
                System.gc();
            }
            updateTextView(debugTextView);
        }
    };

    private final OnBackPressedCallback mDismissPopupOnBackPressed = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            if (mPopup != null) {
                mPopup.dismiss();
            } else {
                Log.e(TAG, "mPopup is null when back pressed");
            }
        }
    };

    private Permission permissionManager;
    private TextView debugTextView;

    /**
     * Everything that has to do with the UI behaviour
     */
    public final Behaviour behaviour = new Behaviour();
    /**
     * Manage live wallpaper interaction
     */
    public final LiveWallpaper liveWallpaper = new LiveWallpaper();
    /**
     * The dock / quick access bar
     */
    public final QuickList quickList = new QuickList();
    /**
     * Everything that has to do with the UI customization (drawables and colors)
     */
    public final CustomizeUI customizeUI = new CustomizeUI();
    /**
     * Manage widgets
     */
    public final WidgetManager widgetManager = new WidgetManager();

    private boolean bLayoutUpdateRequired = false;
    private SharedPreferences mPref;
    private KeyboardTriggerBehaviour mKeyboardListener;
    private FullscreenHelper mFullscreenHelper;
    public ActionHelper mActionHelper;
    private ISearchActivity mSearchHelper;
    private KeyboardToggleHelper mKeyboardHandler;
    private RecycleScrollListener mRecycleScrollListener;
    private ListPopup mPopup = null;
    private RecycleAdapter mResultAdapter;

    /**
     * Layout views
     */
    public View mDecorView;
    private EditText mSearchEditText;
    private View mClearButton;
    private View mMenuButton;
    private ImageView mLauncherButton;
    public View mResultLayout;
    private RecyclerList mResultList;

    private final Runnable mShowKeyboardRunnable = () -> {
        if (WindowInsetsHelper.isKeyboardVisible(mSearchEditText))
            this.mKeyboardHandler.mRequestOpen = false;
        else
            this.mKeyboardHandler.showKeyboard();
    };

    private final Runnable mOnKeyboardClosedByUser = () -> {
        Log.i(TAG, "on keyboard closed by user");
        if (dismissPopup())
            return;
        LauncherState state = TBApplication.state();
        if (LauncherState.Desktop.SEARCH == state.getDesktop()) {
            if (PrefCache.linkCloseKeyboardToBackButton(this.mPref))
                onBackPressed();
        }
        if (LauncherState.Desktop.SEARCH == state.getDesktop()) {
            if (state.isKeyboardHidden() && PrefCache.modeSearchFullscreen(this.mPref))
                enableFullscreen(0);
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LauncherViewModel viewModel = new ViewModelProvider(this).get(LauncherViewModel.class);
        mPref = PreferenceManager.getDefaultSharedPreferences(this);

        widgetManager.start(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        final TBApplication app = TBApplication.getApplication(this);
        app.onCreateActivity(this);

        /*
         * Initialize preferences
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Permission Manager
         */
        permissionManager = new Permission(this);

        /*
         * Initialize data handler and start loading providers
         */
        IntentFilter intentFilterLoad = new IntentFilter(START_LOAD);
        IntentFilter intentFilterLoadOver = new IntentFilter(LOAD_OVER);
        IntentFilter intentFilterFullLoadOver = new IntentFilter(FULL_LOAD_OVER);

        ActivityCompat.registerReceiver(this, mReceiver, intentFilterLoad, ContextCompat.RECEIVER_NOT_EXPORTED);
        ActivityCompat.registerReceiver(this, mReceiver, intentFilterLoadOver, ContextCompat.RECEIVER_NOT_EXPORTED);
        ActivityCompat.registerReceiver(this, mReceiver, intentFilterFullLoadOver, ContextCompat.RECEIVER_NOT_EXPORTED);

        // init DataHandler after we register the receiver
        app.initDataHandler();

        setContentView(R.layout.activity_fullscreen);
        debugTextView = findViewById(R.id.debugText);

        mKeyboardListener = new KeyboardTriggerBehaviour(findViewById(R.id.root_layout));
        mKeyboardListener.observe(this, status -> {
            if (status == KeyboardTriggerBehaviour.Status.CLOSED) {
                onKeyboardClosedEvent();
            } else {
                onKeyboardOpenedEvent();
            }
        });

        initResultLayout();
        initSearchBarContainer();
        initLauncherButtons();
        initLauncherSearchEditText();

        mFullscreenHelper = new FullscreenHelper(this);
        mActionHelper = new ActionHelper(this);
        mSearchHelper = new SearchActivity(this);

        // KeyboardHandler needs SearchEditText initialized
        mKeyboardHandler = newKeyboardHandler();

        mLauncherButton.setOnClickListener((v) -> mActionHelper.executeButtonAction("button-launcher"));

        // lock decor view
        mDecorView = getWindow().getDecorView();

        if (BuildConfig.DEBUG) {
            DeviceUtils.showDeviceInfo("TBLauncher", this);
        }

        Log.d(TAG, "onCreateActivity(" + this + ")");
        // call after all views are set
        behaviour.onCreateActivity(this);
        customizeUI.onCreateActivity(this);
        quickList.onCreateActivity(this);
        liveWallpaper.onCreateActivity(this);
        widgetManager.onCreateActivity(this);

        getOnBackPressedDispatcher().addCallback(mDismissPopupOnBackPressed);
    }

    private void onKeyboardClosedEvent() {
        LauncherState state = TBApplication.state();
        boolean keyboardClosedByUser = true;
        if (state.getSearchBarVisibility() == LauncherState.AnimatedVisibility.ANIM_TO_VISIBLE) {
            Log.i(TAG, "keyboard closed - app start");
            // don't call onKeyboardClosed() when we start the app
            keyboardClosedByUser = false;
        } else if (mKeyboardHandler != null && mKeyboardHandler.mHiddenByScrolling) {
            Log.i(TAG, "keyboard closed - scrolling results");
            // keyboard closed because the result list was scrolled
            keyboardClosedByUser = false;
        } else if (behaviour.isFragmentDialogVisible()) {
            Log.i(TAG, "keyboard closed - fragment dialog");
            // don't send keyboard close event while we have a dialog open
            keyboardClosedByUser = false;
        }

        if (keyboardClosedByUser) {
            if (mKeyboardHandler != null && mKeyboardHandler.mRequestOpen) {
                Log.i(TAG, "keyboard closed - while mRequestOpen true");
                mKeyboardHandler.mRequestOpen = false;
            } else {
                Log.i(TAG, "keyboard closed - user");
                // delay keyboard closed event to make sure the keyboard is not just glitching
                mDecorView.removeCallbacks(mOnKeyboardClosedByUser);
                mDecorView.postDelayed(mOnKeyboardClosedByUser, UI_ANIMATION_DURATION);
            }
        }

        // collapse search pill
        if (state.isSearchBarVisible()) {
            int duration = 0;
            if (mPref.getBoolean("search-bar-animation", true))
                duration = UI_ANIMATION_DURATION;
            customizeUI.collapseSearchPill(duration);
        }
    }

    private void onKeyboardOpenedEvent() {
        if (mKeyboardHandler != null) {
            // request to open fulfilled
            mKeyboardHandler.mRequestOpen = false;
            // reset HiddenByScrolling flag when keyboard opens
            mKeyboardHandler.mHiddenByScrolling = false;
        }

        // don't call the keyboard closed event if keyboard opened
        mDecorView.removeCallbacks(mOnKeyboardClosedByUser);

        LauncherState state = TBApplication.state();
        // expand search pill
        if (state.isSearchBarVisible()) {
            int duration = 0;
            if (mPref.getBoolean("search-bar-animation", true))
                duration = UI_ANIMATION_DURATION;
            customizeUI.expandSearchPill(duration);
            mSearchEditText.requestFocus();
        }
    }

    private void initResultLayout() {
        mResultLayout = inflateViewStub(R.id.resultLayout);

        mResultList = mResultLayout.findViewById(R.id.resultList);
        if (mResultList == null)
            throw new IllegalStateException("mResultList==null");

        mRecycleScrollListener = new RecycleScrollListener(new KeyboardHandler() {
            @Override
            public void showKeyboard() {
                mKeyboardHandler.showKeyboard();
            }

            @Override
            public void hideKeyboard() {
                mKeyboardHandler.hideKeyboard();
                mKeyboardHandler.mHiddenByScrolling = true;
            }
        });

        mResultAdapter = new RecycleAdapter(TBLauncherActivity.this, new ArrayList<>());

        mResultList.setHasFixedSize(true);
        mResultList.setAdapter(mResultAdapter);
        mResultList.addOnScrollListener(mRecycleScrollListener);
//        mResultList.addOnLayoutChangeListener(recycleScrollListener);

        int vertical = getResources().getDimensionPixelSize(R.dimen.result_margin_vertical);
        mResultList.addItemDecoration(new ResultItemDecoration(0, vertical, true));

        setListLayout();
    }

    private void initSearchBarContainer() {
        int layout = PrefCache.getSearchBarLayout(mPref);
        View searchBarContainer;
        if (PrefCache.searchBarAtBottom(mPref)) {
            searchBarContainer = inflateViewStub(R.id.stubSearchBottom, layout);
        } else {
            searchBarContainer = inflateViewStub(R.id.stubSearchTop, layout);
        }
        if (searchBarContainer == null)
            throw new IllegalStateException("searchBarContainer==null");

        mLauncherButton = searchBarContainer.findViewById(R.id.launcherButton);
        mSearchEditText = searchBarContainer.findViewById(R.id.launcherSearch);
        mClearButton = searchBarContainer.findViewById(R.id.clearButton);
        mMenuButton = searchBarContainer.findViewById(R.id.menuButton);

        // when pill search bar expanded, show keyboard
        TBLauncherActivity.this.customizeUI.setExpandedSearchPillListener(this::showKeyboard);
    }

    private void initLauncherButtons() {
        final ListPopup buttonMenu;
        if (PrefCache.getSearchBarLayout(mPref) == R.layout.search_pill)
            buttonMenu = Behaviour.getButtonPopup(this, ButtonHelper.BTN_ID_LAUNCHER_PILL, R.drawable.launcher_pill);
        else
            buttonMenu = Behaviour.getButtonPopup(this, ButtonHelper.BTN_ID_LAUNCHER_WHITE, R.drawable.launcher_white);

        mLauncherButton.setOnLongClickListener((v) -> ButtonHelper.showButtonPopup(v, buttonMenu));

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

    private void initLauncherSearchEditText() {
        mSearchEditText.setTextIsSelectable(false);
        mSearchEditText.addTextChangedListener(mSearchTextWatcher);

        // On validate, launch first record
        mSearchEditText.setOnEditorActionListener((view, actionId, event) -> {
            // Return true if you have consumed the action, else false.

            // if keyboard close action issued
            if (actionId == android.R.id.closeButton) {
                LauncherState state = TBApplication.state();
                // Fix for #238
                state.syncKeyboardVisibility(view);
                if (state.isKeyboardHidden()) {
                    Log.i(TAG, "Keyboard - closeButton while keyboard hidden");
                    return false;
                }
                if (state.isSearchBarVisible() && PrefCache.linkKeyboardAndSearchBar(mPref)) {
                    // consume action to avoid closing the keyboard
                    Log.i(TAG, "Keyboard - closeButton - linkKeyboardAndSearchBar");
                    return true;
                }
                // close the keyboard
                Log.i(TAG, "Keyboard - closeButton - close");
                return false;
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

    public void clearSearch() {
        if (mSearchEditText == null)
            return;

        mSearchEditText.setText("");
        clearAdapter();
    }

    public void clearSearchText() {
        if (mSearchEditText == null)
            return;

        mSearchEditText.removeTextChangedListener(mSearchTextWatcher);
        mSearchEditText.setText("");
        mSearchEditText.addTextChangedListener(mSearchTextWatcher);
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
            Context c = TBLauncherActivity.this;
            if (stringId == R.string.menu_popup_tags_manager) {
                behaviour.launchTagsManagerDialog(c);
            } else if (stringId == R.string.menu_popup_tags_menu) {
                ListPopup tagsMenu = TBApplication.tagsHandler(ctx).getTagsMenu(ctx);
                registerPopup(tagsMenu);
                tagsMenu.showCenter(mDecorView);
            } else if (stringId == R.string.menu_popup_launcher_settings) {
                Intent intent = new Intent(mClearButton.getContext(), SettingsActivity.class);
                behaviour.launchIntent(mClearButton, intent);
            } else if (stringId == R.string.change_wallpaper) {
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                intent = Intent.createChooser(intent, c.getString(R.string.change_wallpaper));
                behaviour.launchIntent(mClearButton, intent);
            } else if (stringId == R.string.menu_widget_add) {
                TBApplication.widgetManager(c).showSelectWidget(TBLauncherActivity.this);
            } else if (stringId == R.string.menu_widget_remove) {
                TBApplication.widgetManager(c).showRemoveWidgetPopup();
            } else if (stringId == R.string.menu_popup_android_settings) {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                behaviour.launchIntent(mClearButton, intent);
            }
        });

        return menu;
    }

    private KeyboardToggleHelper newKeyboardHandler() {
        return new KeyboardToggleHelper(mSearchEditText) {
            @Override
            public void showKeyboard() {
                LauncherState state = TBApplication.state();
                if (TBApplication.activityInvalid(TBLauncherActivity.this)) {
                    Log.e(TAG, "[activityInvalid] showKeyboard");
                    return;
                }

                if (state.isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref)) {
                    showSystemBars();
                    disableFullscreen();
                }

                Log.i(TAG, "Keyboard - SHOW");
                dismissPopup();

                mSearchEditText.requestFocus();

                super.showKeyboard();
            }

            @Override
            public void hideKeyboard() {
                if (TBApplication.activityInvalid(TBLauncherActivity.this)) {
                    Log.e(TAG, "[activityInvalid] hideKeyboard");
                    return;
                }
                if (TBApplication.state().isSearchBarVisible() && PrefCache.modeSearchFullscreen(mPref)) {
                    //hideSystemBars();
                    enableFullscreen(0);
                }

                Log.i(TAG, "Keyboard - HIDE");
                dismissPopup();

                View focus = TBLauncherActivity.this.getCurrentFocus();
                if (focus != null)
                    focus.clearFocus();
                mSearchEditText.clearFocus();

                super.hideKeyboard();
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T inflateViewStub(@IdRes int id) {
        View stub = findViewById(id);
        return (T) ViewStubPreview.inflateStub(stub);
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T inflateViewStub(@IdRes int id, @LayoutRes int layoutRes) {
        View stub = findViewById(id);
        return (T) ViewStubPreview.inflateStub(stub, layoutRes);
    }

    public void showContextMenu() {
        mMenuButton.performClick();
    }

    public void setListLayout() {
        // update adapter draw flags
        mResultAdapter.setGridLayout(TBLauncherActivity.this, false);

        // get layout manager
        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
        if (!(layoutManager instanceof CustomRecycleLayoutManager)) {
            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());
            ((CustomRecycleLayoutManager) layoutManager).setOverScrollListener(mRecycleScrollListener);
        }

        CustomRecycleLayoutManager lm = (CustomRecycleLayoutManager) layoutManager;
        lm.setBottomToTop(PrefCache.firstAtBottom(mPref));
        lm.setColumns(1, false);
    }

    public void setGridLayout() {
        setGridLayout(3);
    }

    public void setGridLayout(int columnCount) {
        // update adapter draw flags
        mResultAdapter.setGridLayout(TBLauncherActivity.this, true);

        // get layout manager
        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
        if (!(layoutManager instanceof CustomRecycleLayoutManager)) {
            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());
            ((CustomRecycleLayoutManager) layoutManager).setOverScrollListener(mRecycleScrollListener);
        }

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

    public void clearAdapter() {
        mResultAdapter.clear();
        if (TBApplication.state().isResultListVisible())
            hideResultList(true);
        updateClearButton();
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

    public void showKeyboard() {
        mKeyboardHandler.mRequestOpen = true;

        mKeyboardHandler.showKeyboard();

        mDecorView.removeCallbacks(mShowKeyboardRunnable);
        // UI_ANIMATION_DURATION should be the exact time the full-screen animation ends
        mDecorView.postDelayed(mShowKeyboardRunnable, UI_ANIMATION_DELAY);
    }

    public void hideKeyboard() {
        mKeyboardHandler.mRequestOpen = false;

        mDecorView.removeCallbacks(mShowKeyboardRunnable);
        mKeyboardHandler.hideKeyboard();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        Log.d(TAG, "dispatchKeyEvent " + event);
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart(" + this + ")");
        super.onStart();

        behaviour.setActivityOrientation(this);

        if (DebugInfo.providerStatus(this)) {
            debugTextView.setVisibility(View.VISIBLE);
        }

        String initialDesktop = mPref.getString("initial-desktop", null);
        if (!mActionHelper.executeAction(initialDesktop, null)) {
            Log.d(TAG, "initial desktop `" + initialDesktop + "` can't be executed, setting EMPTY");
            switchToDesktop(LauncherState.Desktop.EMPTY);
        }
        customizeUI.onStart();
        quickList.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop(" + this + ")");
        super.onStop();
    }

    @Override
    protected void onRestart() {
        Log.d(TAG, "onRestart(" + this + ")");
        super.onRestart();

        behaviour.setActivityOrientation(this);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy(" + this + ")");
        mKeyboardListener = null;
        if (behaviour.closeFragmentDialog()) {
            Log.i(TAG, "closed dialog from onDestroy " + this);
        }
        TBApplication.onDestroyActivity(this);
        unregisterReceiver(mReceiver);
        widgetManager.stop();
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        //TBApplication.behaviour(this).onConfigurationChanged(this, newConfig);
        Log.d(TAG, "onConfigurationChanged" +
            " orientation=" + newConfig.orientation +
            " keyboard=" + newConfig.keyboard +
            " keyboardHidden=" + newConfig.keyboardHidden);
        super.onConfigurationChanged(newConfig);
    }

    public boolean isLayoutUpdateRequired() {
        return bLayoutUpdateRequired;
    }

    public void requireLayoutUpdate(boolean require) {
        bLayoutUpdateRequired = require;
    }

    public void requireLayoutUpdate() {
        bLayoutUpdateRequired = true;
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume(" + this + ")");
        super.onResume();

        if (isLayoutUpdateRequired()) {
            requireLayoutUpdate(false);
            Log.i(TAG, "Restarting app after setting changes");
            // Restart current activity to refresh view, since some preferences may require using a new UI
            //getWindow().getDecorView().post(TBLauncherActivity.this::recreate);
            Log.d(TAG, "finish(" + this + ")");
            finish();
            Bundle options = ActivityOptions.makeCustomAnimation(this, android.R.anim.fade_in, android.R.anim.fade_out).toBundle();
            startActivity(new Intent(this, getClass()), options);
            return;
        }

        behaviour.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent(" + this + ")");
        setIntent(intent);
        super.onNewIntent(intent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out);
        } else {
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }

        LauncherState state = TBApplication.state();
        Log.i(TAG, "onNewIntent desktop=" + state.getDesktop());

        // This is called when the user press Home again while already browsing MainActivity
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet

        behaviour.closeFragmentDialog();

        if (intent != null) {
            final String action = intent.getAction();
            if (LauncherApps.ACTION_CONFIRM_PIN_SHORTCUT.equals(action)) {
                // Save single shortcut via a pin request
                ShortcutUtil.addShortcut(TBLauncherActivity.this, intent);
                return;
            }
        }

        mActionHelper.executeButtonAction("button-home");
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        Log.i(TAG, "onSaveInstanceState " + Integer.toHexString(outState.hashCode()) + " " + this);
        super.onSaveInstanceState(outState);
        outState.clear();
    }

    @Override
    public boolean onKeyDown(int keycode, KeyEvent e) {
        // For devices with a physical menu button, we still want to display *our* contextual menu
        if (keycode == KeyEvent.KEYCODE_MENU) {
            showContextMenu();
            return true;
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public void onBackPressed() {
        if (dismissPopup())
            return;

        if (behaviour.closeFragmentDialog())
            return;

        Log.i(TAG, "onBackPressed query=" + mSearchEditText.getText());
        mSearchEditText.setText("");

        LauncherState.Desktop desktop = TBApplication.state().getDesktop();
        if (desktop != null) {
            switch (desktop) {
                case SEARCH:
                    mActionHelper.executeButtonAction("dm-search-back");
                    break;
                case WIDGET:
                    mActionHelper.executeButtonAction("dm-widget-back");
                    break;
                case EMPTY:
                default:
                    mActionHelper.executeButtonAction("dm-empty-back");
                    break;
            }
        }

        // Calling super.onBackPressed() will quit the launcher, only do this if this is not the user's default home.
        // Action not handled (return false) if not the default launcher.
        if (TBApplication.isDefaultLauncher(this))
            return;

        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        LauncherState state = TBApplication.state();
        if (hasFocus) {
            if (state.getDesktop() == LauncherState.Desktop.SEARCH) {
                if (state.isSearchBarVisible() && PrefCache.linkKeyboardAndSearchBar(mPref)) {
                    Log.d(TAG, "SearchBarVisible and linkKeyboardAndSearchBar");
                    showKeyboard();
                } else {
                    //TODO: find why keyboard gets hidden after onWindowFocusChanged
                    Log.d(TAG, "state().isKeyboardHidden=" + TBApplication.state().isKeyboardHidden() + " mRequestOpen=" + mKeyboardHandler.mRequestOpen);
                    if (mKeyboardHandler.mRequestOpen) {
                        showKeyboard();
                    }
                }
                if (TBApplication.state().isResultListVisible() && mResultAdapter.getItemCount() == 0)
                    mActionHelper.refreshDesktop();
                else if (TBApplication.state().isResultListVisible()) {
                    mActionHelper.showResultList(false);
                } else
                    mActionHelper.hideResultList(true);
            } else {
                if (state.getDesktop() == LauncherState.Desktop.WIDGET) {
                    hideKeyboard();
                }
            }
        }
    }

    //    /**
//     * Schedules a call to hide() in delay milliseconds, canceling any
//     * previously scheduled calls.
//     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    public void queueDockReload() {
        quickList.reload();
    }

    public void refreshSearchRecords() {
        if (mResultList != null) {
            mResultList.getRecycledViewPool().clear();
        }
        if (mResultAdapter != null) {
            mResultAdapter.setGridLayout(this, isGridLayout());
            mResultAdapter.refresh();
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean shouldDismissPopup = false;
        ListPopup listPopup = TBApplication.getApplication(this).getPopup();
        if (listPopup != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                // this check is not needed
                // we'll not receive the event if it happened inside the popup
                int x = (int) (event.getRawX() + .5f);
                int y = (int) (event.getRawY() + .5f);
                if (!listPopup.isInsideViewBounds(x, y))
                    shouldDismissPopup = true;
            }
        }
        if (shouldDismissPopup && dismissPopup())
            return true;
        return super.dispatchTouchEvent(event);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Context c = this;
        while (null != c) {
            Log.d(TAG, "Ctx: " + c.toString() + " | Res: " + c.getResources().toString());

            if (c instanceof ContextWrapper)
                c = ((ContextWrapper) c).getBaseContext();
            else
                c = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (widgetManager.onActivityResult(this, requestCode, resultCode, data))
            return;
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateTextView(TextView debugTextView) {
        if (debugTextView == null)
            return;

        StringBuilder text = new StringBuilder();
        TBApplication app = TBApplication.getApplication(this);

        app.getDataHandler().appendDebugText(text);

        debugTextView.setText(text);
    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        mDismissPopupOnBackPressed.setEnabled(true);
        popup.setOnDismissListener(() -> {
            mPopup = null;
            mDismissPopupOnBackPressed.setEnabled(false);
        });
    }

    public boolean dismissPopup() {
        mDismissPopupOnBackPressed.setEnabled(false);
        if (mPopup != null) {
            mPopup.dismiss();
            return true;
        }
        return false;
    }

    @Nullable
    public ListPopup getPopup() {
        return mPopup;
    }

    public void enableFullscreen(int delay) {
        mFullscreenHelper.enableFullscreen(delay);
    }

    public void disableFullscreen() {
        mFullscreenHelper.disableFullscreen();
    }

    public void hideSearchBar(boolean animate) {
        mActionHelper.hideResultList(animate);
    }

    public void showResultList(boolean animate) {
        mResultLayout.setVisibility(View.VISIBLE);
    }

    public void hideResultList(boolean animate) {
        mResultLayout.setVisibility(View.INVISIBLE);
    }

    public void switchToDesktop(LauncherState.Desktop desktop) {
        mActionHelper.mDesktopHelper.switchToDesktop(desktop);
    }

    @NonNull
    public View getResultLayout() {
        if (mResultLayout == null)
            throw new IllegalStateException("result layout is null");
        return mResultLayout;
    }

    @NonNull
    public RecyclerList getResultList() {
        if (mResultList == null)
            throw new IllegalStateException("result list is null");
        return mResultList;
    }

    @NonNull
    public RecycleAdapter getResultAdapter() {
        if (mResultAdapter == null)
            throw new IllegalStateException("result adapter is null");
        return mResultAdapter;
    }

    @NonNull
    public EditText getSearchEditText() {
        if (mSearchEditText == null)
            throw new IllegalStateException("SearchEditText is null");
        return mSearchEditText;
    }

    @NonNull
    public ImageView getLauncherButton() {
        if (mLauncherButton == null)
            throw new IllegalStateException("LauncherButton is null");
        return mLauncherButton;
    }

    @NonNull
    public ISearchActivity getSearchHelper() {
        if (mSearchHelper == null)
            throw new IllegalStateException("SearchActivity is null");
        return mSearchHelper;
    }

    private void updateSearchRecords() {
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
        updateSearchRecords(isRefresh, new QuerySearcher(mSearchHelper, query));
    }

    private void updateSearchRecords(boolean isRefresh, @NonNull Searcher searcher) {
        searcher.setRefresh(isRefresh);

        mSearchHelper.resetTask();
        dismissPopup();

        TBApplication.runTask(TBLauncherActivity.this, searcher);
        showResultList(true);
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
            searcher = constructor.newInstance(mSearchHelper, query);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.e(TAG, "new <? extends Searcher>", e);
        }
        if (searcher != null)
            updateSearchRecords(false, searcher);
    }

//    public void setListLayout() {
//        // update adapter draw flags
//        mResultAdapter.setGridLayout(this, false);
//
//        // get layout manager
//        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
//        if (!(layoutManager instanceof CustomRecycleLayoutManager)) {
//            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());
//            ((CustomRecycleLayoutManager) layoutManager).setOverScrollListener(mRecycleScrollListener);
//        }
//
//        CustomRecycleLayoutManager lm = (CustomRecycleLayoutManager) layoutManager;
//        lm.setBottomToTop(PrefCache.firstAtBottom(mPref));
//        lm.setColumns(1, false);
//    }
//
//    public void setGridLayout() {
//        setGridLayout(3);
//    }
//
//    public void setGridLayout(int columnCount) {
//        // update adapter draw flags
//        mResultAdapter.setGridLayout(this, true);
//
//        // get layout manager
//        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
//        if (!(layoutManager instanceof CustomRecycleLayoutManager)) {
//            mResultList.setLayoutManager(layoutManager = new CustomRecycleLayoutManager());
//            ((CustomRecycleLayoutManager) layoutManager).setOverScrollListener(mRecycleScrollListener);
//        }
//
//        CustomRecycleLayoutManager lm = (CustomRecycleLayoutManager) layoutManager;
//        lm.setBottomToTop(PrefCache.firstAtBottom(mPref));
//        lm.setRightToLeft(PrefCache.rightToLeft(mPref));
//        lm.setColumns(columnCount, false);
//    }
//
//    public boolean isGridLayout() {
//        RecyclerView.LayoutManager layoutManager = mResultList.getLayoutManager();
//        if (layoutManager instanceof CustomRecycleLayoutManager)
//            return ((CustomRecycleLayoutManager) layoutManager).getColumnCount() > 1;
//        return false;
//    }
}
