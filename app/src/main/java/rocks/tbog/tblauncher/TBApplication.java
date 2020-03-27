package rocks.tbog.tblauncher;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import rocks.tbog.tblauncher.searcher.Searcher;

public class TBApplication extends Application {

    public static final int TOUCH_DELAY = 120;
    private DataHandler dataHandler;
    private IconsHandler iconsPackHandler;

    /**
     * Task launched on text change
     */
    private Searcher mSearchTask;

    /**
     * Everything that has to do with the UI behaviour
     */
    private Behaviour mBehaviour = new Behaviour();

    public static TBApplication getApplication(Context context) {
        return (TBApplication) context.getApplicationContext();
    }

    public static Behaviour behaviour(Context context) {
        return getApplication(context).mBehaviour;
    }

    public static void clearBehaviour(Context context) {
        getApplication(context).mBehaviour = new Behaviour();
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


    public DataHandler getDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        }
        return dataHandler;
    }

    public void initDataHandler() {
        if (dataHandler == null) {
            dataHandler = new DataHandler(this);
        } else if (dataHandler.allProvidersHaveLoaded) {
            // Already loaded! We still need to fire the FULL_LOAD event
            Intent i = new Intent(TBLauncherActivity.FULL_LOAD_OVER);
            sendBroadcast(i);
        }
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
}
