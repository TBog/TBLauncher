package rocks.tbog.tblauncher.utils;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.UserManager;

import androidx.annotation.NonNull;


/**
 * Wrapper class for `android.os.UserHandle` that works with all Android versions
 */
public class UserHandleCompat {
    public static final UserHandleCompat CURRENT_USER = new UserHandleCompat();
    private final long serial;
    private final Object handle; // android.os.UserHandle on Android 4.2 and newer

    public UserHandleCompat() {
        this(0, null);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public UserHandleCompat(long serial, android.os.UserHandle user) {
        if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
            // OS does not provide any APIs for multi-user support
            this.serial = 0;
            this.handle = null;
        } else if (user != null && Process.myUserHandle().equals(user)) {
            // For easier processing the current user is also stored as `null`, even
            // if there is multi-user support
            this.serial = 0;
            this.handle = null;
        } else {
            // Store the given user handle
            this.serial = serial;
            this.handle = user;
        }
    }

    public UserHandleCompat(Context context, android.os.UserHandle userHandle) {
        final UserManager manager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        assert manager != null;
        serial = manager.getSerialNumberForUser(userHandle);
        handle = userHandle;
    }

    public static UserHandleCompat fromComponentName(Context ctx, String componentName) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            UserManager manager = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
            assert manager != null;
            long serial = getUserSerial(componentName);
            android.os.UserHandle handle = manager.getUserForSerialNumber(serial);
            return new UserHandleCompat(serial, handle);
        }
        return UserHandleCompat.CURRENT_USER;
    }

    @NonNull
    public static ComponentName unflattenComponentName(@NonNull String name) {
        return new ComponentName(getPackageName(name), getActivityName(name));
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public android.os.UserHandle getRealHandle() {
        if (this.handle != null) {
            return (android.os.UserHandle) this.handle;
        } else {
            return Process.myUserHandle();
        }
    }


    public boolean isCurrentUser() {
        return (this.handle == null);
    }


    private String addUserSuffixToString(String base, char separator) {
        if (this.handle == null) {
            return base;
        } else {
            return base + separator + this.serial;
        }
    }

    @SuppressWarnings("CatchAndPrintStackTrace")
    public boolean hasStringUserSuffix(String string, char separator) {
        long serial = 0;

        int index = string.lastIndexOf((int) separator);
        if (index > -1) {
            String serialText = string.substring(index);
            try {
                serial = Long.parseLong(serialText);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

        return (serial == this.serial);
    }

    public String getUserComponentName(ComponentName component) {
        return addUserSuffixToString(component.getPackageName() + "/" + component.getClassName(), '#');
    }

    public String getUserComponentName(String packageName, String activityName) {
        return addUserSuffixToString(packageName + "/" + activityName, '#');
    }

    public static String getPackageName(@NonNull String componentName) {
        int index = componentName.indexOf('/');
        if (index > 0)
            return componentName.substring(0, index);
        return "";
    }

    public static String getActivityName(@NonNull String componentName) {
        int start = componentName.indexOf('/') + 1;
        int end = componentName.lastIndexOf('#');
        if (end == -1)
            end = componentName.length();
        if (start > 0 && start < end) {
            return componentName.substring(start, end);
        }
        return "";
    }

    public static long getUserSerial(@NonNull String componentName) {
        int index = componentName.indexOf('#') + 1;
        if (index > 0 && index < componentName.length()) {
            try {
                return Long.parseLong(componentName.substring(index));
            } catch (NumberFormatException ignored) {
            }
        }
        return 0;
    }

    public String getBadgedLabelForUser(Context context, String label) {
        if (handle == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return label;
        return context.getPackageManager().getUserBadgedLabel(label, (android.os.UserHandle) handle).toString();
    }
}
