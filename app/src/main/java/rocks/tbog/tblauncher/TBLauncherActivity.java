package rocks.tbog.tblauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.result.IResultList;
import rocks.tbog.tblauncher.ui.BlockableListView;
import rocks.tbog.tblauncher.ui.BottomPullEffectView;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DeviceUtils;

public class TBLauncherActivity extends AppCompatActivity implements IResultList, ActivityCompat.OnRequestPermissionsResultCallback {

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private static final int UI_ANIMATION_DURATION = 300;

    private static final String TAG = "TBL";
    public static final String START_LOAD = "fr.neamar.summon.START_LOAD";
    public static final String LOAD_OVER = "fr.neamar.summon.LOAD_OVER";
    public static final String FULL_LOAD_OVER = "fr.neamar.summon.FULL_LOAD_OVER";

//    private View mResultLayout;
//    private AnimatedListView mResultList;
//    private ResultAdapter mResultAdapter;
//    private EditText mSearchEditText;
//    private View mSearchBarContainer;
//    private View mClearButton;
//    private View mMenuButton;
//    private ImageView mLauncherButton;
//    private ProgressBar mLoaderSpinner;

    private KeyboardScrollHider mHider;
    private PopupWindow mPopup;

    /**
     * Receive events from providers
     */
    private BroadcastReceiver mReceiver;

//    private final Handler mHideHandler = new Handler();
//    private View mDecorView;
//    private final Runnable mHidePart2Runnable = new Runnable() {
//        @SuppressLint("InlinedApi")
//        @Override
//        public void run() {
//            // Delayed removal of status and navigation bar
//
//            // Note that some of these constants are new as of API 16 (Jelly Bean)
//            // and API 19 (KitKat). It is safe to use them, as they are inlined
//            // at compile-time and do nothing on earlier devices.
//            mDecorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
//                    | View.SYSTEM_UI_FLAG_FULLSCREEN
//                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//        }
//    };
//    private final Runnable mHideRunnable = new Runnable() {
//        @Override
//        public void run() {
//            hide();
//        }
//    };

    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
//    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
//        @Override
//        public boolean onTouch(View view, MotionEvent motionEvent) {
//            if (AUTO_HIDE) {
//                delayedHide(AUTO_HIDE_DELAY_MILLIS);
//            }
//            if (motionEvent.getAction() == MotionEvent.ACTION_UP)
//                return view.performClick();
//            return false;
//        }
//    };

//    @Override
//    public void onAttachedToWindow() {
//        super.onAttachedToWindow();
//
//        final Rect padding = new Rect(0, 0, 0, 0);
//        ICutout cutout = Utilities.getNotchCutout(this);
//        if (cutout.hasCutout()) {
//            padding.set(cutout.getSafeZone());
//        }
//
//        // add padding for the status bar
//        cutout = CutoutFactory.getStatusBar(this);
//        padding.top = Math.max(cutout.getSafeZone().top, padding.top);
//
//        findViewById(R.id.root_layout).setPadding(padding.left, padding.top, padding.right, padding.bottom);
//    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        TBApplication.getApplication(this).initDataHandler();

        /*
         * Initialize preferences
         */
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        //prefs = PreferenceManager.getDefaultSharedPreferences(this);

        /*
         * Initialize data handler and start loading providers
         */
        IntentFilter intentFilterLoad = new IntentFilter(START_LOAD);
        IntentFilter intentFilterLoadOver = new IntentFilter(LOAD_OVER);
        IntentFilter intentFilterFullLoadOver = new IntentFilter(FULL_LOAD_OVER);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //noinspection ConstantConditions
                if (intent.getAction().equalsIgnoreCase(LOAD_OVER)) {
                    //updateSearchRecords(true);
                    TBApplication.behaviour(TBLauncherActivity.this).updateSearchRecords();
                } else if (intent.getAction().equalsIgnoreCase(FULL_LOAD_OVER)) {
                    Log.v(TAG, "All providers are done loading.");

                    TBApplication.behaviour(TBLauncherActivity.this).displayLoader(false);

                    // Run GC once to free all the garbage accumulated during provider initialization
                    System.gc();
                }

                // New provider might mean new favorites
                //onFavoriteChange();
            }
        };

        registerReceiver(mReceiver, intentFilterLoad);
        registerReceiver(mReceiver, intentFilterLoadOver);
        registerReceiver(mReceiver, intentFilterFullLoadOver);

        setContentView(R.layout.activity_fullscreen);

        if (BuildConfig.DEBUG) {
            DeviceUtils.showDeviceInfo("TBLauncher", this);
        }

        initKeyboardScrollHider(findViewById(R.id.listEdgeEffect), findViewById(R.id.resultList));

        // call after all views are set
        TBApplication.behaviour(this).onCreateActivity(this);
    }

    @Override
    protected void onDestroy() {
        TBApplication.behaviour(this).onDestroyActivity(this);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keycode, @NonNull KeyEvent e) {
        // For devices with a physical menu button, we still want to display *our* contextual menu
        if (keycode == KeyEvent.KEYCODE_MENU) {
            TBApplication.behaviour(this).showContextMenu();
            return true;
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        if (super.onContextItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));
                return true;
            case R.id.wallpaper:
                //hideKeyboard();
                Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
                startActivity(Intent.createChooser(intent, getString(R.string.menu_wallpaper)));
                return true;
            case R.id.preferences:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return false;
    }

    private void initKeyboardScrollHider(BottomPullEffectView listEdgeEffect, BlockableListView resultList) {
        mHider = new KeyboardScrollHider(TBApplication.behaviour(this), resultList, listEdgeEffect);
        mHider.start();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        TBApplication.behaviour(this).onPostCreate();

        if (!Permission.checkContactPermission(this)) {
            Permission.askContactPermission(this);
        }

    }

    @Override
    public void onBackPressed() {
        if (dismissPopup())
            return;

        if ( TBApplication.behaviour(this).onBackPressed() )
            return;

        super.onBackPressed();
    }

//    /**
//     * Schedules a call to hide() in delay milliseconds, canceling any
//     * previously scheduled calls.
//     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    /**
     * Call this function when we're leaving the activity after clicking a search result
     * to clear the search list.
     * We can't use onPause(), since it may be called for a configuration change
     */
    @Override
    public void launchOccurred() {
        TBApplication.behaviour(this).onLaunchOccurred();
    }

    @Override
    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        //popup.setVisibilityHelper(systemUiVisibilityHelper);
        popup.setOnDismissListener(() -> TBLauncherActivity.this.mPopup = null);
        mHider.fixScroll();
    }

    @Override
    public boolean dismissPopup() {
        if (mPopup != null) {
            mPopup.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Permission.onRequestPermissionsResult(this, requestCode, permissions, grantResults);
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(newBase);

        Context c = this;
        while (null != c) {
            Log.d("TBog", "Ctx: " + c.toString() + " | Res: " + c.getResources().toString());

            if (c instanceof ContextWrapper)
                c = ((ContextWrapper) c).getBaseContext();
            else
                c = null;
        }
    }
}
