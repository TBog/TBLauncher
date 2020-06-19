package rocks.tbog.tblauncher.result;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.utils.Utilities;

public class ResultHelper {
    private ResultHelper() {
        // this is a static class
    }

    /**
     * How to launch a result. Most probably, will fire an intent.
     * This function will record history and then call EntryItem.doLaunch
     *
     * @param pojo the {@link EntryItem} that the user is launching
     * @param v    {@link View} that was touched
     */
    static void launch(@NonNull EntryItem pojo, @NonNull View v) {
        TBApplication.behaviour(v.getContext()).beforeLaunchOccurred();

        Log.i("log", "Launching " + pojo.id);

        recordLaunch(pojo, v.getContext());

        // Launch
        v.postDelayed(() -> {
            pojo.doLaunch(v);
            TBApplication.behaviour(v.getContext()).afterLaunchOccurred();
        }, Behaviour.LAUNCH_DELAY);
    }

    /**
     * Put this item in application history
     *
     * @param pojo    the {@link EntryItem} that the user is launching
     * @param context android context
     */
    public static void recordLaunch(@NonNull EntryItem pojo, @NonNull Context context) {
        // Save in history
        TBApplication.getApplication(context).getDataHandler().addToHistory(pojo.getHistoryId());
    }

    /**
     * Remove the current result from the list
     *
     * @param context android context
     */
    public static void removeFromResultsAndHistory(@NonNull EntryItem pojo, @NonNull Context context) {
        removeFromHistory(pojo, context);
        Toast.makeText(context, R.string.removed_item, Toast.LENGTH_SHORT).show();
        //parent.removeResult(context, this);
        //TODO: parent should be ISearchActivity
        TBApplication.behaviour(context).removeResult(pojo);
    }

    private static void removeFromHistory(@NonNull EntryItem pojo, @NonNull Context context) {
        DBHelper.removeFromHistory(context, pojo.id);
    }

    public static void launchAddToFavorites(@NonNull Context context, EntryItem pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_added);
        TBApplication.getApplication(context).getDataHandler().addToFavorites(pojo);
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    public static void launchRemoveFromFavorites(@NonNull Context context, EntryItem pojo) {
        String msg = context.getResources().getString(R.string.toast_favorites_removed);
        TBApplication.getApplication(context).getDataHandler().removeFromFavorites(pojo);
        Toast.makeText(context, String.format(msg, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    public static void launchMessaging(ContactEntry contactPojo, View v) {
        Context context = v.getContext();
        TBApplication.behaviour(context).beforeLaunchOccurred();

        String url = "sms:" + Uri.encode(contactPojo.phone);
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        Utilities.setIntentSourceBounds(i, v);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        TBApplication.behaviour(context).afterLaunchOccurred();
    }

    public static void launchContactView(ContactEntry contactPojo, Context context, View v) {
        TBApplication.behaviour(context).beforeLaunchOccurred();

        Intent action = new Intent(Intent.ACTION_VIEW);

        action.setData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI,
                String.valueOf(contactPojo.lookupKey)));
        Utilities.setIntentSourceBounds(action, v);

        action.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        action.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(action);

        TBApplication.behaviour(context).afterLaunchOccurred();
    }

    public static void launchCall(Context context, View v, String phone) {
        TBApplication.behaviour(context).beforeLaunchOccurred();

        Intent phoneIntent = new Intent(Intent.ACTION_CALL);
        phoneIntent.setData(Uri.parse("tel:" + Uri.encode(phone)));
        Utilities.setIntentSourceBounds(phoneIntent, v);

        phoneIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        // Make sure we have permission to call someone as this is considered a dangerous permission
        if (!Permission.checkPermission(context, Permission.PERMISSION_CALL_PHONE)) {
            Permission.askPermission(Permission.PERMISSION_CALL_PHONE, new Permission.PermissionResultListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onGranted() {
                    // Great! Start the intent we stored for later use.
                    context.startActivity(phoneIntent);
                }

                @Override
                public void onDenied() {
                    Toast.makeText(context, R.string.permission_denied, Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        // Pre-android 23, or we already have permission
        context.startActivity(phoneIntent);

        TBApplication.behaviour(context).afterLaunchOccurred();
    }
}
