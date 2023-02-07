package rocks.tbog.tblauncher.widgets;

import android.appwidget.AppWidgetProviderInfo;

class WidgetInfo {
    final String appName;
    final String widgetName;
    final String widgetDesc;
    final AppWidgetProviderInfo appWidgetInfo;

    WidgetInfo(String app, String name, String description, AppWidgetProviderInfo appWidgetInfo) {
        this.appName = app;
        this.widgetName = name;
        this.widgetDesc = description;
        this.appWidgetInfo = appWidgetInfo;
    }
}
