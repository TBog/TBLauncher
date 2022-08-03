package rocks.tbog.tblauncher;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import java.util.ArrayList;
import java.util.Collections;

import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;
import rocks.tbog.tblauncher.utils.ViewHolderListAdapter;

public class PickAppWidgetActivity extends AppCompatActivity {
    private static final String TAG = "PickAppWidget";

    private static class WidgetInfo {
        final String appName;
        final String widgetName;
        final String widgetDesc;
        final AppWidgetProviderInfo appWidgetInfo;

        private WidgetInfo(String app, String name, String description, AppWidgetProviderInfo appWidgetInfo) {
            this.appName = app;
            this.widgetName = name;
            this.widgetDesc = description;
            this.appWidgetInfo = appWidgetInfo;
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.widget_picker);

        final Context context = getApplicationContext();
        final ListView list = findViewById(android.R.id.list);
        final WidgetListAdapter adapter = new WidgetListAdapter();
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

        final View widgetLoadingGroup = findViewById(R.id.widgetLoadingGroup);
        widgetLoadingGroup.setVisibility(View.VISIBLE);

        final ArrayList<WidgetInfo> infoArrayList = new ArrayList<>();
        Utilities.runAsync(getLifecycle(), task -> {
            var appWidgetManager = AppWidgetManager.getInstance(context);
            for (var providerInfo : appWidgetManager.getInstalledProviders()) {
                // get widget name
                String label = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    label = providerInfo.loadLabel(context.getPackageManager());
                }
                if (label == null) {
                    label = providerInfo.label;
                }

                // get widget description
                String description = null;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var desc = providerInfo.loadDescription(context);
                    if (desc != null)
                        description = desc.toString();
                }

                String appName = providerInfo.provider.getPackageName();
                try {
                    var appInfo = context.getPackageManager().getApplicationInfo(providerInfo.provider.getPackageName(), 0);
                    appName = appInfo.loadLabel(context.getPackageManager()).toString();
                } catch (Exception e) {
                    Log.e(TAG, "get `" + providerInfo.provider.getPackageName() + "` label");
                }
                infoArrayList.add(new WidgetInfo(appName, label, description, providerInfo));
            }
            Collections.sort(infoArrayList, (o1, o2) -> {
                return o1.appName.compareTo(o2.appName);
            });
        }, task -> {
            String lastApp = null;
            for (var item : infoArrayList) {
                if (!item.appName.equals(lastApp)) {
                    lastApp = item.appName;
                    adapter.addItem(new ItemTitle(item.appName));
                }
                adapter.addItem(new ItemWidget(item));
            }
            widgetLoadingGroup.setVisibility(View.GONE);
        });
    }

    private static Drawable getWidgetPreview(@NonNull Context context, @NonNull AppWidgetProviderInfo info) {
        Drawable preview = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = info.loadPreviewImage(context, context.getResources().getDisplayMetrics().densityDpi);
        }
        if (preview != null)
            return preview;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            preview = info.loadIcon(context, context.getResources().getDisplayMetrics().densityDpi);
        }
        return preview;
    }

    @Override
    public void onBackPressed() {
        setResult(RESULT_CANCELED, getIntent());
        super.onBackPressed();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private interface MenuItem {
        @NonNull
        String getName();
    }

    private static class ItemTitle implements MenuItem {
        @NonNull
        private final String name;

        private ItemTitle(@NonNull String string) {
            this.name = string;
        }

        @NonNull
        @Override
        public String getName() {
            return name;
        }
    }

    private static class ItemWidget implements MenuItem {
        private final WidgetInfo info;

        public ItemWidget(@NonNull WidgetInfo info) {
            this.info = info;
        }

        @NonNull
        @Override
        public String getName() {
            return info.widgetName;
        }
    }

    private static class WidgetListAdapter extends ViewHolderListAdapter<MenuItem, InfoViewHolder> {

        protected WidgetListAdapter() {
            super(InfoViewHolder.class, R.layout.popup_list_item_icon, new ArrayList<>());
        }

        @Override
        public boolean isEnabled(int position) {
            return !(getItem(position) instanceof ItemTitle);
        }

        @Override
        protected int getItemViewTypeLayout(int viewType) {
            if (viewType == 1)
                return R.layout.popup_title;
            return super.getItemViewTypeLayout(viewType);
        }

        public int getItemViewType(int position) {
            return getItem(position) instanceof ItemTitle ? 1 : 0;
        }

        public int getViewTypeCount() {
            return 2;
        }
    }

    public static class InfoViewHolder extends ViewHolderAdapter.ViewHolder<MenuItem> {
        TextView text1;
        ImageView icon;

        public InfoViewHolder(View view) {
            super(view);
            text1 = view.findViewById(android.R.id.text1);
            icon = view.findViewById(android.R.id.icon);
        }

        @Override
        protected void setContent(MenuItem content, int position, @NonNull ViewHolderAdapter<MenuItem, ? extends ViewHolderAdapter.ViewHolder<MenuItem>> adapter) {
            if (content instanceof ItemWidget) {
                WidgetInfo info = ((ItemWidget) content).info;
                final String text;
                if (info.widgetDesc != null) {
                    text1.setSingleLine(false);
                    text1.setMinLines(1);
                    text = text1.getResources().getString(R.string.widget_name_and_desc, info.widgetName, info.widgetDesc);
                } else {
                    text1.setSingleLine();
                    text = text1.getResources().getString(R.string.widget_name, info.widgetName);
                }
                text1.setText(HtmlCompat.fromHtml(text, HtmlCompat.FROM_HTML_MODE_LEGACY));
                Utilities.setIconAsync(icon, context -> getWidgetPreview(context, info.appWidgetInfo));
            } else {
                text1.setSingleLine();
                text1.setText(content.getName());
                if (icon != null)
                    icon.setImageDrawable(null);
            }
        }
    }
}
