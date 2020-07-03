package rocks.tbog.tblauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UserHandleCompat;
import rocks.tbog.tblauncher.utils.Utilities;

/**
 * Inspired from http://stackoverflow.com/questions/31490630/how-to-load-icon-from-icon-pack
 */

public class IconsHandler {

    private static final String TAG = "IconsHandler";
    // map with available icons packs
    private final HashMap<String, String> mIconPackNames = new HashMap<>();
    private final Context ctx;

    private int mContactsShape = DrawableUtils.SHAPE_SYSTEM;
    private int mShortcutsShape = DrawableUtils.SHAPE_SYSTEM;
    private IconPackXML mIconPack = null;
    private SystemIconPack mSystemPack = new SystemIconPack();
    private boolean mForceAdaptive;
    private boolean mForceShape;
    private boolean mContactPackMask;
    private boolean mShortcutPackMask;
    private boolean mShortcutBadgePackMask;

    public IconsHandler(Context ctx) {
        super();
        this.ctx = ctx;

        loadAvailableIconsPacks();
        onPrefChanged(PreferenceManager.getDefaultSharedPreferences(ctx));
    }

    /**
     * Set values from preferences
     */
    public void onPrefChanged(SharedPreferences pref) {
        loadIconsPack(pref.getString("icons-pack", "default"));
        mSystemPack.setAdaptiveShape(getAdaptiveShape(pref, "adaptive-shape"));
        mForceAdaptive = pref.getBoolean("force-adaptive", true);
        mForceShape = pref.getBoolean("force-shape", true);

        mContactPackMask = pref.getBoolean("contact-pack-mask", true);
        mContactsShape = getAdaptiveShape(pref, "contacts-shape");

        mShortcutPackMask = pref.getBoolean("shortcut-pack-mask", true);
        mShortcutsShape = getAdaptiveShape(pref, "shortcut-shape");

        mShortcutBadgePackMask = pref.getBoolean("shortcut-pack-badge-mask", true);
    }

    private static int getAdaptiveShape(SharedPreferences pref, String key) {
        try {
            return Integer.parseInt(pref.getString(key, null));
        } catch (Exception ignored) {
        }
        return DrawableUtils.SHAPE_SYSTEM;
    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    private void loadIconsPack(@NonNull String packageName) {

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
    @WorkerThread
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandleCompat userHandle) {
        // check the icon pack for a resource
        if (mIconPack != null) {
            String componentString = componentName.toString();
            Drawable drawable = mIconPack.getComponentDrawable(componentString);
            if (drawable != null) {
                if (DrawableUtils.isAdaptiveIconDrawable(drawable) || mForceAdaptive) {
                    int shape = mSystemPack.getAdaptiveShape();
                    return DrawableUtils.applyIconMaskShape(ctx, drawable, shape, true);
                } else
                    return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
            }
        }

        // if icon pack doesn't have the drawable, use system drawable
        Drawable systemIcon = mSystemPack.getComponentDrawable(ctx, componentName, userHandle);
        if (systemIcon == null)
            return null;

        // if the icon pack has a mask, use that instead of the adaptive shape
        if (mIconPack != null && mIconPack.hasMask())
            return mIconPack.applyBackgroundAndMask(ctx, systemIcon, false);

        // use adaptive shape
        if (DrawableUtils.isAdaptiveIconDrawable(systemIcon) || mForceAdaptive)
            return mSystemPack.applyBackgroundAndMask(ctx, systemIcon, true);
        else if (mForceShape)
            return mSystemPack.applyBackgroundAndMask(ctx, systemIcon, false);
        else
            return systemIcon;
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
    public IconPackXML getCustomIconPack() {
        return mIconPack;
    }

    @NonNull
    public SystemIconPack getSystemIconPack() {
        return mSystemPack;
    }

    @NonNull
    public IconPack<?> getIconPack() {
        return mIconPack != null ? mIconPack : mSystemPack;
    }

//    private boolean isDrawableInCache(String key) {
//        File drawableFile = cacheGetFileName(key);
//        return drawableFile.isFile();
//    }

//    private void storeDrawable(@NonNull File drawableFile, Drawable drawable) {
//        if (drawable instanceof BitmapDrawable) {
//            FileOutputStream fos;
//            try {
//                fos = new FileOutputStream(drawableFile);
//                ((BitmapDrawable) drawable).getBitmap().compress(CompressFormat.PNG, 100, fos);
//                fos.flush();
//                fos.close();
//            } catch (Exception e) {
//                Log.e(TAG, "Unable to store drawable as " + drawableFile, e);
//            }
//        } else {
//            Log.w(TAG, "Only BitmapDrawable can be stored! " + drawableFile);
//        }
//    }

    private void removeStoredDrawable(@NonNull File drawableFile) {
        try {
            //noinspection ResultOfMethodCallIgnored
            drawableFile.delete();
        } catch (Exception e) {
            Log.e(TAG, "stored drawable " + drawableFile + " can't be deleted!", e);
        }
    }

//    private Drawable cacheGetDrawable(String key) {
//
//        if (!isDrawableInCache(key)) {
//            return null;
//        }
//
//        FileInputStream fis;
//        try {
//            fis = new FileInputStream(cacheGetFileName(key));
//            BitmapDrawable drawable =
//                    new BitmapDrawable(this.ctx.getResources(), BitmapFactory.decodeStream(fis));
//            fis.close();
//            return drawable;
//        } catch (Exception e) {
//            Log.e(TAG, "Unable to get drawable from cache " + e);
//        }
//
//        return null;
//    }

    //    /**
//     * create path for icons cache like this
//     * {cacheDir}/icons/{icons_pack_package_name}_{componentName_hash}.png
//     */
//    private File cacheGetFileName(String key) {
//        String iconsPackPackageName = mIconPack != null ? mIconPack.getPackPackageName() : "";
//        return new File(getIconsCacheDir(), iconsPackPackageName + "_" + key.hashCode() + ".png");
//    }
//
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
                    Log.w(TAG, "Failed to delete cacheIcon: " + item.getAbsolutePath());
                }
            }
        }

        if (!cacheDir.delete())
            Log.w(TAG, "Failed to delete cacheDir: " + cacheDir.getAbsolutePath());
    }

    public Drawable getCustomIcon(String componentName, long customIcon) {
        Bitmap bitmap = TBApplication.dataHandler(ctx).getCustomAppIcon(componentName);
        if (bitmap != null)
            return new BitmapDrawable(ctx.getResources(), bitmap);

        Log.e(TAG, "Unable to get custom icon for " + componentName);
        return null;
    }

    public void changeAppIcon(AppEntry appEntry, Drawable drawable) {
        Bitmap bitmap = Utilities.drawableToBitmap(drawable);
        TBApplication app = TBApplication.getApplication(ctx);
        AppRecord appRecord = app.getDataHandler().setCustomAppIcon(appEntry.getUserComponentName(), bitmap);
        //storeDrawable(customIconFileName(appRecord.componentName, appRecord.dbId), drawable);
        appEntry.setCustomIcon(appRecord.dbId);
        app.getDrawableCache().cacheDrawable(appEntry.id, drawable);
    }

    public void restoreAppIcon(AppEntry appEntry) {
        TBApplication app = TBApplication.getApplication(ctx);
        AppRecord appRecord = app.getDataHandler().removeCustomAppIcon(appEntry.getUserComponentName());
        removeStoredDrawable(customIconFileName(appRecord.componentName, appRecord.dbId));
        appEntry.clearCustomIcon();
        app.getDrawableCache().cacheDrawable(appEntry.id, null);
    }

    public Drawable applyContactMask(@NonNull Context ctx, @NonNull Drawable drawable) {
        if (mIconPack != null && mIconPack.hasMask()) {
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        }
        Drawable output = mSystemPack.applyBackgroundAndMask(ctx, drawable, false);

        // if nothing changed then make it a circle
        if (output == drawable) {
            int size = ctx.getResources().getDimensionPixelSize(R.dimen.icon_height);
            Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            Path path = new Path();
            int h = size / 2;
            path.addCircle(h, h, h, Path.Direction.CCW);
            c.clipPath(path);
            drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
            drawable.draw(c);
            output = new BitmapDrawable(ctx.getResources(), b);
        }
        return output;
    }

    @NonNull
    public Drawable getDefaultActivityIcon(Context context) {
        Resources resources = context.getResources();

        int iconId = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                ? android.R.drawable.sym_def_app_icon
                : android.R.mipmap.sym_def_app_icon;

        Drawable d = null;
        try {
            d = ResourcesCompat.getDrawable(resources, iconId, context.getTheme());
        } catch (Resources.NotFoundException ignored) {
        }

        return (d == null) ? new ColorDrawable(UIColors.getPrimaryColor(context)) : d;
    }
}
