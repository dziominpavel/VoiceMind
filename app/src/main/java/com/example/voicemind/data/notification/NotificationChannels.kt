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
    const val QUIET_VIBRATE = "reminders_quiet_vibrate_v1"

    val DEFAULT_VIBRATE_PATTERN = longArrayOf(0, 300, 150, 300)

    val currentChannelIds = setOf(DEFAULT, ALARM, VIBRATE, SILENT, QUIET_VIBRATE)
    val legacyChannelIds = setOf(
        "reminders_default",
        "reminders_alarm",
        "reminders_silent",
        "reminders_vibrate",
    )

    fun createAll(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Удаляем только старые ID каналов, созданные в ранних версиях. Текущие
        // каналы не трогаем: удаление сбрасывает пользовательские настройки.
        legacyChannelIds.forEach(nm::deleteNotificationChannel)

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

        // Канал для тихих часов с вибрацией — короткая вибрация без звука,
        // без полноэкранного пробуждения.
        val quietVibrateChannel = NotificationChannel(
            QUIET_VIBRATE,
            "Тихие (вибрация)",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Короткая вибрация без звука в тихие часы"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = DEFAULT_VIBRATE_PATTERN
        }

        nm.createNotificationChannels(
            listOf(alarmChannel, defaultChannel, vibrateChannel, silentChannel, quietVibrateChannel),
        )
    }

    fun channelId(deliveryMode: DeliveryMode): String = when (deliveryMode) {
        DeliveryMode.ALARM -> ALARM
        DeliveryMode.NOTIFICATION -> DEFAULT
        DeliveryMode.VIBRATE -> VIBRATE
        DeliveryMode.SILENT -> SILENT
    }
}
