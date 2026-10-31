package com.example.voicemind.data.scheduling

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderIntentsAndroidTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun actionBroadcast_containsActionIdAndSnoozeMinutes() {
        val intent = ReminderIntents.actionBroadcast(
            context = context,
            reminderId = 42L,
            action = ReminderIntents.ACTION_SNOOZE,
            snoozeMinutes = 15,
        )

        assertEquals(ReminderIntents.ACTION_SNOOZE, intent.action)
        assertEquals(42L, intent.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L))
        assertEquals(15, intent.getIntExtra(ReminderIntents.EXTRA_SNOOZE_MINUTES, -1))
        assertEquals(ReminderActionReceiver::class.java.name, intent.component?.className)
    }

    @Test
    fun actionBroadcast_omitsSnoozeMinutesWhenNull() {
        val intent = ReminderIntents.actionBroadcast(
            context = context,
            reminderId = 7L,
            action = ReminderIntents.ACTION_COMPLETE,
        )

        assertEquals(ReminderIntents.ACTION_COMPLETE, intent.action)
        assertEquals(7L, intent.getLongExtra(ReminderIntents.EXTRA_REMINDER_ID, -1L))
        assertFalse(intent.hasExtra(ReminderIntents.EXTRA_SNOOZE_MINUTES))
    }
}
