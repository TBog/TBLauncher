package rocks.tbog.tblauncher.widgets;

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
                int sizeX = UISizes.px2dp(text1.getContext(), info.appWidgetInfo.minWidth) / 40;
                int sizeY = UISizes.px2dp(text1.getContext(), info.appWidgetInfo.minHeight) / 40;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (info.appWidgetInfo.targetCellWidth != 0 && info.appWidgetInfo.targetCellHeight != 0) {
                        sizeX = info.appWidgetInfo.targetCellWidth;
                        sizeY = info.appWidgetInfo.targetCellHeight;
                    }
                }
                final String html;
                if (info.widgetDesc != null) {
                    html = text1.getResources().getString(R.string.widget_name_and_desc, info.widgetName, info.widgetDesc, sizeX, sizeY);
                } else {
                    html = text1.getResources().getString(R.string.widget_name, info.widgetName, sizeX, sizeY);
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
