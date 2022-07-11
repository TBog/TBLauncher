package rocks.tbog.tblauncher.dataprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Objects;

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.broadcast.PackageAddedRemovedHandler;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.loader.LoadAppEntry;
import rocks.tbog.tblauncher.loader.LoadCacheApps;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.ISearcher;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class AppProvider extends Provider<AppEntry> {

    boolean mInitialLoad = true;
    AppsCallback appsCallback = null;
    final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), Intent.ACTION_MANAGED_PROFILE_ADDED)) {
                AppProvider.this.reload(true);
            } else if (Objects.equals(intent.getAction(), Intent.ACTION_MANAGED_PROFILE_REMOVED)) {
//                android.os.UserHandle profile = intent.getParcelableExtra(Intent.EXTRA_USER);

//                final UserManager manager = (UserManager) AppProvider.this.getSystemService(Context.USER_SERVICE);
//                assert manager != null;
//                UserHandleCompat user = new UserHandleCompat(manager.getSerialNumberForUser(profile), profile);

//                DataHandler dataHandler = TBApplication.getApplication(context).getDataHandler();
//                dataHandler.removeFromExcluded(user);
//                dataHandler.removeFromMods(user);
                AppProvider.this.reload(true);
            }
        }
    };
    final PackageAddedRemovedHandler mPackageAddedRemovedHandler = new PackageAddedRemovedHandler();

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static class AppsCallback extends LauncherApps.Callback {
        private final Context context;

        AppsCallback(Context context) {
            this.context = context;

        }

        @Override
        public void onPackageAdded(String packageName, android.os.UserHandle user) {
            if (!Process.myUserHandle().equals(user)) {
                final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                assert manager != null;
                PackageAddedRemovedHandler.handleEvent(context,
                    "android.intent.action.PACKAGE_ADDED",
                    packageName, new UserHandleCompat(manager.getSerialNumberForUser(user), user), false
                );
            }
        }

        @Override
        public void onPackageChanged(String packageName, android.os.UserHandle user) {
            if (!Process.myUserHandle().equals(user)) {
                final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                assert manager != null;
                PackageAddedRemovedHandler.handleEvent(context,
                    "android.intent.action.PACKAGE_ADDED",
                    packageName, new UserHandleCompat(manager.getSerialNumberForUser(user), user), true
                );
            }
        }

        @Override
        public void onPackageRemoved(String packageName, android.os.UserHandle user) {
            if (!Process.myUserHandle().equals(user)) {
                final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                assert manager != null;
                PackageAddedRemovedHandler.handleEvent(context,
                    "android.intent.action.PACKAGE_REMOVED",
                    packageName, new UserHandleCompat(manager.getSerialNumberForUser(user), user), false
                );
            }
        }

        @Override
        public void onPackagesAvailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
            if (!Process.myUserHandle().equals(user)) {
                final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                assert manager != null;
                PackageAddedRemovedHandler.handleEvent(context,
                    "android.intent.action.MEDIA_MOUNTED",
                    null, new UserHandleCompat(manager.getSerialNumberForUser(user), user), false
                );
            }
        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, android.os.UserHandle user, boolean replacing) {
            if (!Process.myUserHandle().equals(user)) {
                final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
                assert manager != null;
                PackageAddedRemovedHandler.handleEvent(context,
                    "android.intent.action.MEDIA_UNMOUNTED",
                    null, new UserHandleCompat(manager.getSerialNumberForUser(user), user), false
                );
            }
        }
    }

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package install/uninstall events for the main
            // profile are still handled using PackageAddedRemovedHandler itself

            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;

            appsCallback = new AppsCallback(this);
            launcher.registerCallback(appsCallback);

            // Try to clean up app-related data when profile is removed
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
            registerReceiver(mProfileReceiver, filter);
        }

        // Get notified when app changes on standard user profile
        IntentFilter appChangedFilter = new IntentFilter();
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        appChangedFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        appChangedFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
        appChangedFilter.addDataScheme("package");
        appChangedFilter.addDataScheme("file");
        this.registerReceiver(mPackageAddedRemovedHandler, appChangedFilter);

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mProfileReceiver);
        unregisterReceiver(mPackageAddedRemovedHandler);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcher != null;
            launcher.unregisterCallback(appsCallback);
        }
        super.onDestroy();
    }


    public void reload(boolean cancelCurrentLoadTask) {
        super.reload(cancelCurrentLoadTask);
        if (!isLoaded() && !isLoading()) {
            if (mInitialLoad) {
                // Use DB cache to speed things up. We'll reload after.
                this.initialize(new LoadCacheApps(this));
            } else {
                this.initialize(new LoadAppEntry(this));
            }
        }
    }

    @Override
    public void loadOver(ArrayList<AppEntry> results) {
        super.loadOver(results);
        if (mInitialLoad) {
            mInitialLoad = false;
            // Got DB cache. Do a reload later.
            TBApplication.dataHandler(this).runAfterLoadOver(() -> {
                this.reload(false);
            });
        } else {
            TBApplication.appsHandler(this).setAppCache(results);
        }
    }

    /**
     * @param query    The string to search for
     * @param searcher The receiver of results
     */

    @WorkerThread
    @Override
    public void requestResults(String query, ISearcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);

        checkAppResults(pojos, fuzzyScore, searcher);
    }

    @WorkerThread
    static void checkAppResults(Iterable<AppEntry> pojos, FuzzyScore fuzzyScore, ISearcher searcher) {
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (AppEntry pojo : pojos) {
            if (pojo.isHiddenByUser()) {
                continue;
            }

            matchInfo = fuzzyScore.match(pojo.normalizedName.codePoints);
            match = matchInfo.match;
            pojo.setRelevance(pojo.normalizedName, matchInfo);

            if (searcher.tagsEnabled()) {
                // check relevance for tags
                for (EntryWithTags.TagDetails tag : pojo.getTags()) {
                    matchInfo = fuzzyScore.match(tag.normalized.codePoints);
                    if (matchInfo.match && (!match || matchInfo.score > pojo.getRelevance())) {
                        match = true;
                        pojo.setRelevance(tag.normalized, matchInfo);
                    }
                }
            }

            if (match && !searcher.addResult(pojo)) {
                return;
            }
        }
    }


    /**
     * Return a Pojo
     *
     * @param id we're looking for
     * @return an AppEntry, or null
     */
    @Override
    public AppEntry findById(@NonNull String id) {
        for (AppEntry pojo : pojos) {
            if (pojo.id.equals(id)) {
                return pojo;
            }
        }

        return null;
    }
}
