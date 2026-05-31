package com.example.voicemind.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.data.notification.AlarmSoundPlayer
import kotlinx.coroutines.runBlocking

class WidgetToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        runBlocking {
            if (intent.action != ACTION_TOGGLE) return@runBlocking

            val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L).takeIf { it != -1L } ?: run {
                Log.w(TAG, "No reminderId in intent")
                return@runBlocking
            }
            val currentStatus = intent.getStringExtra(EXTRA_CURRENT_STATUS) ?: run {
                Log.w(TAG, "No currentStatus in intent")
                return@runBlocking
            }
            Log.d(TAG, "Toggling reminder $id, current=$currentStatus")

            val repo = ReminderRepository.getInstance(context)
            val reminder = repo.getById(id) ?: run {
                Log.w(TAG, "Reminder $id not found")
                return@runBlocking
            }

            if (currentStatus == ReminderStatus.PENDING.name) {
                repo.completeReminder(id)
                Log.d(TAG, "Completed reminder $id")
            } else {
                val now = System.currentTimeMillis()
                val newFireAt = reminder.fireAt.takeIf { it > now } ?: (now + 3_600_000L)
                repo.updateAndSchedule(
                    reminder.copy(
                        fireAt = newFireAt,
                        status = ReminderStatus.PENDING.name,
                        snoozeCount = 0,
                    ),
                )
                Log.d(TAG, "Reactivated reminder $id, newFireAt=$newFireAt")
            }

            AlarmSoundPlayer.stop(context)
            WidgetUpdater.updateAll(context)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.voicemind.ACTION_WIDGET_TOGGLE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_CURRENT_STATUS = "extra_current_status"
        private const val TAG = "WidgetToggleReceiver"
    }
}
