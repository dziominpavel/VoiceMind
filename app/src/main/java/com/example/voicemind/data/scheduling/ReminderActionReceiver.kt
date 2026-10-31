package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.voicemind.data.DismissBehavior
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.notification.AlarmSoundPlayer
import com.example.voicemind.data.notification.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L) ?: return
        if (reminderId < 0) return

        val pendingResult = goAsync()
        val repo = ReminderRepository.getInstance(context)
        val notifier = ReminderNotifier(context)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ReminderIntents.ACTION_DONE -> repo.dismissReminder(reminderId)
                    ReminderIntents.ACTION_COMPLETE -> repo.completeReminder(reminderId)
                    ReminderIntents.ACTION_SNOOZE -> {
                        val minutes = intent.getIntExtra(
                            ReminderIntents.EXTRA_SNOOZE_MINUTES,
                            ReminderRepository.SNOOZE_MINUTES,
                        )
                        repo.snoozeReminder(reminderId, minutes)
                    }
                    ReminderIntents.ACTION_CANCEL -> repo.cancelReminder(reminderId)
                    ReminderIntents.ACTION_DISMISS -> {
                        val behavior = SettingsRepository.getInstance(context).dismissBehavior.first()
                        when (behavior) {
                            DismissBehavior.MARK_DONE -> repo.dismissReminder(reminderId)
                            DismissBehavior.SNOOZE_15 -> repo.snoozeReminder(reminderId, 15)
                        }
                    }
                }
                notifier.cancelNotification(reminderId)
            } catch (e: Exception) {
                Log.e(TAG, "Reminder action ${intent.action} failed for id=$reminderId", e)
            } finally {
                AlarmSoundPlayer.stop(context)
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderActionReceiver"
    }
}
