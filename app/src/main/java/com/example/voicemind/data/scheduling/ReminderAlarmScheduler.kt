package com.example.voicemind.data.scheduling

import com.example.voicemind.data.Reminder

/** Точка планирования alarm, отделённая от AlarmManager для тестируемости. */
interface ReminderAlarmScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    fun rescheduleAll(reminders: List<Reminder>)
}
