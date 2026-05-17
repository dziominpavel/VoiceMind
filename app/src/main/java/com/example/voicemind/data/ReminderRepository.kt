package com.example.voicemind.data

import android.content.Context
import com.example.voicemind.data.notification.ReminderNotifier
import com.example.voicemind.data.scheduling.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ReminderRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = AppDatabase.getInstance(appContext).reminderDao()
    private val scheduler = ReminderScheduler(appContext)

    suspend fun insertAndSchedule(reminder: Reminder): Long = withContext(Dispatchers.IO) {
        val id = dao.insert(reminder)
        val saved = reminder.copy(
            id = id,
            alarmRequestCode = id.requestCode(),
        )
        dao.update(saved)
        scheduler.schedule(saved)
        id
    }

    suspend fun reschedule(reminder: Reminder) = withContext(Dispatchers.IO) {
        scheduler.cancel(reminder.id)
        scheduler.schedule(reminder)
    }

    suspend fun cancelReminder(id: Long) = withContext(Dispatchers.IO) {
        scheduler.cancel(id)
        dao.updateStatus(id, ReminderStatus.CANCELLED.name)
    }

    suspend fun dismissReminder(id: Long) = withContext(Dispatchers.IO) {
        scheduler.cancel(id)
        dao.updateStatus(id, ReminderStatus.DISMISSED.name)
    }

    suspend fun snoozeReminder(id: Long, delayMinutes: Int = SNOOZE_MINUTES) = withContext(Dispatchers.IO) {
        val reminder = dao.getById(id) ?: return@withContext
        val newFireAt = System.currentTimeMillis() + delayMinutes * 60_000L
        scheduler.cancel(id)
        dao.snooze(id, ReminderStatus.SCHEDULED.name, newFireAt)
        val updated = reminder.copy(
            fireAt = newFireAt,
            status = ReminderStatus.SCHEDULED.name,
            snoozeCount = reminder.snoozeCount + 1,
        )
        scheduler.schedule(updated)
    }

    suspend fun markFiredAndShow(id: Long) = withContext(Dispatchers.IO) {
        val reminder = dao.getById(id) ?: return@withContext
        if (reminder.status != ReminderStatus.SCHEDULED.name &&
            reminder.status != ReminderStatus.SNOOZED.name
        ) {
            return@withContext
        }
        dao.updateStatus(id, ReminderStatus.FIRED.name)
        ReminderNotifier(appContext).show(
            reminder.copy(status = ReminderStatus.FIRED.name),
        )
    }

    suspend fun rescheduleAll() = withContext(Dispatchers.IO) {
        scheduler.rescheduleAll(dao.getAllScheduled())
    }

    fun observeUpcoming() = dao.observeUpcomingScheduled()

    fun observeHistory() = dao.observeHistory()

    suspend fun getById(id: Long): Reminder? = withContext(Dispatchers.IO) {
        dao.getById(id)
    }

    suspend fun updateAndSchedule(reminder: Reminder) = withContext(Dispatchers.IO) {
        val updated = reminder.copy(alarmRequestCode = reminder.id.requestCode())
        dao.update(updated)
        scheduler.cancel(reminder.id)
        if (
            (updated.status == ReminderStatus.SCHEDULED.name || updated.status == ReminderStatus.SNOOZED.name) &&
            updated.fireAt > System.currentTimeMillis()
        ) {
            scheduler.schedule(updated)
        }
    }

    companion object {
        const val SNOOZE_MINUTES = 10

        @Volatile
        private var instance: ReminderRepository? = null

        fun getInstance(context: Context): ReminderRepository =
            instance ?: synchronized(this) {
                instance ?: ReminderRepository(context).also { instance = it }
            }
    }
}

private fun Long.requestCode(): Int = (this and 0x7FFFFFFF).toInt()
