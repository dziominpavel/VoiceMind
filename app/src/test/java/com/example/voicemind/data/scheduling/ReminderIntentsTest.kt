package com.example.voicemind.data.scheduling

import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderIntentsTest {

    @Test
    fun completeAction_hasStableName() {
        assertEquals(
            "com.example.voicemind.ACTION_REMINDER_COMPLETE",
            ReminderIntents.ACTION_COMPLETE,
        )
    }

    @Test
    fun snoozeMinutesExtra_hasStableName() {
        assertEquals("snooze_minutes", ReminderIntents.EXTRA_SNOOZE_MINUTES)
    }
}
