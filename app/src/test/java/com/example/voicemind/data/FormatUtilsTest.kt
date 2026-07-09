package com.example.voicemind.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class FormatUtilsTest {

    private val zone = ZoneId.of("Europe/Moscow")

    private fun dt(year: Int, month: Int, day: Int, hour: Int = 12, minute: Int = 0): Long =
        ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zone)
            .toInstant()
            .toEpochMilli()

    @Test
    fun statusLabel_triggeredIsBezOtveta() {
        assertEquals("Без ответа", FormatUtils.statusLabel("TRIGGERED"))
        assertEquals("Ожидает", FormatUtils.statusLabel("PENDING"))
        assertEquals("Выполнено", FormatUtils.statusLabel("DONE"))
        assertEquals("Отменено", FormatUtils.statusLabel("CANCELLED"))
    }

    @Test
    fun formatHistoryDate_todayYesterdayAndPast() {
        val now = dt(2026, 7, 10, 15, 0)
        assertEquals("сегодня", FormatUtils.formatHistoryDate(dt(2026, 7, 10, 9, 0), now, zone))
        assertEquals("вчера", FormatUtils.formatHistoryDate(dt(2026, 7, 9, 9, 0), now, zone))
        val older = FormatUtils.formatHistoryDate(dt(2026, 6, 3, 9, 0), now, zone)
        assertFalse(older.contains("просрочено"))
        assertFalse(older.equals("сегодня", ignoreCase = true))
    }

    @Test
    fun formatHistoryDate_neverSaysOverdue() {
        val now = dt(2026, 7, 10, 15, 0)
        val past = FormatUtils.formatHistoryDate(dt(2026, 1, 1, 9, 0), now, zone)
        assertFalse(past.contains("просрочено"))
    }

    @Test
    fun formatRelativeDateShort_overdueOnlyForPast() {
        val now = dt(2026, 7, 10, 15, 0)
        assertEquals("просрочено", FormatUtils.formatRelativeDateShort(dt(2026, 7, 10, 10, 0), now, zone))
        assertEquals("сегодня", FormatUtils.formatRelativeDateShort(dt(2026, 7, 10, 18, 0), now, zone))
    }

    @Test
    fun formatRelativeFireAt_overdueForPast() {
        val now = dt(2026, 7, 10, 15, 0)
        assertEquals("просрочено", FormatUtils.formatRelativeFireAt(dt(2026, 7, 9, 9, 0), now, zone))
    }
}
