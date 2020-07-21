package rocks.tbog.tblauncher;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.Nullable;

import java.util.ArrayList;

public class WidgetManager {
    private static final String TAG = "Wdg";
    private AppWidgetManager mAppWidgetManager;
    private WidgetHost mAppWidgetHost;
    private ViewGroup mLayout;
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

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int[] appWidgetIds = mAppWidgetHost.getAppWidgetIds();
            for (int appWidgetId : appWidgetIds) {
                restoreWidget(appWidgetId);
            }
        }
    }

    private void restoreWidget(int appWidgetId) {
        Context ctx = mLayout.getContext().getApplicationContext();
        AppWidgetProviderInfo appWidgetInfo = mAppWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = mAppWidgetHost.createView(ctx, appWidgetId, appWidgetInfo);
        hostView.setAppWidget(appWidgetId, appWidgetInfo);

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
        hostView.setAppWidget(appWidgetId, appWidgetInfo);
        mLayout.addView(hostView);
    }

    /**
     * Removes the widget displayed by this AppWidgetHostView.
     */
    public void removeWidget(AppWidgetHostView hostView) {
        mAppWidgetHost.deleteAppWidgetId(hostView.getAppWidgetId());
        mLayout.removeView(hostView);
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
                mAppWidgetHost.deleteAppWidgetId(appWidgetId);
                return true;
            }
        }
        return false;
    }

    static class WidgetHost extends AppWidgetHost {
        public WidgetHost(Context context) {
            super(context, APPWIDGET_HOST_ID);
        }
    }
}
