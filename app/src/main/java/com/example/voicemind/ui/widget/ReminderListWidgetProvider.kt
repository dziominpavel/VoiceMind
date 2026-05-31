package com.example.voicemind.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.view.View
import android.widget.RemoteViews
import com.example.voicemind.MainActivity
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import kotlinx.coroutines.runBlocking

class ReminderListWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        android.util.Log.d(TAG, "onUpdate ids=${appWidgetIds.toList()}")
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        private const val TAG = "ReminderListWidgetProvider"

        private val ROW_IDS = listOf(R.id.row_1, R.id.row_2, R.id.row_3)
        private val BODY_IDS = listOf(R.id.body_1, R.id.body_2, R.id.body_3)
        private val TIME_IDS = listOf(R.id.time_1, R.id.time_2, R.id.time_3)
        private val CHECK_IDS = listOf(R.id.checkbox_1, R.id.checkbox_2, R.id.checkbox_3)

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
        ) {
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_reminder_list)

            // Mic button click → open voice input
            val micIntent = Intent(context, MainActivity::class.java).apply {
                action = WidgetActions.ACTION_START_VOICE
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val micPending = PendingIntent.getActivity(
                context, 0, micIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            remoteViews.setOnClickPendingIntent(R.id.mic_button, micPending)

            // Load reminders (3 items max)
            val items = runBlocking {
                try {
                    WidgetReminderProvider.loadItems(context)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to load widget items", e)
                    emptyList()
                }
            }

            // Fill 3 static rows
            for (i in 0..2) {
                if (i < items.size) {
                    val item = items[i]

                    // Body: done → перечёркнутый текст; time: done → "выполнено", upcoming → relative time
                    remoteViews.setTextViewText(BODY_IDS[i], item.body)
                    if (item.isDone) {
                        remoteViews.setInt(BODY_IDS[i], "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG or Paint.ANTI_ALIAS_FLAG)
                        remoteViews.setTextViewText(TIME_IDS[i], "выполнено")
                        remoteViews.setInt(TIME_IDS[i], "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                    } else {
                        remoteViews.setInt(BODY_IDS[i], "setPaintFlags", Paint.ANTI_ALIAS_FLAG)
                        val timeText = FormatUtils.formatRelativeFireAt(item.fireAt)
                        remoteViews.setTextViewText(TIME_IDS[i], timeText)
                    }
                    remoteViews.setViewVisibility(TIME_IDS[i], View.VISIBLE)

                    // Checkbox
                    val checkRes = if (item.isDone) R.drawable.ic_checkbox_checked else R.drawable.ic_checkbox_unchecked
                    remoteViews.setImageViewResource(CHECK_IDS[i], checkRes)
                    remoteViews.setViewVisibility(CHECK_IDS[i], View.VISIBLE)

                    // Body click → open reminder
                    val openIntent = Intent(context, MainActivity::class.java).apply {
                        action = WidgetActions.ACTION_OPEN_REMINDER
                        putExtra(WidgetActions.EXTRA_REMINDER_ID, item.id)
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    val openPending = PendingIntent.getActivity(
                        context, item.id.toInt(), openIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    remoteViews.setOnClickPendingIntent(BODY_IDS[i], openPending)

                    // Checkbox click → toggle status
                    val toggleIntent = Intent(context, WidgetToggleReceiver::class.java).apply {
                        action = WidgetToggleReceiver.ACTION_TOGGLE
                        putExtra(WidgetToggleReceiver.EXTRA_REMINDER_ID, item.id)
                        putExtra(WidgetToggleReceiver.EXTRA_CURRENT_STATUS, item.status)
                    }
                    val togglePending = PendingIntent.getBroadcast(
                        context, item.id.toInt(), toggleIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    remoteViews.setOnClickPendingIntent(CHECK_IDS[i], togglePending)

                    remoteViews.setViewVisibility(ROW_IDS[i], View.VISIBLE)
                } else {
                    remoteViews.setViewVisibility(ROW_IDS[i], View.GONE)
                }
            }

            // Empty state
            if (items.isEmpty()) {
                remoteViews.setTextViewText(R.id.body_1, context.getString(R.string.list_empty))
                remoteViews.setViewVisibility(R.id.time_1, View.GONE)
                remoteViews.setViewVisibility(R.id.checkbox_1, View.GONE)
                remoteViews.setViewVisibility(R.id.row_1, View.VISIBLE)
            }

            appWidgetManager.updateAppWidget(appWidgetId, remoteViews)
            android.util.Log.d(TAG, "Updated widget id=$appWidgetId with ${items.size} items")
        }
    }
}
