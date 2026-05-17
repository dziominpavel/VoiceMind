package com.example.voicemind.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.voicemind.MainActivity
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.scheduling.ReminderIntents

class ReminderNotifier(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun show(reminder: Reminder) {
        NotificationChannels.createAll(context)
        val mode = runCatching { DeliveryMode.valueOf(reminder.deliveryMode) }
            .getOrDefault(DeliveryMode.NOTIFICATION)
        val channelId = NotificationChannels.channelIdFor(mode)

        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.requestCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notification_title))
            .setContentText(reminder.body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "${reminder.body}\n${FormatUtils.formatFireAt(reminder.fireAt)}",
                    ),
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(
                if (mode == DeliveryMode.ALARM) {
                    NotificationCompat.PRIORITY_MAX
                } else {
                    NotificationCompat.PRIORITY_HIGH
                },
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_done),
                ReminderIntents.actionIntent(context, reminder.id, ReminderIntents.ACTION_DONE),
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_snooze),
                ReminderIntents.actionIntent(context, reminder.id, ReminderIntents.ACTION_SNOOZE),
            )
            .addAction(
                R.drawable.ic_notification,
                context.getString(R.string.notification_action_cancel),
                ReminderIntents.actionIntent(context, reminder.id, ReminderIntents.ACTION_CANCEL),
            )

        when (mode) {
            DeliveryMode.VIBRATE_ONLY -> {
                builder.setSound(null)
                builder.setVibrate(longArrayOf(0, 300, 150, 300))
            }
            DeliveryMode.SILENT -> {
                builder.setSound(null)
                builder.setVibrate(null)
            }
            DeliveryMode.ALARM -> {
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }
            DeliveryMode.NOTIFICATION -> Unit
        }

        notificationManager.notify(reminder.id.notificationId(), builder.build())
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.notificationId())
    }

    private fun Long.notificationId(): Int = (this and 0x7FFFFFFF).toInt()
    private fun Long.requestCode(): Int = notificationId()
}
