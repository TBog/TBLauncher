package rocks.tbog.tblauncher.widgets;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;

import rocks.tbog.tblauncher.R;
import rocks.tbog.tblauncher.utils.UISizes;
import rocks.tbog.tblauncher.utils.Utilities;
import rocks.tbog.tblauncher.utils.ViewHolderAdapter;

class ItemWidget implements MenuItem {
    private static final String TAG = ItemWidget.class.getSimpleName();
    protected final WidgetInfo info;

    public ItemWidget(@NonNull WidgetInfo info) {
        this.info = info;
    }

    @NonNull
    @Override
    public String getName() {
        return info.widgetName;
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
            final CharSequence text;
            if (content instanceof ItemWidget) {
                WidgetInfo info = ((ItemWidget) content).info;
                Context ctx = text1.getContext();

                ApplicationInfo widgetAppInfo = null;
                try {
                    final String widgetPackage = info.appWidgetInfo.provider.getPackageName();
                    widgetAppInfo = ctx.getPackageManager().getApplicationInfo(widgetPackage, 0);
                } catch (Exception e) {
                    Log.w(TAG, "widget " + info.appWidgetInfo.provider, e);
                }
                int widgetSdkVer = 0;
                if (widgetAppInfo != null) {
                    widgetSdkVer = widgetAppInfo.targetSdkVersion;
                }

                int cellX = 0;
                int cellY = 0;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    cellX = info.appWidgetInfo.targetCellWidth;
                    cellY = info.appWidgetInfo.targetCellHeight;
                }
                if (cellX == 0 || cellY == 0) {
                    if (widgetSdkVer >= android.os.Build.VERSION_CODES.S) {
                        // (73×n-16) × (118×m-16)
                        cellX = (int) ((UISizes.px2dp_float(ctx, info.appWidgetInfo.minWidth) + 16f) / 73f);
                        cellY = (int) ((UISizes.px2dp_float(ctx, info.appWidgetInfo.minHeight) + 16f) / 118f);
                    } else {
                        // Android 11 and lower
                        // 70×n−30
                        cellX = (int) ((UISizes.px2dp(ctx, info.appWidgetInfo.minWidth) + 30f) / 70f);
                        cellY = (int) ((UISizes.px2dp(ctx, info.appWidgetInfo.minHeight) + 30f) / 70f);
                    }
                }
                cellX = Math.max(1, cellX);
                cellY = Math.max(1, cellY);
                final String html;
                if (info.widgetDesc != null) {
                    html = text1.getResources().getString(R.string.widget_name_and_desc, info.widgetName, info.widgetDesc, cellX, cellY);
                } else {
                    html = text1.getResources().getString(R.string.widget_name, info.widgetName, cellX, cellY);
                }
                text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_LEGACY);
                Utilities.setIconAsync(icon, context -> WidgetManager.getWidgetPreview(context, info.appWidgetInfo));
            } else {
                text = content.getName();
                if (icon != null)
                    icon.setImageDrawable(null);
            }
            text1.setText(text);
        }
    }
}
