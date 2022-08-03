package rocks.tbog.tblauncher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;

import java.util.ArrayList;
import java.util.Iterator;

import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.PlaceholderWidgetRecord;
import rocks.tbog.tblauncher.db.WidgetRecord;
import rocks.tbog.tblauncher.drawable.DrawableUtils;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.LinearAdapterPlus;
import rocks.tbog.tblauncher.ui.ListPopup;
import rocks.tbog.tblauncher.ui.WidgetLayout;
import rocks.tbog.tblauncher.ui.WidgetView;
import rocks.tbog.tblauncher.utils.DebugInfo;
import rocks.tbog.tblauncher.utils.Utilities;

public class WidgetManager {
    private static final String TAG = "Wdg";
    public static final int INVALID_WIDGET_ID = AppWidgetManager.INVALID_APPWIDGET_ID;
    private AppWidgetManager mAppWidgetManager;
    private WidgetHost mAppWidgetHost;
    private WidgetLayout mLayout;
    private WidgetLayout.Handle mLastMoveType = WidgetLayout.Handle.MOVE_FREE;
    private WidgetLayout.Handle mLastResizeType = WidgetLayout.Handle.RESIZE_DIAGONAL;
    private WidgetLayout.Handle mLastMoveResizeType = WidgetLayout.Handle.MOVE_FREE_RESIZE_AXIAL;
    private final ArrayMap<Integer, WidgetRecord> mWidgets = new ArrayMap<>(0);
    private final ArrayList<PlaceholderWidgetRecord> mPlaceholders = new ArrayList<>(0);
    private static final int APPWIDGET_HOST_ID = 1337;
    private static final int REQUEST_PICK_APPWIDGET = 101;
    private static final int REQUEST_CONFIGURE_APPWIDGET = 102;
    private static final int REQUEST_BIND_APPWIDGET = 103;
    public static final String EXTRA_WIDGET_BIND_ALLOWED = "widgetBindAllowed";

    /**
     * Registers the AppWidgetHost to listen for updates to any widgets this app has.
     */
    boolean start(Context context) {
        Context ctx = context.getApplicationContext();
        mAppWidgetManager = AppWidgetManager.getInstance(ctx);
        try {
            mAppWidgetHost = new WidgetHost(ctx);
            mAppWidgetHost.startListening();
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "startListening failed", e);
            // Widget app was just updated? See https://github.com/Neamar/KISS/issues/959
            mAppWidgetHost = null;
            return false;
        }
        return true;
    }

    void stop() {
        mAppWidgetHost.stopListening();
        mAppWidgetHost = null;
    }

    /**
     * Called on the creation of the activity.
     */
    public void onCreateActivity(Activity activity) {
        mLayout = activity.findViewById(R.id.widgetContainer);
        mLayout.setPageCount(LiveWallpaper.SCREEN_COUNT_HORIZONTAL, LiveWallpaper.SCREEN_COUNT_VERTICAL);
        restoreWidgets();
        // post the scroll event to happen after the measure and layout phase
        final LiveWallpaper lw = TBApplication.liveWallpaper(activity);
        mLayout.addOnAfterLayoutTask(() -> {
            PointF offset = lw.getWallpaperOffset();
            WidgetManager.this.scroll(offset.x, offset.y);
        });
//        mLayout.post(new Runnable() {
//            @Override
//            public void run() {
//                if (mLayout.getMeasuredWidth() == 0) {
//                    // layout did not happen yet, wait some more
//                    mLayout.removeCallbacks(this);
//                    mLayout.post(this);
//                    return;
//                }
//                PointF offset = lw.getWallpaperOffset();
//                WidgetManager.this.scroll(offset.x, offset.y);
//            }
//        });
    }

    private void loadFromDB(Context context) {
        ArrayList<WidgetRecord> widgets = DBHelper.getWidgets(context);
        mWidgets.clear();
        mPlaceholders.clear();
        // we expect no placeholders
        mWidgets.ensureCapacity(widgets.size());
        for (WidgetRecord record : widgets) {
            if (record instanceof PlaceholderWidgetRecord) {
                mPlaceholders.add((PlaceholderWidgetRecord) record);
            } else {
                mWidgets.put(record.appWidgetId, record);
            }
        }
    }

    private void restoreWidgets() {
        mLayout.removeAllViews();

        if (mAppWidgetHost == null) {
            Log.w(TAG, "`restoreWidgets` called prior to `startListening`");
            if (!start(mLayout.getContext())) {
                Log.w(TAG, "`start` failed, try `restoreWidgets` after 500ms");
                mLayout.postDelayed(this::restoreWidgets, 500);
                return;
            }
        }

        int[] appWidgetIds;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            appWidgetIds = mAppWidgetHost.getAppWidgetIds();
        } else {
            appWidgetIds = new int[0];
        }

        loadFromDB(mLayout.getContext());

        // sync DB with AppWidgetHost
        for (int appWidgetId : appWidgetIds) {
            if (!mWidgets.containsKey(appWidgetId)) {
                // remove widget that has no info in DB
                removeWidget(appWidgetId);
            }
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            ArrayList<Integer> toDelete = new ArrayList<>(0);
            for (WidgetRecord rec : mWidgets.values()) {
                boolean found = false;
                for (int appWidgetId : appWidgetIds)
                    if (appWidgetId == rec.appWidgetId) {
                        found = true;
                        break;
                    }
                if (!found) {
                    // remove widget from DB
                    toDelete.add(rec.appWidgetId);
                }
            }
            for (int appWidgetId : toDelete)
                removeWidget(appWidgetId);
        }

        // restore widgets
        for (WidgetRecord rec : mWidgets.values()) {
            restoreWidget(rec);
        }

        // restore placeholders
        for (PlaceholderWidgetRecord placeholderWidget : mPlaceholders) {
            addPlaceholderToLayout(placeholderWidget);
        }
    }

//    public List<AppWidgetProviderInfo> getWidgetsForPackage(String packageName) {
//        List<AppWidgetProviderInfo> providers;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            providers = mAppWidgetManager.getInstalledProvidersForPackage(packageName, null);
//        } else {
//            providers = new ArrayList<>();
//            for (AppWidgetProviderInfo prov : mAppWidgetManager.getInstalledProviders()) {
//                if (prov.provider.getPackageName().equals(packageName)) {
//                    providers.add(prov);
//                }
//            }
//        }
//        return providers;
//    }

    private void restoreWidget(WidgetRecord rec) {
        final int appWidgetId = rec.appWidgetId;
        Context ctx = mLayout.getContext().getApplicationContext();
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null)
            return;
        AppWidgetHostView hostView = mAppWidgetHost.createView(ctx, appWidgetId, appWidgetInfo);

        addWidgetToLayout(hostView, appWidgetInfo, rec);
    }

    public void onBeforeRestoreFromBackup(boolean clearAll) {
        if (mAppWidgetHost == null) {
            Log.e(TAG, "`onBeforeRestoreFromBackup` called prior to `startListening`");
            return;
        }

        loadFromDB(mLayout.getContext());

        if (clearAll) {
            mLayout.removeAllViews();
            mPlaceholders.clear();
        }
    }

    public void onAfterRestoreFromBackup(boolean clearExtra) {
        if (clearExtra) {
            // remove all widgets not found in mLayout
            int[] appWidgetIds = new int[mWidgets.size()];
            //Arrays.fill(appWidgetIds, INVALID_WIDGET_ID);
            // copy widget ids we may need to remove
            {
                int idx = 0;
                for (WidgetRecord rec : mWidgets.values())
                    appWidgetIds[idx++] = rec.appWidgetId;
            }
            for (int appWidgetId : appWidgetIds) {
                AppWidgetHostView widgetHostView = mLayout.getWidget(appWidgetId);
                if (widgetHostView != null)
                    removeWidget(widgetHostView);
            }
        } else {
            // restore widgets from mWidgets that are not in mLayout yet
            for (WidgetRecord rec : mWidgets.values()) {
                if (mLayout.getWidget(rec.appWidgetId) == null)
                    restoreWidget(rec);
            }
        }

        Context ctx = mLayout.getContext();
        for (WidgetRecord rec : mWidgets.values()) {
            DBHelper.setWidgetProperties(ctx, rec);
        }
        // remove all placeholders
        DBHelper.removeWidget(ctx, INVALID_WIDGET_ID);
        // add back all placeholders
        for (PlaceholderWidgetRecord placeholder : mPlaceholders) {
            DBHelper.addWidget(ctx, placeholder);
        }
    }

    /**
     * called when importing from backup / XML
     *
     * @param append if true widget information will not be changed / imported / restored
     * @param record placeholder information
     */
    public void restoreFromBackup(boolean append, PlaceholderWidgetRecord record/*, String name, ComponentName provider, byte[] preview, WidgetRecord record*/) {
        int appWidgetId = record.appWidgetId;
        boolean bFound = false;
        {
            WidgetRecord widgetRecord = mWidgets.get(appWidgetId);
            AppWidgetProviderInfo info = widgetRecord != null ? getWidgetProviderInfo(widgetRecord.appWidgetId) : null;
            // check if appWidgetId can be restored
            if (info != null) {
                bFound = record.provider.equals(info.provider);
            }
        }
        // check if we can find a provider match
        if (!bFound) {
            for (WidgetRecord rec : mWidgets.values()) {
                // if we already restored this widget, skip
                if (mLayout.getWidget(rec.appWidgetId) != null)
                    continue;
                AppWidgetProviderInfo info = getWidgetProviderInfo(rec.appWidgetId);
                if (info == null)
                    continue;
                if (record.provider.equals(info.provider)) {
                    appWidgetId = rec.appWidgetId;
                    bFound = true;
                    break;
                }
            }
        }
        //TODO: check if we can recycle a widget based on provider

        if (bFound) {
            record.appWidgetId = appWidgetId;
            if (!append) {
                // widget found, apply the properties
                mWidgets.put(appWidgetId, record);
            }
            mLayout.removeWidget(appWidgetId);
            restoreWidget(record);
        } else {
            // widget not found, add a placeholder
            record.appWidgetId = INVALID_WIDGET_ID;
            mPlaceholders.add(record);
            addPlaceholderToLayout(record);
        }
    }

    private void addWidgetToLayout(AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo, WidgetRecord rec) {
        View placeholder = mLayout.getPlaceholder(appWidgetInfo.provider);
        WidgetLayout.LayoutParams params = null;
        if (placeholder != null)
            params = (WidgetLayout.LayoutParams) placeholder.getLayoutParams();
        if (params == null) {
            params = new WidgetLayout.LayoutParams(rec.width, rec.height);
            params.leftMargin = rec.left;
            params.topMargin = rec.top;
            params.screenPage = rec.screen;
            params.placement = WidgetLayout.LayoutParams.Placement.MARGIN_TL_AS_POSITION;
        }
        hostView.setMinimumWidth(appWidgetInfo.minWidth);
        hostView.setMinimumHeight(appWidgetInfo.minHeight);

        hostView.setAppWidget(rec.appWidgetId, appWidgetInfo);
        hostView.updateAppWidgetSize(null, rec.width, rec.height, rec.width, rec.height);

        hostView.setOnLongClickListener(v -> {
            if (v instanceof WidgetView) {
                ListPopup menu = getConfigPopup((WidgetView) v);
                TBApplication.getApplication(v.getContext()).registerPopup(menu);
                menu.show(v, 0.f);
                return true;
            }
            return false;
        });

        // replace placeholder (if it exists) with the widget
        {
            int insertPosition = mLayout.indexOfChild(placeholder);
            if (insertPosition != -1)
                mLayout.removeViewAt(insertPosition);
            mLayout.addView(hostView, insertPosition, params);
        }

        Context context = mLayout.getContext();
        // remove from `mPlaceholders`
        {
            for (Iterator<PlaceholderWidgetRecord> iterator = mPlaceholders.iterator(); iterator.hasNext(); ) {
                PlaceholderWidgetRecord placeholderWidget = iterator.next();
                if (placeholderWidget.provider.equals(appWidgetInfo.provider)) {
                    DBHelper.removeWidgetPlaceholder(context, INVALID_WIDGET_ID, placeholderWidget.provider.flattenToString());
                    iterator.remove();
                }
            }
        }
    }

    private void addPlaceholderToLayout(@NonNull PlaceholderWidgetRecord rec) {
        final Context context = mLayout.getContext();
        Drawable preview = DrawableUtils.getBitmapDrawable(context, rec.preview);

        View placeholder = LayoutInflater.from(context).inflate(R.layout.widget_placeholder, mLayout, false);
        {
            WidgetLayout.LayoutParams params = new WidgetLayout.LayoutParams(rec.width, rec.height);
            params.leftMargin = rec.left;
            params.topMargin = rec.top;
            params.screenPage = rec.screen;
            params.placement = WidgetLayout.LayoutParams.Placement.MARGIN_TL_AS_POSITION;
            placeholder.setLayoutParams(params);
        }
        {
            TextView text = placeholder.findViewById(android.R.id.text1);
            text.setText(context.getString(R.string.widget_placeholder, rec.name));
        }
        {
            ImageView icon = placeholder.findViewById(android.R.id.icon);
            icon.setImageDrawable(preview);
        }
        final ComponentName provider = rec.provider != null ? rec.provider : new ComponentName("null", "null");
        placeholder.setOnClickListener(v -> {
            Activity activity = Utilities.getActivity(v);
            if (activity == null)
                return;

            int appWidgetId = mAppWidgetHost.allocateAppWidgetId();
            boolean hasPermission;
            try {
                hasPermission = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, provider);
            } catch (Throwable ignored) {
                hasPermission = false;
            }

            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
            if (hasPermission) {
                configureWidget(activity, intent);
            } else {
                activity.startActivityForResult(intent, REQUEST_PICK_APPWIDGET/*REQUEST_BIND*/);
            }
            //Toast.makeText(activity, provider.flattenToString(), Toast.LENGTH_SHORT).show();
        });
        placeholder.setOnLongClickListener(placeholderView -> {
            Activity activity = Utilities.getActivity(placeholderView);
            if (activity == null)
                return false;
            TextView text = placeholderView.findViewById(android.R.id.text1);
            final CharSequence placeholderName = text != null ? text.getText() : "null";
            ContextThemeWrapper ctxDialog = new ContextThemeWrapper(activity, R.style.TitleDialogTheme);
            new AlertDialog.Builder(ctxDialog)
                    .setTitle(R.string.widget_placeholder_remove)
                    .setMessage(placeholderName + "\n" + provider.flattenToShortString())
                    .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                        mLayout.removeView(placeholderView);
                        for (Iterator<PlaceholderWidgetRecord> iterator = mPlaceholders.iterator(); iterator.hasNext(); ) {
                            PlaceholderWidgetRecord placeholderWidgetRecord = iterator.next();
                            if (provider.equals(placeholderWidgetRecord.provider)) {
                                DBHelper.removeWidgetPlaceholder(activity, INVALID_WIDGET_ID, placeholderWidgetRecord.provider.flattenToString());
                                iterator.remove();
                            }
                        }
                        dialog.dismiss();
                    })
                    .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                    .show();
            return true;
        });

        mLayout.addPlaceholder(placeholder, provider);
    }

    public boolean usingActivity(Activity activity) {
        return activity.findViewById(R.id.widgetContainer) == mLayout;
    }

    /**
     * Launches the menu to select the widget. The selected widget will be on
     * the result of the activity.
     */
    public void showSelectWidget(Activity activity) {
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(activity, PickAppWidgetActivity.class);
        //Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        //addEmptyData(pickIntent);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        activity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    /**
     * This avoids a bug in the com.android.settings.AppWidgetPickActivity,
     * which is used to select widgets. This just adds empty extras to the
     * intent, avoiding the bug.
     * <p>
     * See more: http://code.google.com/p/android/issues/detail?id=4272
     */
    public static void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>(1);
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<>(1);
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    private static void requestBindWidget(@NonNull Activity activity, @NonNull Intent data) {
        Bundle extras = data.getExtras();
        if (extras == null)
            return;
        final int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, INVALID_WIDGET_ID);
        final ComponentName provider = extras.getParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER);
        final UserHandle profile;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            profile = extras.getParcelable(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE);
        } else {
            profile = null;
        }

        new Handler().postDelayed(() -> {
            Log.d(TAG, "asking for permission");

            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, provider);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, profile);
            }
            addEmptyData(intent);

            activity.startActivityForResult(intent, REQUEST_BIND_APPWIDGET);
        }, 500);
    }

    /**
     * Checks if the widget needs any configuration. If it needs, launches the
     * configuration activity.
     */
    private void configureWidget(Activity activity, Intent data) {
        Bundle extras = data != null ? data.getExtras() : null;
        if (extras == null)
            return;
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, INVALID_WIDGET_ID);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

        /* See https://stackoverflow.com/a/40269593
         * If you use the AppWidgetManager.ACTION_APPWIDGET_PICK inten to pick the intent from the chooser displayed by the Android OS, there is no need to bind as the framework automatically binds the widget.
         * If you implement a custom chooser (for example, something which shows the preview images of widgets which is implemented in lots of custom launchers), then binding is necessary.
         */
//        boolean hasPermission = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetInfo.provider);
//        if (!hasPermission) {
//            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
//            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);
//            activity.startActivityForResult(intent, REQUEST_PICK_APPWIDGET/*REQUEST_BIND*/);
//        }

        if (appWidgetInfo.configure != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                startConfigActivity(appWidgetId, activity, REQUEST_CONFIGURE_APPWIDGET);
            } else {
                Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
                intent.setComponent(appWidgetInfo.configure);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
                try {
                    activity.startActivityForResult(intent, REQUEST_CONFIGURE_APPWIDGET);
                } catch (SecurityException e) {
                    Log.e(TAG, "ACTION_APPWIDGET_CONFIGURE", e);
                    Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        } else {
            createWidget(activity, data);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startConfigActivity(int widgetId, Activity activity, int requestCode) {
        try {
            mAppWidgetHost.startAppWidgetConfigureActivityForResult(activity, widgetId, 0, requestCode, null);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(activity, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Creates the widget and adds it to our view layout.
     */
    public void createWidget(Activity activity, Intent data) {
        Bundle extras = data != null ? data.getExtras() : null;
        if (extras == null)
            return;
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, INVALID_WIDGET_ID);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null)
            return;
        AppWidgetHostView hostView = mAppWidgetHost.createView(activity.getApplicationContext(), appWidgetId, appWidgetInfo);

        WidgetRecord rec = new WidgetRecord();
        rec.appWidgetId = appWidgetId;
        rec.width = appWidgetInfo.minWidth;
        rec.height = appWidgetInfo.minHeight;

        DBHelper.addWidget(activity, rec);
        mWidgets.put(rec.appWidgetId, rec);

        addWidgetToLayout(hostView, appWidgetInfo, rec);
    }

    /**
     * Removes the widget displayed by this AppWidgetHostView.
     */
    public void removeWidget(AppWidgetHostView hostView) {
        final int appWidgetId = hostView.getAppWidgetId();
        mLayout.removeWidget(hostView);

        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        DBHelper.removeWidget(mLayout.getContext(), appWidgetId);
        mWidgets.remove(appWidgetId);
    }

    public void removeWidget(int appWidgetId) {
        mLayout.removeWidget(appWidgetId);

        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        DBHelper.removeWidget(mLayout.getContext(), appWidgetId);
        mWidgets.remove(appWidgetId);
    }

    /**
     * Called from the main activity, should return true to stop further processing of this result
     * If the user has selected an widget, the result will be in the 'data' when this function is called.
     *
     * @param activity    The activity that received the result
     * @param requestCode The integer request code originally supplied to
     *                    startActivityForResult(), allowing you to identify who this
     *                    result came from.
     * @param resultCode  The integer result code returned by the child activity
     *                    through its setResult().
     * @param data        An Intent, which can return result data to the caller
     *                    (various data can be attached to Intent "extras").
     * @return if this result was processed here
     */
    public boolean onActivityResult(@NonNull Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        switch (resultCode) {
            case Activity.RESULT_OK:
                if (requestCode == REQUEST_PICK_APPWIDGET) {
                    if (data != null && !data.getBooleanExtra(EXTRA_WIDGET_BIND_ALLOWED, false))
                        requestBindWidget(activity, data);
                    else
                        configureWidget(activity, data);
                    return true;
                } else if (requestCode == REQUEST_BIND_APPWIDGET) {
                    configureWidget(activity, data);
                    return true;
                } else if (requestCode == REQUEST_CONFIGURE_APPWIDGET) {
                    createWidget(activity, data);
                    return true;
                }
                break;
            case Activity.RESULT_CANCELED:
                if (data != null) {
                    int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, INVALID_WIDGET_ID);
                    if (appWidgetId != INVALID_WIDGET_ID) {
                        removeWidget(appWidgetId);
                        return true;
                    }
                } else if (requestCode == REQUEST_PICK_APPWIDGET) {
                    Toast.makeText(activity, R.string.add_widget_failed, Toast.LENGTH_LONG).show();
                } else if (requestCode == REQUEST_BIND_APPWIDGET) {
                    Toast.makeText(activity, R.string.bind_widget_failed, Toast.LENGTH_LONG).show();
                }
                break;
        }
        return false;
    }

    public int widgetCount() {
        return mWidgets.size() + mPlaceholders.size();
    }

    /**
     * A popup with all active widgets to choose one
     *
     * @return the menu
     */
    public ListPopup getWidgetListPopup(@StringRes int title) {
        Context ctx = mLayout.getContext();
        LinearAdapter adapter = new LinearAdapterPlus();

        adapter.add(new LinearAdapter.ItemTitle(ctx, title));

        for (WidgetRecord rec : mWidgets.values()) {
            adapter.add(WidgetPopupItem.create(ctx, mAppWidgetManager, rec.appWidgetId));
        }

        for (PlaceholderWidgetRecord placeholder : mPlaceholders) {
            adapter.add(PlaceholderPopupItem.create(ctx, placeholder));
        }

        return ListPopup.create(ctx, adapter);
    }

    /**
     * Popup with options for all widgets
     *
     * @param activity used to start the widget select popup
     * @return the popup menu
     */
    public ListPopup getConfigPopup(Activity activity) {
        LinearAdapter adapter = new LinearAdapter();

        adapter.add(new LinearAdapter.ItemTitle(activity, R.string.menu_widget_title));
        adapter.add(new LinearAdapter.Item(activity, R.string.menu_widget_add));
        if (widgetCount() > 0) {
            adapter.add(new LinearAdapter.Item(activity, R.string.menu_widget_configure));
            adapter.add(new LinearAdapter.Item(activity, R.string.menu_widget_remove));
        }
        adapter.add(new LinearAdapter.Item(activity, R.string.menu_popup_launcher_settings));

        ListPopup menu = ListPopup.create(activity, adapter);
        menu.setOnItemClickListener((a, v, pos) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) a).getItem(pos);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            switch (stringId) {
                case R.string.menu_widget_add:
                    TBApplication.widgetManager(activity).showSelectWidget(activity);
                    break;
                case R.string.menu_widget_configure: {
                    ListPopup configWidgetPopup = TBApplication.widgetManager(activity).getWidgetListPopup(R.string.menu_widget_configure);
                    configWidgetPopup.setOnItemClickListener((a1, v1, pos1) -> {
                        Object item1 = a1.getItem(pos1);
                        if (item1 instanceof WidgetPopupItem) {
                            AppWidgetHostView widgetView = mLayout.getWidget(((WidgetPopupItem) item1).appWidgetId);
                            if (widgetView == null)
                                return;
                            ListPopup popup = getConfigPopup((WidgetView) widgetView);
                            TBApplication.getApplication(mLayout.getContext()).registerPopup(popup);
                            popup.show(widgetView, 0.f);
                        } else if (item1 instanceof PlaceholderPopupItem) {
                            PlaceholderWidgetRecord placeholder = ((PlaceholderPopupItem) item1).placeholder;
                            View placeholderView = mLayout.getPlaceholder(placeholder.provider);
                            if (placeholderView != null)
                                placeholderView.performClick();
                        }
                    });

                    TBApplication.getApplication(activity).registerPopup(configWidgetPopup);
                    configWidgetPopup.showCenter(activity.getWindow().getDecorView());
                    break;
                }
                case R.string.menu_widget_remove:
                    showRemoveWidgetPopup();
                    break;
                case R.string.menu_popup_launcher_settings:
                    var intent = new Intent(Utilities.getActivity(mLayout), SettingsActivity.class);
                    Utilities.setIntentSourceBounds(intent, v);
                    Bundle startActivityOptions = Utilities.makeStartActivityOptions(v);
                    mLayout.postDelayed(() -> {
                        var act = Utilities.getActivity(mLayout);
                        if (act == null)
                            return;
                        try {
                            act.startActivity(intent, startActivityOptions);
                        } catch (ActivityNotFoundException ignored) {
                            // ignored
                        }
                    }, Behaviour.LAUNCH_DELAY);
                    break;
            }
        });
        return menu;
    }

    public void showRemoveWidgetPopup() {
        Context context = mLayout.getContext();
        ListPopup removeWidgetPopup = TBApplication.widgetManager(context).getWidgetListPopup(R.string.menu_widget_remove);
        removeWidgetPopup.setOnItemClickListener((a1, v1, pos1) -> {
            Object item1 = a1.getItem(pos1);
            if (item1 instanceof WidgetPopupItem) {
                removeWidget(((WidgetPopupItem) item1).appWidgetId);
            } else if (item1 instanceof PlaceholderPopupItem) {
                PlaceholderWidgetRecord placeholder = ((PlaceholderPopupItem) item1).placeholder;
                View placeholderView = mLayout.getPlaceholder(placeholder.provider);
                if (placeholderView != null)
                    placeholderView.performLongClick();
            }
        });

        TBApplication.getApplication(context).registerPopup(removeWidgetPopup);
        removeWidgetPopup.showCenter(mLayout);
    }

    private static boolean canMoveToPage(WidgetLayout layout, int from, int to) {
        if (from != WidgetLayout.LayoutParams.PAGE_MIDDLE)
            return to == WidgetLayout.LayoutParams.PAGE_MIDDLE;
        if (layout == null)
            return false;

        boolean ok = false;
        if (layout.getVerticalPageCount() > 1)
            ok = ok || to == WidgetLayout.LayoutParams.PAGE_UP || to == WidgetLayout.LayoutParams.PAGE_DOWN;
        if (layout.getHorizontalPageCount() > 1)
            ok = ok || to == WidgetLayout.LayoutParams.PAGE_LEFT || to == WidgetLayout.LayoutParams.PAGE_RIGHT;
        return ok;
    }

    /**
     * Popup with options for the widget in the view
     *
     * @param view of the widget
     * @return the popup menu
     */
    protected ListPopup getConfigPopup(WidgetView view) {
        final int appWidgetId = view.getAppWidgetId();
        Context ctx = mLayout.getContext();
        LinearAdapter adapter = new LinearAdapter();

        WidgetRecord widget = mWidgets.get(appWidgetId);
        if (widget != null) {
            addConfigPopupItems(mLayout, adapter, view, widget);
        } else {
            adapter.add(new LinearAdapter.ItemString("ERROR: Not found"));
        }

        ListPopup menu = ListPopup.create(ctx, adapter);
        menu.setOnItemClickListener((a, v, position) -> handleConfigPopupItemClick(a, view, position));
        return menu;
    }

    private static void addConfigPopupItems(WidgetLayout widgetLayout, LinearAdapter adapter, WidgetView view, WidgetRecord widget) {
        Context ctx = widgetLayout.getContext();
        adapter.add(new LinearAdapter.ItemTitle(getWidgetName(ctx, view.getAppWidgetInfo())));
        final WidgetLayout.Handle handleType = widgetLayout.getHandleType(view);
        if (handleType.isMove()) {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move_switch, WidgetOptionItem.Action.MOVE_SWITCH));
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move_exit, WidgetOptionItem.Action.RESET));
        } else {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move, WidgetOptionItem.Action.MOVE));
        }

        if (handleType.isResize()) {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_resize_switch, WidgetOptionItem.Action.RESIZE_SWITCH));
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_resize_exit, WidgetOptionItem.Action.RESET));
        } else {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_resize, WidgetOptionItem.Action.RESIZE));
        }

        if (handleType.isMoveResize()) {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move_resize, WidgetOptionItem.Action.MOVE_RESIZE_SWITCH));
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move_resize_exit, WidgetOptionItem.Action.RESET));
        } else {
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_move_resize, WidgetOptionItem.Action.MOVE_RESIZE));
        }

        adapter.add(new LinearAdapter.ItemDivider());
        final ViewGroup.LayoutParams lp = view.getLayoutParams();
        if (lp instanceof WidgetLayout.LayoutParams) {
            final int screenPage = ((WidgetLayout.LayoutParams) lp).screenPage;
            if (canMoveToPage(widgetLayout, screenPage, WidgetLayout.LayoutParams.PAGE_LEFT))
                adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_screen_left, WidgetOptionItem.Action.MOVE2SCREEN_LEFT));
            if (canMoveToPage(widgetLayout, screenPage, WidgetLayout.LayoutParams.PAGE_UP))
                adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_screen_up, WidgetOptionItem.Action.MOVE2SCREEN_UP));
            if (canMoveToPage(widgetLayout, screenPage, WidgetLayout.LayoutParams.PAGE_MIDDLE))
                adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_screen_middle, WidgetOptionItem.Action.MOVE2SCREEN_MIDDLE));
            if (canMoveToPage(widgetLayout, screenPage, WidgetLayout.LayoutParams.PAGE_RIGHT))
                adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_screen_right, WidgetOptionItem.Action.MOVE2SCREEN_RIGHT));
            if (canMoveToPage(widgetLayout, screenPage, WidgetLayout.LayoutParams.PAGE_DOWN))
                adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_screen_down, WidgetOptionItem.Action.MOVE2SCREEN_DOWN));
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_back, WidgetOptionItem.Action.MOVE_BELOW));
            adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_front, WidgetOptionItem.Action.MOVE_ABOVE));
        }

        adapter.add(new WidgetOptionItem(ctx, R.string.cfg_widget_remove, WidgetOptionItem.Action.REMOVE));

        if (DebugInfo.widgetInfo(ctx)) {
            adapter.add(new LinearAdapter.ItemTitle("Debug info"));
            adapter.add(new LinearAdapter.ItemString("Name: " + getWidgetName(ctx, view.getAppWidgetInfo())));
            adapter.add(new LinearAdapter.ItemText(widget.packedProperties()));
            adapter.add(new LinearAdapter.ItemString("ID: " + widget.appWidgetId));
        }
    }

    private void handleConfigPopupItemClick(ListAdapter adapter, WidgetView view, int position) {
        Object item = adapter.getItem(position);
        if (item instanceof WidgetOptionItem) {
            switch (((WidgetOptionItem) item).mAction) {
                case MOVE:
                    view.setOnClickListener(v1 -> {
                        view.setOnClickListener(null);
                        view.setOnDoubleClickListener(null);
                        mLayout.disableHandle(view);
                        saveWidgetProperties(view);
                    });
                    view.setOnDoubleClickListener(v1 -> {
                        if (mLayout.getHandleType(view) == WidgetLayout.Handle.MOVE_FREE) {
                            mLastMoveType = WidgetLayout.Handle.MOVE_AXIAL;
                        } else {
                            mLastMoveType = WidgetLayout.Handle.MOVE_FREE;
                        }
                        mLayout.enableHandle(view, mLastMoveType);
                    });
                    mLayout.enableHandle(view, mLastMoveType);
                    break;
                case MOVE_SWITCH:
                    if (mLayout.getHandleType(view) == WidgetLayout.Handle.MOVE_FREE) {
                        mLastMoveType = WidgetLayout.Handle.MOVE_AXIAL;
                    } else {
                        mLastMoveType = WidgetLayout.Handle.MOVE_FREE;
                    }
                    mLayout.enableHandle(view, mLastMoveType);
                    break;
                case RESIZE:
                    view.setOnClickListener(v1 -> {
                        view.setOnClickListener(null);
                        view.setOnDoubleClickListener(null);
                        mLayout.disableHandle(view);
                        saveWidgetProperties(view);
                    });
                    view.setOnDoubleClickListener(v1 -> {
                        if (mLayout.getHandleType(view) == WidgetLayout.Handle.RESIZE_DIAGONAL) {
                            mLastResizeType = WidgetLayout.Handle.RESIZE_AXIAL;
                        } else {
                            mLastResizeType = WidgetLayout.Handle.RESIZE_DIAGONAL;
                        }
                        mLayout.enableHandle(view, mLastResizeType);
                    });
                    mLayout.enableHandle(view, mLastResizeType);
                    break;
                case RESIZE_SWITCH:
                    if (mLayout.getHandleType(view) == WidgetLayout.Handle.RESIZE_DIAGONAL) {
                        mLastResizeType = WidgetLayout.Handle.RESIZE_AXIAL;
                    } else {
                        mLastResizeType = WidgetLayout.Handle.RESIZE_DIAGONAL;
                    }
                    mLayout.enableHandle(view, mLastResizeType);
                    break;
                case MOVE_RESIZE:
                    view.setOnClickListener(v1 -> {
                        view.setOnClickListener(null);
                        view.setOnDoubleClickListener(null);
                        mLayout.disableHandle(view);
                        saveWidgetProperties(view);
                    });
                    view.setOnDoubleClickListener(v1 -> {
                        if (mLayout.getHandleType(view) == WidgetLayout.Handle.MOVE_FREE_RESIZE_AXIAL) {
                            mLastMoveResizeType = WidgetLayout.Handle.RESIZE_DIAGONAL_MOVE_AXIAL;
                        } else {
                            mLastMoveResizeType = WidgetLayout.Handle.MOVE_FREE_RESIZE_AXIAL;
                        }
                        mLayout.enableHandle(view, mLastMoveResizeType);
                    });
                    mLayout.enableHandle(view, mLastMoveResizeType);
                case MOVE_RESIZE_SWITCH:
                    if (mLayout.getHandleType(view) == WidgetLayout.Handle.MOVE_FREE_RESIZE_AXIAL) {
                        mLastMoveResizeType = WidgetLayout.Handle.RESIZE_DIAGONAL_MOVE_AXIAL;
                    } else {
                        mLastMoveResizeType = WidgetLayout.Handle.MOVE_FREE_RESIZE_AXIAL;
                    }
                    mLayout.enableHandle(view, mLastMoveResizeType);
                    break;
                case RESET:
                    view.setOnClickListener(null);
                    view.setOnDoubleClickListener(null);
                    mLayout.disableHandle(view);
                    saveWidgetProperties(view);
                    break;
                case REMOVE:
                    removeWidget(view);
                    break;
                case MOVE2SCREEN_LEFT: {
                    final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
                    lp.screenPage = WidgetLayout.LayoutParams.PAGE_LEFT;
                    view.setLayoutParams(lp);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE2SCREEN_UP: {
                    final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
                    lp.screenPage = WidgetLayout.LayoutParams.PAGE_UP;
                    view.setLayoutParams(lp);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE2SCREEN_RIGHT: {
                    final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
                    lp.screenPage = WidgetLayout.LayoutParams.PAGE_RIGHT;
                    view.setLayoutParams(lp);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE2SCREEN_DOWN: {
                    final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
                    lp.screenPage = WidgetLayout.LayoutParams.PAGE_DOWN;
                    view.setLayoutParams(lp);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE2SCREEN_MIDDLE: {
                    final WidgetLayout.LayoutParams lp = (WidgetLayout.LayoutParams) view.getLayoutParams();
                    lp.screenPage = WidgetLayout.LayoutParams.PAGE_MIDDLE;
                    view.setLayoutParams(lp);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE_ABOVE: {
                    int idx = mLayout.indexOfChild(view);
                    mLayout.removeViewAt(idx);
                    mLayout.addView(view);
                    saveWidgetProperties(view);
                    break;
                }
                case MOVE_BELOW: {
                    int idx = mLayout.indexOfChild(view);
                    mLayout.removeViewAt(idx);
                    mLayout.addView(view, 0);
                    saveWidgetProperties(view);
                    break;
                }
            }
        }
    }

    private void saveWidgetProperties(WidgetView view) {
        final int appWidgetId = view.getAppWidgetId();
//        WidgetRecord rec = mWidgets.get(appWidgetId);
//        if (rec == null)
//            return;
        mLayout.addOnAfterLayoutTask(() -> {
            WidgetRecord rec = mWidgets.get(appWidgetId);
            AppWidgetHostView widgetHostView = mLayout.getWidget(appWidgetId);
            if (rec != null && widgetHostView != null) {
                rec.saveProperties(widgetHostView);
                Utilities.runAsync(() -> DBHelper.setWidgetProperties(mLayout.getContext(), rec));
            }
        });
        mLayout.requestLayout();
    }

    public void setPageCount(int horizontal, int vertical) {
        if (mLayout == null)
            return;
        mLayout.setPageCount(horizontal, vertical);
    }

    /**
     * Scroll to page, just like the wallpaper
     *
     * @param scrollX horizontal scroll position 0.f .. 1.f
     * @param scrollY vertical scroll position 0.f .. 1.f
     */
    public void scroll(float scrollX, float scrollY) {
        if (mLayout == null)
            return;

        final int pageCountX = mLayout.getHorizontalPageCount();
        final float pageX = pageCountX * scrollX;

        final int pageCountY = mLayout.getVerticalPageCount();
        final float pageY = pageCountY * scrollY;

        mLayout.scrollToPage(pageX, pageY);
    }

    @Nullable
    public static AppWidgetProviderInfo getWidgetProviderInfo(@NonNull Context ctx, int appWidgetId) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(ctx);
        AppWidgetProviderInfo info;
        try {
            info = appWidgetManager.getAppWidgetInfo(appWidgetId);
        } catch (Exception ignored) {
            return null;
        }
        return info;
    }

    @Nullable
    public AppWidgetProviderInfo getWidgetProviderInfo(int appWidgetId) {
        AppWidgetProviderInfo info;
        try {
            info = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        } catch (Exception ignored) {
            return null;
        }
        return info;
    }

    @NonNull
    public static String getWidgetName(@NonNull Context ctx, @Nullable AppWidgetProviderInfo info) {
        String name = null;
        if (info != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                name = info.loadLabel(ctx.getPackageManager());
            } else {
                name = info.label;
            }
        }
        return name == null ? "[null]" : name;
    }

    @NonNull
    public static Drawable getWidgetPreview(Context ctx, @Nullable AppWidgetProviderInfo info) {
        Drawable icon = null;
        if (info != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                icon = info.loadPreviewImage(ctx, 0);
            } else {
                icon = ctx.getPackageManager().getDrawable(info.provider.getPackageName(), info.previewImage, null);
            }
            if (icon == null) {
                try {
                    icon = ctx.getPackageManager().getApplicationLogo(info.provider.getPackageName());
                } catch (PackageManager.NameNotFoundException ignored) {
                }
            }
        }
        if (icon == null) {
            if (info == null) {
                icon = ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.ic_android, null);
            } else {
                try {
                    icon = ctx.getPackageManager().getApplicationIcon(info.provider.getPackageName());
                } catch (PackageManager.NameNotFoundException ignored) {
                    icon = ResourcesCompat.getDrawable(ctx.getResources(), R.drawable.ic_android, null);
                }
            }
        }
        assert icon != null;
        return icon;
    }

    static class WidgetOptionItem extends LinearAdapter.Item {
        enum Action {
            MOVE, MOVE_SWITCH,
            RESIZE, RESIZE_SWITCH,
            MOVE_RESIZE, MOVE_RESIZE_SWITCH,
            RESET,
            REMOVE,
            MOVE2SCREEN_LEFT, MOVE2SCREEN_UP, MOVE2SCREEN_MIDDLE, MOVE2SCREEN_RIGHT, MOVE2SCREEN_DOWN,
            MOVE_BELOW, MOVE_ABOVE,
        }

        final Action mAction;

        public WidgetOptionItem(Context ctx, @StringRes int stringId, Action action) {
            super(ctx, stringId);
            mAction = action;
        }
    }

    static class PlaceholderPopupItem extends LinearAdapterPlus.ItemStringIcon {
        final PlaceholderWidgetRecord placeholder;

        @NonNull
        static PlaceholderPopupItem create(Context ctx, PlaceholderWidgetRecord placeholder) {
            Drawable icon = DrawableUtils.getBitmapDrawable(ctx, placeholder.preview);
            String name = ctx.getString(R.string.widget_placeholder, placeholder.name);
            return new PlaceholderPopupItem(placeholder, name, icon);
        }

        private PlaceholderPopupItem(@NonNull PlaceholderWidgetRecord placeholder, @NonNull String name, Drawable icon) {
            super(name, icon);
            this.placeholder = placeholder;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item_icon;
        }
    }

    static class WidgetPopupItem extends LinearAdapterPlus.ItemStringIcon {
        int appWidgetId;

        @NonNull
        static WidgetPopupItem create(Context ctx, AppWidgetManager appWidgetManager, int appWidgetId) {
            AppWidgetProviderInfo info = appWidgetManager.getAppWidgetInfo(appWidgetId);
            String name = getWidgetName(ctx, info);
            Drawable icon = getWidgetPreview(ctx, info);
            return new WidgetPopupItem(name, appWidgetId, icon);
        }

        private WidgetPopupItem(@NonNull String string, int appWidgetId, @NonNull Drawable icon) {
            super(string, icon);
            this.appWidgetId = appWidgetId;
        }

        @Override
        public int getLayoutResource() {
            return R.layout.popup_list_item_icon;
        }
    }

    static class WidgetHost extends AppWidgetHost {
        public WidgetHost(Context context) {
            super(context, APPWIDGET_HOST_ID);
        }

        @Override
        protected AppWidgetHostView onCreateView(Context context, int appWidgetId, AppWidgetProviderInfo appWidget) {
            return new WidgetView(context);
        }
    }

}
