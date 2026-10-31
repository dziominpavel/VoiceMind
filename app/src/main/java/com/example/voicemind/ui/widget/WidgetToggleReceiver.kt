package com.example.voicemind.ui.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.data.notification.AlarmSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetToggleReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_TOGGLE) return

        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
        if (id == -1L) {
            Log.w(TAG, "No reminderId in intent")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ReminderRepository.getInstance(context)
                val reminder = repo.getById(id)
                if (reminder == null) {
                    Log.w(TAG, "Reminder $id not found")
                    return@launch
                }

                if (reminder.status == ReminderStatus.PENDING.name) {
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
            } catch (e: Exception) {
                Log.e(TAG, "Failed to toggle reminder $id", e)
            } finally {
                AlarmSoundPlayer.stop(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.example.voicemind.ACTION_WIDGET_TOGGLE"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        private const val TAG = "WidgetToggleReceiver"
    }
}
