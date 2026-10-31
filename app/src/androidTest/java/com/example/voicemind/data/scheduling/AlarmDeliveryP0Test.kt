package com.example.voicemind.data.scheduling

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.voicemind.data.AppDatabase
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderDao
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.ui.screens.AlarmActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AlarmDeliveryP0Test {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dao: ReminderDao

    @Before
    fun setUp() {
        dao = AppDatabase.getInstance(context).reminderDao()
    }

    @After
    fun tearDown() = runBlocking {
        dao.getAllScheduled().forEach { dao.delete(it.id) }
        dao.getRecentHistory(50).forEach { dao.delete(it.id) }
    }

    @Test
    fun actionComplete_changesStatusWithoutActivity() {
        val id = insertPendingReminder("complete")

        context.sendBroadcast(
            ReminderIntents.actionBroadcast(context, id, ReminderIntents.ACTION_COMPLETE),
        )

        awaitStatus(id, ReminderStatus.DONE)
    }

    @Test
    fun actionSnooze_usesProvidedMinutes() {
        val id = insertPendingReminder("snooze")
        val before = System.currentTimeMillis()

        context.sendBroadcast(
            ReminderIntents.actionBroadcast(
                context,
                id,
                ReminderIntents.ACTION_SNOOZE,
                snoozeMinutes = 15,
            ),
        )

        val reminder = awaitReminder(id) { it.snoozeCount == 1 }
        val expectedMin = before + 14 * 60_000L
        val expectedMax = System.currentTimeMillis() + 16 * 60_000L
        assertTrue(reminder.fireAt in expectedMin..expectedMax)
        assertEquals(ReminderStatus.PENDING.name, reminder.status)
    }

    @Test
    fun backFromAlarmActivity_cancelsReminder() {
        val id = insertPendingReminder("back")
        val intent = Intent(context, AlarmActivity::class.java).apply {
            putExtra(AlarmActivity.EXTRA_REMINDER_ID, id)
        }

        ActivityScenario.launch<AlarmActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
            awaitStatus(id, ReminderStatus.CANCELLED)
        }
    }

    private fun insertPendingReminder(body: String): Long = runBlocking {
        dao.insert(
            Reminder(
                clientId = UUID.randomUUID().toString(),
                fireAt = System.currentTimeMillis() + 60_000L,
                body = body,
                rawPhrase = null,
                status = ReminderStatus.PENDING.name,
                createdAt = System.currentTimeMillis(),
                deliveryMode = DeliveryMode.VIBRATE.name,
            ),
        )
    }

    private fun awaitStatus(id: Long, expected: ReminderStatus) {
        awaitReminder(id) { it.status == expected.name }
    }

    private fun awaitReminder(
        id: Long,
        predicate: (Reminder) -> Boolean,
    ): Reminder {
        val deadline = System.currentTimeMillis() + 5_000L
        while (System.currentTimeMillis() < deadline) {
            val reminder = runBlocking { dao.getById(id) }
            if (reminder != null && predicate(reminder)) {
                return reminder
            }
            Thread.sleep(50)
        }
        fail("Reminder $id did not reach expected state")
        throw AssertionError("unreachable")
    }
}
