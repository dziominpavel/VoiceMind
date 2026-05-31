package com.example.voicemind.ui.widget

import android.content.Context
import android.util.Log
import com.example.voicemind.data.AppDatabase
import com.example.voicemind.data.ReminderStatus

object WidgetReminderProvider {

    private const val TAG = "WidgetReminderProvider"

    suspend fun loadItems(context: Context): List<WidgetReminderItem> {
        val dao = AppDatabase.getInstance(context).reminderDao()
        val cutoff = System.currentTimeMillis() - 30 * 60_000L
        val recentDoneAll = dao.getWidgetRecentDone(cutoff = cutoff, limit = 5)
        val upcomingAll = dao.getWidgetUpcoming(limit = 5)
        Log.d(TAG, "DAO: upcoming=${upcomingAll.size}, recentDone=${recentDoneAll.size}, cutoff=$cutoff")

        val recentDone = recentDoneAll.take(3)
        val upcoming = upcomingAll.take(3)

        val merged = when {
            upcoming.isNotEmpty() && recentDone.isNotEmpty() ->
                (listOf(recentDone.first()) + upcoming).take(3)
            upcoming.isNotEmpty() -> upcoming.take(3)
            else -> recentDone.take(3)
        }

        Log.d(TAG, "Merged result: ${merged.size} items")
        return merged.map { reminder ->
            WidgetReminderItem(
                id = reminder.id,
                body = reminder.body,
                fireAt = reminder.fireAt,
                isDone = reminder.status != ReminderStatus.PENDING.name,
                status = reminder.status,
            )
        }
    }

    data class WidgetReminderItem(
        val id: Long,
        val body: String,
        val fireAt: Long,
        val isDone: Boolean,
        val status: String,
    )
}
