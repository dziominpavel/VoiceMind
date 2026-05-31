package com.example.voicemind.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.voicemind.MainActivity
import com.example.voicemind.R

class MicWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_mic)
            val intent = Intent(context, MainActivity::class.java).apply {
                action = WidgetActions.ACTION_START_VOICE
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pending = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_mic_root, pending)
            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
        }
    }
}
