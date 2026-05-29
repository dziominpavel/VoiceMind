package com.example.voicemind.data.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.voicemind.MainActivity
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.SettingsRepository
import com.example.voicemind.data.scheduling.ReminderIntents
import kotlinx.coroutines.flow.first

class ReminderNotifier(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val settings = SettingsRepository.getInstance(context)

    suspend fun show(reminder: Reminder) {
        NotificationChannels.createAll(context)

        val useAlarmSound = settings.useAlarmSound.first()
        val usePush = settings.usePushNotification.first()
        val useVibration = settings.useVibration.first()

        val channelId = NotificationChannels.channelId()

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

        when {
            useAlarmSound && useVibration -> {
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setVibrate(longArrayOf(0, 500, 200, 500))
            }
            useAlarmSound -> {
                builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                builder.setVibrate(null)
            }
            usePush && useVibration -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setVibrate(longArrayOf(0, 300, 150, 300))
            }
            usePush -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                builder.setVibrate(null)
            }
            useVibration -> {
                builder.setSound(null)
                builder.setPriority(NotificationCompat.PRIORITY_DEFAULT)
                builder.setVibrate(longArrayOf(0, 300, 150, 300))
            }
            else -> {
                builder.setSound(null)
                builder.setPriority(NotificationCompat.PRIORITY_LOW)
                builder.setVibrate(null)
            }
        }

        notificationManager.notify(reminder.id.notificationId(), builder.build())
    }

    fun cancelNotification(reminderId: Long) {
        notificationManager.cancel(reminderId.notificationId())
    }

    private fun Long.notificationId(): Int = (this and 0x7FFFFFFF).toInt()
    private fun Long.requestCode(): Int = notificationId()
}
