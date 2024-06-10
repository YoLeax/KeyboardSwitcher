package com.kunzisoft.keyboard.switcher;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.widget.RemoteViews;

public class KeyboardWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int widgetId : appWidgetIds) {
            RemoteViews remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.icon_widget);

            remoteViews.setOnClickPendingIntent(
                    R.id.icon_widget_view,
                    KeyboardManagerActivity.getPendingIntent(context)
            );
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }
    }
}
