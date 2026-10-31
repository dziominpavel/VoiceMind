package com.example.voicemind.data.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.RecurrenceCalculator
import com.example.voicemind.data.RecurrenceRule
import com.example.voicemind.data.ReminderRepository
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.notification.AlarmSoundPlayer
import com.example.voicemind.data.notification.ReminderNotifier
import com.example.voicemind.data.resolvedDeliveryMode
import com.example.voicemind.ui.screens.AlarmActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val reminderId = intent?.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L) ?: return
        if (reminderId < 0) return

        val settingsFallback = DeliveryMode.NOTIFICATION
        val modeFromExtras = intent.getStringExtra(ReminderIntents.EXTRA_DELIVERY_MODE)
            ?.let { runCatching { DeliveryMode.valueOf(it) }.getOrNull() }
            ?: settingsFallback

        val settings = SettingsRepository.getInstance(context)

        // Короткое чтение DataStore: нужно решить, можно ли будить экран, до
        // синхронного startActivity (Android 10+ allowlist для full-screen).
        // При таймауте считаем, что тихие часы не активны: лучше разбудить,
        // чем потерять напоминание из-за зависшего чтения.
        val quietMode = runBlocking {
            withTimeoutOrNull(1_000L) {
                settings.getQuietHours().takeIf { it.isActive() }?.mode
            }
        }

        // Sync startActivity before goAsync (Android 10+ allowlist). Only ALARM/VIBRATE.
        if (quietMode == null &&
            (modeFromExtras == DeliveryMode.ALARM || modeFromExtras == DeliveryMode.VIBRATE)
        ) {
            try {
                val alarmIntent = Intent(context, AlarmActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AlarmActivity.EXTRA_REMINDER_ID, reminderId)
                }
                context.startActivity(alarmIntent)
            } catch (e: Exception) {
                Log.e(TAG, "startActivity for AlarmActivity failed, relying on notification fallback", e)
            }
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            var wakeLock: PowerManager.WakeLock? = null
            try {
                val repo = ReminderRepository.getInstance(context)
                val reminder = repo.getById(reminderId)
                if (reminder == null || reminder.status != ReminderStatus.PENDING.name) {
                    return@launch
                }

                val fallback = settings.getDefaultDeliveryMode()
                val deliveryMode = reminder.resolvedDeliveryMode(fallback)

                // В тихие часы не будим экран и не играем звук/долгую вибрацию.
                if (quietMode == null &&
                    (deliveryMode == DeliveryMode.ALARM || deliveryMode == DeliveryMode.VIBRATE)
                ) {
                    val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                    wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK or
                            PowerManager.ACQUIRE_CAUSES_WAKEUP or
                            PowerManager.ON_AFTER_RELEASE,
                        "VoiceMind:AlarmWakeLock",
                    )
                    wakeLock.acquire(10_000L)
                }

                if (quietMode == null) {
                    if (deliveryMode == DeliveryMode.ALARM) {
                        val customUri = settings.alarmRingtoneUri.first()
                        val volume = settings.alarmVolume.first()
                        val useVibration = settings.useVibration.first()
                        AlarmSoundPlayer.play(context, customUri, volume, useVibration)
                    } else if (deliveryMode == DeliveryMode.VIBRATE) {
                        AlarmSoundPlayer.playVibrationOnly(context)
                    }
                }

                ReminderNotifier(context).show(reminder, quietMode)

                val rule = RecurrenceRule.parse(reminder.recurrenceRule)
                if (rule != null) {
                    val nextFireAt = RecurrenceCalculator.nextOccurrence(
                        rule,
                        System.currentTimeMillis(),
                    )
                    repo.updateAndSchedule(
                        reminder.copy(
                            fireAt = nextFireAt,
                            status = ReminderStatus.PENDING.name,
                        ),
                    )
                } else {
                    repo.markFired(reminderId)
                }
            } finally {
                wakeLock?.let {
                    if (it.isHeld) it.release()
                }
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "ReminderAlarmReceiver"
    }
}
