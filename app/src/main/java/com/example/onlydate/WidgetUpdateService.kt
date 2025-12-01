package com.example.onlydate

import android.app.Notification
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

/**
 * Foreground Service that dynamically registers a BroadcastReceiver for ACTION_USER_PRESENT.
 * This is required for Android 8.0+ where static registration in AndroidManifest doesn't work
 * for implicit broadcasts like ACTION_USER_PRESENT.
 */
class WidgetUpdateService : Service() {

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Log.d(TAG, "Device unlocked, updating widgets")
                updateAllWidgets(context)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "WidgetUpdateService created")

        // Start as foreground service (required for Android 8.0+)
        startForegroundService()

        // Register the broadcast receiver dynamically
        registerUnlockReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "WidgetUpdateService destroyed")

        // Unregister the receiver to prevent memory leaks
        try {
            unregisterReceiver(unlockReceiver)
        } catch (e: IllegalArgumentException) {
            // Receiver was already unregistered
            Log.w(TAG, "Receiver already unregistered", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundService() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Widget Update Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps widget updated when device is unlocked"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Build notification
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Widget Active")
                .setContentText("Updating on device unlock")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Widget Active")
                .setContentText("Updating on device unlock")
                .setSmallIcon(android.R.drawable.ic_menu_today)
                .setOngoing(true)
                .build()
        }

        // Start foreground
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun registerUnlockReceiver() {
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)

        // Android 14+ (API 34) requires RECEIVER_NOT_EXPORTED flag
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            Log.d(TAG, "Registered unlock receiver with RECEIVER_NOT_EXPORTED")
        } else {
            registerReceiver(unlockReceiver, filter)
            Log.d(TAG, "Registered unlock receiver")
        }
    }

    private fun updateAllWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        if (appWidgetIds.isNotEmpty()) {
            Log.d(TAG, "Updating ${appWidgetIds.size} widget(s)")
            // Trigger update through the provider
            val updateIntent = Intent(context, DateWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
        } else {
            Log.d(TAG, "No widgets to update")
        }
    }

    companion object {
        private const val TAG = "WidgetUpdateService"
        private const val CHANNEL_ID = "widget_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
