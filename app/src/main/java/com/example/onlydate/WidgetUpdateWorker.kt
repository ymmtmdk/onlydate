package com.example.onlydate

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetUpdateWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Logger.d(TAG, "Update from WorkManager: Starting work")
            val context = applicationContext
            
            // Fetch temperature if enabled
            val showTemp = WidgetSettings.loadShowTemp(context)
            val temp = if (showTemp) {
                DateWidgetProvider.fetchTemperaturePublic(context)
            } else {
                null
            }

            // Update widgets
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, DateWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

            if (appWidgetIds.isNotEmpty()) {
                Logger.d(TAG, "Update from WorkManager: Updating ${appWidgetIds.size} widgets")
                DateWidgetProvider.updateAppWidget(context, appWidgetManager, appWidgetIds, temp)
            }

            Result.success()
        } catch (e: Exception) {
            Logger.e(TAG, "Update from WorkManager: Failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "OnlyDate"
    }
}
