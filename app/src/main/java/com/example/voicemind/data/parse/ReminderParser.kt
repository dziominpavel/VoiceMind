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

        // Если exact time пересекается с HOURS_PART — возьмём широкий span, чтобы маркер не утёк в body
        if (bestTime != null && (bestTime.type == TimeType.HH_MM || bestTime.type == TimeType.HHMM)) {
            candidates.timeCandidates.find {
                it.type == TimeType.HOURS_PART &&
                    it.span.first <= bestTime.span.first &&
                    it.span.last >= bestTime.span.last
            }?.let { spans += it.span }
        }

        var date = bestDate?.date ?: zonedNow.toLocalDate()
        var time = bestTime?.time
        var hadExplicitDate = bestDate != null
        var hadExplicitTime = bestTime != null
        var hadTodayWord = bestDate?.type == DateType.TODAY
        var hadWeekday = bestDate?.type == DateType.WEEKDAY || bestDate?.type == DateType.NEXT_WEEKDAY || bestDate?.type == DateType.WEEKEND
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
        } else if (hadExplicitDate) {
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
                hadExplicitTime && !hadExplicitDate -> {
                    fireAt = fireAt.plus(1, ChronoUnit.DAYS)
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

    private enum class DateType { RELATIVE_DAYS, TODAY, TOMORROW, DAY_AFTER_TOMORROW, WEEKDAY, NEXT_WEEKDAY, WEEKEND, DMY, DAY_MONTH, ORDINAL_DAY }
    private enum class TimeType { HH_MM, HHMM, HOURS_MINUTES, HOURS_PART, MIDNIGHT_NOON, HOURS_WORD, HOURS_SHORT, PART_OF_DAY, HALF_PAST, QUARTER_TO, QUARTER_PAST, HALF_WITH, PART_PREFIX }

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

        if (relativeInstant == null) {
            RELATIVE_COUPLE_HOURS.find(lowerText)?.let { m ->
                relativeSpans += m.range
                relativeInstant = zonedNow.plusHours(2).toInstant()
            }
        }

        if (relativeInstant == null) {
            RELATIVE_FEW_MINUTES.find(lowerText)?.let { m ->
                relativeSpans += m.range
                relativeInstant = zonedNow.plusMinutes(5).toInstant()
            }
        }

        RELATIVE_WEEK.find(lowerText)?.let { m ->
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate().plusDays(7),
                span = m.range,
                type = DateType.RELATIVE_DAYS,
                score = 45,
                relativeOnly = true,
            )
        }

        RELATIVE_MONTH.findAll(lowerText).forEach { m ->
            val months = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: 1
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate().plusMonths(months.toLong()),
                span = m.range,
                type = DateType.RELATIVE_DAYS,
                score = 45,
                relativeOnly = true,
            )
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

        NEXT_WEEKDAY.findAll(lowerText).forEach { m ->
            val dow = weekdayFromGroup(m.groupValues[2])
            dateCandidates += DateCandidate(
                date = nextWeekday(zonedNow.toLocalDate(), dow),
                span = m.range,
                type = DateType.NEXT_WEEKDAY,
                score = 85,
            )
        }

        WEEKEND.find(lowerText)?.let { m ->
            dateCandidates += DateCandidate(
                date = nextOrSameWeekday(zonedNow.toLocalDate(), DayOfWeek.SATURDAY, zonedNow.toLocalTime()),
                span = m.range,
                type = DateType.WEEKEND,
                score = 75,
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
            val monthName = m.groupValues[2]
            val month = monthFromName(monthName).let { if (it == 12 && (monthName.endsWith("ь") || monthName == "май" || monthName.endsWith("м"))) monthFromNominative(monthName) else it }
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

        DATE_ORDINAL.findAll(lowerText).forEach { m ->
            val day = ordinalDayFromGroup(m.groupValues[1])
            if (day in 1..31) {
                var candidate = LocalDate.of(zonedNow.year, zonedNow.monthValue, day)
                if (candidate.isBefore(zonedNow.toLocalDate())) {
                    candidate = candidate.plusMonths(1)
                }
                dateCandidates += DateCandidate(
                    date = candidate,
                    span = m.range,
                    type = DateType.ORDINAL_DAY,
                    score = 85,
                )
            }
        }

        DATE_ORDINAL_DIGIT.findAll(lowerText).forEach { m ->
            val day = m.groupValues[1].toInt()
            if (day in 1..31) {
                var candidate = LocalDate.of(zonedNow.year, zonedNow.monthValue, day)
                if (candidate.isBefore(zonedNow.toLocalDate())) {
                    candidate = candidate.plusMonths(1)
                }
                dateCandidates += DateCandidate(
                    date = candidate,
                    span = m.range,
                    type = DateType.ORDINAL_DAY,
                    score = 85,
                )
            }
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
                        score = 90,
                    )
                }
            }
        }

        TIME_HOURS_PART.findAll(lowerText).forEach { m ->
            val hour = m.groupValues[1].toInt()
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val part = m.groupValues[3]
            val h = when (part) {
                "утра", "утром" -> hour
                "дня", "днём", "днем" -> if (hour >= 12) hour else hour + 12
                "вечера", "вечером" -> if (hour >= 12) hour else hour + 12
                "ночи", "ночью" -> if (hour == 12) 0 else hour
                else -> hour
            }
            val score = when (part) {
                "вечера", "вечером", "ночи", "ночью" -> 100
                else -> 80
            }
            timeCandidates += TimeCandidate(
                time = LocalTime.of(h.coerceIn(0, 23), min),
                span = m.range,
                type = TimeType.HOURS_PART,
                score = score,
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

        TIME_HALF_PAST.findAll(lowerText).forEach { m ->
            val hourWord = m.groupValues[1]
            val h = hourWordToInt(hourWord)
            if (h > 0) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of((h - 1).coerceIn(0, 23), 30),
                    span = m.range,
                    type = TimeType.HALF_PAST,
                    score = 90,
                )
            }
        }

        TIME_QUARTER_TO.findAll(lowerText).forEach { m ->
            val minuteWord = m.groupValues[1]
            val minutes = when {
                minuteWord == "четверть" -> 15
                minuteWord == "пятнадцати" || minuteWord == "15" -> 15
                else -> 15
            }
            val hourWord = m.groupValues[2]
            val h = hourWordToInt(hourWord)
            if (h > 0) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of((h - 1).coerceIn(0, 23), 60 - minutes),
                    span = m.range,
                    type = TimeType.QUARTER_TO,
                    score = 90,
                )
            }
        }

        TIME_QUARTER_PAST.findAll(lowerText).forEach { m ->
            val hourWord = m.groupValues[1]
            val h = hourWordToInt(hourWord)
            if (h > 0) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of((h - 1).coerceIn(0, 23), 15),
                    span = m.range,
                    type = TimeType.QUARTER_PAST,
                    score = 90,
                )
            }
        }

        TIME_HALF_WITH.findAll(lowerText).forEach { m ->
            val hourWord = m.groupValues[1]
            val h = hourWordToInt(hourWord)
            if (h > 0) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h.coerceIn(0, 23), 30),
                    span = m.range,
                    type = TimeType.HALF_WITH,
                    score = 85,
                )
            }
        }

        TIME_PART_PREFIX.findAll(lowerText).forEach { m ->
            val part = m.groupValues[1]
            val hour = m.groupValues[2].toInt()
            val h = when (part.lowercase()) {
                "утром" -> hour
                "днём", "днем" -> if (hour == 12) 12 else hour + 12
                "вечером" -> if (hour == 12) 12 else hour + 12
                "ночью" -> if (hour == 12) 0 else hour
                else -> hour
            }
            timeCandidates += TimeCandidate(
                time = LocalTime.of(h.coerceIn(0, 23), 0),
                span = m.range,
                type = TimeType.PART_PREFIX,
                score = 85,
            )
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
        val remove = BooleanArray(text.length)
        spans.forEach { range ->
            val start = range.first.coerceIn(0, text.length)
            val end = (range.last + 1).coerceIn(0, text.length)
            for (i in start until end) remove[i] = true
        }
        val sb = StringBuilder()
        text.forEachIndexed { index, char ->
            if (!remove[index]) sb.append(char)
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

    private fun ordinalDayFromGroup(phrase: String): Int {
        val normalized = phrase.lowercase().replace("-", " ").replace(Regex("\\s+"), " ").trim()
        return ORDINAL_DAY_MAP[normalized] ?: -1
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

    private fun monthFromNominative(name: String): Int = when (name.lowercase()) {
        "январь" -> 1
        "февраль" -> 2
        "март" -> 3
        "апрель" -> 4
        "май" -> 5
        "июнь" -> 6
        "июль" -> 7
        "август" -> 8
        "сентябрь" -> 9
        "октябрь" -> 10
        "ноябрь" -> 11
        else -> 12
    }

    private fun hourWordToInt(word: String): Int {
        return when (word.lowercase().trim()) {
            "один", "первого" -> 1
            "два", "второго" -> 2
            "три", "третьего" -> 3
            "четыре", "четвёртого", "четвертого" -> 4
            "пять", "пятого" -> 5
            "шесть", "шестого" -> 6
            "семь", "седьмого" -> 7
            "восемь", "восьмого" -> 8
            "девять", "девятого" -> 9
            "десять", "десятого" -> 10
            "одиннадцать", "одиннадцатого" -> 11
            "двенадцать", "двенадцатого" -> 12
            else -> -1
        }
    }

    private fun nextWeekday(today: LocalDate, target: DayOfWeek): LocalDate {
        var d = today
        while (d.dayOfWeek != target) {
            d = d.plusDays(1)
        }
        return d.plusDays(7)
    }

    private fun partOfDayTime(token: String): LocalTime = when (token.lowercase()) {
        "утром" -> LocalTime.of(9, 0)
        "днём", "днем" -> LocalTime.of(13, 0)
        "вечером" -> LocalTime.of(22, 0)
        "ночью" -> LocalTime.of(1, 0)
        else -> LocalTime.of(1, 0)
    }

    companion object {
        private val DEFAULT_MORNING = LocalTime.of(9, 0)

        /** Java \\b не работает с кириллицей — свои границы слова. */
        private const val WB = """(?<![\p{L}\d])"""
        private const val WE = """(?![\p{L}\d])"""

        private val PREFIXES = listOf(
            "сделай напоминание",
            "поставь напоминалку",
            "чтобы не забыть",
            "напомни мне",
            "не забудь",
            "напоминание",
            "напомни",
            "нужно",
        )

        private val RELATIVE_DELTA = Regex(
            """через\s+(?:(\d+)\s*)?(минуты|минуту|минут|мин|часа|часов|час|ч|дня|дней|день)""",
        )
        private val RELATIVE_HALF = Regex("""${WB}через\s+полчаса${WE}""")
        private val RELATIVE_ONE_HALF = Regex("""${WB}через\s+полтора\s+часа${WE}""")
        private val RELATIVE_COUPLE_HOURS = Regex("""${WB}через\s+пару\s+часов${WE}""")
        private val RELATIVE_FEW_MINUTES = Regex("""${WB}через\s+несколько\s+минут${WE}""")
        private val RELATIVE_WEEK = Regex("""${WB}через\s+неделю${WE}""")
        private val RELATIVE_MONTH = Regex("""${WB}через\s+(?:(\d+)\s+)?месяц(?:а|ев)?${WE}""")
        private val DAY_TODAY = Regex("""${WB}сегодня${WE}""")
        private val DAY_TOMORROW = Regex("""${WB}завтра${WE}""")
        private val DAY_AFTER_TOMORROW = Regex("""${WB}послезавтра${WE}""")
        private val WEEKDAY = Regex(
            """${WB}[вво]+\s+(понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье|воскресенья)${WE}""",
        )
        private val NEXT_WEEKDAY = Regex(
            """${WB}в\s+следующ(ий|ую)\s+(понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье|воскресенья)${WE}""",
        )
        private val WEEKEND = Regex("""${WB}на\s+выходных${WE}""")
        // Только с «в », чтобы не спутать 01.06.2026 с временем 6:20
        private val TIME_COLON = Regex("""${WB}(?:в\s+)?(\d{1,2})[:.](\d{2})""")
        private val TIME_HOURS_PART = Regex("""${WB}в\s+(\d{1,2})(?:[:.](\d{2}))?\s+(утра|утром|дня|днём|днем|вечера|вечером|ночи|ночью)${WE}""")
        private val TIME_MIDNIGHT_NOON = Regex("""${WB}в\s+(полночь|полдень|полдня|12\s+ночи|12\s+дня|12\s+утра|12\s+вечера)${WE}""")
        private val TIME_HOURS = Regex("""${WB}в\s+(\d{1,2})\s+час(?:а|ов)?${WE}""")
        private val TIME_HOURS_MIN = Regex(
            """${WB}в\s+(\d{1,2})\s+час(?:а|ов)?\s+(\d{1,2})\s+минут""",
        )
        private val TIME_HOURS_SHORT = Regex("""${WB}в\s+(\d{1,2})${WE}""")
        private val TIME_4DIGIT = Regex("""${WB}(?:в\s+)?(\d{1,2})\s?(\d{2})${WE}""")
        private val TIME_HALF_PAST = Regex("""${WB}(?:в\s+)?половин[ае]\s+(двенадцатого|одиннадцатого|десятого|девятого|восьмого|седьмого|шестого|пятого|четвёртого|четвертого|третьего|второго|первого)${WE}""")
        private val TIME_QUARTER_TO = Regex("""${WB}без\s+(четверти|пятнадцати|15)\s+(?:часа\s+)?(двенадцать|одиннадцать|десять|девять|восемь|семь|шесть|пять|четыре|три|два|один)${WE}""")
        private val TIME_QUARTER_PAST = Regex("""${WB}(?:в\s+)?четверть\s+(двенадцатого|одиннадцатого|десятого|девятого|восьмого|седьмого|шестого|пятого|четвёртого|четвертого|третьего|второго|первого)${WE}""")
        private val TIME_HALF_WITH = Regex("""${WB}(?:в\s+)?(двенадцать|одиннадцать|десять|девять|восемь|семь|шесть|пять|четыре|три|два|один)\s+с\s+половиной${WE}""")
        private val TIME_PART_PREFIX = Regex("""${WB}(утром|днём|днем|вечером|ночью)\s+в\s+(\d{1,2})${WE}""")
        private val PART_OF_DAY = Regex("""${WB}(утром|днём|днем|вечером|ночью)${WE}""")
        private val DATE_DMY = Regex("""${WB}(\d{1,2})\.(\d{1,2})(?:\.(\d{4}))?${WE}""")
        private val DATE_DAY_MONTH = Regex(
            """${WB}(\d{1,2})\s+(января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря|январь|февраль|март|апрель|май|июнь|июль|август|сентябрь|октябрь|ноябрь|декабрь)(?:\s+(\d{4}))?${WE}""",
        )

        private val ORDINAL_WORDS = listOf(
            "первого", "второго", "третьего",
            "четвёртого", "четвертого", "пятого", "шестого", "седьмого", "восьмого", "девятого", "десятого",
            "одиннадцатого", "двенадцатого", "тринадцатого", "четырнадцатого", "пятнадцатого",
            "шестнадцатого", "семнадцатого", "восемнадцатого", "девятнадцатого", "двадцатого",
            "двадцать первого", "двадцать второго", "двадцать третьего",
            "двадцать четвёртого", "двадцать четвертого", "двадцать пятого", "двадцать шестого",
            "двадцать седьмого", "двадцать восьмого", "двадцать девятого", "тридцатого",
            "тридцать первого",
        )

        private val ORDINAL_DAY_MAP: Map<String, Int> = mapOf(
            "первого" to 1,
            "второго" to 2,
            "третьего" to 3,
            "четвёртого" to 4,
            "четвертого" to 4,
            "пятого" to 5,
            "шестого" to 6,
            "седьмого" to 7,
            "восьмого" to 8,
            "девятого" to 9,
            "десятого" to 10,
            "одиннадцатого" to 11,
            "двенадцатого" to 12,
            "тринадцатого" to 13,
            "четырнадцатого" to 14,
            "пятнадцатого" to 15,
            "шестнадцатого" to 16,
            "семнадцатого" to 17,
            "восемнадцатого" to 18,
            "девятнадцатого" to 19,
            "двадцатого" to 20,
            "двадцать первого" to 21,
            "двадцать второго" to 22,
            "двадцать третьего" to 23,
            "двадцать четвёртого" to 24,
            "двадцать четвертого" to 24,
            "двадцать пятого" to 25,
            "двадцать шестого" to 26,
            "двадцать седьмого" to 27,
            "двадцать восьмого" to 28,
            "двадцать девятого" to 29,
            "тридцатого" to 30,
            "тридцать первого" to 31,
        )

        private val DATE_ORDINAL = Regex(
            """${WB}(${ORDINAL_WORDS.joinToString("|")})\s+числа${WE}""",
        )
        private val DATE_ORDINAL_DIGIT = Regex("""${WB}(\d{1,2})(?:-го)?\s+числа${WE}""")
    }
}
