package rocks.tbog.tblauncher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.ArrayList;
import java.util.Iterator;

import rocks.tbog.tblauncher.db.DBHelper;
import rocks.tbog.tblauncher.db.WidgetRecord;
import rocks.tbog.tblauncher.ui.LinearAdapter;
import rocks.tbog.tblauncher.ui.ListPopup;

public class WidgetManager {
    private static final String TAG = "Wdg";
    private AppWidgetManager mAppWidgetManager;
    private WidgetHost mAppWidgetHost;
    private ViewGroup mLayout;
    private final ArrayList<WidgetRecord> mWidgets = new ArrayList<>(0);
    private static final int APPWIDGET_HOST_ID = 1337;
    private static final int REQUEST_PICK_APPWIDGET = 101;
    private static final int REQUEST_CREATE_APPWIDGET = 102;

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

        restoreWidgets();
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

        {
            ArrayList<WidgetRecord> widgets = DBHelper.getWidgets(mLayout.getContext());
            mWidgets.clear();
            mWidgets.addAll(widgets);
        }

        // sync DB with AppWidgetHost
        for (int appWidgetId : appWidgetIds) {
            boolean foundInDB = false;
            for (WidgetRecord rec : mWidgets) {
                if (rec.appWidgetId == appWidgetId) {
                    foundInDB = true;
                    break;
                }
            }
            if (!foundInDB) {
                // remove zombie widget
                removeWidget(appWidgetId);
            }
        }

        // restore widgets
        for (WidgetRecord rec : mWidgets) {
            restoreWidget(rec);
        }
    }

    private void restoreWidget(WidgetRecord rec) {
        final int appWidgetId = rec.appWidgetId;
        Context ctx = mLayout.getContext().getApplicationContext();
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetInfo == null)
            return;
        AppWidgetHostView hostView = mAppWidgetHost.createView(ctx, appWidgetId, appWidgetInfo);

        addWidgetToLayout(hostView, appWidgetInfo, rec);
    }

    private void addWidgetToLayout(AppWidgetHostView hostView, AppWidgetProviderInfo appWidgetInfo, WidgetRecord rec) {
        ViewGroup.LayoutParams params = new LinearLayout.LayoutParams(rec.width, rec.height);
        hostView.setLayoutParams(params);
        hostView.setMinimumWidth(rec.width);
        hostView.setMinimumHeight(rec.height);

        hostView.setAppWidget(rec.appWidgetId, appWidgetInfo);
        hostView.updateAppWidgetSize(null, rec.width, rec.height, rec.width, rec.height);

//        hostView.setOnClickListener();
//        hostView.setOnLongClickListener();

        mLayout.addView(hostView);
    }

    public boolean usingActivity(Activity activity) {
        return activity.findViewById(R.id.widgetContainer) == mLayout;
    }

    /**
     * Launches the menu to select the widget. The selected widget will be on
     * the result of the activity.
     */
    public void selectWidget(Activity activity) {
        int appWidgetId = this.mAppWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK);
        pickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        addEmptyData(pickIntent);
        activity.startActivityForResult(pickIntent, REQUEST_PICK_APPWIDGET);
    }

    /**
     * This avoids a bug in the com.android.settings.AppWidgetPickActivity,
     * which is used to select widgets. This just adds empty extras to the
     * intent, avoiding the bug.
     * <p>
     * See more: http://code.google.com/p/android/issues/detail?id=4272
     */
    private void addEmptyData(Intent pickIntent) {
        ArrayList<AppWidgetProviderInfo> customInfo = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_INFO, customInfo);
        ArrayList<Bundle> customExtras = new ArrayList<>();
        pickIntent.putParcelableArrayListExtra(AppWidgetManager.EXTRA_CUSTOM_EXTRAS, customExtras);
    }

    /**
     * Checks if the widget needs any configuration. If it needs, launches the
     * configuration activity.
     */
    private void configureWidget(Activity activity, Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);

//        Bundle opts = new Bundle();
//        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, maxWidth);
//        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, minHeight);
//        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, maxWidth);
//        opts.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, maxHeight);

        boolean hasPermission = mAppWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, appWidgetInfo.provider);
        if (!hasPermission) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_BIND);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, appWidgetInfo.provider);
            activity.startActivityForResult(intent, REQUEST_PICK_APPWIDGET/*REQUEST_BIND*/);
        }

        if (appWidgetInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            activity.startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        } else {
            createWidget(activity, data);
        }
    }

    /**
     * Creates the widget and adds it to our view layout.
     */
    public void createWidget(Activity activity, Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = mAppWidgetHost.createView(activity.getApplicationContext(), appWidgetId, appWidgetInfo);

        WidgetRecord rec = new WidgetRecord();
        rec.appWidgetId = appWidgetId;
        rec.width = appWidgetInfo.minWidth;
        rec.height = appWidgetInfo.minHeight;

        DBHelper.addWidget(activity, rec);
        mWidgets.add(rec);

        addWidgetToLayout(hostView, appWidgetInfo, rec);
    }

    /**
     * Removes the widget displayed by this AppWidgetHostView.
     */
    public void removeWidget(AppWidgetHostView hostView) {
        final int appWidgetId = hostView.getAppWidgetId();
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        mLayout.removeView(hostView);
        DBHelper.removeWidget(mLayout.getContext(), appWidgetId);
        for (Iterator<WidgetRecord> iterator = mWidgets.iterator(); iterator.hasNext(); ) {
            WidgetRecord rec = iterator.next();
            if (rec.appWidgetId == appWidgetId)
                iterator.remove();
        }
    }

    public void removeWidget(int appWidgetId) {
        int childCount = mLayout.getChildCount();
        for (int child = 0; child < childCount; child += 1) {
            View view = mLayout.getChildAt(child);
            if (view instanceof AppWidgetHostView) {
                int viewAppWidgetId = ((AppWidgetHostView) view).getAppWidgetId();
                if (viewAppWidgetId == appWidgetId) {
                    removeWidget((AppWidgetHostView) view);
                    return;
                }
            }
        }
        // if we reach this point then the layout does not have the widget
        mAppWidgetHost.deleteAppWidgetId(appWidgetId);
        DBHelper.removeWidget(mLayout.getContext(), appWidgetId);
        for (Iterator<WidgetRecord> iterator = mWidgets.iterator(); iterator.hasNext(); ) {
            WidgetRecord rec = iterator.next();
            if (rec.appWidgetId == appWidgetId)
                iterator.remove();
        }
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
    public boolean onActivityResult(Activity activity, int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(activity, data);
                return true;
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidget(activity, data);
                return true;
            }
        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                removeWidget(appWidgetId);
                return true;
            }
        }
        return false;
    }

    public int widgetCount() {
        return mWidgets.size();
    }

    public ListPopup getRemoveWidgetPopup() {
        Context ctx = mLayout.getContext();
        LinearAdapter adapter = new LinearAdapter();

        adapter.add(new LinearAdapter.ItemTitle(ctx, R.string.menu_widget_remove));

        for (WidgetRecord rec : mWidgets) {
            AppWidgetProviderInfo info = mAppWidgetManager.getAppWidgetInfo(rec.appWidgetId);
            if (info == null)
                continue;
            String name;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                name = info.loadLabel(ctx.getPackageManager());
            } else {
                name = info.label;
            }
            adapter.add(new WidgetPopupItem(name, rec.appWidgetId));
        }

        ListPopup menu = ListPopup.create(ctx, adapter);
        menu.setOnItemClickListener((a, v, pos) -> {
            Object item = a.getItem(pos);
            if (item instanceof WidgetPopupItem) {
                removeWidget(((WidgetPopupItem) item).appWidgetId);
            }
        });
        return menu;
    }

    public ListPopup getConfigPopup(Activity activity) {
        LinearAdapter adapter = new LinearAdapter();

        adapter.add(new LinearAdapter.Item(activity, R.string.menu_widget_add));
        if (widgetCount() > 0)
            adapter.add(new LinearAdapter.Item(activity, R.string.menu_widget_remove));

        ListPopup menu = ListPopup.create(activity, adapter);
        menu.setOnItemClickListener((a, v, pos) -> {
            LinearAdapter.MenuItem item = ((LinearAdapter) a).getItem(pos);
            @StringRes int stringId = 0;
            if (item instanceof LinearAdapter.Item) {
                stringId = ((LinearAdapter.Item) a.getItem(pos)).stringId;
            }
            switch (stringId) {
                case R.string.menu_widget_add:
                    TBApplication.widgetManager(activity).selectWidget(activity);
                    break;
                case R.string.menu_widget_remove: {
                    ListPopup removeWidgetPopup = TBApplication.widgetManager(activity).getRemoveWidgetPopup();
                    TBApplication.behaviour(activity).registerPopup(removeWidgetPopup);
                    removeWidgetPopup.showCenter(activity.getWindow().getDecorView());
                    break;
                }
            }
        });
        return menu;
    }

    static class WidgetPopupItem extends LinearAdapter.ItemString {
        int appWidgetId;

        public WidgetPopupItem(@NonNull String string, int appWidgetId) {
            super(string);
            this.appWidgetId = appWidgetId;
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

    static class WidgetView extends AppWidgetHostView {
        private OnClickListener mOnClickListener = null;
        private OnLongClickListener mOnLongClickListener = null;
        private static final int longPressTimeout = ViewConfiguration.getLongPressTimeout();

        public WidgetView(Context context) {
            super(context);
        }

        @Override
        public void setOnLongClickListener(@Nullable OnLongClickListener listener) {
            setLongClickable(true);
            mOnLongClickListener = listener;
        }

        @Override
        public void setOnClickListener(@Nullable OnClickListener listener) {
            setClickable(true);
            mOnClickListener = listener;
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent event) {
            View view = this;
            int act = event.getActionMasked();
            if (view.isPressed() && act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_MOVE) {
                long eventDuration = event.getEventTime() - event.getDownTime();
                if (eventDuration > longPressTimeout) {
                    if (mOnLongClickListener != null && mOnLongClickListener.onLongClick(view))
                    {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        return true;
                    }
                } else {
                    if (mOnClickListener != null) {
                        mOnClickListener.onClick(view);
                        return true;
                    }
                }
            }
            return super.onInterceptTouchEvent(event);
        }
    }
}
