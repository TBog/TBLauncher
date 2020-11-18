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
import rocks.tbog.tblauncher.BuildConfig;
import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.FilterEntry;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.utils.Utilities;

public class ResultHelper {
    private ResultHelper() {
        // this is a static class
    }

    /**
     * How to launch a result. Most probably, will fire an intent.
     * This function will record history and then call EntryItem.doLaunch
     *
     * @param view    {@link View} that was touched
     * @param pojo the {@link EntryItem} that the user is launching
     */
    static void launch(@NonNull View view, @NonNull EntryItem pojo) {
        TBApplication.behaviour(view.getContext()).beforeLaunchOccurred();

        Log.i("log", "Launching " + pojo.id);

        recordLaunch(pojo, view.getContext());

        // Launch
        view.postDelayed(() -> {
            pojo.doLaunch(view);
            TBApplication.behaviour(view.getContext()).afterLaunchOccurred();
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
        //TODO: remove from results only if we are showing history
        TBApplication.behaviour(context).removeResult(pojo);
        //TODO: make an UndoBar
        Toast.makeText(context, context.getString(R.string.removed_item, pojo.getName()), Toast.LENGTH_SHORT).show();
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

    public static int getItemViewTypeCount() {
        return 7;
    }

    public static int getItemViewType(EntryItem item) {
        if (item instanceof AppEntry)
            return 1;
        if (item instanceof ContactEntry)
            return 2;
        if (item instanceof FilterEntry)
            return 3;
        if (item instanceof ShortcutEntry)
            return 4;
        if (item instanceof StaticEntry)
            return 5;
        if (item instanceof SearchEntry)
            return 6;
        if (BuildConfig.DEBUG)
            throw new IllegalStateException("view type not set for adapter");
        return 0;
    }
}
