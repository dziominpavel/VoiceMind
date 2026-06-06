package com.example.voicemind.ui.screens

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
}
