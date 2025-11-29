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
        // There may be multiple widgets active, so update all of them
        updateAppWidget(context, appWidgetManager, appWidgetIds)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        // With global settings, we don't delete preferences when a widget is deleted.
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId)
            }
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Load global settings (no appWidgetId needed)
            val textColor = WidgetConfigActivity.loadTextColor(context)
            val bgColor = WidgetConfigActivity.loadBgColor(context)
            val bgOpacity = WidgetConfigActivity.loadBgOpacity(context)
            val showYear = WidgetConfigActivity.loadShowYear(context)
            val showDayOfWeek = WidgetConfigActivity.loadShowDayOfWeek(context)
            val language = WidgetConfigActivity.loadLanguage(context)

            views.setTextColor(R.id.widget_date, textColor)
            views.setTextColor(R.id.widget_day_of_week, textColor)

            val finalBgColor = Color.argb(bgOpacity, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            views.setInt(R.id.widget_container, "setBackgroundColor", finalBgColor)

            val date = Date()
            val dateFormatString = if (showYear) "yyyy/MM/dd" else "MM/dd"
            val sdfDate = SimpleDateFormat(dateFormatString, Locale.getDefault())
            views.setTextViewText(R.id.widget_date, sdfDate.format(date))

            if (showDayOfWeek) {
                views.setViewVisibility(R.id.widget_day_of_week, View.VISIBLE)
                
                val locale = when (language) {
                    WidgetConfigActivity.LANG_ENGLISH -> Locale.ENGLISH
                    WidgetConfigActivity.LANG_JAPANESE -> Locale.JAPAN
                    else -> Locale.getDefault()
                }
                
                // Use "EEE" for abbreviated day name (e.g., Mon, 月)
                val sdfDay = SimpleDateFormat("EEE", locale)
                val dayString = sdfDay.format(date)
                views.setTextViewText(R.id.widget_day_of_week, "($dayString)")
            } else {
                views.setViewVisibility(R.id.widget_day_of_week, View.GONE)
            }

            // Clicking the widget opens the configuration activity
            val intent = Intent(context, WidgetConfigActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}