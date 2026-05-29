package com.example.voicemind.data.parse

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Rule-based parser for Russian reminder phrases. JVM-only — unit-testable.
 */
class ReminderParser(
    private val zone: ZoneId = ZoneId.systemDefault(),
) {

    fun parse(rawPhrase: String, now: Instant = Instant.now()): ParseResult {
        val warnings = mutableListOf<ParseWarning>()
        var text = rawPhrase.trim()
        if (text.isEmpty()) {
            return emptyResult(rawPhrase, warnings)
        }

        val lowerOrig = text.lowercase(Locale.forLanguageTag("ru"))
        val lower = stripPrefixes(lowerOrig)
        val prefixStrip = lowerOrig.length - lower.length
        text = text.substring(prefixStrip)
        val lowerText = text.lowercase(Locale.forLanguageTag("ru"))

        val spans = mutableListOf<IntRange>()
        val zonedNow = now.atZone(zone)
        var date = zonedNow.toLocalDate()
        var time: LocalTime? = null
        var hadExplicitDate = false
        var hadExplicitTime = false
        var hadTodayWord = false
        var usedPartOfDay = false
        var relativeOnly = false
        var hadWeekday = false

        // Relative: через полчаса
        RELATIVE_HALF.find(lowerText)?.let { m ->
            spans += m.range
            val fire = zonedNow.plusMinutes(30)
            return finish(
                rawPhrase, text, spans, fire.toInstant(), warnings,
                confidence = 0.85f, relativeOnly = true,
            )
        }

        // Relative: через полтора часа
        RELATIVE_ONE_HALF.find(lowerText)?.let { m ->
            spans += m.range
            val fire = zonedNow.plusMinutes(90)
            return finish(
                rawPhrase, text, spans, fire.toInstant(), warnings,
                confidence = 0.85f, relativeOnly = true,
            )
        }

        // Relative: через N минут/часов/дней
        RELATIVE_DELTA.find(lowerText)?.let { m ->
            spans += m.range
            val amount = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: 1
            when {
                m.groupValues[2].startsWith("мин") -> {
                    val fire = zonedNow.plusMinutes(amount.toLong())
                    return finish(
                        rawPhrase, text, spans, fire.toInstant(), warnings,
                        confidence = 0.85f, relativeOnly = true,
                    )
                }
                m.groupValues[2].startsWith("ч") -> {
                    val fire = zonedNow.plusHours(amount.toLong())
                    return finish(
                        rawPhrase, text, spans, fire.toInstant(), warnings,
                        confidence = 0.85f, relativeOnly = true,
                    )
                }
                else -> {
                    date = zonedNow.toLocalDate().plusDays(amount.toLong())
                    hadExplicitDate = true
                    relativeOnly = true
                }
            }
        }

        // Day words
        when {
            DAY_TOMORROW.containsMatchIn(lowerText) -> {
                spans += DAY_TOMORROW.find(lowerText)!!.range
                date = zonedNow.toLocalDate().plusDays(1)
                hadExplicitDate = true
            }
            DAY_AFTER_TOMORROW.containsMatchIn(lowerText) -> {
                spans += DAY_AFTER_TOMORROW.find(lowerText)!!.range
                date = zonedNow.toLocalDate().plusDays(2)
                hadExplicitDate = true
            }
            DAY_TODAY.containsMatchIn(lowerText) -> {
                spans += DAY_TODAY.find(lowerText)!!.range
                hadTodayWord = true
                hadExplicitDate = true
            }
        }

        // Weekday
        WEEKDAY.find(lowerText)?.let { m ->
            spans += m.range
            val dow = weekdayFromGroup(m.groupValues[1])
            date = nextOrSameWeekday(zonedNow.toLocalDate(), dow, zonedNow.toLocalTime())
            hadExplicitDate = true
            hadWeekday = true
        }

        // Absolute date dd.MM.yyyy
        DATE_DMY.find(lowerText)?.let { m ->
            spans += m.range
            val d = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            var y = m.groupValues[3].toIntOrNull() ?: zonedNow.year
            var candidate = try {
                LocalDate.of(y, mo, d)
            } catch (_: Exception) {
                null
            }
            if (candidate != null && m.groupValues[3].isEmpty() && candidate.isBefore(zonedNow.toLocalDate())) {
                candidate = candidate.plusYears(1)
                warnings += ParseWarning.DATE_MISSING_YEAR
            }
            if (candidate != null) {
                date = candidate
                hadExplicitDate = true
            }
        }

        // 25 мая [2026]
        DATE_DAY_MONTH.find(lowerText)?.let { m ->
            spans += m.range
            val d = m.groupValues[1].toInt()
            val month = monthFromName(m.groupValues[2])
            var y = m.groupValues[3].toIntOrNull() ?: zonedNow.year
            var candidate = LocalDate.of(y, month, d)
            if (m.groupValues[3].isEmpty() && candidate.isBefore(zonedNow.toLocalDate())) {
                candidate = candidate.plusYears(1)
                warnings += ParseWarning.DATE_MISSING_YEAR
            }
            date = candidate
            hadExplicitDate = true
        }

        // Time HH:mm
        TIME_COLON.find(lowerText)?.let { m ->
            spans += m.range
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) {
                time = LocalTime.of(h, min)
                hadExplicitTime = true
            }
        }

        // в 9 вечера / в 10 утра / в 3 дня / в 2 ночи
        if (time == null) {
            TIME_HOURS_PART.find(lowerText)?.let { m ->
                spans += m.range
                val hour = m.groupValues[1].toInt()
                val part = m.groupValues[2]
                val h = when (part) {
                    "утра", "утром" -> hour
                    "дня", "днём", "днем" -> if (hour == 12) 12 else hour + 12
                    "вечера", "вечером" -> if (hour == 12) 12 else hour + 12
                    "ночи", "ночью" -> if (hour == 12) 0 else hour
                    else -> hour
                }
                time = LocalTime.of(h.coerceIn(0, 23), 0)
                hadExplicitTime = true
            }
        }

        // в полночь / в полдень / в полдня / в 12 ночи / в 12 дня ...
        if (time == null) {
            TIME_MIDNIGHT_NOON.find(lowerText)?.let { m ->
                spans += m.range
                val token = m.groupValues[1]
                time = when {
                    token.startsWith("полночь") || token.contains("ночи") -> LocalTime.of(0, 0)
                    else -> LocalTime.of(12, 0)
                }
                hadExplicitTime = true
            }
        }

        // в N часов [M минут]
        if (time == null) {
            TIME_HOURS_MIN.find(lowerText)?.let { m ->
                spans += m.range
                time = LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt())
                hadExplicitTime = true
            }
        }
        if (time == null) {
            TIME_HOURS.find(lowerText)?.let { m ->
                spans += m.range
                val h = m.groupValues[1].toInt()
                time = LocalTime.of(h, 0)
                hadExplicitTime = true
                if (h in 1..11 && !PART_OF_DAY.containsMatchIn(lowerText)) {
                    warnings += ParseWarning.TIME_AMBIGUOUS
                }
            }
        }

        // в N (short ambiguous, e.g. "в 9")
        if (time == null) {
            TIME_HOURS_SHORT.find(lowerText)?.let { m ->
                spans += m.range
                val h = m.groupValues[1].toInt()
                if (h in 0..23) {
                    time = LocalTime.of(h, 0)
                    hadExplicitTime = true
                    if (h in 1..11) {
                        warnings += ParseWarning.TIME_AMBIGUOUS
                    }
                }
            }
        }

        // утром / вечером (standalone or override)
        PART_OF_DAY.find(lowerText)?.let { m ->
            spans += m.range
            time = partOfDayTime(m.groupValues[1])
            hadExplicitTime = true
            usedPartOfDay = true
        }

        var fireAt: Instant? = null
        if (hadExplicitTime && time != null) {
            var dt = LocalDateTime.of(date, time)
            fireAt = dt.atZone(zone).toInstant()
        } else if (relativeOnly && hadExplicitDate) {
            fireAt = date.atTime(DEFAULT_MORNING).atZone(zone).toInstant()
            warnings += ParseWarning.NO_TIME_FOUND
        }

        if (fireAt == null && !hadExplicitTime) {
            warnings += ParseWarning.NO_TIME_FOUND
        }

        // Past time adjustment
        if (fireAt != null && fireAt.isBefore(now)) {
            when {
                hadTodayWord -> {
                    fireAt = fireAt.plus(1, ChronoUnit.DAYS)
                    warnings += ParseWarning.PAST_TIME_ADJUSTED
                }
                hadWeekday -> {
                    fireAt = fireAt.plus(7, ChronoUnit.DAYS)
                    warnings += ParseWarning.PAST_TIME_ADJUSTED
                }
            }
        }

        val confidence = computeConfidence(
            hadExplicitDate = hadExplicitDate,
            hadExplicitTime = hadExplicitTime,
            usedPartOfDay = usedPartOfDay,
            warnings = warnings,
            relativeOnly = relativeOnly,
        )

        return finish(rawPhrase, text, spans, fireAt, warnings, confidence, relativeOnly)
    }

    private fun finish(
        rawPhrase: String,
        text: String,
        spans: List<IntRange>,
        fireAt: Instant?,
        warnings: MutableList<ParseWarning>,
        confidence: Float,
        relativeOnly: Boolean,
    ): ParseResult {
        val body = extractBody(text, spans)
        val w = warnings.toMutableList()
        if (body.isBlank()) w += ParseWarning.BODY_EMPTY
        val mergedSpan = spans.minByOrNull { it.first }?.let { first ->
            val last = spans.maxByOrNull { it.last }!!
            first.first..last.last
        }
        return ParseResult(
            fireAt = fireAt,
            body = body,
            confidence = confidence,
            warnings = w.distinct(),
            matchedTimeSpan = mergedSpan,
            rawPhrase = rawPhrase,
        )
    }

    private fun emptyResult(rawPhrase: String, warnings: MutableList<ParseWarning>): ParseResult {
        warnings += ParseWarning.NO_TIME_FOUND
        warnings += ParseWarning.BODY_EMPTY
        return ParseResult(null, "", 0f, warnings.distinct(), null, rawPhrase)
    }

    private fun stripPrefixes(lower: String): String {
        var s = lower
        var changed = true
        while (changed) {
            changed = false
            PREFIXES.forEach { prefix ->
                if (s.startsWith(prefix)) {
                    s = s.removePrefix(prefix).trimStart()
                    changed = true
                }
            }
        }
        return s
    }

    private fun extractBody(text: String, spans: List<IntRange>): String {
        if (spans.isEmpty()) return text.trim().trim(',', '.', ' ')
        val sorted = spans.sortedByDescending { it.first }
        val sb = StringBuilder(text)
        sorted.forEach { range ->
            val start = range.first.coerceIn(0, sb.length)
            val end = (range.last + 1).coerceIn(0, sb.length)
            if (start < end) sb.delete(start, end)
        }
        return sb.toString()
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd(',', '.')
            .trimStart(',', '.')
    }

    private fun computeConfidence(
        hadExplicitDate: Boolean,
        hadExplicitTime: Boolean,
        usedPartOfDay: Boolean,
        warnings: List<ParseWarning>,
        relativeOnly: Boolean,
    ): Float {
        if (warnings.contains(ParseWarning.NO_TIME_FOUND)) return 0f
        if (warnings.contains(ParseWarning.TIME_AMBIGUOUS)) return 0.5f
        if (relativeOnly && hadExplicitTime) return 0.85f
        if (hadExplicitDate && hadExplicitTime && !usedPartOfDay) return 0.92f
        if (hadExplicitDate && usedPartOfDay) return 0.75f
        if (hadExplicitTime) return 0.8f
        return 0.6f
    }

    private fun nextOrSameWeekday(today: LocalDate, target: DayOfWeek, nowTime: LocalTime): LocalDate {
        var d = today
        while (d.dayOfWeek != target) {
            d = d.plusDays(1)
        }
        return d
    }

    private fun weekdayFromGroup(word: String): DayOfWeek = when (word.lowercase()) {
        "понедельник" -> DayOfWeek.MONDAY
        "вторник" -> DayOfWeek.TUESDAY
        "среду", "среда" -> DayOfWeek.WEDNESDAY
        "четверг" -> DayOfWeek.THURSDAY
        "пятницу", "пятница" -> DayOfWeek.FRIDAY
        "субботу", "суббота" -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun monthFromName(name: String): Int = when (name.lowercase()) {
        "января" -> 1
        "февраля" -> 2
        "марта" -> 3
        "апреля" -> 4
        "мая" -> 5
        "июня" -> 6
        "июля" -> 7
        "августа" -> 8
        "сентября" -> 9
        "октября" -> 10
        "ноября" -> 11
        else -> 12
    }

    private fun partOfDayTime(token: String): LocalTime = when (token.lowercase()) {
        "утром" -> LocalTime.of(9, 0)
        "днём", "днем" -> LocalTime.of(13, 0)
        "вечером" -> LocalTime.of(19, 0)
        else -> LocalTime.of(22, 0)
    }

    companion object {
        private val DEFAULT_MORNING = LocalTime.of(9, 0)

        /** Java \\b не работает с кириллицей — свои границы слова. */
        private const val WB = """(?<![\p{L}\d])"""
        private const val WE = """(?![\p{L}\d])"""

        private val PREFIXES = listOf(
            "напомни",
            "напоминание",
            "нужно",
            "не забудь",
        )

        private val RELATIVE_DELTA = Regex(
            """через\s+(?:(\d+)\s*)?(минут|минуты|минуту|мин|час|часа|часов|ч|день|дня|дней)""",
        )
        private val RELATIVE_HALF = Regex("""${WB}через\s+полчаса${WE}""")
        private val RELATIVE_ONE_HALF = Regex("""${WB}через\s+полтора\s+часа${WE}""")
        private val DAY_TODAY = Regex("""${WB}сегодня${WE}""")
        private val DAY_TOMORROW = Regex("""${WB}завтра${WE}""")
        private val DAY_AFTER_TOMORROW = Regex("""${WB}послезавтра${WE}""")
        private val WEEKDAY = Regex(
            """${WB}[вво]+\s+(понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье|воскресенья)${WE}""",
        )
        // Только с «в », чтобы не спутать 01.06.2026 с временем 6:20
        private val TIME_COLON = Regex("""${WB}в\s+(\d{1,2})[:.](\d{2})""")
        private val TIME_HOURS_PART = Regex("""${WB}в\s+(\d{1,2})\s+(утра|утром|дня|днём|днем|вечера|вечером|ночи|ночью)${WE}""")
        private val TIME_MIDNIGHT_NOON = Regex("""${WB}в\s+(полночь|полдень|полдня|12\s+ночи|12\s+дня|12\s+утра|12\s+вечера)${WE}""")
        private val TIME_HOURS = Regex("""${WB}в\s+(\d{1,2})\s+час(?:а|ов)?${WE}""")
        private val TIME_HOURS_MIN = Regex(
            """${WB}в\s+(\d{1,2})\s+час(?:а|ов)?\s+(\d{1,2})\s+минут""",
        )
        private val TIME_HOURS_SHORT = Regex("""${WB}в\s+(\d{1,2})${WE}""")
        private val PART_OF_DAY = Regex("""${WB}(утром|днём|днем|вечером|ночью)${WE}""")
        private val DATE_DMY = Regex("""${WB}(\d{1,2})\.(\d{1,2})(?:\.(\d{4}))?${WE}""")
        private val DATE_DAY_MONTH = Regex(
            """${WB}(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)(?:\s+(\d{4}))?${WE}""",
        )
    }
}
