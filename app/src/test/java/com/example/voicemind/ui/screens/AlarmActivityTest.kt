package com.example.voicemind.ui.screens

import com.example.voicemind.data.scheduling.ReminderIntents
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmActivityIntentExtrasTest {

    @Test
    fun `extra reminder id key is correct`() {
        assertEquals("reminder_id", AlarmActivity.EXTRA_REMINDER_ID)
    }

    @Test
    fun `extra reminder body key is correct`() {
        assertEquals("reminder_body", AlarmActivity.EXTRA_REMINDER_BODY)
    }

    @Test
    fun `extra reminder fireAt key is correct`() {
        assertEquals("reminder_fire_at", AlarmActivity.EXTRA_REMINDER_FIRE_AT)
    }

    @Test
    fun `AlarmActivity EXTRA_REMINDER_ID matches ReminderIntents EXTRA_REMINDER_ID`() {
        // ReminderAlarmReceiver reads reminderId via ReminderIntents.EXTRA_REMINDER_ID,
        // then passes it to AlarmActivity via AlarmActivity.EXTRA_REMINDER_ID.
        // These constants MUST match so the id flows through correctly.
        assertEquals(ReminderIntents.EXTRA_REMINDER_ID, AlarmActivity.EXTRA_REMINDER_ID)
    }
}
