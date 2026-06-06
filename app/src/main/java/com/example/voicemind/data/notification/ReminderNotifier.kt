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
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.scheduling.ReminderIntents
import com.example.voicemind.ui.screens.AlarmActivity
import kotlinx.coroutines.flow.first

class ReminderNotifier(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun show(reminder: Reminder) {
        NotificationChannels.createAll(context)

        val deliveryMode = DeliveryMode.valueOf(reminder.deliveryMode)
        val channelId = NotificationChannels.channelId(deliveryMode)
        val settings = SettingsRepository.getInstance(context)
        val useVibration = settings.useVibration.first()

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
            .setDeleteIntent(ReminderIntents.actionIntent(context, reminder.id, ReminderIntents.ACTION_DISMISS))
            .setAutoCancel(true)
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

        if (deliveryMode == DeliveryMode.ALARM) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setPriority(NotificationCompat.PRIORITY_MAX)
            builder.setSilent(true)
            builder.setDefaults(0)
            builder.setSound(null)
            if (useVibration) {
                builder.setVibrate(NotificationChannels.DEFAULT_VIBRATE_PATTERN)
            } else {
                builder.setVibrate(null)
            }

            val fullScreenIntent = PendingIntent.getActivity(
                context,
                reminder.id.notificationId(),
                Intent(context, AlarmActivity::class.java).apply {
                    putExtra(AlarmActivity.EXTRA_REMINDER_ID, reminder.id)
                    putExtra(AlarmActivity.EXTRA_REMINDER_BODY, reminder.body)
                    putExtra(AlarmActivity.EXTRA_REMINDER_FIRE_AT, reminder.fireAt)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.setFullScreenIntent(fullScreenIntent, true)
        } else if (deliveryMode == DeliveryMode.NOTIFICATION) {
            builder.setPriority(NotificationCompat.PRIORITY_HIGH)
            if (useVibration) {
                builder.setVibrate(NotificationChannels.DEFAULT_VIBRATE_PATTERN)
            } else {
                builder.setVibrate(null)
            }
        } else if (deliveryMode == DeliveryMode.VIBRATE_ONLY) {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
            builder.setVibrate(NotificationChannels.DEFAULT_VIBRATE_PATTERN)
        } else {
            builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }

        notificationManager.notify(reminder.id.notificationId(), builder.build())
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.notificationId())
    }

    private fun Long.notificationId(): Int = (this and 0x7FFFFFFF).toInt()
    private fun Long.requestCode(): Int = notificationId()
}
