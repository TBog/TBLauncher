package rocks.tbog.tblauncher.shortcut;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.preference.PreferenceManager;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.ShortcutRecord;
import rocks.tbog.tblauncher.utils.Utilities;

import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST;
import static android.content.pm.LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED;

public class ShortcutUtil {

    final static private String TAG = "ShortcutUtil";

    /**
     * @return true if shortcuts are enabled in settings and android version is higher or equals android 8
     */
    public static boolean areShortcutsEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                prefs.getBoolean("enable-shortcuts", true);

    }

    /**
     * Save all oreo shortcuts to DB
     */
    public static void addAllShortcuts(Context context) {
        new SaveAllOreoShortcutsAsync(context).execute();
    }

    /**
     * Save single shortcut to DB via pin request
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static void addShortcut(Context context, Intent intent) {
        new SaveSingleOreoShortcutAsync(context, intent).execute();
    }

    /**
     * Remove all shortcuts saved in the database
     */
    public static void removeAllShortcuts(Context context) {
        DBHelper.removeAllShortcuts(context);
    }

    /**
     * @return all shortcuts from all applications available on the device
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getAllShortcuts(Context context) {
        return getShortcut(context, null);
    }

    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcut(Context context, String packageName) {
        return getShortcut(context, packageName, FLAG_MATCH_MANIFEST | FLAG_MATCH_PINNED);
    }

    /**
     * @return all shortcuts for given package name
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static List<ShortcutInfo> getShortcut(Context context, String packageName, int queryFlags) {
        List<ShortcutInfo> shortcutInfoList = new ArrayList<>();

        UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        assert manager != null;
        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;
        if (launcherApps.hasShortcutHostPermission()) {
            LauncherApps.ShortcutQuery shortcutQuery = new LauncherApps.ShortcutQuery();
            shortcutQuery.setQueryFlags(queryFlags);

            if (!TextUtils.isEmpty(packageName)) {
                shortcutQuery.setPackage(packageName);
            }

            for (android.os.UserHandle profile : manager.getUserProfiles()) {
                List<ShortcutInfo> list;
                try {
                    list = launcherApps.getShortcuts(shortcutQuery, profile);
                } catch (IllegalStateException e) {
                    // profile is locked or user not running
                    list = null;
                }
                if (list != null)
                    shortcutInfoList.addAll(list);
            }
        }
        return shortcutInfoList;
    }

    /**
     * Create ShortcutPojo from ShortcutInfo
     */
    @TargetApi(Build.VERSION_CODES.O)
    public static ShortcutRecord createShortcutRecord(Context context, ShortcutInfo shortcutInfo, boolean includePackageName) {
        ShortcutRecord record = new ShortcutRecord();
        record.packageName = shortcutInfo.getPackage();
        record.infoData = shortcutInfo.getId();
        record.addFlags(ShortcutRecord.FLAG_OREO);

        LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        assert launcherApps != null;
        final Drawable iconDrawable = launcherApps.getShortcutIconDrawable(shortcutInfo, 0);
        record.iconPng = iconDrawable != null ? getIconBlob(iconDrawable) : null;

        String appName = includePackageName ? getAppNameFromPackageName(context, shortcutInfo.getPackage()) : null;

        if (shortcutInfo.getShortLabel() != null) {
            if (!TextUtils.isEmpty(appName)) {
                record.displayName = appName + ": " + shortcutInfo.getShortLabel().toString();
            } else {
                record.displayName = shortcutInfo.getShortLabel().toString();
            }
        } else if (shortcutInfo.getLongLabel() != null) {
            if (!TextUtils.isEmpty(appName)) {
                record.displayName = appName + ": " + shortcutInfo.getLongLabel().toString();
            } else {
                record.displayName = shortcutInfo.getLongLabel().toString();
            }
        } else {
            Log.d(TAG, "Invalid shortcut for " + record.packageName + ", ignoring");
            return null;
        }

        return record;
    }

    /**
     * @return App name from package name
     */
    @NonNull
    public static String getAppNameFromPackageName(Context context, String packageName) {
        try {
            PackageManager packageManager = context.getPackageManager();
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            return packageManager.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException ignored) {
            return "";
        }
    }

    @NonNull
    public static byte[] getIconBlob(@NonNull Drawable iconDrawable) {
        Bitmap icon = Utilities.drawableToBitmap(iconDrawable);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        // Can't user WEBP compression because from API v18 to v21 there is no alpha encoding
        // see: https://stackoverflow.com/questions/38753798/android-webp-encoding-in-api-v18-and-above-bitmap-compressbitmap-compressforma
        icon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Removes the given shortcut from the current list of pinned shortcuts.
     * (Should run on background thread)
     */
    @WorkerThread
    public static void removeShortcut(@NonNull Context context, @NonNull ShortcutInfo shortcutInfo) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
            assert launcherApps != null;
            String packageName = shortcutInfo.getPackage();
            String id = shortcutInfo.getId();
            UserHandle user = shortcutInfo.getUserHandle();

            // query for pinned shortcuts
            List<String> shortcutIds;
            {
                LauncherApps.ShortcutQuery q = new LauncherApps.ShortcutQuery();
                q.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED);
                List<ShortcutInfo> shortcutInfos = launcherApps.getShortcuts(q, Process.myUserHandle());
                if (shortcutInfos == null)
                    shortcutInfos = Collections.emptyList();
                shortcutIds = new ArrayList<>(shortcutInfos.size());
                for (ShortcutInfo info : shortcutInfos)
                    shortcutIds.add(info.getId());
            }

            // unpin the shortcut
            shortcutIds.remove(id);
            try {
                launcherApps.pinShortcuts(packageName, shortcutIds, user);
            } catch (SecurityException | IllegalStateException e) {
                Log.w(TAG, "Failed to unpin shortcut", e);
            }
        }
    }
}
