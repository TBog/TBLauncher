package rocks.tbog.tblauncher;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import rocks.tbog.tblauncher.utils.DrawableUtils;
import rocks.tbog.tblauncher.utils.UserHandle;

public class SystemIconPack implements IIconPack {

    private static final String TAG = SystemIconPack.class.getSimpleName();

    @Override
    public String getPackPackageName() {
        return "default";
    }

    @Override
    public void load(PackageManager packageManager) {
    }

    @Nullable
    @Override
    public Drawable getComponentNameDrawable(String componentName) {
        return null;
    }

    @NonNull
    public Drawable getDefaultAppDrawable(@NonNull Context ctx, @NonNull ComponentName componentName, @NonNull UserHandle userHandle) {
        Drawable drawable = null;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                LauncherApps launcher = (LauncherApps) ctx.getSystemService(Context.LAUNCHER_APPS_SERVICE);
                List<LauncherActivityInfo> icons = launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
                for (LauncherActivityInfo info : icons) {
                    if (info.getComponentName().equals(componentName)) {
                        drawable = info.getBadgedIcon(0);
                        break;
                    }
                }

                // This should never happen, let's just return the first icon
                if (drawable == null)
                    drawable = icons.get(0).getBadgedIcon(0);
            } else {
                drawable = ctx.getPackageManager().getActivityIcon(componentName);
            }
        } catch (PackageManager.NameNotFoundException | IndexOutOfBoundsException e) {
            Log.e(TAG, "Unable to find component " + componentName.toString() + e);
            return new ColorDrawable(Color.WHITE);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return DrawableUtils.applyIconMaskShape(ctx, drawable);
        }
        return drawable;
    }
}
