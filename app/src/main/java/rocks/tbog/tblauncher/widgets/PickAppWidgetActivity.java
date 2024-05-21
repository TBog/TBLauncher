package rocks.tbog.tblauncher.widgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.normalizer.StringNormalizer;
import rocks.tbog.tblauncher.utils.FuzzyScore;

public class PickAppWidgetActivity extends AppCompatActivity {
    private static final String TAG = "PickAppWidget";
    private TextView mSearch;
    View widgetLoadingGroup;
    WidgetListAdapter adapter;
    LoadWidgetsAsync loadWidgetsAsync = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_picker);

        final Context context = getApplicationContext();
        final ListView list = findViewById(android.R.id.list);
        adapter = new WidgetListAdapter();
        widgetLoadingGroup = findViewById(R.id.widgetLoadingGroup);

        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> {
            Object item = parent.getAdapter().getItem(position);
            WidgetInfo info = null;
            if (item instanceof ItemWidget)
                info = ((ItemWidget) item).info;
            if (info == null)
                return;
            Intent intent = getIntent();
            var appWidgetManager = AppWidgetManager.getInstance(context);
            int appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, WidgetManager.INVALID_WIDGET_ID);
            if (appWidgetId != WidgetManager.INVALID_WIDGET_ID) {
                boolean bindAllowed;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.appWidgetInfo.getProfile(), info.appWidgetInfo.provider, null);
                } else {
                    bindAllowed = appWidgetManager.bindAppWidgetIdIfAllowed(appWidgetId, info.appWidgetInfo.provider);
                }
                intent.putExtra(WidgetManager.EXTRA_WIDGET_BIND_ALLOWED, bindAllowed);
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.appWidgetInfo.provider);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.appWidgetInfo.getProfile());
                }
                setResult(RESULT_OK, intent);
            } else {
                setResult(RESULT_CANCELED, intent);
            }
            finish();
        });

        // set page search bar
        mSearch = findViewById(R.id.search);
        mSearch.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // Auto left-trim text.
                if (s.length() > 0 && s.charAt(0) == ' ')
                    s.delete(0, 1);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mSearch.post(() -> refreshList());
            }
        });
        mSearch.requestFocus();
        refreshList();
    }

    private synchronized void refreshList() {
        final Context context = getApplicationContext();
        widgetLoadingGroup.setVisibility(View.VISIBLE);

        if (loadWidgetsAsync != null)
            loadWidgetsAsync.cancel(false);

        loadWidgetsAsync = adapter.newLoadAsyncList(LoadWidgetsAsync.class, () -> {
            // get widget list
            var widgetList = getWidgetList(context);

            var text = mSearch.getText();
            if (text.length() > 0) {
                StringNormalizer.Result normalized = StringNormalizer.normalizeWithResult(text, true);
                FuzzyScore fuzzyScore = new FuzzyScore(normalized.codePoints);
                for (Iterator<WidgetInfo> iterator = widgetList.iterator(); iterator.hasNext(); ) {
                    WidgetInfo widgetInfo = iterator.next();
                    var matchAppName = !TextUtils.isEmpty(widgetInfo.appName) && fuzzyScore.match(widgetInfo.appName).match;
                    var matchWidgetName = !TextUtils.isEmpty(widgetInfo.widgetName) && fuzzyScore.match(widgetInfo.widgetName).match;
                    var matchDescription = !TextUtils.isEmpty(widgetInfo.widgetDesc) && fuzzyScore.match(widgetInfo.widgetDesc).match;

                    if (!matchAppName && !matchWidgetName && !matchDescription)
                        iterator.remove();
                }
            }

            // sort list
            Collections.sort(widgetList, Comparator.comparing(o -> o.appName));

            //StringBuilder dbgList = new StringBuilder();
            // assuming the list is sorted by apps, add titles with app name
            ArrayList<MenuItem> adapterList = new ArrayList<>(widgetList.size());
            String lastApp = null;
            for (var item : widgetList) {
                if (!item.appName.equals(lastApp)) {
                    //dbgList
                    //    .append("\napp=`")
                    //    .append(item.appName)
                    //    .append("`");

                    lastApp = item.appName;
                    adapterList.add(new ItemTitle(item.appName));
                }
                //dbgList
                //    .append("\n\twidget=`")
                //    .append(item.widgetName)
                //    .append("`\n\t\tdesc=`")
                //    .append(item.widgetDesc)
                //    .append("`");
                adapterList.add(new ItemWidget(item));
            }
            //Log.d(TAG, dbgList.toString());
            Log.d(TAG, "list size=" + adapterList.size());
            return adapterList;
        });

        if (loadWidgetsAsync != null) {
            loadWidgetsAsync.whenDone = () -> {
                widgetLoadingGroup.setVisibility(View.GONE);
                synchronized (PickAppWidgetActivity.this) {
                    loadWidgetsAsync = null;
                }
            };
            loadWidgetsAsync.execute();
        } else {
            finish();
            Toast.makeText(context, R.string.add_widget_failed, Toast.LENGTH_LONG).show();
        }
    }

    @WorkerThread
    private static ArrayList<WidgetInfo> getWidgetList(@NonNull Context context) {
        var appWidgetManager = AppWidgetManager.getInstance(context);
        var installedProviders = appWidgetManager.getInstalledProviders();
        var infoArrayList = new ArrayList<WidgetInfo>(installedProviders.size());
        var packageManager = context.getPackageManager();
        for (AppWidgetProviderInfo providerInfo : installedProviders) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                if (providerInfo.widgetFeatures == AppWidgetProviderInfo.WIDGET_FEATURE_HIDE_FROM_PICKER) {
                    // widget is hidden
                    continue;
                }
            }
            if ((providerInfo.widgetCategory & (AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN | AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX)) == 0) {
                // widget is not for home screen usage
                continue;
            }
            // get widget name
            String label = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                label = providerInfo.loadLabel(packageManager);
            }
            if (label == null) {
                label = providerInfo.label;
            }
            if (label == null)
                label = providerInfo.provider.flattenToShortString();

            // get widget description
            String description = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                var desc = providerInfo.loadDescription(context);
                if (desc != null)
                    description = desc.toString();
            }

            // it's useless to have the description the same as the label
            if (label.equals(description))
                description = null;

            String appName = providerInfo.provider.getPackageName();
            try {
                var appInfo = packageManager.getApplicationInfo(providerInfo.provider.getPackageName(), 0);
                appName = appInfo.loadLabel(packageManager).toString();
            } catch (Exception e) {
                Log.e(TAG, "get `" + providerInfo.provider.getPackageName() + "` label");
            }
            infoArrayList.add(new WidgetInfo(appName, label, description, providerInfo));
        }
        return infoArrayList;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, getIntent());
        super.onBackPressed();
    }

}
