package com.example.voicemind.data.scheduling

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(reminder: Reminder) {
        if (reminder.status != ReminderStatus.PENDING.name) {
            return
        }
        val triggerAt = reminder.fireAt
        if (triggerAt <= System.currentTimeMillis()) {
            Log.w(TAG, "skip past reminder id=${reminder.id} fireAt=$triggerAt")
            return
        }
        val pendingIntent = ReminderIntents.alarmIntent(context, reminder.id)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent,
                )
            }
            Log.d(TAG, "scheduled id=${reminder.id} at=$triggerAt")
        } catch (e: SecurityException) {
            Log.e(TAG, "schedule failed id=${reminder.id}", e)
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent,
            )
        }
    }

    fun cancel(reminderId: Long) {
        val pendingIntent = ReminderIntents.alarmIntent(context, reminderId)
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "cancelled id=$reminderId")
    }

    fun rescheduleAll(reminders: List<Reminder>) {
        reminders.forEach { schedule(it) }
        Log.d(TAG, "rescheduled count=${reminders.size}")
    }

    companion object {
        private const val TAG = "ReminderScheduler"
    }
}
