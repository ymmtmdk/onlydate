package com.example.onlydate

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.*

class DateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            val prefs = context.getSharedPreferences(WidgetConfigActivity.PREFS_NAME, 0).edit()
            prefs.remove(WidgetConfigActivity.PREF_TEXT_COLOR_KEY + appWidgetId)
            prefs.remove(WidgetConfigActivity.PREF_BG_COLOR_KEY + appWidgetId)
            prefs.remove(WidgetConfigActivity.PREF_BG_OPACITY_KEY + appWidgetId)
            prefs.remove(WidgetConfigActivity.PREF_SHOW_DOW_KEY + appWidgetId)
            prefs.apply()
        }
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            val textColor = WidgetConfigActivity.loadTextColor(context, appWidgetId)
            val bgColor = WidgetConfigActivity.loadBgColor(context, appWidgetId)
            val bgOpacity = WidgetConfigActivity.loadBgOpacity(context, appWidgetId)
            val showDayOfWeek = WidgetConfigActivity.loadShowDayOfWeek(context, appWidgetId)

            views.setTextColor(R.id.widget_date, textColor)
            views.setTextColor(R.id.widget_day_of_week, textColor)

            val finalBgColor = Color.argb(bgOpacity, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            views.setInt(R.id.widget_container, "setBackgroundColor", finalBgColor)

            val date = Date()
            val sdfDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
            views.setTextViewText(R.id.widget_date, sdfDate.format(date))

            if (showDayOfWeek) {
                views.setViewVisibility(R.id.widget_day_of_week, View.VISIBLE)
                val sdfDay = SimpleDateFormat("EEE", Locale.getDefault())
                views.setTextViewText(R.id.widget_day_of_week, sdfDay.format(date))
            } else {
                views.setViewVisibility(R.id.widget_day_of_week, View.GONE)
            }

            val intent = Intent(context, WidgetConfigActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}