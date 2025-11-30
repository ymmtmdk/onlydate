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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import org.json.JSONObject

class DateWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        val pendingResult = goAsync()
        Thread {
            try {
                val showTemp = WidgetConfigActivity.loadShowTemp(context)
                val temp = if (showTemp) fetchTemperature(context) else null
                updateAppWidget(context, appWidgetManager, appWidgetIds, temp)
            } catch (e: Exception) {
                e.printStackTrace()
                updateAppWidget(context, appWidgetManager, appWidgetIds)
            } finally {
                pendingResult.finish()
            }
        }.start()
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
        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray, temp: Double? = null) {
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, temp)
            }
        }

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, temp: Double? = null) {
            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            // Load global settings (no appWidgetId needed)
            val textColor = WidgetConfigActivity.loadTextColor(context)
            val bgColor = WidgetConfigActivity.loadBgColor(context)
            val bgOpacity = WidgetConfigActivity.loadBgOpacity(context)
            val showYear = WidgetConfigActivity.loadShowYear(context)
            val showDayOfWeek = WidgetConfigActivity.loadShowDayOfWeek(context)
            val showTemp = WidgetConfigActivity.loadShowTemp(context)
            val language = WidgetConfigActivity.loadLanguage(context)

            views.setTextColor(R.id.widget_text, textColor)

            val finalBgColor = Color.argb(bgOpacity, Color.red(bgColor), Color.green(bgColor), Color.blue(bgColor))
            views.setInt(R.id.widget_container, "setBackgroundColor", finalBgColor)

            val date = Date()
            val dateFormatString = if (showYear) "yyyy/MM/dd" else "MM/dd"
            val sdfDate = SimpleDateFormat(dateFormatString, Locale.getDefault())
            val dateString = sdfDate.format(date)

            val finalText = if (showDayOfWeek) {
                val locale = when (language) {
                    WidgetConfigActivity.LANG_ENGLISH -> Locale.ENGLISH
                    WidgetConfigActivity.LANG_JAPANESE -> Locale.JAPAN
                    else -> Locale.getDefault()
                }
                
                // Use "EEE" for abbreviated day name (e.g., Mon, 月)
                val sdfDay = SimpleDateFormat("EEE", locale)
                val dayString = sdfDay.format(date)
                "$dateString ($dayString)"
            } else {
                dateString
            }

            val displayTemp = if (showTemp) {
                temp ?: loadLastTemp(context)
            } else {
                null
            }
            val textWithTemp = if (displayTemp != null) {
                "$finalText $displayTemp°C"
            } else {
                finalText
            }

            views.setTextViewText(R.id.widget_text, textWithTemp)

            // Clicking the widget opens the configuration activity
            val intent = Intent(context, WidgetConfigActivity::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            val pendingIntent = PendingIntent.getActivity(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private const val PREF_LAST_TEMP_KEY = "last_temp"

        private fun fetchTemperature(context: Context): Double? {
            return try {
                val url = URL("https://my-worker-dev.tmtfctry.workers.dev/")
                val connection = url.openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val stream = connection.getInputStream()
                val reader = BufferedReader(InputStreamReader(stream))
                val response = reader.readText()
                reader.close()
                
                val json = JSONObject(response)
                val tempArray = json.getJSONArray("temp")
                val temp = tempArray.getDouble(0)
                
                saveLastTemp(context, temp)
                temp
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        private fun saveLastTemp(context: Context, temp: Double) {
            val prefs = context.getSharedPreferences(WidgetConfigActivity.PREFS_NAME, 0).edit()
            prefs.putFloat(PREF_LAST_TEMP_KEY, temp.toFloat())
            prefs.apply()
        }

        private fun loadLastTemp(context: Context): Double? {
            val prefs = context.getSharedPreferences(WidgetConfigActivity.PREFS_NAME, 0)
            if (!prefs.contains(PREF_LAST_TEMP_KEY)) return null
            return prefs.getFloat(PREF_LAST_TEMP_KEY, 0f).toDouble()
        }
    }
}