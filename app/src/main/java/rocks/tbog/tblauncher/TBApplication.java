package rocks.tbog.tblauncher;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceManager;

import org.acra.ACRA;
import org.acra.config.CoreConfigurationBuilder;
import org.acra.config.MailSenderConfigurationBuilder;
import org.acra.data.StringFormat;

import java.lang.ref.WeakReference;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;

import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.handler.TagsHandler;
import rocks.tbog.tblauncher.icons.IconPackCache;
import rocks.tbog.tblauncher.quicklist.QuickList;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.utils.PrefCache;
import rocks.tbog.tblauncher.utils.RootHandler;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.widgets.WidgetManager;

public class TBApplication extends Application {
    private static final String TAG = "APP";

    /**
     * The state of certain launcher features
     */
    @NonNull
    private static final LauncherState mState = new LauncherState();

    private DataHandler dataHandler = null;
    private IconsHandler iconsPackHandler = null;
    private TagsHandler tagsHandler = null;
    private AppsHandler appsHandler = null;
    private SharedPreferences mSharedPreferences = null;
    private ListPopup mPopup = null;

    /**
     * List of running launcher activities
     */
    private final LinkedList<WeakReference<TBLauncherActivity>> mActivities = new LinkedList<>();

    /**
     * Task launched on text change
     */
    private Searcher mSearchTask;
    /**
     * We store a number of drawables in memory for fast redraw
     */
    private final DrawableCache mDrawableCache = new DrawableCache();
    /**
     * We store a number of icon packs so we don't have to parse the XML
     */
    private final IconPackCache mIconPackCache = new IconPackCache();
    /**
     * We store a number of icon packs so we don't have to parse the XML
     */
    private final MimeTypeCache mMimeTypeCache = new MimeTypeCache();
    /**
     * Root handler - su
     */
    private RootHandler mRootHandler = null;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        //MultiDex.install(this);
        ACRA.init(this, new CoreConfigurationBuilder()
            .withBuildConfigClass(BuildConfig.class)
            .withReportFormat(StringFormat.JSON)
            .withPluginConfigurations(new MailSenderConfigurationBuilder()
                .withMailTo("tblauncher.acra@tbog.rocks")
                .withReportAsFile(false)
                .build()));
    }

    @NonNull
    public static TBApplication getApplication(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof TBApplication)
            return (TBApplication) appContext;
        throw new IllegalStateException("appContext " + appContext + " not of type " + TBApplication.class.getSimpleName());
    }

    @NonNull
    private TBLauncherActivity validateActivity(@NonNull Context context) {
        Activity activity = Utilities.getActivity(context);
        if (activity == null)
            throw new IllegalStateException("context " + context + " null activity");
        TBLauncherActivity foundActivity = null;
        for (WeakReference<TBLauncherActivity> ref : mActivities) {
            TBLauncherActivity launcherActivity = ref.get();
            if (launcherActivity == activity)
                foundActivity = launcherActivity;
        }
        if (foundActivity == null)
            throw new IllegalStateException("activity " + activity + " not registered");
        return foundActivity;
    }

    @NonNull
    private TBLauncherActivity getActivity() {
        WeakReference<TBLauncherActivity> ref = mActivities.peekFirst();
        if (ref == null)
            throw new IllegalStateException("no activity registered");
        TBLauncherActivity launcherActivity = ref.get();
        while (launcherActivity == null) {
            if (!mActivities.remove(ref))
                throw new ConcurrentModificationException();
            ref = mActivities.peekFirst();
            if (ref == null)
                throw new IllegalStateException("all registered activities released");
            launcherActivity = ref.get();
        }
        if (launcherActivity.getLifecycle().getCurrentState().compareTo(Lifecycle.State.DESTROYED) == 0)
            throw new IllegalStateException("activity destroyed");
        return launcherActivity;
    }

    /**
     * There should be only one activity, but for short periods of time there can be:
     * - none when launcher got shut down for memory reasons
     * - two when the activity gets recreated (user pressed the "home" button for example)
     *
     * @return most recently registered launcher activity or null
     */
    @Nullable
    public TBLauncherActivity launcherActivity() {
        WeakReference<TBLauncherActivity> ref = mActivities.peekFirst();
        TBLauncherActivity launcherActivity = ref == null ? null : ref.get();
        if (launcherActivity != null && launcherActivity.getLifecycle().getCurrentState().compareTo(Lifecycle.State.DESTROYED) == 0)
            return null;
        Log.d(TAG, "launcherActivity=" + launcherActivity);
        return launcherActivity;
    }

    /**
     * Same as the getting application from context then calling launcherActivity()
     *
     * @param context to get application from
     * @return most recently registered launcher activity or null
     */
    @Nullable
    public static TBLauncherActivity launcherActivity(@NonNull Context context) {
        return getApplication(context).launcherActivity();
    }

    public static boolean activityInvalid(@Nullable View view) {
        if (view != null && view.isAttachedToWindow())
            return activityInvalid(view.getContext());
        return false;
    }

    public static boolean activityInvalid(@Nullable Context ctx) {
        return !activityValid(ctx);
    }

    public static boolean activityValid(@Nullable Context context) {
        Context ctx = context;
        while (ctx instanceof ContextWrapper) {
            if (ctx instanceof Activity) {
                Activity act = (Activity) ctx;
                if (act.isFinishing() || act.isDestroyed()) {
                    // activity is no more
                    return false;
                }
                TBApplication app = getApplication(act);
                for (WeakReference<TBLauncherActivity> ref : app.mActivities) {
                    TBLauncherActivity launcherActivity = ref.get();
                    if (act.equals(launcherActivity)) {
                        Lifecycle.State state = launcherActivity.getLifecycle().getCurrentState();
                        return state.isAtLeast(Lifecycle.State.INITIALIZED);
                    }
                }
                // activity not registered
                return false;
            }
            ctx = ((ContextWrapper) ctx).getBaseContext();
        }
        // context is null
        return false;
    }

    public void onCreateActivity(TBLauncherActivity activity) {
        // clean list
        for (Iterator<WeakReference<TBLauncherActivity>> iterator = mActivities.iterator(); iterator.hasNext(); ) {
            WeakReference<TBLauncherActivity> ref = iterator.next();
            TBLauncherActivity launcherActivity = ref.get();
            if (launcherActivity == null)
                iterator.remove();
        }
        // add to list
        mActivities.push(new WeakReference<>(activity));
        Log.d(TAG, "activities.size=" + mActivities.size());
    }

    @NonNull
    public SharedPreferences preferences() {
        return mSharedPreferences;
    }

    public static Behaviour behaviour(@NonNull Context context) {
        TBApplication app = getApplication(context);
        return app.validateActivity(context).behaviour;
    }

    @NonNull
    public Behaviour behaviour() {
        return getActivity().behaviour;
    }

    @NonNull
    public static LiveWallpaper liveWallpaper(Context context) {
        TBApplication app = getApplication(context);
        return app.validateActivity(context).liveWallpaper;
    }

    public static QuickList quickList(Context context) {
        TBApplication app = getApplication(context);
        return app.validateActivity(context).quickList;
    }

    public static CustomizeUI ui(Context context) {
        TBApplication app = getApplication(context);
        return app.validateActivity(context).customizeUI;
    }

    @NonNull
    public static WidgetManager widgetManager(Context context) {
        TBApplication app = getApplication(context);
        return app.validateActivity(context).widgetManager;
    }

    @NonNull
    public static DrawableCache drawableCache(Context context) {
        return getApplication(context).mDrawableCache;
    }

    @NonNull
    public static IconPackCache iconPackCache(Context context) {
        return getApplication(context).mIconPackCache;
    }

    @NonNull
    public static MimeTypeCache mimeTypeCache(Context context) {
        return getApplication(context).mMimeTypeCache;
    }

    @NonNull
    public static TagsHandler tagsHandler(Context context) {
        return getApplication(context).tagsHandler();
    }

    @NonNull
    public static AppsHandler appsHandler(Context context) {
        return getApplication(context).appsHandler();
    }

    @NonNull
    public static DataHandler dataHandler(Context context) {
        return getApplication(context).getDataHandler();
    }

    @NonNull
    public static RootHandler rootHandler(Context context) {
        return getApplication(context).rootHandler();
    }

    @NonNull
    public static LauncherState state() {
        return mState;
    }

    public static void onDestroyActivity(TBLauncherActivity activity) {
        TBApplication app = getApplication(activity);
        Activity popupActivity = null;
        if (app.mPopup != null)
            popupActivity = Utilities.getActivity(app.mPopup.getContentView());
        if (popupActivity == null && app.dismissPopup())
            Log.i(TAG, "Popup dismissed in onDestroyActivity");

        for (Iterator<WeakReference<TBLauncherActivity>> iterator = app.mActivities.iterator(); iterator.hasNext(); ) {
            WeakReference<TBLauncherActivity> ref = iterator.next();
            TBLauncherActivity launcherActivity = ref.get();
            if (launcherActivity == null || launcherActivity == activity) {
                if (activity == popupActivity && app.dismissPopup())
                    Log.i(TAG, "Popup dismissed in onDestroyActivity " + activity);
                iterator.remove();
            }
        }
    }

    public static void runTask(Context context, Searcher task) {
        resetTask(context);
        getApplication(context).mSearchTask = task;
        task.execute();
    }

    public static void resetTask(Context context) {
        TBApplication app = getApplication(context);
        if (app.mSearchTask != null) {
            app.mSearchTask.cancel(true);
            app.mSearchTask = null;
        }
    }

    public static boolean hasSearchTask(Context context) {
        TBApplication app = getApplication(context);
        return app.mSearchTask != null;
    }

    public static IconsHandler iconsHandler(Context ctx) {
        return getApplication(ctx).iconsHandler();
    }

    public static boolean isDefaultLauncher(Context context) {
        String homePackage;
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_HOME);
            PackageManager pm = context.getPackageManager();
            final ResolveInfo mInfo = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
            homePackage = mInfo == null ? "null" : mInfo.activityInfo.packageName;
        } catch (Exception e) {
            homePackage = "unknown";
        }

        return homePackage.equals(context.getPackageName());
    }

    public static void resetDefaultLauncherAndOpenChooser(Context context) {
        PackageManager packageManager = context.getPackageManager();
        ComponentName componentName = new ComponentName(context, DummyLauncherActivity.class);
        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        Intent selector = new Intent(Intent.ACTION_MAIN);
        selector.addCategory(Intent.CATEGORY_HOME);
        selector.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(selector);

        packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        PreferenceManager.setDefaultValues(this, R.xml.preference_features, true);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (PrefCache.isMigrateRequired(mSharedPreferences) && PrefCache.migratePreferences(this, mSharedPreferences)) {
            Log.i(TAG, "Preferences migration done.");
        }

//        SharedPreferences.Editor editor = mSharedPreferences.edit();
//        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet() )
//        {
//            if (entry.getKey().startsWith("gesture-")) {
//                Log.d("Pref", entry.getKey() + "=" + entry.getValue());
//                editor.putString(entry.getKey(), "none");
//            }
//        }
//        editor.commit();

        mDrawableCache.onPrefChanged(this, mSharedPreferences);
    }

    @Override
    public void onTerminate() {
        TBLauncherActivity launcherActivity = launcherActivity();
        if (launcherActivity != null)
            launcherActivity.widgetManager.stop();
        super.onTerminate();
    }

    @NonNull
    public TagsHandler tagsHandler() {
        if (tagsHandler == null)
            tagsHandler = new TagsHandler(this);
        return tagsHandler;
    }

    @NonNull
    public AppsHandler appsHandler() {
        if (appsHandler == null)
            appsHandler = new AppsHandler(this);
        return appsHandler;
    }

    @NonNull
    public DataHandler getDataHandler() {
        synchronized (this) {
            if (dataHandler == null) {
                dataHandler = new DataHandler(this);
            }
        }
        return dataHandler;
    }

    @NonNull
    public DrawableCache drawableCache() {
        return mDrawableCache;
    }

    public void initDataHandler() {
        synchronized (this) {
            if (dataHandler == null) {
                dataHandler = new DataHandler(this);
            }
        }
        if (dataHandler.fullLoadOverSent()) {
            // Already loaded! We still need to fire the FULL_LOAD event
            DataHandler.sendBroadcast(this, TBLauncherActivity.FULL_LOAD_OVER, TAG);
        }
    }

    @NonNull
    public IconsHandler iconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
    }

    @NonNull
    public RootHandler rootHandler() {
        if (mRootHandler == null)
            mRootHandler = new RootHandler(mSharedPreferences);
        return mRootHandler;
    }

    public void requireLayoutUpdate() {
        for (WeakReference<TBLauncherActivity> ref : mActivities) {
            TBLauncherActivity launcherActivity = ref.get();
            if (launcherActivity != null)
                launcherActivity.requireLayoutUpdate();
        }
    }

    public void registerPopup(ListPopup popup) {
        if (mPopup == popup)
            return;
        dismissPopup();
        mPopup = popup;
        popup.setOnDismissListener(() -> mPopup = null);
    }

    public boolean dismissPopup() {
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

    /**
     * Release memory when the UI becomes hidden or when system resources become low.
     *
     * @param level the memory-related event that was raised.
     */
    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);

        if (level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
            // the process had been showing a user interface, and is no longer doing so
            mDrawableCache.clearCache();
        }
        if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // this is called every time the screen is off
            SQLiteDatabase.releaseMemory();
            mIconPackCache.clearCache(this);
            if (mSharedPreferences.getBoolean("screen-off-cache-clear", false))
                mDrawableCache.clearCache();
            mMimeTypeCache.clearCache();
        }
    }
}
