package com.example.onlydate

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Foreground Service that dynamically registers a BroadcastReceiver for ACTION_USER_PRESENT.
 * This is required for Android 8.0+ where static registration in AndroidManifest doesn't work
 * for implicit broadcasts like ACTION_USER_PRESENT.
 */
class WidgetUpdateService : Service() {

    private val unlockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            Logger.d(TAG, "[$timestamp] Received broadcast: ${intent.action}")
            
            if (intent.action == Intent.ACTION_USER_PRESENT) {
                Logger.d(TAG, "[$timestamp] ✓ Device unlocked, updating widgets")
                updateAllWidgets(context)
            } else {
                Logger.d(TAG, "[$timestamp] Ignoring non-unlock action: ${intent.action}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        Logger.d(TAG, "[$timestamp] ═══ WidgetUpdateService onCreate ═══")
        Logger.d(TAG, "[$timestamp] Android API: ${Build.VERSION.SDK_INT}")
        
        // Log battery optimization status
        logBatteryOptimizationStatus()

        // Start as foreground service (required for Android 8.0+)
        startForegroundService()

        // Register the broadcast receiver dynamically
        registerUnlockReceiver()
        
        Logger.d(TAG, "[$timestamp] Service initialization complete")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        Logger.d(TAG, "[$timestamp] onStartCommand called (startId=$startId)")
        // START_STICKY: サービスが停止されても再起動を試みる
        return START_STICKY
    }
    
    override fun onTaskRemoved(intent: Intent?) {
        super.onTaskRemoved(intent)
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        Logger.w(TAG, "[$timestamp] ⚠ Task removed - attempting service restart")
        
        try {
            // サービス再起動を試みる
            val restartIntent = Intent(applicationContext, WidgetUpdateService::class.java)
            val pendingIntent = PendingIntent.getService(
                applicationContext,
                0,
                restartIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
            val restartTime = System.currentTimeMillis() + 1000 // 1秒後
            alarmManager.set(android.app.AlarmManager.RTC_WAKEUP, restartTime, pendingIntent)
            Logger.d(TAG, "[$timestamp] ✓ Service restart scheduled")
        } catch (e: Exception) {
            Logger.e(TAG, "[$timestamp] ✗ Failed to schedule service restart", e)
            // 再起動失敗してもAlarmManagerとWorkManagerが動く
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        Logger.d(TAG, "[$timestamp] ═══ WidgetUpdateService onDestroy ═══")

        // Unregister the receiver to prevent memory leaks
        try {
            unregisterReceiver(unlockReceiver)
            Logger.d(TAG, "[$timestamp] ✓ Receiver unregistered successfully")
        } catch (e: IllegalArgumentException) {
            // Receiver was already unregistered
            Logger.w(TAG, "[$timestamp] ✗ Receiver already unregistered", e)
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
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        startForeground(NOTIFICATION_ID, notification)
        Logger.d(TAG, "[$timestamp] ✓ Started as foreground service")
    }

    private fun registerUnlockReceiver() {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val filter = IntentFilter(Intent.ACTION_USER_PRESENT)
        Logger.d(TAG, "[$timestamp] Registering receiver for: ${Intent.ACTION_USER_PRESENT}")

        try {
            // Android 14+ (API 34) requires RECEIVER_NOT_EXPORTED flag
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(unlockReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
                Logger.d(TAG, "[$timestamp] ✓ Registered with RECEIVER_NOT_EXPORTED (API ${Build.VERSION.SDK_INT})")
            } else {
                registerReceiver(unlockReceiver, filter)
                Logger.d(TAG, "[$timestamp] ✓ Registered without flags (API ${Build.VERSION.SDK_INT})")
            }
        } catch (e: Exception) {
            Logger.e(TAG, "[$timestamp] ✗ Failed to register receiver", e)
        }
    }
    
    private fun logBatteryOptimizationStatus() {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val isIgnoringOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
            Logger.d(TAG, "[$timestamp] Battery optimization ignored: $isIgnoringOptimizations")
            
            if (!isIgnoringOptimizations) {
                Logger.w(TAG, "[$timestamp] ⚠ App is subject to battery optimization - may affect background service")
            }
        }
    }

    private fun updateAllWidgets(context: Context) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, DateWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        Logger.d(TAG, "[$timestamp] Found ${appWidgetIds.size} widget(s) to update")
        
        if (appWidgetIds.isNotEmpty()) {
            // Trigger update through the provider
            val updateIntent = Intent(context, DateWidgetProvider::class.java).apply {
                action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            }
            context.sendBroadcast(updateIntent)
            Logger.d(TAG, "[$timestamp] ✓ Broadcast sent to update widgets")
        } else {
            Logger.d(TAG, "[$timestamp] No widgets to update")
        }
    }

    companion object {
        private const val TAG = "OnlyDate"
        private const val CHANNEL_ID = "widget_service_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
