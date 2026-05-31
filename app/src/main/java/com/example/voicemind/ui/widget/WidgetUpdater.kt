package com.example.voicemind.ui.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.util.Log

object WidgetUpdater {

    private const val TAG = "WidgetUpdater"

    suspend fun updateAll(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        // Update MicWidget
        val micComponent = ComponentName(context, MicWidgetReceiver::class.java)
        val micIds = appWidgetManager.getAppWidgetIds(micComponent)
        Log.d(TAG, "MicWidget ids=${micIds.toList()}")
        micIds.forEach { id ->
            Log.d(TAG, "Updating MicWidget id=$id")
            MicWidgetReceiver.updateWidget(context, appWidgetManager, id)
        }

        // Update ReminderListWidget
        val listComponent = ComponentName(context, ReminderListWidgetProvider::class.java)
        val listIds = appWidgetManager.getAppWidgetIds(listComponent)
        Log.d(TAG, "ReminderListWidget ids=${listIds.toList()}")
        listIds.forEach { id ->
            Log.d(TAG, "Updating ReminderListWidget id=$id")
            ReminderListWidgetProvider.updateWidget(context, appWidgetManager, id)
        }
    }
}
