package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.notification.AlarmSoundPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L) ?: return
        if (reminderId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = ReminderRepository.getInstance(context)
                val reminder = repo.getById(reminderId)
                if (reminder == null || reminder.status != ReminderStatus.PENDING.name) {
                    return@launch
                }

                val deliveryMode = DeliveryMode.valueOf(reminder.deliveryMode)
                if (deliveryMode == DeliveryMode.ALARM) {
                    val settings = SettingsRepository.getInstance(context)
                    val customUri = settings.alarmRingtoneUri.first()
                    val volume = settings.alarmVolume.first()
                    AlarmSoundPlayer.play(context, customUri, volume)
                }

                repo.markFiredAndShow(reminderId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
