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

import rocks.tbog.tblauncher.searcher.Searcher;

public class TBApplication extends Application {

    private DataHandler dataHandler;
    private IconsHandler iconsPackHandler;
    private TagsHandler tagsHandler = null;
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
     * Manage live wallpaper interaction
     */
    private LiveWallpaper mLiveWallpaper = new LiveWallpaper();

    @Override
    public void onCreate() {
        super.onCreate();
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mDrawableCache.onPrefChanged(this, mSharedPreferences);
    }

    public static TBApplication getApplication(Context context) {
        return (TBApplication) context.getApplicationContext();
    }

    public Behaviour behaviour() {
        return mBehaviour;
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
    public static LiveWallpaper liveWallpaper(Context context) {
        return getApplication(context).mLiveWallpaper;
    }

    @NonNull
    public static TagsHandler tagsHandler(Context context) {
        TBApplication app = getApplication(context);
        if (app.tagsHandler == null)
            app.tagsHandler = new TagsHandler(app);
        return app.tagsHandler;
    }

    @NonNull
    public static DataHandler dataHandler(Context context) {
        return getApplication(context).getDataHandler();
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
    }

    public static void runTask(Context context, Searcher task) {
        resetTask(context);
        getApplication(context).mSearchTask = task;
        task.executeOnExecutor(Searcher.SEARCH_THREAD);
    }

    public static void resetTask(Context context) {
        TBApplication app = getApplication(context);
        if (app.mSearchTask != null) {
            app.mSearchTask.cancel(true);
            app.mSearchTask = null;
        }
    }

    @NonNull
    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    @NonNull
    public DrawableCache getDrawableCache() {
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

    public static IconsHandler iconsHandler(Context ctx) {
        return getApplication(ctx).getIconsHandler();
    }

    public IconsHandler getIconsHandler() {
        if (iconsPackHandler == null) {
            iconsPackHandler = new IconsHandler(this);
        }

        return iconsPackHandler;
    }

    public void resetIconsHandler() {
        iconsPackHandler = new IconsHandler(this);
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
        } else if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // this is called every time the screen is off
            SQLiteDatabase.releaseMemory();
            if (mSharedPreferences.getBoolean("screen-off-cache-clear", false))
                mDrawableCache.clearCache();
        }
    }
}
