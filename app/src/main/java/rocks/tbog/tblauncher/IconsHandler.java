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

import java.util.HashMap;
import java.util.List;

import rocks.tbog.tblauncher.WorkAsync.RunnableTask;
import rocks.tbog.tblauncher.db.AppRecord;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.drawable.TextDrawable;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.icons.IconPack;
import rocks.tbog.tblauncher.icons.IconPackXML;
import rocks.tbog.tblauncher.icons.SystemIconPack;
import rocks.tbog.tblauncher.utils.Timer;
import rocks.tbog.tblauncher.utils.UIColors;
import rocks.tbog.tblauncher.utils.UISizes;
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

    private int mContactsShape = DrawableUtils.SHAPE_NONE;
    private int mShortcutsShape = DrawableUtils.SHAPE_NONE;
    private IconPackXML mIconPack = null;
    private SystemIconPack mSystemPack = new SystemIconPack();
    private boolean mForceAdaptive;
    private boolean mForceShape;
    private boolean mContactPackMask;
    private boolean mShortcutPackMask;
    private boolean mShortcutBadgePackMask;
    private RunnableTask mLoadIconsPackTask = null;

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
        loadIconsPack(pref.getString("icons-pack", null));
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
        return DrawableUtils.SHAPE_NONE;
    }

    /**
     * Parse icons pack metadata
     *
     * @param packageName Android package ID of the package to parse
     */
    private void loadIconsPack(@Nullable String packageName) {
        // system icons, nothing to do
        if (packageName == null || packageName.equalsIgnoreCase("default")) {
            mIconPack = null;
            return;
        }

        // don't reload the icon pack
        if (mIconPack == null
                || !mIconPack.getPackPackageName().equals(packageName)
                || (mLoadIconsPackTask == null && !mIconPack.isLoaded())) {
            if (mLoadIconsPackTask != null) {
                mLoadIconsPackTask.cancel();
                mLoadIconsPackTask = null;
            }
            final IconPackXML iconPack = TBApplication.iconPackCache(ctx).getIconPack(packageName);

            // timer start
            Timer timer = Timer.startMilli();

            Log.i(TAG, "[start] loading default icon pack: " + packageName);
            // set the current icon pack
            mIconPack = iconPack;
            // start async loading
            mLoadIconsPackTask = Utilities.runAsync((task) -> {
                if (task == mLoadIconsPackTask)
                    iconPack.load(ctx.getPackageManager());
            }, (task) -> {
                // timer end
                timer.stop();

                if (!task.isCancelled() && task == mLoadIconsPackTask) {
                    Log.i(TAG, "[end] loading default icon pack: " + packageName);

                    Log.i("time", timer + " to load icon pack " + packageName);
                    mLoadIconsPackTask = null;
                    TBApplication app = TBApplication.getApplication(ctx);
                    app.behaviour().refreshSearchRecords();
                    app.quickList().onFavoritesChanged();
                }
            });
        }
    }

    /**
     * Get or generate icon for an app
     */
    @WorkerThread
    public Drawable getDrawableIconForPackage(ComponentName componentName, UserHandleCompat userHandle) {
        // check the icon pack for a resource
        if (mIconPack != null) {
            // just checking will make this thread wait for the icon pack to load
            if (!mIconPack.isLoaded()) {
                if (mLoadIconsPackTask == null) {
                    Log.w(TAG, "icon pack `" + mIconPack.getPackPackageName() + "` not loaded, reload");
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(ctx);
                    loadIconsPack(pref.getString("icons-pack", null));
                } else {
                    Log.w(TAG, "icon pack `" + mIconPack.getPackPackageName() + "` not loaded, wait");
                    try {
                        mLoadIconsPackTask.wait();
                    } catch (InterruptedException ignored) {
                    }
                }
                return null;
            }
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
        if (mIconPack != null && mIconPack.hasMask() && !mForceShape)
            return mIconPack.applyBackgroundAndMask(ctx, systemIcon, false);

        boolean fitInside = mForceAdaptive || !mForceShape;
        return mSystemPack.applyBackgroundAndMask(ctx, systemIcon, fitInside);
    }

    /**
     * Get or generate icon to use as a badge for an app
     */
    @WorkerThread
    public Drawable getDrawableBadgeForPackage(ComponentName componentName, UserHandleCompat userHandle) {
        // check the icon pack for a resource
        if (mIconPack != null) {
            // just checking will make this thread wait for the icon pack to load
            if (!mIconPack.isLoaded())
                return null;
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
        if (mShortcutBadgePackMask && mIconPack != null && mIconPack.hasMask())
            return mIconPack.applyBackgroundAndMask(ctx, systemIcon, false);

        boolean fitInside = mForceAdaptive || !mForceShape;
        return mSystemPack.applyBackgroundAndMask(ctx, systemIcon, fitInside);
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

    public HashMap<String, String> getIconPackNames() {
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

    public Drawable getCustomIcon(StaticEntry staticEntry) {
        Bitmap bitmap = TBApplication.dataHandler(ctx).getCustomStaticEntryIcon(staticEntry);
        if (bitmap != null)
            return new BitmapDrawable(ctx.getResources(), bitmap);

        Log.e(TAG, "Unable to get custom icon for " + staticEntry.id);
        return null;
    }

    public Drawable getCustomIcon(ShortcutEntry shortcutEntry) {
        Bitmap bitmap = TBApplication.dataHandler(ctx).getCustomShortcutIcon(shortcutEntry);
        if (bitmap != null)
            return new BitmapDrawable(ctx.getResources(), bitmap);

        Log.e(TAG, "Unable to get custom icon for " + shortcutEntry.id);
        return null;
    }

    public Drawable getCustomIcon(String componentName, long customIcon) {
        Bitmap bitmap = TBApplication.dataHandler(ctx).getCustomAppIcon(componentName);
        if (bitmap != null)
            return new BitmapDrawable(ctx.getResources(), bitmap);

        Log.e(TAG, "Unable to get custom icon for " + componentName);
        return null;
    }

    private static Bitmap getIconBitmap(Context ctx, Drawable drawable) {
        if (drawable instanceof TextDrawable) {
            int size = UISizes.getResultIconSize(ctx);
            return DrawableUtils.drawableToBitmap(drawable, size, size);
        }
        return Utilities.drawableToBitmap(drawable);
    }

    public void changeIcon(AppEntry appEntry, Drawable drawable) {
        Bitmap bitmap = getIconBitmap(ctx, drawable);
        TBApplication app = TBApplication.getApplication(ctx);
        AppRecord appRecord = app.getDataHandler().setCustomAppIcon(appEntry.getUserComponentName(), bitmap);
        //storeDrawable(customIconFileName(appRecord.componentName, appRecord.dbId), drawable);
        appEntry.setCustomIcon(appRecord.dbId);
        app.drawableCache().cacheDrawable(appEntry.id, drawable);
    }

    public void changeIcon(ShortcutEntry shortcutEntry, Drawable drawable) {
        Bitmap bitmap = getIconBitmap(ctx, drawable);
        TBApplication app = TBApplication.getApplication(ctx);
        app.getDataHandler().setCustomStaticEntryIcon(shortcutEntry.id, bitmap);
        shortcutEntry.setCustomIcon();
        app.drawableCache().cacheDrawable(shortcutEntry.id, drawable);
    }

    public void changeIcon(StaticEntry staticEntry, Drawable drawable) {
        Bitmap bitmap = getIconBitmap(ctx, drawable);
        TBApplication app = TBApplication.getApplication(ctx);
        app.getDataHandler().setCustomStaticEntryIcon(staticEntry.id, bitmap);
        staticEntry.setCustomIcon();
        app.drawableCache().cacheDrawable(staticEntry.id, drawable);
    }

    public void restoreDefaultIcon(AppEntry appEntry) {
        TBApplication app = TBApplication.getApplication(ctx);
        AppRecord appRecord = app.getDataHandler().removeCustomAppIcon(appEntry.getUserComponentName());
        appEntry.clearCustomIcon();
        app.drawableCache().cacheDrawable(appEntry.id, null);
    }

    public void restoreDefaultIcon(ShortcutEntry shortcutEntry) {
        TBApplication app = TBApplication.getApplication(ctx);
        app.getDataHandler().removeCustomStaticEntryIcon(shortcutEntry.id);
        shortcutEntry.clearCustomIcon();
        app.drawableCache().cacheDrawable(shortcutEntry.id, null);
    }

    public void restoreDefaultIcon(StaticEntry staticEntry) {
        TBApplication app = TBApplication.getApplication(ctx);
        app.getDataHandler().removeCustomStaticEntryIcon(staticEntry.id);
        staticEntry.clearCustomIcon();
        app.drawableCache().cacheDrawable(staticEntry.id, null);
    }

    public Drawable applyContactMask(@NonNull Context ctx, @NonNull Drawable drawable) {
        if (!mContactPackMask)
            return DrawableUtils.applyIconMaskShape(ctx, drawable, mContactsShape, false);
        if (mIconPack != null && mIconPack.hasMask())
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        // if pack has no mask, make it a circle
        int size = ctx.getResources().getDimensionPixelSize(R.dimen.icon_size);
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Path path = new Path();
        int h = size / 2;
        path.addCircle(h, h, h, Path.Direction.CCW);
        c.clipPath(path);
        drawable.setBounds(0, 0, c.getWidth(), c.getHeight());
        drawable.draw(c);
        return new BitmapDrawable(ctx.getResources(), b);
    }

    public Drawable applyShortcutMask(@NonNull Context ctx, Bitmap bitmap) {
        Drawable drawable = new BitmapDrawable(ctx.getResources(), bitmap);
        if (!mShortcutPackMask)
            return DrawableUtils.applyIconMaskShape(ctx, drawable, mShortcutsShape, true);
        if (mIconPack != null && mIconPack.hasMask())
            return mIconPack.applyBackgroundAndMask(ctx, drawable, false);
        return drawable;
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

        return (d == null) ? new ColorDrawable(UIColors.getDefaultColor(context)) : d;
    }
}
