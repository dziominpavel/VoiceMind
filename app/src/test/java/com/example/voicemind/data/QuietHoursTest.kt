package com.example.voicemind.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class QuietHoursTest {

    private fun minute(hour: Int, minute: Int = 0): Int = hour * 60 + minute

    @Test
    fun sameDayInterval_includesStartAndExcludesEnd() {
        val start = minute(9)
        val end = minute(17)

        assertFalse(QuietHoursSettings.isWithinRange(minute(8, 59), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(9), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(16, 59), start, end))
        assertFalse(QuietHoursSettings.isWithinRange(minute(17), start, end))
        assertFalse(QuietHoursSettings.isWithinRange(minute(12), minute(9), minute(9)))
    }

    @Test
    fun overnightInterval_includesBothSidesOfMidnight() {
        val start = minute(22)
        val end = minute(7)

        assertFalse(QuietHoursSettings.isWithinRange(minute(21, 59), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(22), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(23, 59), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(0), start, end))
        assertTrue(QuietHoursSettings.isWithinRange(minute(6, 59), start, end))
        assertFalse(QuietHoursSettings.isWithinRange(minute(7), start, end))
        assertFalse(QuietHoursSettings.isWithinRange(minute(12), start, end))
    }

    @Test
    fun equalStartAndEnd_disablesQuietHours() {
        assertFalse(QuietHoursSettings.isWithinRange(minute(22), minute(22), minute(22)))
        assertFalse(QuietHoursSettings.isWithinRange(minute(0), minute(22), minute(22)))
    }

    @Test
    fun disabledSettings_neverActive() {
        val settings = QuietHoursSettings(
            enabled = false,
            startMinute = minute(22),
            endMinute = minute(7),
        )

        assertFalse(settings.isActive(LocalTime.of(23, 0)))
        assertFalse(settings.isActive(LocalTime.of(3, 0)))
    }

    @Test
    fun enabledSettings_useLocalTime() {
        val settings = QuietHoursSettings(
            enabled = true,
            startMinute = minute(22),
            endMinute = minute(7),
        )

        assertTrue(settings.isActive(LocalTime.of(23, 0)))
        assertTrue(settings.isActive(LocalTime.of(3, 0)))
        assertFalse(settings.isActive(LocalTime.of(7, 0)))
        assertFalse(settings.isActive(LocalTime.of(12, 0)))
    }

    @Test
    fun normalizeMinute_clampsToDayRange() {
        assertEquals(0, QuietHoursSettings.normalizeMinute(-10))
        assertEquals(0, QuietHoursSettings.normalizeMinute(0))
        assertEquals(1439, QuietHoursSettings.normalizeMinute(1439))
        assertEquals(1439, QuietHoursSettings.normalizeMinute(1440))
    }
}
