package com.example.onlydate

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat

class ScreenOnService : Service() {

    private val screenOnReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_ON || intent.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "Screen event detected: ${intent.action}, updating widgets")
                updateWidgets(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        
        // Android 8.0+ requires foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("OnlyDate Widget")
                .setContentText("Monitoring screen events")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build()
            startForeground(NOTIFICATION_ID, notification)
        }
        
        val filter = IntentFilter(Intent.ACTION_SCREEN_ON)
        filter.addAction(Intent.ACTION_USER_PRESENT)
        registerReceiver(screenOnReceiver, filter)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        unregisterReceiver(screenOnReceiver)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        // Fetch temperature in background if enabled
        val showTemp = WidgetSettings.loadShowTemp(context)
        if (showTemp) {
            Thread {
                try {
                    val temp = DateWidgetProvider.fetchTemperaturePublic(context)
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds, temp)
                } catch (e: Exception) {
                    e.printStackTrace()
                    DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
                }
            }.start()
        } else {
            DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Service",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "OnlyDate widget background service"
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "ScreenOnService"
        private const val CHANNEL_ID = "onlydate_widget_service"
        private const val NOTIFICATION_ID = 1
    }
}
