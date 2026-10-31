package com.example.voicemind.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun updateAll(context: Context) = withContext(Dispatchers.IO) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update MicWidget
        val micComponent = ComponentName(context, MicWidgetReceiver::class.java)
        val micIds = appWidgetManager.getAppWidgetIds(micComponent)
        Log.d(TAG, "MicWidget ids=${micIds.toList()}")
        micIds.forEach { id ->
            try {
                MicWidgetReceiver.updateWidget(context, appWidgetManager, id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update MicWidget id=$id", e)
            }
        }

        // Update ReminderListWidget
        val listComponent = ComponentName(context, ReminderListWidgetProvider::class.java)
        val listIds = appWidgetManager.getAppWidgetIds(listComponent)
        Log.d(TAG, "ReminderListWidget ids=${listIds.toList()}")
        listIds.forEach { id ->
            try {
                ReminderListWidgetProvider.updateWidget(context, appWidgetManager, id)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update ReminderListWidget id=$id", e)
            }
        }
    }
}
