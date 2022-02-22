package rocks.tbog.tblauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.quicklist.QuickList;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.DebugInfo;
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

    /**
     * Receive events from providers
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (START_LOAD.equalsIgnoreCase(intent.getAction())) {
                behaviour.displayLoader(true);
            } else if (LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                behaviour.updateSearchRecords();
            } else if (FULL_LOAD_OVER.equalsIgnoreCase(intent.getAction())) {
                Log.v(TAG, "All providers are done loading.");

                TBApplication app = TBApplication.getApplication(TBLauncherActivity.this);
                app.getDataHandler().executeAfterLoadOverTasks();
                behaviour.displayLoader(false);

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        widgetManager.start(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }

        final TBApplication app = TBApplication.getApplication(this);
        app.onCreateActivity(this);
        app.initDataHandler();

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

        registerReceiver(mReceiver, intentFilterLoad);
        registerReceiver(mReceiver, intentFilterLoadOver);
        registerReceiver(mReceiver, intentFilterFullLoadOver);

        setContentView(R.layout.activity_fullscreen);
        debugTextView = findViewById(R.id.debugText);

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
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart(" + this + ")");
        super.onStart();

        if (DebugInfo.providerStatus(this)) {
            debugTextView.setVisibility(View.VISIBLE);
        }

        behaviour.onStart();
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

    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy(" + this + ")");
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
            startActivity(new Intent(this, getClass()));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            return;
        }

        behaviour.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent(" + this + ")");
        setIntent(intent);
        super.onNewIntent(intent);

        // This is called when the user press Home again while already browsing MainActivity
        // onResume() will be called right after, hiding the kissbar if any.
        // http://developer.android.com/reference/android/app/Activity.html#onNewIntent(android.content.Intent)
        // Animation can't happen in this method, since the activity is not resumed yet, so they'll happen in the onResume()
        // https://github.com/Neamar/KISS/issues/569
        behaviour.onNewIntent();
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
            behaviour.showContextMenu();
            return true;
        }

        return super.onKeyDown(keycode, e);
    }

    @Override
    public void onBackPressed() {
        if (TBApplication.getApplication(this).dismissPopup())
            return;

        if (behaviour.onBackPressed())
            return;

        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        behaviour.onWindowFocusChanged(hasFocus);
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
        behaviour.refreshSearchRecords();
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
        if (shouldDismissPopup && TBApplication.getApplication(this).dismissPopup())
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
}
