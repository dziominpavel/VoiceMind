package com.example.voicemind.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {

    const val DEFAULT = "reminders_default"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(
                DEFAULT,
                "Напоминания",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Голосовые напоминания"
            },
        )
    }

    fun channelId(): String = DEFAULT
}
