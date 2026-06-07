package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
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
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val repo = ReminderRepository.getInstance(context)
                val reminder = repo.getById(reminderId)
                if (reminder == null || reminder.status != ReminderStatus.PENDING.name) {
                    return@launch
                }

                val settings = SettingsRepository.getInstance(context)
                val deliveryMode = settings.getDefaultDeliveryMode()
                if (deliveryMode == DeliveryMode.ALARM || deliveryMode == DeliveryMode.VIBRATE) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                        "VoiceMind:AlarmWakeLock",
                    )
                    wakeLock.acquire(10_000L)
                }

                if (deliveryMode == DeliveryMode.ALARM) {
                    val customUri = settings.alarmRingtoneUri.first()
                    val volume = settings.alarmVolume.first()
                    val useVibration = settings.useVibration.first()
                    AlarmSoundPlayer.play(context, customUri, volume, useVibration)
                } else if (deliveryMode == DeliveryMode.VIBRATE) {
                    AlarmSoundPlayer.playVibrationOnly(context)
                }

                repo.markFiredAndShow(reminderId)
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                pendingResult.finish()
            }
        }
    }
}
