package com.example.voicemind.data

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

object RecurrenceCalculator {

    /**
     * Вычисляет ближайшее будущее срабатывание строго после [fromMillis].
     * Для пропущенных срабатываний — всегда ближайшее будущее, а не накопление.
     */
    fun nextOccurrence(
        rule: RecurrenceRule,
        fromMillis: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long {
        val from = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(fromMillis), zone)
        val candidate = when (rule.type) {
            RecurrenceType.DAILY -> nextDaily(from, rule.interval)
            RecurrenceType.WEEKDAYS -> nextWeekdays(from)
            RecurrenceType.WEEKENDS -> nextWeekends(from)
            RecurrenceType.WEEKLY -> nextWeekly(from, rule.dayOfWeek ?: 1, rule.interval)
            RecurrenceType.MONTHLY -> nextMonthly(from, rule.dayOfMonth ?: 1, rule.interval)
        }
        return candidate.toInstant().toEpochMilli()
    }

    private fun nextDaily(from: ZonedDateTime, interval: Int): ZonedDateTime {
        return from.plusDays(interval.toLong()).withSameTime(from)
    }

    private fun nextWeekdays(from: ZonedDateTime): ZonedDateTime {
        var candidate = from.plusDays(1).withSameTime(from)
        while (candidate.dayOfWeek.value > 5) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextWeekends(from: ZonedDateTime): ZonedDateTime {
        var candidate = from.plusDays(1).withSameTime(from)
        while (candidate.dayOfWeek.value < 6) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun nextWeekly(from: ZonedDateTime, dayOfWeek: Int, interval: Int): ZonedDateTime {
        var candidate = from.plusWeeks(interval.toLong()).withSameTime(from)
        val targetDow = dayOfWeek.coerceIn(1, 7)
        val currentDow = candidate.dayOfWeek.value
        val delta = (targetDow - currentDow + 7) % 7
        candidate = candidate.plusDays(delta.toLong())
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusWeeks(interval.toLong())
        }
        return candidate
    }

    private fun nextMonthly(from: ZonedDateTime, dayOfMonth: Int, interval: Int): ZonedDateTime {
        var candidate = from.plusMonths(interval.toLong()).withSameTime(from)
        val maxDay = candidate.toLocalDate().lengthOfMonth()
        val targetDay = dayOfMonth.coerceIn(1, maxDay)
        candidate = candidate.withDayOfMonth(targetDay)
        if (!candidate.isAfter(from)) {
            candidate = candidate.plusMonths(interval.toLong())
            val newMaxDay = candidate.toLocalDate().lengthOfMonth()
            candidate = candidate.withDayOfMonth(dayOfMonth.coerceIn(1, newMaxDay))
        }
        return candidate
    }

    private fun ZonedDateTime.withSameTime(other: ZonedDateTime): ZonedDateTime {
        return this.withHour(other.hour)
            .withMinute(other.minute)
            .withSecond(other.second)
            .withNano(other.nano)
    }
}
