package rocks.tbog.tblauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.result.AppResult;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

/**
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

public class IconsHandler {

    private static final String TAG = "IconsHandler";
    // map with available icons packs
    private final HashMap<String, String> mIconPackNames = new HashMap<>();
    private final Context ctx;

    private IconPackXML mIconPack = null;
    private SystemIconPack mSystemPack = new SystemIconPack();


    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;

        loadAvailableIconsPacks();
        loadIconsPack();
    }

    /**
     * Load configured icons pack
     */
    private void loadIconsPack() {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        loadIconsPack(prefs.getString("icons-pack", "default"));

    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    void loadIconsPack(@NonNull String packageName) {

        //clear icons pack
        mIconPack = null;
        cacheClear();

        // system icons, nothing to do
        if (packageName.equalsIgnoreCase("default")) {
            return;
        }

        mIconPack = new IconPackXML(packageName);
        mIconPack.load(ctx.getPackageManager());
    }

    /**
     * Get or generate icon for an app
     */
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandleCompat userHandle) {
        // system icons, nothing to do
        if (mIconPack == null) {
            Drawable drawable = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return DrawableUtils.applyIconMaskShape(ctx, drawable);
            }
            return drawable;
        }

        String componentString = componentName.toString();

        // Search first in cache
        {
            Drawable cacheIcon = cacheGetDrawable(componentString);
            if (cacheIcon != null)
                return cacheIcon;
        }

        // check the icon pack for a resource
        {
            Drawable drawable = mIconPack.getComponentDrawable(componentString);
            if (drawable != null)
                return drawable;
        }

        // apply icon pack back, mask and front over the system drawable
        Drawable systemIcon = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
        if (systemIcon == null)
            return null;
        BitmapDrawable generated = mIconPack.applyBackgroundAndMask(ctx, systemIcon);
        storeDrawable(cacheGetFileName(componentName.toString()), generated);
        return generated;
    }

    /**
     * Scan for installed icons packs
     */
    private void loadAvailableIconsPacks() {
        PackageManager pm = ctx.getPackageManager();

        List<ResolveInfo> launcherThemes = pm.queryIntentActivities(new Intent("org.adw.launcher.THEMES"), PackageManager.GET_META_DATA);

        for (ResolveInfo ri : launcherThemes) {
            String packageName = ri.activityInfo.packageName;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                String name = pm.getApplicationLabel(ai).toString();
                mIconPackNames.put(packageName, name);
            } catch (NameNotFoundException e) {
                // shouldn't happen
                Log.e(TAG, "Unable to find package " + packageName, e);
            }
        }
    }

    HashMap<String, String> getIconPackNames() {
        return mIconPackNames;
    }

    @Nullable
    public IconPack getCurrentIconPack() {
        return mIconPack;
    }

    @NonNull
    public IconPack getSystemIconPack() {
        return mSystemPack;
    }

    private boolean isDrawableInCache(String key) {
        File drawableFile = cacheGetFileName(key);
        return drawableFile.isFile();
    }

    private void storeDrawable(@NonNull File drawableFile, Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(drawableFile);
                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (Exception e) {
                Log.e(TAG, "Unable to store drawable as " + drawableFile, e);
            }
        } else {
            Log.w(TAG, "Only BitmapDrawable can be stored! " + drawableFile);
        }
    }

    private void removeStoredDrawable(@NonNull File drawableFile) {
        try {
            //noinspection ResultOfMethodCallIgnored
            drawableFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "stored drawable " + drawableFile + " can't be deleted!", e);
        }
    }

    private Drawable cacheGetDrawable(String key) {

        if (!isDrawableInCache(key)) {
            return null;
        }

        FileInputStream fis;
        try {
            fis = new FileInputStream(cacheGetFileName(key));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get drawable from cache " + e);
        }

        return null;
    }

    /**
     * create path for icons cache like this
     * {cacheDir}/icons/{icons_pack_package_name}_{componentName_hash}.png
     */
    private File cacheGetFileName(String key) {
        String iconsPackPackageName = mIconPack != null ? mIconPack.getPackPackageName() : "";
        return new File(getIconsCacheDir(), iconsPackPackageName + "_" + key.hashCode() + ".png");
    }

    private File getIconsCacheDir() {
        File dir = new File(this.ctx.getCacheDir(), "icons");
        if (!dir.exists() && !dir.mkdir())
            throw new IllegalStateException("failed to create path " + dir.getPath());
        return dir;
    }

    /**
     * create path for custom icons like this
     * {cacheDir}/custom_icons/{DB row id}_{componentName_hash}.png
     */
    private File customIconFileName(String componentName, long customIcon) {
        StringBuilder name = new StringBuilder();
        if (customIcon > 0) {
            name.append(customIcon).append('_');
        }
        name.append(componentName.hashCode()).append(".png");
        return new File(getCustomIconsDir(), name.toString());
    }

    private File getCustomIconsDir() {
        File dir = new File(this.ctx.getCacheDir(), "custom_icons");
        if (!dir.exists() && !dir.mkdir())
            throw new IllegalStateException("failed to create path " + dir.getPath());
        return dir;
    }

    /**
     * Clear cache
     */
    private void cacheClear() {
        File cacheDir = this.getIconsCacheDir();

        if (!cacheDir.isDirectory())
            return;

        File[] fileList = cacheDir.listFiles();
        if (fileList != null) {
            for (File item : fileList) {
                if (!item.delete()) {
                    Log.w(TAG, "Failed to delete file: " + item.getAbsolutePath());
                }
            }
        }
    }

    public Drawable getCustomIcon(String componentName, long customIcon) {
        try {
            FileInputStream fis = new FileInputStream(customIconFileName(componentName, customIcon));
            BitmapDrawable drawable =
                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
            fis.close();
            return drawable;
        } catch (Exception e) {
            Log.e(TAG, "Unable to get custom icon " + e);
        }

        return null;
    }

    public void changeAppIcon(AppResult appResult, Drawable drawable) {
        AppRecord appRecord = TBApplication.getApplication(ctx).getDataHandler().setCustomAppIcon(appResult.getUserComponentName());
        storeDrawable(customIconFileName(appRecord.componentName, appRecord.dbId), drawable);
        appResult.setCustomIcon(appRecord.dbId, drawable);
    }

    public void restoreAppIcon(AppResult appResult) {
        AppRecord appRecord = TBApplication.getApplication(ctx).getDataHandler().removeCustomAppIcon(appResult.getUserComponentName());
        removeStoredDrawable(customIconFileName(appRecord.componentName, appRecord.dbId));
        appResult.clearCustomIcon();
    }
}
