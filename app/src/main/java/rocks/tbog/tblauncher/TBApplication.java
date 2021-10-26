package rocks.tbog.tblauncher;

import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import rocks.tbog.tblauncher.handler.AppsHandler;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.handler.IconsHandler;
import rocks.tbog.tblauncher.handler.TagsHandler;
import rocks.tbog.tblauncher.icons.IconPackCache;
import rocks.tbog.tblauncher.quicklist.QuickList;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.utils.RootHandler;

public class TBApplication extends Application {

    /**
     * The state of certain launcher features
     */
    @NonNull
    private static final LauncherState mState = new LauncherState();
    private DataHandler dataHandler = null;
    private IconsHandler iconsPackHandler = null;
    private TagsHandler tagsHandler = null;
    private AppsHandler appsHandler = null;
    private boolean bLayoutUpdateRequired = false;
    private SharedPreferences mSharedPreferences = null;
    /**
     * Task launched on text change
     */
    private Searcher mSearchTask;
    /**
     * Everything that has to do with the UI behaviour
     */
    private Behaviour mBehaviour = new Behaviour();
    /**
     * Everything that has to do with the UI customization (drawables and colors)
     */
    private CustomizeUI mCustomizeUI = new CustomizeUI();
    /**
     * The favorite / quick access bar
     */
    private QuickList mQuickList = new QuickList();
    /**
     * We store a number of drawables in memory for fast redraw
     */
    private DrawableCache mDrawableCache = new DrawableCache();
    /**
     * We store a number of icon packs so we don't have to parse the XML
     */
    private IconPackCache mIconPackCache = new IconPackCache();
    /**
     * Manage live wallpaper interaction
     */
    private LiveWallpaper mLiveWallpaper = new LiveWallpaper();
    /**
     * Manage widgets
     */
    private WidgetManager mWidgetManager = new WidgetManager();
    /**
     * Root handler - su
     */
    private RootHandler mRootHandler = null;

    public static TBApplication getApplication(Context context) {
        return (TBApplication) context.getApplicationContext();
    }

    public static Behaviour behaviour(Context context) {
        return getApplication(context).mBehaviour;
    }

    public static CustomizeUI ui(Context context) {
        return getApplication(context).mCustomizeUI;
    }

    public static QuickList quickList(Context context) {
        return getApplication(context).mQuickList;
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
    public static LiveWallpaper liveWallpaper(Context context) {
        return getApplication(context).mLiveWallpaper;
    }

    @NonNull
    public static WidgetManager widgetManager(Context context) {
        return getApplication(context).mWidgetManager;
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
        TBApplication tbApplication = getApplication(activity);

        // to make sure we don't keep any references to activity or it's views
        if (tbApplication.mBehaviour.getContext() == activity)
            tbApplication.mBehaviour = new Behaviour();
        if (tbApplication.mCustomizeUI.getContext() == activity)
            tbApplication.mCustomizeUI = new CustomizeUI();
        if (tbApplication.mQuickList.getContext() == activity)
            tbApplication.mQuickList = new QuickList();
        if (tbApplication.mLiveWallpaper.getContext() == activity)
            tbApplication.mLiveWallpaper = new LiveWallpaper();
        if (tbApplication.mWidgetManager.usingActivity(activity))
            tbApplication.mWidgetManager = new WidgetManager();
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
        mWidgetManager.start(this);
    }

    @Override
    public void onTerminate() {
        mWidgetManager.stop();
        super.onTerminate();
    }

    public Behaviour behaviour() {
        return mBehaviour;
    }

    public CustomizeUI ui() {
        return mCustomizeUI;
    }

    public QuickList quickList() {
        return mQuickList;
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
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    @NonNull
    public DrawableCache drawableCache() {
        return mDrawableCache;
    }

    public void initDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        } else if (dataHandler.fullLoadOverSent()) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(TBLauncherActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
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
            mRootHandler = new RootHandler(this);
        return mRootHandler;
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
        }
    }
}
