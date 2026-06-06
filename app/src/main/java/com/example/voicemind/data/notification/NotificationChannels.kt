package com.example.voicemind.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.voicemind.data.DeliveryMode

object NotificationChannels {

    const val DEFAULT = "reminders_default_v2"
    const val ALARM = "reminders_alarm_v2"
    const val VIBRATE = "reminders_vibrate_v2"
    const val SILENT = "reminders_silent_v2"

    private val ALARM_VIBRATE_PATTERN = longArrayOf(0, 500, 200, 500, 200, 500)
    val DEFAULT_VIBRATE_PATTERN = longArrayOf(0, 300, 150, 300)

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Удаляем старые каналы, созданные в ранних версиях, чтобы система не кешировала
        // неправильные звук / вибрацию.
        nm.deleteNotificationChannel("reminders_default")
        nm.deleteNotificationChannel(VIBRATE)

        // Канал "Как будильник" — без звука на канале (звук играет явно через AlarmSoundPlayer),
        // bypass DND, вибрация тоже явная
        val alarmChannel = NotificationChannel(
            ALARM,
            "Будильник",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Звук и вибрация как у будильника"
            setSound(null, null)
            enableVibration(false)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setBypassDnd(true)
            }
        }

        // Канал "Уведомление" — стандартный звук, короткая вибрация
        val defaultChannel = NotificationChannel(
            DEFAULT,
            "Уведомления",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Обычные push-уведомления"
            enableVibration(true)
            vibrationPattern = DEFAULT_VIBRATE_PATTERN
        }

        // Канал "Только вибрация" — без звука и без встроенной вибрации
        // (вибрация управляется явно через AlarmSoundPlayer / Vibrator API)
        val vibrateChannel = NotificationChannel(
            VIBRATE,
            "Только вибрация",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Вибрация без звука"
            setSound(null, null)
            enableVibration(false)
        }

        // Канал "Тихое" — без звука и вибрации
        val silentChannel = NotificationChannel(
            SILENT,
            "Тихие",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Без звука и вибрации"
            setSound(null, null)
            enableVibration(false)
        }

        nm.createNotificationChannels(
            listOf(alarmChannel, defaultChannel, vibrateChannel, silentChannel),
        )
    }

    fun channelId(deliveryMode: DeliveryMode): String = when (deliveryMode) {
        DeliveryMode.ALARM -> ALARM
        DeliveryMode.NOTIFICATION -> DEFAULT
        DeliveryMode.VIBRATE_ONLY -> VIBRATE
        DeliveryMode.SILENT -> SILENT
    }
}
