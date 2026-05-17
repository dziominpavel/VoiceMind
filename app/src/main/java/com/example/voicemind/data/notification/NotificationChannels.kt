package com.example.voicemind.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.voicemind.data.DeliveryMode

object NotificationChannels {

    const val DEFAULT = "reminders_default"
    const val ALARM = "reminders_alarm"
    const val SILENT = "reminders_silent"
    const val VIBRATE = "reminders_vibrate"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                DEFAULT,
                "Напоминания",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Обычные голосовые напоминания"
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                ALARM,
                "Как будильник",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Громкие напоминания"
                enableVibration(true)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                SILENT,
                "Тихие",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Без звука и вибрации"
                setSound(null, null)
                enableVibration(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                VIBRATE,
                "Только вибрация",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Вибрация без звука"
                setSound(null, null)
                enableVibration(true)
            },
        )
    }

    fun channelIdFor(mode: DeliveryMode): String = when (mode) {
        DeliveryMode.NOTIFICATION -> DEFAULT
        DeliveryMode.ALARM -> ALARM
        DeliveryMode.VIBRATE_ONLY -> VIBRATE
        DeliveryMode.SILENT -> SILENT
    }
}
