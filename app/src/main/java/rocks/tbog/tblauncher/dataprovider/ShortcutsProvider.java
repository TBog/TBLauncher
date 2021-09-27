package rocks.tbog.tblauncher.dataprovider;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.LauncherApps;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcelable;
import android.os.UserHandle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.EntryWithTags;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.loader.LoadShortcutsEntryItem;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.searcher.Searcher;
import rocks.tbog.tblauncher.shortcut.ShortcutUtil;
import rocks.tbog.tblauncher.utils.FuzzyScore;
import rocks.tbog.tblauncher.utils.Utilities;

public class ShortcutsProvider extends Provider<ShortcutEntry> {
    private static boolean notifiedKissNotDefaultLauncher = false;

    private static final String ACTION_INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    AppsCallback appsCallback = null;
    final BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_MANAGED_PROFILE_ADDED.equals(intent.getAction())) {
//                ShortcutsProvider.this.reload();
            } else if (Intent.ACTION_MANAGED_PROFILE_REMOVED.equals(intent.getAction())) {
//                android.os.UserHandle profile = intent.getParcelableExtra(Intent.EXTRA_USER);
//
//                final UserManager manager = (UserManager) ShortcutsProvider.this.getSystemService(Context.USER_SERVICE);
//                assert manager != null;
//                UserHandleCompat user = new UserHandleCompat(manager.getSerialNumberForUser(profile), profile);
//
//                DataHandler dataHandler = TBApplication.getApplication(context).getDataHandler();
//                dataHandler.removeFromExcluded(user);
//                dataHandler.removeFromFavorites(user);
//                ShortcutsProvider.this.reload();
            } else if (ACTION_INSTALL_SHORTCUT.equals(intent.getAction())) {
                Intent i = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_INTENT);
                String name = intent.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
                Parcelable bitmap = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON);

                if (i == null) {
                    Log.w("SHC", "Shortcut intent is null " + name);
                    return;
                }

                Drawable icon = null;
                if (bitmap instanceof Bitmap) {
                    icon = Utilities.createIconDrawable((Bitmap) bitmap, context);
                } else {
                    Parcelable extra = intent.getParcelableExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE);
                    if (extra instanceof Intent.ShortcutIconResource) {
                        icon = Utilities.createIconDrawable((Intent.ShortcutIconResource) extra, context);
                    }
                }
                if (icon == null) {
                    icon = TBApplication.getApplication(context).iconsHandler().getDefaultActivityIcon(context);
                }

                ShortcutRecord record = new ShortcutRecord();
                record.displayName = name;
                record.infoData = i.toUri(Intent.URI_INTENT_SCHEME);
                record.iconPng = ShortcutUtil.getIconBlob(icon);
                record.packageName = i.getPackage();
                if (record.packageName == null)
                    record.packageName = i.getComponent() != null ? i.getComponent().getPackageName() : "";
                if (!TBApplication.getApplication(context).getDataHandler().addShortcut(record))
                    Log.w("SHC", "Failed to add shortcut " + name);
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    static class AppsCallback extends LauncherApps.Callback {
        private final Context context;

        AppsCallback(Context context) {
            this.context = context;

        }

        @Override
        public void onPackageRemoved(String packageName, UserHandle user) {

        }

        @Override
        public void onPackageAdded(String packageName, UserHandle user) {

        }

        @Override
        public void onPackageChanged(String packageName, UserHandle user) {

        }

        @Override
        public void onPackagesAvailable(String[] packageNames, UserHandle user, boolean replacing) {

        }

        @Override
        public void onPackagesUnavailable(String[] packageNames, UserHandle user, boolean replacing) {

        }

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onShortcutsChanged(@NonNull String packageName, @NonNull List<ShortcutInfo> shortcuts, @NonNull UserHandle user) {
            super.onShortcutsChanged(packageName, shortcuts, user);
            for (ShortcutInfo info : shortcuts) {
                String action = null;
                ComponentName component = null;
                String intentPackage = null;
                Intent i = info.getIntent();
                if (i != null) {
                    action = i.getAction();
                    component = i.getComponent();
                    intentPackage = i.getPackage();
                }
                Log.i("SHC", "Shortcut changed for `" + packageName + "`" +
                        "\naction      " + action +
                        "\ncomponent   " + component +
                        "\nintentPack  " + intentPackage +
                        "\nshortLabel  " + info.getShortLabel() +
                        "\nlongLabel   " + info.getLongLabel() +
                        "\nisImmutable " + info.isImmutable() +
                        "\nisEnabled   " + info.isEnabled() +
                        "\nisPinned    " + info.isPinned() +
                        "\ninManifest  " + info.isDeclaredInManifest() +
                        "\nisDynamic   " + info.isDynamic());
            }
        }
    }

    @Override
    public void onCreate() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Package install/uninstall events for the main
            // profile are still handled using PackageAddedRemovedHandler itself

//            final LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
//            assert launcher != null;
//
//            appsCallback = new AppsCallback(this);
//            launcher.registerCallback(appsCallback);

            // Try to clean up app-related data when profile is removed
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_ADDED);
            filter.addAction(Intent.ACTION_MANAGED_PROFILE_REMOVED);
            filter.addAction(ACTION_INSTALL_SHORTCUT);
            registerReceiver(mProfileReceiver, filter);
        }

        super.onCreate();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mProfileReceiver);
        if (appsCallback != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) this.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                launcher.unregisterCallback(appsCallback);
            }
        }
        super.onDestroy();
    }

    @Override
    public void reload(boolean cancelCurrentLoadTask) {
        super.reload(cancelCurrentLoadTask);

        if (!isLoaded() && !isLoading()) {
            try {
                // If the user tries to add a new shortcut, but KISS isn't the default launcher
                // AND the services are not running (low memory), then we won't be able to
                // spawn a new service on Android 8.1+.
                this.initialize(new LoadShortcutsEntryItem(this));
            } catch (IllegalStateException e) {
                if (!notifiedKissNotDefaultLauncher) {
                    // Only display this message once per process
                    Toast.makeText(this, R.string.unable_to_initialize_shortcuts, Toast.LENGTH_LONG).show();
                }
                notifiedKissNotDefaultLauncher = true;
                e.printStackTrace();
            }
        }
    }

    @Override
    public void requestResults(String query, Searcher searcher) {
        StringNormalizer.Result queryNormalized = StringNormalizer.normalizeWithResult(query, false);

        if (queryNormalized.codePoints.length == 0) {
            return;
        }

        FuzzyScore fuzzyScore = new FuzzyScore(queryNormalized.codePoints);
        FuzzyScore.MatchInfo matchInfo;
        boolean match;

        for (ShortcutEntry pojo : pojos) {
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

    public EntryItem findByName(String name) {
        for (EntryItem pojo : pojos) {
            if (pojo.getName().equals(name))
                return pojo;
        }
        return null;
    }
}
