package com.example.voicemind.data.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationChannelsTest {

    @Test
    fun currentChannelIds_doNotOverlapLegacyIds() {
        assertTrue(
            NotificationChannels.currentChannelIds
                .intersect(NotificationChannels.legacyChannelIds)
                .isEmpty(),
        )
    }

    @Test
    fun legacyChannelIds_containOnlyOldNames() {
        assertEquals(
            setOf(
                "reminders_default",
                "reminders_alarm",
                "reminders_silent",
                "reminders_vibrate",
            ),
            NotificationChannels.legacyChannelIds,
        )
    }

    @Test
    fun currentChannelIds_includeAllDeliveryChannels() {
        assertEquals(
            setOf(
                "reminders_default_v2",
                "reminders_alarm_v2",
                "reminders_vibrate_v2",
                "reminders_silent_v2",
                "reminders_quiet_vibrate_v1",
            ),
            NotificationChannels.currentChannelIds,
        )
    }
}
