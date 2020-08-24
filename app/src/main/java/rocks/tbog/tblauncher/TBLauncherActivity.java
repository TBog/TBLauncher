package rocks.tbog.tblauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.ui.BlockableListView;
import rocks.tbog.tblauncher.ui.BottomPullEffectView;
import rocks.tbog.tblauncher.ui.KeyboardScrollHider;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DeviceUtils;

public class TBLauncherActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

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

    private KeyboardScrollHider mHider;
    private PopupWindow mPopup;

    /**
     * Receive events from providers
     */
    private BroadcastReceiver mReceiver;

    private Permission permissionManager;

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
         * Permission Manager
         */
        permissionManager = new Permission(this);

        /*
         * Initialize data handler and start loading providers
         */
        IntentFilter intentFilterLoad = new IntentFilter(START_LOAD);
        IntentFilter intentFilterLoadOver = new IntentFilter(LOAD_OVER);
        IntentFilter intentFilterFullLoadOver = new IntentFilter(FULL_LOAD_OVER);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (START_LOAD.equalsIgnoreCase(intent.getAction())) {
                    TBApplication.behaviour(TBLauncherActivity.this).displayLoader(true);
                } else if (LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                    //updateSearchRecords(true);
                    TBApplication.behaviour(TBLauncherActivity.this).updateSearchRecords();
                } else if (FULL_LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                    Log.v(TAG, "All providers are done loading.");

                    TBApplication.behaviour(TBLauncherActivity.this).displayLoader(false);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TBLauncherActivity.this);
                    TBApplication.drawableCache(TBLauncherActivity.this).onPrefChanged(TBLauncherActivity.this, prefs);

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
        TBApplication.ui(this).onCreateActivity(this);
        TBApplication.quickList(this).onCreateActivity(this);
        TBApplication.liveWallpaper(this).onCreateActivity(this);
        TBApplication.widgetManager(this).onCreateActivity(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        TBApplication.behaviour(this).onPostCreate();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy()");
        TBApplication.onDestroyActivity(this);
        unregisterReceiver(mReceiver);
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

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume()");
        super.onResume();

        if (TBApplication.getApplication(this).isLayoutUpdateRequired()) {
            TBApplication.getApplication(this).requireLayoutUpdate(false);
            Log.i(TAG, "Restarting app after setting changes");
            // Restart current activity to refresh view, since some preferences may require using a new UI
            this.recreate();
            return;
        }

        TBApplication.ui(this).onResume();
        TBApplication.behaviour(this).onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        super.onNewIntent(intent);

        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after, hiding the kissbar if any.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        TBApplication.behaviour(this).onNewIntent();
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

    private void initKeyboardScrollHider(BottomPullEffectView listEdgeEffect, BlockableListView resultList) {
        mHider = new KeyboardScrollHider(TBApplication.behaviour(this), resultList, listEdgeEffect);
        mHider.start();
    }

    @Override
    public void onBackPressed() {
        if (dismissPopup())
            return;

        if (TBApplication.behaviour(this).onBackPressed())
            return;

        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        TBApplication.behaviour(this).onWindowFocusChanged(hasFocus);
    }

    //    /**
//     * Schedules a call to hide() in delay milliseconds, canceling any
//     * previously scheduled calls.
//     */
//    private void delayedHide(int delayMillis) {
//        mHideHandler.removeCallbacks(mHideRunnable);
//        mHideHandler.postDelayed(mHideRunnable, delayMillis);
//    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        //popup.setVisibilityHelper(systemUiVisibilityHelper);
        popup.setOnDismissListener(() -> TBLauncherActivity.this.mPopup = null);
        mHider.fixScroll();
    }

    public boolean dismissPopup() {
        if (mPopup != null) {
            mPopup.dismiss();
            return true;
        }
        return false;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        boolean shouldDismissPopup = false;
        if (mPopup != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                if (mPopup instanceof ListPopup) {
                    // this check is not needed
                    // we'll not receive the event if it happened inside the popup
                    int x = (int) (event.getRawX() + .5f);
                    int y = (int) (event.getRawY() + .5f);
                    if (!((ListPopup) mPopup).isInsideViewBounds(x, y))
                        shouldDismissPopup = true;
                } else {
                    shouldDismissPopup = true;
                }
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
            Log.d("TBog", "Ctx: " + c.toString() + " | Res: " + c.getResources().toString());

            if (c instanceof ContextWrapper)
                c = ((ContextWrapper) c).getBaseContext();
            else
                c = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (TBApplication.widgetManager(this).onActivityResult(this, requestCode, resultCode, data))
            return;
        super.onActivityResult(requestCode, resultCode, data);
    }
}
