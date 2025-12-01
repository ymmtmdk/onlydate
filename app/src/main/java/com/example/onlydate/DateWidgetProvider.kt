package com.example.onlydate

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import org.json.JSONArray

class DateWidgetProvider : AppWidgetProvider() {

    private fun doUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        Thread {
            try {
                val showTemp = WidgetSettings.loadShowTemp(appContext)
                val temp = if (showTemp) fetchTemperature(appContext) else null
                updateAppWidget(appContext, appWidgetManager, appWidgetIds, temp)
            } catch (e: Exception) {
                e.printStackTrace()
                updateAppWidget(appContext, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        doUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        Log.d(TAG, "Widget onReceive: ${intent.action}")
        
        // Handle ACTION_USER_PRESENT to update widget on device unlock
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d(TAG, "Device unlocked, updating widgets")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DateWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            doUpdate(context, appWidgetManager, appWidgetIds)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        // When the user deletes the widget, delete the preference associated with it.
        // With global settings, we don't delete preferences when a widget is deleted.
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        // Widget enabled - no service needed, using ACTION_USER_PRESENT broadcast
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        // Widget disabled - no service to stop
    }

    companion object {
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, temp: Double? = null) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, temp)
            }
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, temp: Double? = null) {
            // Load global settings
            val textColor = WidgetSettings.loadTextColor(context)
            val bgColor = WidgetSettings.loadBgColor(context)
            val bgOpacity = WidgetSettings.loadBgOpacity(context)
            val showYear = WidgetSettings.loadShowYear(context)
            val showDayOfWeek = WidgetSettings.loadShowDayOfWeek(context)
            val showTemp = WidgetSettings.loadShowTemp(context)
            val language = WidgetSettings.loadLanguage(context)
            val dateSizeSp = WidgetSettings.loadDateSize(context).toFloat()
            val daySizeSp = WidgetSettings.loadDaySize(context).toFloat()
            val tempSizeSp = WidgetSettings.loadTempSize(context).toFloat()

            // Prepare strings
            val date = Date()
            val dateFormatString = if (showYear) "yyyy/MM/dd" else "MM/dd"
            val sdfDate = SimpleDateFormat(dateFormatString, Locale.getDefault())
            val dateString = sdfDate.format(date)

            val dayString = if (showDayOfWeek) {
                val locale = when (language) {
                    WidgetSettings.LANG_ENGLISH -> Locale.ENGLISH
                    WidgetSettings.LANG_JAPANESE -> Locale.JAPAN
                    else -> Locale.getDefault()
                }
                val sdfDay = SimpleDateFormat("EEE", locale)
                "(" + sdfDay.format(date) + ")"
            } else {
                ""
            }

            val displayTemp = if (showTemp) {
                temp ?: WidgetSettings.loadLastTemp(context)
            } else {
                null
            }
            val tempString = if (displayTemp != null) {
                "%.1f°C".format(displayTemp)
            } else {
                ""
            }

            val layoutType = WidgetSettings.loadLayoutType(context)
            val layoutId = when (layoutType) {
                WidgetSettings.LAYOUT_HORIZONTAL -> R.layout.widget_horizontal
                WidgetSettings.LAYOUT_TWO_ROWS_TEMP -> R.layout.widget_two_rows_temp
                WidgetSettings.LAYOUT_TWO_ROWS_WEEKDAY_TEMP -> R.layout.widget_two_rows_weekday_temp
                WidgetSettings.LAYOUT_THREE_ROWS -> R.layout.widget_three_rows
                else -> R.layout.widget_horizontal
            }
            val views = RemoteViews(context.packageName, layoutId)

            val finalBgColor = Color.argb(bgOpacity, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            views.setInt(R.id.widget_container, "setBackgroundColor", finalBgColor)

            // Set text and styles
            views.setTextViewText(R.id.text_date, dateString)
            views.setTextColor(R.id.text_date, textColor)
            views.setTextViewTextSize(R.id.text_date, TypedValue.COMPLEX_UNIT_SP, dateSizeSp)

            if (showDayOfWeek) {
                views.setViewVisibility(R.id.text_weekday, View.VISIBLE)
                // Add leading space if horizontal layout or same row
                val dayText = if (layoutId == R.layout.widget_horizontal || layoutId == R.layout.widget_two_rows_temp) " $dayString" else dayString
                views.setTextViewText(R.id.text_weekday, dayText)
                views.setTextColor(R.id.text_weekday, textColor)
                views.setTextViewTextSize(R.id.text_weekday, TypedValue.COMPLEX_UNIT_SP, daySizeSp)
            } else {
                views.setViewVisibility(R.id.text_weekday, View.GONE)
            }

            if (showTemp && tempString.isNotEmpty()) {
                views.setViewVisibility(R.id.text_temp, View.VISIBLE)
                // Add leading space if horizontal layout or same row
                val tempText = if (layoutId == R.layout.widget_horizontal || layoutId == R.layout.widget_two_rows_weekday_temp) " $tempString" else tempString
                views.setTextViewText(R.id.text_temp, tempText)
                views.setTextColor(R.id.text_temp, textColor)
                views.setTextViewTextSize(R.id.text_temp, TypedValue.COMPLEX_UNIT_SP, tempSizeSp)
            } else {
                views.setViewVisibility(R.id.text_temp, View.GONE)
            }

            // Clicking the widget opens the configuration activity
            val intent = Intent(context, WidgetConfigActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private const val TAG = "OnlyDate"

        // Public wrapper for fetching temperature from WidgetConfigActivity
        internal fun fetchTemperaturePublic(context: Context): Double? {
            return fetchTemperature(context)
        }

        private fun fetchTemperature(context: Context): Double? {
            Log.d(TAG, "Fetching temperature...")
            return try {
                val url = URL("https://my-worker-dev.tmtfctry.workers.dev/46106/temp")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val stream = connection.getInputStream()
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()

                val temp = JSONArray(response).getDouble(0)
                Log.d(TAG, "Fetched temperature: $temp")
                WidgetSettings.saveLastTemp(context, temp)
                temp
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch temperature", e)
                e.printStackTrace()
                null
            }
        }
    }
}
