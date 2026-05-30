package com.example.voicemind.data.parse

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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
        val normalized = normalize(rawPhrase)
        if (normalized.text.isEmpty()) {
            return emptyResult(rawPhrase, warnings)
        }

        val zonedNow = now.atZone(zone)
        val candidates = findAllCandidates(normalized.lowerText, zonedNow)

        // Early-return for pure relative time (через N минут/часов/полчаса/полтора)
        if (candidates.relativeInstant != null) {
            return finish(
                rawPhrase = rawPhrase,
                text = normalized.text,
                spans = candidates.relativeSpans,
                fireAt = candidates.relativeInstant,
                warnings = warnings,
                confidence = 0.85f,
                relativeOnly = true,
            )
        }

        val bestDate = resolveBestDate(candidates.dateCandidates)
        val bestTime = resolveBestTime(candidates.timeCandidates)

        val spans = mutableListOf<IntRange>()
        bestDate?.span?.let { spans += it }
        bestTime?.span?.let { spans += it }

        var date = bestDate?.date ?: zonedNow.toLocalDate()
        var time = bestTime?.time
        var hadExplicitDate = bestDate != null
        var hadExplicitTime = bestTime != null
        var hadTodayWord = bestDate?.type == DateType.TODAY
        var hadWeekday = bestDate?.type == DateType.WEEKDAY
        var usedPartOfDay = bestTime?.type == TimeType.PART_OF_DAY
        var relativeOnly = bestDate?.relativeOnly == true

        if (bestDate?.missingYear == true) {
            warnings += ParseWarning.DATE_MISSING_YEAR
        }

        // TIME_AMBIGUOUS warning for short hour forms
        if (bestTime != null && (bestTime.type == TimeType.HOURS_WORD || bestTime.type == TimeType.HOURS_SHORT)) {
            val hour = bestTime.time.hour
            if (hour in 1..11 && !PART_OF_DAY.containsMatchIn(normalized.lowerText)) {
                warnings += ParseWarning.TIME_AMBIGUOUS
            }
        }

        var fireAt: Instant? = null
        if (hadExplicitTime && time != null) {
            val dt = LocalDateTime.of(date, time)
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

        return finish(rawPhrase, normalized.text, spans, fireAt, warnings, confidence, relativeOnly)
    }

    // --- Normalization ---

    private data class NormalizedPhrase(
        val text: String,
        val lowerText: String,
    )

    private fun normalize(rawPhrase: String): NormalizedPhrase {
        val trimmed = rawPhrase.trim()
        val lower = trimmed.lowercase(Locale.forLanguageTag("ru"))
        val stripped = stripPrefixes(lower)
        val prefixStrip = lower.length - stripped.length
        val text = trimmed.substring(prefixStrip)
        return NormalizedPhrase(text, text.lowercase(Locale.forLanguageTag("ru")))
    }

    // --- Candidate extraction ---

    private data class CandidateSet(
        val relativeInstant: Instant?,
        val relativeSpans: List<IntRange>,
        val dateCandidates: List<DateCandidate>,
        val timeCandidates: List<TimeCandidate>,
    )

    private data class DateCandidate(
        val date: LocalDate,
        val span: IntRange,
        val type: DateType,
        val score: Int,
        val relativeOnly: Boolean = false,
        val missingYear: Boolean = false,
    )

    private data class TimeCandidate(
        val time: LocalTime,
        val span: IntRange,
        val type: TimeType,
        val score: Int,
    )

    private enum class DateType { RELATIVE_DAYS, TODAY, TOMORROW, DAY_AFTER_TOMORROW, WEEKDAY, DMY, DAY_MONTH }
    private enum class TimeType { HH_MM, HHMM, HOURS_MINUTES, HOURS_PART, MIDNIGHT_NOON, HOURS_WORD, HOURS_SHORT, PART_OF_DAY }

    private fun findAllCandidates(lowerText: String, zonedNow: ZonedDateTime): CandidateSet {
        val dateCandidates = mutableListOf<DateCandidate>()
        val timeCandidates = mutableListOf<TimeCandidate>()
        var relativeInstant: Instant? = null
        val relativeSpans = mutableListOf<IntRange>()

        RELATIVE_HALF.find(lowerText)?.let { m ->
            relativeSpans += m.range
            relativeInstant = zonedNow.plusMinutes(30).toInstant()
        }

        if (relativeInstant == null) {
            RELATIVE_ONE_HALF.find(lowerText)?.let { m ->
                relativeSpans += m.range
                relativeInstant = zonedNow.plusMinutes(90).toInstant()
            }
        }

        if (relativeInstant == null) {
            RELATIVE_DELTA.find(lowerText)?.let { m ->
                val amount = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: 1
                val unitWord = m.groupValues[2]
                when {
                    unitWord.startsWith("мин") -> {
                        relativeSpans += m.range
                        relativeInstant = zonedNow.plusMinutes(amount.toLong()).toInstant()
                    }
                    unitWord.startsWith("ч") -> {
                        relativeSpans += m.range
                        relativeInstant = zonedNow.plusHours(amount.toLong()).toInstant()
                    }
                    else -> {
                        dateCandidates += DateCandidate(
                            date = zonedNow.toLocalDate().plusDays(amount.toLong()),
                            span = m.range,
                            type = DateType.RELATIVE_DAYS,
                            score = 40,
                            relativeOnly = true,
                        )
                    }
                }
            }
        }

        DAY_AFTER_TOMORROW.find(lowerText)?.let { m ->
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate().plusDays(2),
                span = m.range,
                type = DateType.DAY_AFTER_TOMORROW,
                score = 70,
            )
        }
        DAY_TOMORROW.find(lowerText)?.let { m ->
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate().plusDays(1),
                span = m.range,
                type = DateType.TOMORROW,
                score = 60,
            )
        }
        DAY_TODAY.find(lowerText)?.let { m ->
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate(),
                span = m.range,
                type = DateType.TODAY,
                score = 50,
            )
        }

        WEEKDAY.findAll(lowerText).forEach { m ->
            val dow = weekdayFromGroup(m.groupValues[1])
            dateCandidates += DateCandidate(
                date = nextOrSameWeekday(zonedNow.toLocalDate(), dow, zonedNow.toLocalTime()),
                span = m.range,
                type = DateType.WEEKDAY,
                score = 80,
            )
        }

        DATE_DMY.findAll(lowerText).forEach { m ->
            val d = m.groupValues[1].toInt()
            val mo = m.groupValues[2].toInt()
            var y = m.groupValues[3].toIntOrNull() ?: zonedNow.year
            var candidate = try { LocalDate.of(y, mo, d) } catch (_: Exception) { null }
            var missingYear = false
            if (candidate != null) {
                if (m.groupValues[3].isEmpty() && candidate.isBefore(zonedNow.toLocalDate())) {
                    candidate = candidate.plusYears(1)
                    missingYear = true
                }
                dateCandidates += DateCandidate(
                    date = candidate,
                    span = m.range,
                    type = DateType.DMY,
                    score = 100,
                    missingYear = missingYear,
                )
            }
        }

        DATE_DAY_MONTH.findAll(lowerText).forEach { m ->
            val d = m.groupValues[1].toInt()
            val month = monthFromName(m.groupValues[2])
            var y = m.groupValues[3].toIntOrNull() ?: zonedNow.year
            var candidate = LocalDate.of(y, month, d)
            var missingYear = false
            if (m.groupValues[3].isEmpty() && candidate.isBefore(zonedNow.toLocalDate())) {
                candidate = candidate.plusYears(1)
                missingYear = true
            }
            dateCandidates += DateCandidate(
                date = candidate,
                span = m.range,
                type = DateType.DAY_MONTH,
                score = 90,
                missingYear = missingYear,
            )
        }

        val dateSpans = dateCandidates.map { it.span }

        TIME_COLON.findAll(lowerText).forEach { m ->
            if (!isInsideDateSpan(m.range, dateSpans)) {
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toInt()
                if (h in 0..23 && min in 0..59) {
                    timeCandidates += TimeCandidate(
                        time = LocalTime.of(h, min),
                        span = m.range,
                        type = TimeType.HH_MM,
                        score = 100,
                    )
                }
            }
        }

        TIME_HOURS_PART.findAll(lowerText).forEach { m ->
            val hour = m.groupValues[1].toInt()
            val part = m.groupValues[2]
            val h = when (part) {
                "утра", "утром" -> hour
                "дня", "днём", "днем" -> if (hour == 12) 12 else hour + 12
                "вечера", "вечером" -> if (hour == 12) 12 else hour + 12
                "ночи", "ночью" -> if (hour == 12) 0 else hour
                else -> hour
            }
            timeCandidates += TimeCandidate(
                time = LocalTime.of(h.coerceIn(0, 23), 0),
                span = m.range,
                type = TimeType.HOURS_PART,
                score = 80,
            )
        }

        TIME_MIDNIGHT_NOON.findAll(lowerText).forEach { m ->
            val token = m.groupValues[1]
            val t = when {
                token.startsWith("полночь") || token.contains("ночи") -> LocalTime.of(0, 0)
                else -> LocalTime.of(12, 0)
            }
            timeCandidates += TimeCandidate(
                time = t,
                span = m.range,
                type = TimeType.MIDNIGHT_NOON,
                score = 75,
            )
        }

        TIME_HOURS_MIN.findAll(lowerText).forEach { m ->
            timeCandidates += TimeCandidate(
                time = LocalTime.of(m.groupValues[1].toInt(), m.groupValues[2].toInt()),
                span = m.range,
                type = TimeType.HOURS_MINUTES,
                score = 90,
            )
        }

        TIME_HOURS.findAll(lowerText).forEach { m ->
            val h = m.groupValues[1].toInt()
            timeCandidates += TimeCandidate(
                time = LocalTime.of(h, 0),
                span = m.range,
                type = TimeType.HOURS_WORD,
                score = 60,
            )
        }

        TIME_HOURS_SHORT.findAll(lowerText).forEach { m ->
            val h = m.groupValues[1].toInt()
            if (h in 0..23) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h, 0),
                    span = m.range,
                    type = TimeType.HOURS_SHORT,
                    score = 50,
                )
            }
        }

        TIME_4DIGIT.findAll(lowerText).forEach { m ->
            if (!isInsideDateSpan(m.range, dateSpans)) {
                val h = m.groupValues[1].toInt()
                val min = m.groupValues[2].toInt()
                if (h in 0..23 && min in 0..59) {
                    timeCandidates += TimeCandidate(
                        time = LocalTime.of(h, min),
                        span = m.range,
                        type = TimeType.HHMM,
                        score = 95,
                    )
                }
            }
        }

        PART_OF_DAY.findAll(lowerText).forEach { m ->
            timeCandidates += TimeCandidate(
                time = partOfDayTime(m.groupValues[1]),
                span = m.range,
                type = TimeType.PART_OF_DAY,
                score = 40,
            )
        }

        return CandidateSet(relativeInstant, relativeSpans, dateCandidates, timeCandidates)
    }

    private fun resolveBestDate(candidates: List<DateCandidate>): DateCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.maxWith(compareBy({ it.score }, { -it.span.first }))
    }

    private fun resolveBestTime(candidates: List<TimeCandidate>): TimeCandidate? {
        if (candidates.isEmpty()) return null
        return candidates.maxWith(compareBy({ it.score }, { -it.span.first }))
    }

    private fun isInsideDateSpan(range: IntRange, dateSpans: List<IntRange>): Boolean {
        return dateSpans.any { range.first >= it.first && range.last <= it.last }
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
        private val TIME_COLON = Regex("""${WB}(?:в\s+)?(\d{1,2})[:.](\d{2})""")
        private val TIME_HOURS_PART = Regex("""${WB}в\s+(\d{1,2})\s+(утра|утром|дня|днём|днем|вечера|вечером|ночи|ночью)${WE}""")
        private val TIME_MIDNIGHT_NOON = Regex("""${WB}в\s+(полночь|полдень|полдня|12\s+ночи|12\s+дня|12\s+утра|12\s+вечера)${WE}""")
        private val TIME_HOURS = Regex("""${WB}в\s+(\d{1,2})\s+час(?:а|ов)?${WE}""")
        private val TIME_HOURS_MIN = Regex(
            """${WB}в\s+(\d{1,2})\s+час(?:а|ов)?\s+(\d{1,2})\s+минут""",
        )
        private val TIME_HOURS_SHORT = Regex("""${WB}в\s+(\d{1,2})${WE}""")
        private val TIME_4DIGIT = Regex("""${WB}(?:в\s+)?(\d{1,2})\s?(\d{2})${WE}""")
        private val PART_OF_DAY = Regex("""${WB}(утром|днём|днем|вечером|ночью)${WE}""")
        private val DATE_DMY = Regex("""${WB}(\d{1,2})\.(\d{1,2})(?:\.(\d{4}))?${WE}""")
        private val DATE_DAY_MONTH = Regex(
            """${WB}(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря)(?:\s+(\d{4}))?${WE}""",
        )
    }
}
