package com.example.voicemind.data

import com.example.voicemind.data.scheduling.ReminderAlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRepositorySyncDeliveryModeTest {

    @Test
    fun syncDeliveryModes_reschedulesOnlyPendingWithNewMode() = runBlocking {
        val reminders = listOf(
            reminder(1, ReminderStatus.PENDING, DeliveryMode.NOTIFICATION),
            reminder(2, ReminderStatus.PENDING, DeliveryMode.ALARM),
            reminder(3, ReminderStatus.DONE, DeliveryMode.VIBRATE),
            reminder(4, ReminderStatus.CANCELLED, DeliveryMode.SILENT),
        )
        val dao = FakeReminderDao(reminders)
        val scheduler = RecordingScheduler()

        syncDeliveryModes(dao, scheduler, DeliveryMode.SILENT)

        assertEquals(DeliveryMode.SILENT.name, dao.updatedDeliveryMode)
        assertEquals(setOf(1L, 2L), scheduler.scheduled.map { it.id }.toSet())
        assertTrue(
            scheduler.scheduled.all { it.deliveryMode == DeliveryMode.SILENT.name },
        )
    }

    private fun reminder(
        id: Long,
        status: ReminderStatus,
        mode: DeliveryMode,
    ) = Reminder(
        id = id,
        clientId = "client-$id",
        fireAt = 1_000_000L + id,
        body = "body-$id",
        rawPhrase = null,
        status = status.name,
        createdAt = 0,
        deliveryMode = mode.name,
    )

    private class RecordingScheduler : ReminderAlarmScheduler {
        val scheduled = mutableListOf<Reminder>()

        override fun schedule(reminder: Reminder) {
            scheduled += reminder
        }

        override fun cancel(reminderId: Long) = Unit

        override fun rescheduleAll(reminders: List<Reminder>) {
            scheduled += reminders
        }
    }

    private class FakeReminderDao(
        private val reminders: List<Reminder>,
    ) : ReminderDao {
        var updatedDeliveryMode: String? = null

        override suspend fun getAllScheduled(): List<Reminder> =
            reminders.filter { it.status == ReminderStatus.PENDING.name }

        override suspend fun updateAllDeliveryModes(mode: String) {
            updatedDeliveryMode = mode
        }

        override suspend fun getById(id: Long): Reminder? = reminders.find { it.id == id }

        override fun observeUpcomingScheduled(): Flow<List<Reminder>> =
            flowOf(getAllScheduledBlocking())

        override fun observeHistory(): Flow<List<Reminder>> = flowOf(emptyList())

        override suspend fun insert(reminder: Reminder): Long = error("not used")

        override suspend fun update(reminder: Reminder) = error("not used")

        override suspend fun updateStatus(id: Long, status: String) = error("not used")

        override suspend fun complete(id: Long, fireAt: Long) = error("not used")

        override suspend fun snooze(id: Long, fireAt: Long) = error("not used")

        override suspend fun delete(id: Long) = error("not used")

        override suspend fun getRecentHistory(limit: Int): List<Reminder> = emptyList()

        override suspend fun getWidgetUpcoming(limit: Int): List<Reminder> = emptyList()

        override suspend fun getWidgetRecentDone(cutoff: Long, limit: Int): List<Reminder> =
            emptyList()

        override suspend fun getOverduePending(now: Long): List<Reminder> = emptyList()

        private fun getAllScheduledBlocking(): List<Reminder> =
            reminders.filter { it.status == ReminderStatus.PENDING.name }
    }
}
