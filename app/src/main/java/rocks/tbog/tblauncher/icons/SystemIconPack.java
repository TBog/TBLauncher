package rocks.tbog.tblauncher.icons;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.utils.GoogleCalendarIcon;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public class SystemIconPack implements IconPack<Void> {

    private static final String TAG = SystemIconPack.class.getSimpleName();
    private int mAdaptiveShape = DrawableUtils.SHAPE_NONE;

    @NonNull
    @Override
    public String getPackPackageName() {
        return "default";
    }

    @Override
    public boolean isLoaded() {
        return true;
    }

    @Override
    public void load(PackageManager packageManager) {
    }

    public int getAdaptiveShape() {
        return mAdaptiveShape;
    }

    public void setAdaptiveShape(int shape) {
        mAdaptiveShape = shape;
    }

    @Nullable
    @Override
    public Drawable getComponentDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandleCompat userHandle) {
        Drawable drawable = null;
        if (isComponentDynamic(componentName)) {
            drawable = GoogleCalendarIcon.getDrawable(ctx, componentName.getClassName());
        }
        if (drawable == null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                assert launcher != null;
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        drawable = info.getBadgedIcon(0);
                        break;
                    }
                }

                // This should never happen, let's just return the first icon
                if (drawable == null && !icons.isEmpty())
                    drawable = icons.get(0).getBadgedIcon(0);
            }
        }

        if (drawable == null) {
            try {
                drawable = ctx.getPackageManager().getActivityIcon(componentName);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to find activity icon " + componentName.toString(), e);
            }
        }

        if (drawable == null) {
            try {
                drawable = ctx.getPackageManager().getApplicationIcon(componentName.getPackageName());
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Unable to find app icon " + componentName.toString(), e);
            }
        }

        if (drawable == null)
            Log.e(TAG, "Unable to find component drawable " + componentName.toString());

        return drawable;
    }

    @Override
    public boolean isComponentDynamic(@NonNull ComponentName componentName) {
        return GoogleCalendarIcon.GOOGLE_CALENDAR.equals(componentName.getPackageName());
    }

    @NonNull
    @Override
    public Drawable applyBackgroundAndMask(@NonNull Context ctx, @NonNull Drawable icon, boolean fitInside) {
        return DrawableUtils.applyIconMaskShape(ctx, icon, mAdaptiveShape, fitInside);
    }

    @NonNull
    @Override
    public Collection<Void> getDrawableList() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Drawable getDrawable(@NonNull Void aVoid) {
        return null;
    }
}
