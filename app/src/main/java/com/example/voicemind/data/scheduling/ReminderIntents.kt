package com.example.voicemind.data.scheduling

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.example.voicemind.data.DeliveryMode

object ReminderIntents {

    const val EXTRA_REMINDER_ID = "reminder_id"
    const val EXTRA_DELIVERY_MODE = "delivery_mode"

    const val ACTION_ALARM = "com.example.voicemind.ACTION_REMINDER_ALARM"
    const val ACTION_DONE = "com.example.voicemind.ACTION_REMINDER_DONE"
    const val ACTION_SNOOZE = "com.example.voicemind.ACTION_REMINDER_SNOOZE"
    const val ACTION_CANCEL = "com.example.voicemind.ACTION_REMINDER_CANCEL"
    const val ACTION_DISMISS = "com.example.voicemind.ACTION_REMINDER_DISMISS"

    fun alarmIntent(
        context: Context,
        reminderId: Long,
        deliveryMode: String = DeliveryMode.NOTIFICATION.name,
    ): PendingIntent {
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_ALARM
            putExtra(EXTRA_REMINDER_ID, reminderId)
            putExtra(EXTRA_DELIVERY_MODE, deliveryMode)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.requestCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun actionIntent(context: Context, reminderId: Long, action: String): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            (reminderId.toString() + action).hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun Long.requestCode(): Int = (this and 0x7FFFFFFF).toInt()
}
