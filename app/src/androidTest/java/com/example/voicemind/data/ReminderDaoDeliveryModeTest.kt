package com.example.voicemind.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class ReminderDaoDeliveryModeTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: ReminderDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.reminderDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun updateAllDeliveryModes_updatesAllStatuses() = runBlocking {
        val now = System.currentTimeMillis()
        val pendingId = dao.insert(
            reminder(
                body = "pending",
                status = ReminderStatus.PENDING.name,
                deliveryMode = DeliveryMode.NOTIFICATION.name,
                fireAt = now + 60_000L,
                createdAt = now,
            ),
        )
        val doneId = dao.insert(
            reminder(
                body = "done",
                status = ReminderStatus.DONE.name,
                deliveryMode = DeliveryMode.VIBRATE.name,
                fireAt = now - 60_000L,
                createdAt = now,
            ),
        )
        val cancelledId = dao.insert(
            reminder(
                body = "cancelled",
                status = ReminderStatus.CANCELLED.name,
                deliveryMode = DeliveryMode.SILENT.name,
                fireAt = now - 120_000L,
                createdAt = now,
            ),
        )

        dao.updateAllDeliveryModes(DeliveryMode.ALARM.name)

        assertEquals(DeliveryMode.ALARM.name, dao.getById(pendingId)?.deliveryMode)
        assertEquals(DeliveryMode.ALARM.name, dao.getById(doneId)?.deliveryMode)
        assertEquals(DeliveryMode.ALARM.name, dao.getById(cancelledId)?.deliveryMode)
    }

    private fun reminder(
        body: String,
        status: String,
        deliveryMode: String,
        fireAt: Long,
        createdAt: Long,
    ) = Reminder(
        clientId = UUID.randomUUID().toString(),
        fireAt = fireAt,
        body = body,
        rawPhrase = null,
        status = status,
        createdAt = createdAt,
        deliveryMode = deliveryMode,
    )
}
