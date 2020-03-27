package rocks.tbog.tblauncher;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.DragEvent;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;

import java.util.ArrayList;

import rocks.tbog.tblauncher.result.Result;
import rocks.tbog.tblauncher.result.ResultAdapter;
import rocks.tbog.tblauncher.searcher.ISearchActivity;
import rocks.tbog.tblauncher.searcher.QuerySearcher;
import rocks.tbog.tblauncher.ui.AnimatedListView;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;

public class Behaviour implements ISearchActivity, KeyboardScrollHider.KeyboardHandler {

    private static final int UI_ANIMATION_DELAY = 300;
    private static final int UI_ANIMATION_DURATION = 300;
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    private TBLauncherActivity mTBLauncherActivity = null;

    private boolean bSearchBarHidden;

    private View mResultLayout;
    private AnimatedListView mResultList;
    private ResultAdapter mResultAdapter;
    private EditText mSearchEditText;
    private View mSearchBarContainer;
    private View mClearButton;
    private View mMenuButton;
    private ImageView mLauncherButton;
    private ProgressBar mLoaderSpinner;
    private View mDecorView;
    private Handler mHideHandler;

    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
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

    private void initResultLayout(ViewGroup resultLayout) {
        mResultLayout = resultLayout;
        mResultAdapter = new ResultAdapter(mTBLauncherActivity, new ArrayList<>());
        mResultList = resultLayout.findViewById(R.id.resultList);
        mResultList.setAdapter(mResultAdapter);
        mResultList.setOnItemClickListener((parent, view, position, id) -> mResultAdapter.onClick(position, view));
        mResultList.setOnItemLongClickListener((parent, view, position, id) -> mResultAdapter.onLongClick(position, view));
    }

    private void initLauncherButton(ImageView launcherButton, ProgressBar loaderBar) {
        mLauncherButton = launcherButton;
        mLoaderSpinner = loaderBar;
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        //mLauncherButton.setOnTouchListener(mDelayHideTouchListener);
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
                displayClearOnInput();
            }
        });

        // On validate, launch first record
        searchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                // if keyboard closed
                if (actionId == android.R.id.closeButton) {
                    if (mTBLauncherActivity.dismissPopup())
                        return true;
                    //mHider.fixScroll();
                    mSearchEditText.setText("");
                    hideSearchBar();
                    return false;
                }

                // launch most relevant result
                mResultAdapter.onClick(mResultAdapter.getCount() - 1, v);
                return true;
            }
        });
    }

    public void onCreateActivity(TBLauncherActivity tbLauncherActivity) {
        mTBLauncherActivity = tbLauncherActivity;

        bSearchBarHidden = true;
        mSearchBarContainer = findViewById(R.id.searchBarContainer);
        mClearButton = findViewById(R.id.clearButton);
        mMenuButton = findViewById(R.id.menuButton);

        mDecorView = mTBLauncherActivity.getWindow().getDecorView();
        mHideHandler = new Handler(Looper.getMainLooper());

        // Set up the user interaction to manually show or hide the system UI.
        findViewById(R.id.root_layout).setOnClickListener(view -> toggleSearchBar());

        initResultLayout(findViewById(R.id.resultLayout));
        initLauncherButton(findViewById(R.id.launcherButton), findViewById(R.id.loaderBar));
        initLauncherSearchEditText(findViewById(R.id.launcherSearch));

        mTBLauncherActivity.registerForContextMenu(mMenuButton);
        mClearButton.setOnClickListener(v -> mSearchEditText.setText(""));
        mMenuButton.setOnClickListener(View::showContextMenu);
    }

    public void onDestroyActivity(TBLauncherActivity tbLauncherActivity) {
        if (mTBLauncherActivity != tbLauncherActivity)
            throw new RuntimeException("Destroy called from a different activity");

        // to make sure we don't keep any references, recreate this class
        TBApplication.clearBehaviour(tbLauncherActivity);
    }

    public void onPostCreate() {
        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
        hideSearchBar();
        displayClearOnInput();
    }

    @SuppressWarnings("TypeParameterUnusedInFormals")
    private <T extends View> T findViewById(@IdRes int id) {
        return mTBLauncherActivity.findViewById(id);
    }

    private void displayClearOnInput() {
        if (mSearchEditText.getText().length() > 0) {
            mClearButton.setVisibility(View.VISIBLE);
            mMenuButton.setVisibility(View.INVISIBLE);
        } else {
            mClearButton.setVisibility(View.INVISIBLE);
            mMenuButton.setVisibility(View.VISIBLE);
        }
    }

    private void toggleSearchBar() {
        if (bSearchBarHidden) {
            showKeyboard();
            showSearchBar();
        } else {
            hideKeyboard();
            hideSearchBar();
        }
    }

    private void showSearchBar() {
        // Show the system bar
        mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);

        mSearchBarContainer.animate()
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        mSearchBarContainer.setVisibility(View.VISIBLE);
                    }
                })
                .setStartDelay(0)
                .alpha(1f)
                .translationY(0f)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        bSearchBarHidden = false;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    private void hideSearchBar() {
        // Hide UI first
        ActionBar actionBar = mTBLauncherActivity != null ? mTBLauncherActivity.getSupportActionBar() : null;
        if (actionBar != null) {
            actionBar.hide();
        }

        //TODO: animate mResultLayout to fill the space freed by mSearchBarContainer
        mSearchBarContainer.animate()
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mSearchBarContainer.setVisibility(View.INVISIBLE);
                    }
                })
                .setStartDelay(UI_ANIMATION_DELAY)
                .alpha(0f)
                .translationY(mSearchBarContainer.getHeight() * 2f)
                .setDuration(UI_ANIMATION_DURATION)
                .setInterpolator(new AccelerateInterpolator())
                .start();
        clearAdapter();
        bSearchBarHidden = true;


        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        //mHideHandler.post(mHidePart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    public void showKeyboard() {
        mSearchEditText.requestFocus();
        InputMethodManager mgr = (InputMethodManager) mTBLauncherActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
        assert mgr != null;
        mgr.showSoftInput(mSearchEditText, InputMethodManager.SHOW_IMPLICIT);

        //systemUiVisibilityHelper.onKeyboardVisibilityChanged(true);
    }

    public void hideKeyboard() {
        // Check if no view has focus:
        View view = mTBLauncherActivity.getCurrentFocus();
        if (view != null) {
            InputMethodManager inputManager = (InputMethodManager) mTBLauncherActivity.getSystemService(Context.INPUT_METHOD_SERVICE);
            //noinspection ConstantConditions
            inputManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }

        //systemUiVisibilityHelper.onKeyboardVisibilityChanged(false);
        mTBLauncherActivity.dismissPopup();

        mSearchEditText.clearFocus();
    }

    @Override
    public void applyScrollSystemUi() {
        //systemUiVisibilityHelper.applyScrollSystemUi();
    }

    @Override
    public void displayLoader(boolean display) {
        int animationDuration = mTBLauncherActivity.getResources().getInteger(android.R.integer.config_longAnimTime);

        // Do not display animation if launcher button is already visible
        if (!display && mLauncherButton.getVisibility() == View.INVISIBLE) {
            mLauncherButton.setVisibility(View.VISIBLE);

            // Animate transition from loader to launch button
            mLauncherButton.setAlpha(0f);
            mLauncherButton.animate()
                    .alpha(1f)
                    .setDuration(animationDuration)
                    .setListener(null);
            mLoaderSpinner.animate()
                    .alpha(0f)
                    .setDuration(animationDuration)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoaderSpinner.setVisibility(View.GONE);
                            mLoaderSpinner.setAlpha(1f);
                        }
                    });
        } else if (display) {
            mLauncherButton.setVisibility(View.INVISIBLE);
            mLoaderSpinner.setVisibility(View.VISIBLE);
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
        mResultLayout.setVisibility(View.INVISIBLE);
    }

    @Override
    public void updateAdapter(ArrayList<Result> results, boolean isRefresh, String query) {
        mResultLayout.setVisibility(View.VISIBLE);
        mResultList.prepareChangeAnim();
        mResultAdapter.updateResults(results, query);
        if (isRefresh) {
            // We're refreshing an existing dataset, do not reset scroll!
            temporarilyDisableTranscriptMode();
        }
        mResultList.animateChange();
    }

    @Override
    public void removeResult(Result result) {
        mResultAdapter.removeResult(result);
        // Do not reset scroll, we want the remaining items to still be in view
        temporarilyDisableTranscriptMode();
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

    public void updateSearchRecords() {
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
    private void updateSearchRecords(boolean isRefresh, String query) {
//        if (isRefresh && isViewingAllApps()) {
//            // Refreshing while viewing all apps (for instance app installed or uninstalled in the background)
//            Searcher searcher = new ApplicationsSearcher(this);
//            searcher.setRefresh(isRefresh);
//            runTask(searcher);
//            return;
//        }

        resetTask();
        mTBLauncherActivity.dismissPopup();

//        forwarderManager.updateSearchRecords(isRefresh, query);

        if (query.isEmpty()) {
            clearAdapter();
//            systemUiVisibilityHelper.resetScroll();
        } else {
            QuerySearcher querySearcher = new QuerySearcher(this, query);
            querySearcher.setRefresh(isRefresh);
            TBApplication.runTask(getContext(), querySearcher);
        }
    }

    public void onLaunchOccurred() {
        // We selected an item on the list, now we can cleanup the filter:
        if (!mSearchEditText.getText().toString().isEmpty()) {
            mSearchEditText.setText("");
            displayClearOnInput();
            hideKeyboard();
        }
    }

    public void showContextMenu() {
        mMenuButton.showContextMenu();
        mMenuButton.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
    }

    /**
     * Handle the back button press. Returns true if action handled.
     * @return returns true if action handled
     */
    public boolean onBackPressed() {
        // Empty the search bar
        // (this will trigger a new event if the search bar was already empty)
        // (which means pressing back in minimalistic mode with history displayed
        // will hide history again)
        mSearchEditText.setText("");
        hideSearchBar();

        // Calling super.onBackPressed() will quit the launcher, only do this if KISS is not the user's default home.
        // Action not handled (return false) if not the default launcher.
        return TBApplication.isDefaultLauncher(mTBLauncherActivity);
    }
}
