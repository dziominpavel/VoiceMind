package com.example.voicemind.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class RecurrenceCalculatorTest {

    private val zone = ZoneId.of("Europe/Moscow")

    private fun dt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return ZonedDateTime.of(LocalDateTime.of(year, month, day, hour, minute), zone)
            .toInstant().toEpochMilli()
    }

    @Test
    fun daily_nextDay() {
        val from = dt(2026, 6, 19, 8, 0)
        val rule = RecurrenceRule(RecurrenceType.DAILY, interval = 1)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 20, 8, 0), next)
    }

    @Test
    fun daily_interval2() {
        val from = dt(2026, 6, 19, 8, 0)
        val rule = RecurrenceRule(RecurrenceType.DAILY, interval = 2)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 21, 8, 0), next)
    }

    @Test
    fun weekdays_fridayToMonday() {
        val from = dt(2026, 6, 19, 9, 0) // Friday
        val rule = RecurrenceRule(RecurrenceType.WEEKDAYS)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 22, 9, 0), next) // Monday
    }

    @Test
    fun weekdays_mondayToTuesday() {
        val from = dt(2026, 6, 22, 9, 0) // Monday
        val rule = RecurrenceRule(RecurrenceType.WEEKDAYS)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 23, 9, 0), next) // Tuesday
    }

    @Test
    fun weekends_fridayToSaturday() {
        val from = dt(2026, 6, 19, 9, 0) // Friday
        val rule = RecurrenceRule(RecurrenceType.WEEKENDS)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 20, 9, 0), next) // Saturday
    }

    @Test
    fun weekends_sundayToSaturday() {
        val from = dt(2026, 6, 21, 9, 0) // Sunday
        val rule = RecurrenceRule(RecurrenceType.WEEKENDS)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 27, 9, 0), next) // Next Saturday
    }

    @Test
    fun weekly_sameDay() {
        val from = dt(2026, 6, 19, 7, 0) // Friday
        val rule = RecurrenceRule(RecurrenceType.WEEKLY, dayOfWeek = DayOfWeek.FRIDAY.value)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 6, 26, 7, 0), next)
    }

    @Test
    fun monthly_sameDay() {
        val from = dt(2026, 6, 15, 10, 0)
        val rule = RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = 15)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 7, 15, 10, 0), next)
    }

    @Test
    fun monthly_31st_coerces() {
        val from = dt(2026, 1, 31, 10, 0)
        val rule = RecurrenceRule(RecurrenceType.MONTHLY, dayOfMonth = 31)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertEquals(dt(2026, 2, 28, 10, 0), next)
    }

    @Test
    fun skipped_occurrence_jumps_to_future() {
        val from = dt(2026, 6, 1, 8, 0)
        val rule = RecurrenceRule(RecurrenceType.DAILY, interval = 1)
        val next = RecurrenceCalculator.nextOccurrence(rule, from, zone)
        assertTrue(next > dt(2026, 6, 1, 8, 0))
        assertEquals(dt(2026, 6, 2, 8, 0), next)
    }
}
