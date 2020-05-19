package rocks.tbog.tblauncher.entry;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.List;

import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.IconsHandler;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.utils.UserHandleCompat;

public final class AppEntry extends EntryWithTags {

    public static final String SCHEME = "app://";
    @NonNull
    public final ComponentName componentName;
    @NonNull
    private final UserHandleCompat userHandle;

    private long customIcon = 0;
    private boolean excluded = false;
    private boolean excludedFromHistory = false;

    public AppEntry(@NonNull String id, @NonNull String packageName, @NonNull String className, @NonNull UserHandleCompat userHandle) {
        super(id);
        if (BuildConfig.DEBUG && !id.startsWith(SCHEME)) {
            throw new IllegalStateException("Invalid " + AppEntry.class.getSimpleName() + " id `" + id + "`");
        }
        this.componentName = new ComponentName(packageName, className);
        this.userHandle = userHandle;
    }

    public String getUserComponentName() {
        return userHandle.getUserComponentName(componentName);
    }

    public String getPackageName() {
        return componentName.getPackageName();
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean excluded) {
        this.excluded = excluded;
    }

    public boolean isExcludedFromHistory() {
        return excludedFromHistory;
    }

    public void setExcludedFromHistory(boolean excludedFromHistory) {
        this.excludedFromHistory = excludedFromHistory;
    }

    public boolean canUninstall() {
        return userHandle.isCurrentUser();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public List<LauncherActivityInfo> getActivityList(LauncherApps launcher) {
        return launcher.getActivityList(componentName.getPackageName(), userHandle.getRealHandle());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void launchAppDetails(LauncherApps launcher) {
        launcher.startAppDetailsActivity(componentName, userHandle.getRealHandle(), null, null);
    }

    public Drawable getIconDrawable(Context context) {
        IconsHandler iconsHandler = TBApplication.getApplication(context).getIconsHandler();
        if (customIcon != 0) {
            Drawable drawable = iconsHandler.getCustomIcon(getUserComponentName(), customIcon);
            if (drawable != null)
                return drawable;
        }
        return iconsHandler.getDrawableIconForPackage(componentName, userHandle);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        return userHandle.getRealHandle();
    }

    public void setCustomIcon(long dbId) {
        customIcon = dbId;
    }

    public long getCustomIcon() {
        return customIcon;
    }

}
