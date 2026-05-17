package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.notification.ReminderNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                    ReminderIntents.ACTION_SNOOZE -> repo.snoozeReminder(reminderId)
                    ReminderIntents.ACTION_CANCEL -> repo.cancelReminder(reminderId)
                }
                notifier.cancelNotification(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
