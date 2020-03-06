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

import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.utils.UserHandle;

public final class AppEntry extends EntryWithTags {

    @NonNull
    public final String packageName;
    @NonNull
    public final String activityName;
    @NonNull
    private final UserHandle userHandle;

    private boolean excluded;
    private boolean excludedFromHistory;

    public AppEntry(String id, @NonNull String packageName, @NonNull String activityName, @NonNull UserHandle userHandle,
                    boolean isExcluded, boolean isExcludedFromHistory) {
        super(id);

        this.packageName = packageName;
        this.activityName = activityName;
        this.userHandle = userHandle;

        this.excluded = isExcluded;
        this.excludedFromHistory = isExcludedFromHistory;
    }

    public String getComponentName() {
        return userHandle.getComponentName(packageName, activityName);
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
        return launcher.getActivityList(packageName, userHandle.getRealHandle());
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void launchAppDetails(LauncherApps launcher) {
        launcher.startAppDetailsActivity(new ComponentName(packageName, activityName), userHandle.getRealHandle(), null, null);
    }

    public Drawable getIconDrawable(Context context) {
        return TBApplication.getApplication(context).getIconsHandler()
                .getDrawableIconForPackage(new ComponentName(packageName, activityName), userHandle);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        return userHandle.getRealHandle();
    }
}
