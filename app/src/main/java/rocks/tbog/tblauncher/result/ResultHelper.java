package rocks.tbog.tblauncher.result;

import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_QUICK_LIST;
import static rocks.tbog.tblauncher.entry.EntryItem.LAUNCHED_FROM_RESULT_LIST;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.collection.ArraySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rocks.tbog.tblauncher.Behaviour;
import rocks.tbog.tblauncher.Permission;
import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.TBApplication;
import rocks.tbog.tblauncher.TBLauncherActivity;
import rocks.tbog.tblauncher.dataprovider.QuickListProvider;
import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.entry.AppEntry;
import rocks.tbog.tblauncher.entry.ContactEntry;
import rocks.tbog.tblauncher.entry.EntryItem;
import rocks.tbog.tblauncher.entry.SearchEntry;
import rocks.tbog.tblauncher.entry.ShortcutEntry;
import rocks.tbog.tblauncher.entry.StaticEntry;
import rocks.tbog.tblauncher.handler.DataHandler;
import rocks.tbog.tblauncher.utils.MimeTypeUtils;
import rocks.tbog.tblauncher.utils.Utilities;

public class ResultHelper {

    /**
     * Use index as view type
     * Value is the layout id
     */
    @NonNull
    static final private ArraySet<Integer> sEntryViewType = new ArraySet<>(8);

    static {
        addViewTypes(AppEntry.getResultLayout());
        addViewTypes(ContactEntry.getResultLayout());
        addViewTypes(StaticEntry.getResultLayout());
        addViewTypes(ShortcutEntry.getResultLayout());
        addViewTypes(SearchEntry.getResultLayout());
        Log.i("log", "result view type count=" + sEntryViewType.size());
    }

    private ResultHelper() {
        // this is a static class
    }

    private static void addViewTypes(int[] viewTypes) {
        for (int viewType : viewTypes)
            sEntryViewType.add(viewType);
    }

    /**
     * How to launch a result. Most probably, will fire an intent.
     * This function will record history and then call EntryItem.doLaunch
     *
     * @param view {@link View} that was touched
     * @param pojo the {@link EntryItem} that the user is launching
     */
    public static void launch(@NonNull View view, @NonNull EntryItem pojo) {
        final int launchedFrom;
        if (TBApplication.quickList(view.getContext()).isViewInList(view))
            launchedFrom = LAUNCHED_FROM_QUICK_LIST;
        else
            launchedFrom = LAUNCHED_FROM_RESULT_LIST;

        launch(view, pojo, launchedFrom);
    }

    /**
     * How to launch a result. Most probably, will fire an intent.
     * This function will record history and then call EntryItem.doLaunch
     *
     * @param view         {@link View} that was touched
     * @param pojo         the {@link EntryItem} that the user is launching
     * @param launchedFrom is the view from QuickList, ResultList, Gesture?
     */
    public static void launch(@NonNull View view, @NonNull EntryItem pojo, int launchedFrom) {
        if (pojo instanceof StaticEntry) {
            Log.i("log", "Launching StaticEntry " + pojo.id);

            recordLaunch(pojo, view.getContext());
            pojo.doLaunch(view, launchedFrom);
            return;
        }

        TBApplication.behaviour(view.getContext()).beforeLaunchOccurred();

        Log.i("log", "Launching " + pojo.id);

        recordLaunch(pojo, view.getContext());

        // Launch
        view.postDelayed(() -> {
            pojo.doLaunch(view, launchedFrom);
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
        if (pojo.isExcludedFromHistory())
            return;
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
        var activity = Utilities.getActivity(context);
        if (activity instanceof TBLauncherActivity) {
            ((TBLauncherActivity) activity).getSearchHelper().removeResult(pojo);
        }
        //TODO: make an UndoBar
        Toast.makeText(context, context.getString(R.string.removed_item, pojo.getName()), Toast.LENGTH_SHORT).show();
    }

    private static void removeFromHistory(@NonNull EntryItem pojo, @NonNull Context context) {
        DBHelper.removeFromHistory(context, pojo.id);
    }

    public static void launchAddToQuickList(@NonNull Context context, EntryItem pojo) {
        final DataHandler dataHandler = TBApplication.dataHandler(context);
        QuickListProvider provider = dataHandler.getQuickListProvider();

        // get current Quick List content
        List<? extends EntryItem> list = provider != null ? provider.getPojos() : Collections.emptyList();
        final ArrayList<String> idList = new ArrayList<>(list.size());
        for (EntryItem entryItem : list) {
            idList.add(entryItem.id);
        }

        // add the new entry
        idList.add(pojo.id);

        // save Quick List
        dataHandler.setQuickList(idList);
    }

    public static void launchRemoveFromQuickList(@NonNull Context context, EntryItem pojo) {
        final DataHandler dataHandler = TBApplication.dataHandler(context);
        QuickListProvider provider = dataHandler.getQuickListProvider();

        // get current Quick List content
        List<? extends EntryItem> list = provider != null ? provider.getPojos() : Collections.emptyList();
        final ArrayList<String> idList = new ArrayList<>(list.size());
        for (EntryItem entryItem : list) {
            idList.add(entryItem.id);
        }

        // remove the entry
        idList.remove(pojo.id);

        // save Quick List
        dataHandler.setQuickList(idList);
    }

    public static void launchMessaging(ContactEntry contactPojo, View v) {
        Context context = v.getContext();
        TBApplication.behaviour(context).beforeLaunchOccurred();

        String url = "sms:" + Uri.encode(contactPojo.getPhone());
        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.parse(url));
        Utilities.setIntentSourceBounds(i, v);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);

        TBApplication.behaviour(context).afterLaunchOccurred();
    }

    public static void launchIm(ContactEntry.ImData imData, View v) {
        Context context = v.getContext();

        Intent intent = MimeTypeUtils.getRegisteredIntentByMimeType(context, imData.getMimeType(), imData.getId(), imData.getIdentifier());
        if (intent != null) {
            TBApplication.behaviour(context).beforeLaunchOccurred();

            Utilities.setIntentSourceBounds(intent, v);
            context.startActivity(intent);

            TBApplication.behaviour(context).afterLaunchOccurred();
        }
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
        return sEntryViewType.size();
    }

    @LayoutRes
    public static int getItemViewLayout(int viewType) {
        if (viewType < 0 || viewType >= sEntryViewType.size())
            throw new IllegalStateException("view type " + viewType + " out of range");
        Integer layout = sEntryViewType.valueAt(viewType);
        if (layout == null || layout == 0)
            throw new IllegalStateException("view type " + viewType + " has invalid layout");
        return layout;
    }

    public static int getItemViewType(EntryItem item, int drawFlags) {
        int layout = item.getResultLayout(drawFlags);
        int viewType = sEntryViewType.indexOf(layout);
        if (viewType < 0)
            throw new IllegalStateException("no view type for " + item.getClass().getName() + " drawFlags=" + drawFlags);
        return viewType;

//        if (item instanceof AppEntry)
//            return 1;
//        if (item instanceof ContactEntry)
//            return 2;
//        if (item instanceof FilterEntry)
//            return 3;
//        if (item instanceof ShortcutEntry)
//            return 4;
//        if (item instanceof StaticEntry)
//            return 5;
//        if (item instanceof SearchEntry)
//            return 6;
//        if (BuildConfig.DEBUG)
//            throw new IllegalStateException("view type not set for adapter");
//        return 0;
    }
}
