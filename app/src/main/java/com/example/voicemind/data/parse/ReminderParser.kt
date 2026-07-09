package com.example.voicemind.data.parse

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
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

        val (recurrenceRule, textWithoutRecurrence, lowerWithoutRecurrence) = extractRecurrence(
            normalized.text,
            normalized.lowerText,
        )
        val zonedNow = now.atZone(zone)
        val candidates = findAllCandidates(lowerWithoutRecurrence, zonedNow)

        // Early-return for pure relative time (через N минут/часов/полчаса/полтора)
        if (candidates.relativeInstant != null) {
            val relativeSpans = candidates.relativeSpans + candidates.dateCandidates.map { it.span }
            return finish(
                rawPhrase = rawPhrase,
                text = textWithoutRecurrence,
                spans = relativeSpans,
                fireAt = candidates.relativeInstant,
                warnings = warnings,
                confidence = 0.85f,
                relativeOnly = true,
                recurrenceRule = recurrenceRule,
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

        // Проигравшие маркеры суток всё равно убираем из body
        candidates.timeCandidates
            .filter { it.type == TimeType.PART_OF_DAY || it.type == TimeType.PART_PREFIX }
            .forEach { spans += it.span }

        var date = bestDate?.date ?: zonedNow.toLocalDate()
        var time = bestTime?.time
        var hadExplicitDate = bestDate != null
        var hadExplicitTime = bestTime != null
        var hadTodayWord = bestDate?.type == DateType.TODAY
        var hadWeekday = bestDate?.type == DateType.WEEKDAY || bestDate?.type == DateType.NEXT_WEEKDAY || bestDate?.type == DateType.WEEKEND
        var usedPartOfDay = bestTime?.type == TimeType.PART_OF_DAY
        var relativeOnly = bestDate?.relativeOnly == true
        var isApproximate = bestTime?.type == TimeType.WORD_HOUR && bestTime.score < 60
        var isTimeRange = bestTime?.type == TimeType.WORD_RANGE
        var isHoliday = bestDate?.type == DateType.HOLIDAY

        if (bestDate?.missingYear == true) {
            warnings += ParseWarning.DATE_MISSING_YEAR
        }

        if (isApproximate) {
            warnings += ParseWarning.APPROXIMATE_TIME
        }

        if (isTimeRange) {
            warnings += ParseWarning.TIME_RANGE
        }

        if (isHoliday) {
            warnings += ParseWarning.CLARIFY_DATE
        }

        if (usedPartOfDay) {
            warnings += ParseWarning.APPROXIMATE_TIME
        }

        // TIME_AMBIGUOUS warning for short hour forms (digit or word)
        if (bestTime != null && (
                bestTime.type == TimeType.HOURS_WORD ||
                    bestTime.type == TimeType.HOURS_SHORT ||
                    (bestTime.type == TimeType.WORD_HOUR && bestTime.score >= 60)
                )
        ) {
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

        // Past time adjustment (calendar days in local zone — DST-safe)
        if (fireAt != null && fireAt.isBefore(now)) {
            when {
                hadTodayWord -> {
                    fireAt = fireAt.atZone(zone).plusDays(1).toInstant()
                    warnings += ParseWarning.PAST_TIME_ADJUSTED
                }
                hadWeekday -> {
                    fireAt = fireAt.atZone(zone).plusDays(7).toInstant()
                    warnings += ParseWarning.PAST_TIME_ADJUSTED
                }
                hadExplicitTime && !hadExplicitDate -> {
                    fireAt = fireAt.atZone(zone).plusDays(1).toInstant()
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

        return finish(rawPhrase, textWithoutRecurrence, spans, fireAt, warnings, confidence, relativeOnly, recurrenceRule)
    }

    private fun extractRecurrence(text: String, lower: String): Triple<String?, String, String> {
        REC_DAILY.find(lower)?.let {
            return Triple(
                com.example.voicemind.data.RecurrenceRule(com.example.voicemind.data.RecurrenceType.DAILY).serialize(),
                text.removeRange(it.range).trim(),
                lower.removeRange(it.range).trim(),
            )
        }
        REC_WEEKDAYS.find(lower)?.let {
            return Triple(
                com.example.voicemind.data.RecurrenceRule(com.example.voicemind.data.RecurrenceType.WEEKDAYS).serialize(),
                text.removeRange(it.range).trim(),
                lower.removeRange(it.range).trim(),
            )
        }
        REC_WEEKENDS.find(lower)?.let {
            return Triple(
                com.example.voicemind.data.RecurrenceRule(com.example.voicemind.data.RecurrenceType.WEEKENDS).serialize(),
                text.removeRange(it.range).trim(),
                lower.removeRange(it.range).trim(),
            )
        }
        REC_WEEKLY.find(lower)?.let { match ->
            val dayName = match.groupValues[2]
            val dow = WEEKDAY_MAP[dayName] ?: 1
            return Triple(
                com.example.voicemind.data.RecurrenceRule(
                    com.example.voicemind.data.RecurrenceType.WEEKLY,
                    dayOfWeek = dow,
                ).serialize(),
                text.removeRange(match.range).trim(),
                lower.removeRange(match.range).trim(),
            )
        }
        REC_MONTHLY.find(lower)?.let { match ->
            val day = match.groupValues[1].toIntOrNull() ?: 1
            return Triple(
                com.example.voicemind.data.RecurrenceRule(
                    com.example.voicemind.data.RecurrenceType.MONTHLY,
                    dayOfMonth = day,
                ).serialize(),
                text.removeRange(match.range).trim(),
                lower.removeRange(match.range).trim(),
            )
        }
        return Triple(null, text, lower)
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

    private enum class DateType { RELATIVE_DAYS, TODAY, TOMORROW, DAY_AFTER_TOMORROW, WEEKDAY, NEXT_WEEKDAY, WEEKEND, DMY, DAY_MONTH, ORDINAL_DAY, ORDINAL_WORD_DAY, HOLIDAY }
    private enum class TimeType { HH_MM, HHMM, HOURS_MINUTES, HOURS_PART, MIDNIGHT_NOON, HOURS_WORD, HOURS_SHORT, PART_OF_DAY, HALF_PAST, QUARTER_TO, QUARTER_PAST, HALF_WITH, PART_PREFIX, WORD_HOUR, WORD_RANGE }

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
            RELATIVE_DELTA_WORDS.find(lowerText)?.let { m ->
                val amount = wordNumberToInt(m.groupValues[1]).takeIf { it > 0 } ?: 1
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
                    unitWord.startsWith("месяц") -> {
                        dateCandidates += DateCandidate(
                            date = zonedNow.toLocalDate().plusMonths(amount.toLong()),
                            span = m.range,
                            type = DateType.RELATIVE_DAYS,
                            score = 45,
                            relativeOnly = true,
                        )
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
            val month = monthFromName(monthName)
            if (month < 1) return@forEach
            var y = m.groupValues[3].toIntOrNull() ?: zonedNow.year
            var candidate = try { LocalDate.of(y, month, d) } catch (_: Exception) { null }
            var missingYear = false
            if (candidate != null) {
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
        }

        DATE_ORDINAL.findAll(lowerText).forEach { m ->
            val day = ordinalDayFromGroup(m.groupValues[1])
            if (day in 1..31) {
                var candidate = try {
                    LocalDate.of(zonedNow.year, zonedNow.monthValue, day)
                } catch (_: Exception) { null }
                if (candidate != null) {
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
        }

        DATE_ORDINAL_DIGIT.findAll(lowerText).forEach { m ->
            val day = m.groupValues[1].toInt()
            if (day in 1..31) {
                var candidate = try {
                    LocalDate.of(zonedNow.year, zonedNow.monthValue, day)
                } catch (_: Exception) { null }
                if (candidate != null) {
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
                "вечера", "вечером" -> when {
                    hour == 12 -> 0
                    hour >= 12 -> hour
                    else -> hour + 12
                }
                "ночи", "ночью" -> if (hour == 12) 0 else hour
                else -> hour
            }
            val score = when (part) {
                "вечера", "вечером", "ночи", "ночью" -> 100
                else -> 80
            }
            if (h in 0..23 && min in 0..59) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h, min),
                    span = m.range,
                    type = TimeType.HOURS_PART,
                    score = score,
                )
            }
        }

        TIME_MIDNIGHT_NOON.findAll(lowerText).forEach { m ->
            val token = m.groupValues[1]
            val t = when {
                token.startsWith("полночь") ||
                    token.contains("ночи") ||
                    token.contains("вечера") -> LocalTime.of(0, 0)
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
            val h = m.groupValues[1].toInt()
            val min = m.groupValues[2].toInt()
            if (h in 0..23 && min in 0..59) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h, min),
                    span = m.range,
                    type = TimeType.HOURS_MINUTES,
                    score = 90,
                )
            }
        }

        TIME_HOURS.findAll(lowerText).forEach { m ->
            val h = m.groupValues[1].toInt()
            if (h in 0..23) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h, 0),
                    span = m.range,
                    type = TimeType.HOURS_WORD,
                    score = 60,
                )
            }
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
                val h = m.groupValues[1].ifEmpty { m.groupValues[3] }.toInt()
                val min = m.groupValues[2].ifEmpty { m.groupValues[4] }.toInt()
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
            val min = m.groupValues[3].toIntOrNull() ?: 0
            val h = when (part.lowercase()) {
                "утром" -> hour
                "днём", "днем" -> if (hour == 12) 12 else hour + 12
                "вечером" -> if (hour == 12) 12 else hour + 12
                "ночью" -> if (hour == 12) 0 else hour
                else -> hour
            }
            if (h in 0..23 && min in 0..59) {
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h, min),
                    span = m.range,
                    type = TimeType.PART_PREFIX,
                    score = 95,
                )
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

        TIME_WORD_HOUR.findAll(lowerText).forEach { m ->
            val rawH = wordNumberToInt(m.groupValues[1])
            if (rawH in 0..23) {
                val prefix = lowerText.substring(m.range).lowercase()
                val isApproximate = prefix.startsWith("к ") ||
                    prefix.startsWith("около ") ||
                    prefix.startsWith("примерно ")
                val applyPmHeuristic = isApproximate && rawH in 1..11
                val h = if (applyPmHeuristic) rawH + 12 else rawH
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h.coerceIn(0, 23), 0),
                    span = m.range,
                    type = TimeType.WORD_HOUR,
                    score = if (isApproximate) 55 else 65,
                )
            }
        }

        TIME_RANGE.findAll(lowerText).forEach { m ->
            val hStr = m.groupValues[1]
            val rawH = hStr.toIntOrNull() ?: wordNumberToInt(hStr)
            if (rawH in 0..23) {
                val h = if (rawH in 1..11) rawH + 12 else rawH
                timeCandidates += TimeCandidate(
                    time = LocalTime.of(h.coerceIn(0, 23), 0),
                    span = m.range,
                    type = TimeType.WORD_RANGE,
                    score = 50,
                )
            }
        }

        DATE_ORDINAL_WORDS.findAll(lowerText).forEach { m ->
            val word = m.groupValues[1]
            val day = ORDINAL_DAY_MAP[word] ?: ORDINAL_NEUTER_MAP[word] ?: -1
            if (day in 1..31) {
                var candidate = try {
                    LocalDate.of(zonedNow.year, zonedNow.monthValue, day)
                } catch (_: Exception) { null }
                if (candidate != null) {
                    if (candidate.isBefore(zonedNow.toLocalDate())) {
                        candidate = candidate.plusMonths(1)
                    }
                    dateCandidates += DateCandidate(
                        date = candidate,
                        span = m.range,
                        type = DateType.ORDINAL_WORD_DAY,
                        score = 85,
                    )
                }
            }
        }

        HOLIDAY_PLACEHOLDER.findAll(lowerText).forEach { m ->
            dateCandidates += DateCandidate(
                date = zonedNow.toLocalDate(),
                span = m.range,
                type = DateType.HOLIDAY,
                score = 10,
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
        recurrenceRule: String? = null,
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
            recurrenceRule = recurrenceRule,
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
                    val after = s.substring(prefix.length)
                    if (after.isEmpty() || after.first().isWhitespace()) {
                        s = after.trimStart()
                        changed = true
                    }
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
        if (warnings.contains(ParseWarning.CLARIFY_DATE)) return 0.2f
        if (warnings.contains(ParseWarning.NO_TIME_FOUND)) return 0f
        if (warnings.contains(ParseWarning.TIME_RANGE)) return 0.70f
        if (warnings.contains(ParseWarning.APPROXIMATE_TIME)) {
            if (usedPartOfDay) return 0.80f
            if (warnings.contains(ParseWarning.TIME_AMBIGUOUS)) return 0.5f
            return 0.75f
        }
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

    private fun monthFromName(name: String): Int = MONTH_MAP[name.lowercase()] ?: -1

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

    private fun wordNumberToInt(word: String): Int {
        return WORD_NUMBERS[word.lowercase().trim()] ?: -1
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
        "в обед", "обедом" -> LocalTime.of(13, 0)
        "днём", "днем" -> LocalTime.of(14, 0)
        "вечером" -> LocalTime.of(19, 0)
        "ночью" -> LocalTime.of(1, 0)
        else -> LocalTime.of(1, 0)
    }

    companion object {
        private val DEFAULT_MORNING = LocalTime.of(9, 0)

        /**
         * Объединённая карта имён месяцев: genitive (января…) + nominative (январь…).
         * Нераспознанное имя месяца → -1 (не молчаливый декабрь).
         */
        private val MONTH_MAP: Map<String, Int> = buildMap {
            listOf(
                "января", "февраля", "марта", "апреля", "мая", "июня",
                "июля", "августа", "сентября", "октября", "ноября", "декабря",
            ).forEachIndexed { i, name -> put(name, i + 1) }
            listOf(
                "январь", "февраль", "март", "апрель", "май", "июнь",
                "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь",
            ).forEachIndexed { i, name -> put(name, i + 1) }
        }

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
            """${WB}в\s+следующ(ий|ую|ее)\s+(понедельник|вторник|среду|среда|четверг|пятницу|пятница|субботу|суббота|воскресенье|воскресенья)${WE}""",
        )
        private val WEEKEND = Regex("""${WB}на\s+выходных${WE}""")
        // Только с «в », чтобы не спутать 01.06.2026 с временем 6:20
        private val TIME_COLON = Regex("""${WB}(?:в\s+)?(\d{1,2})[:.](\d{2})""")
        private val TIME_HOURS_PART = Regex(
            """${WB}в\s+(\d{1,2})(?:[:.](\d{2}))?(?:\s+час(?:а|ов)?)?\s+(утра|утром|дня|днём|днем|вечера|вечером|ночи|ночью)${WE}""",
        )
        private val TIME_MIDNIGHT_NOON = Regex("""${WB}в\s+(полночь|полдень|полдня|12\s+ночи|12\s+дня|12\s+утра|12\s+вечера)${WE}""")
        private val TIME_HOURS = Regex("""${WB}в\s+(\d{1,2})\s+час(?:а|ов)?${WE}""")
        private val TIME_HOURS_MIN = Regex(
            """${WB}в\s+(\d{1,2})\s+час(?:а|ов)?\s+(\d{1,2})\s+минут""",
        )
        private val TIME_HOURS_SHORT = Regex("""${WB}в\s+(\d{1,2})${WE}""")
        private val TIME_4DIGIT = Regex("""${WB}(?:в\s+(\d{1,2})\s?(\d{2})|(\d{2})\s?(\d{2}))${WE}""")
        private val TIME_HALF_PAST = Regex("""${WB}(?:в\s+)?половин[ае]\s+(двенадцатого|одиннадцатого|десятого|девятого|восьмого|седьмого|шестого|пятого|четвёртого|четвертого|третьего|второго|первого)${WE}""")
        private val TIME_QUARTER_TO = Regex("""${WB}без\s+(четверти|пятнадцати|15)\s+(?:часа\s+)?(двенадцать|одиннадцать|десять|девять|восемь|семь|шесть|пять|четыре|три|два|один)${WE}""")
        private val TIME_QUARTER_PAST = Regex("""${WB}(?:в\s+)?четверть\s+(двенадцатого|одиннадцатого|десятого|девятого|восьмого|седьмого|шестого|пятого|четвёртого|четвертого|третьего|второго|первого)${WE}""")
        private val TIME_HALF_WITH = Regex("""${WB}(?:в\s+)?(двенадцать|одиннадцать|десять|девять|восемь|семь|шесть|пять|четыре|три|два|один)\s+с\s+половиной${WE}""")
        private val TIME_PART_PREFIX = Regex(
            """${WB}(утром|днём|днем|вечером|ночью)\s+в\s+(\d{1,2})(?:[:.](\d{2}))?${WE}""",
        )
        private val PART_OF_DAY = Regex("""${WB}(утром|в обед|днём|днем|вечером|ночью)${WE}""")
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

        private val ORDINAL_NEUTER_MAP: Map<String, Int> = mapOf(
            "первое" to 1, "второе" to 2, "третье" to 3,
            "четвёртое" to 4, "четвертое" to 4, "пятое" to 5, "шестое" to 6,
            "седьмое" to 7, "восьмое" to 8, "девятое" to 9, "десятое" to 10,
            "одиннадцатое" to 11, "двенадцатое" to 12, "тринадцатое" to 13,
            "четырнадцатое" to 14, "пятнадцатое" to 15, "шестнадцатое" to 16,
            "семнадцатое" to 17, "восемнадцатое" to 18, "девятнадцатое" to 19,
            "двадцатое" to 20,
            "двадцать первое" to 21, "двадцать второе" to 22, "двадцать третье" to 23,
            "двадцать четвёртое" to 24, "двадцать четвертое" to 24, "двадцать пятое" to 25,
            "двадцать шестое" to 26, "двадцать седьмое" to 27, "двадцать восьмое" to 28,
            "двадцать девятое" to 29, "тридцатое" to 30, "тридцать первое" to 31,
        )

        private val ORDINAL_WORDS_FOR_DATE = (
            ORDINAL_DAY_MAP.keys + ORDINAL_NEUTER_MAP.keys
        ).sortedByDescending { it.length }.joinToString("|") { Regex.escape(it) }

        private val REC_DAILY = Regex("""${WB}(каждый\s+день|ежедневно)${WE}""")
        private val REC_WEEKDAYS = Regex("""${WB}по\s+будням${WE}""")
        private val REC_WEEKENDS = Regex("""${WB}по\s+выходным${WE}""")
        private val REC_WEEKLY = Regex(
            """${WB}кажд(ый|ую|ое)\s+(понедельник|вторник|среду|четверг|пятницу|субботу|воскресенье)${WE}""",
        )
        private val REC_MONTHLY = Regex(
            """${WB}каждое\s+(\d{1,2})(?:-е)?\s+число${WE}""",
        )

        private val WEEKDAY_MAP = mapOf(
            "понедельник" to 1, "вторник" to 2, "среду" to 3, "среда" to 3,
            "четверг" to 4, "пятницу" to 5, "пятница" to 5,
            "субботу" to 6, "суббота" to 6, "воскресенье" to 7,
        )

        private val WORD_NUMBERS: Map<String, Int> = mapOf(
            "ноль" to 0, "нуля" to 0,
            "один" to 1, "одна" to 1, "одно" to 1, "одного" to 1, "одну" to 1,
            "два" to 2, "две" to 2, "двух" to 2,
            "три" to 3, "трёх" to 3, "трех" to 3,
            "четыре" to 4, "четырёх" to 4, "четырех" to 4,
            "пять" to 5, "пяти" to 5,
            "шесть" to 6, "шести" to 6,
            "семь" to 7, "семи" to 7,
            "восемь" to 8, "восьми" to 8,
            "девять" to 9, "девяти" to 9,
            "десять" to 10, "десяти" to 10,
            "одиннадцать" to 11, "одиннадцати" to 11,
            "двенадцать" to 12, "двенадцати" to 12,
            "тринадцать" to 13, "тринадцати" to 13,
            "четырнадцать" to 14, "четырнадцати" to 14,
            "пятнадцать" to 15, "пятнадцати" to 15,
            "шестнадцать" to 16, "шестнадцати" to 16,
            "семнадцать" to 17, "семнадцати" to 17,
            "восемнадцать" to 18, "восемнадцати" to 18,
            "девятнадцать" to 19, "девятнадцати" to 19,
            "двадцать" to 20, "двадцати" to 20,
            "двадцать один" to 21, "двадцать одного" to 21,
            "двадцать два" to 22, "двадцать две" to 22, "двадцать двух" to 22,
            "двадцать три" to 23, "двадцать трёх" to 23, "двадцать трех" to 23,
            "двадцать четыре" to 24, "двадцать четырёх" to 24, "двадцать четырех" to 24,
            "двадцать пять" to 25, "двадцать пяти" to 25,
            "двадцать шесть" to 26, "двадцать шести" to 26,
            "двадцать семь" to 27, "двадцать семи" to 27,
            "двадцать восемь" to 28, "двадцать восьми" to 28,
            "двадцать девять" to 29, "двадцать девяти" to 29,
            "тридцать" to 30, "тридцати" to 30,
            "тридцать один" to 31, "тридцать одного" to 31,
            "тридцать два" to 32, "тридцать две" to 32, "тридцать двух" to 32,
            "тридцать три" to 33, "тридцать трёх" to 33, "тридцать трех" to 33,
            "тридцать четыре" to 34, "тридцать четырёх" to 34, "тридцать четырех" to 34,
            "тридцать пять" to 35, "тридцать пяти" to 35,
            "тридцать шесть" to 36, "тридцать шести" to 36,
            "тридцать семь" to 37, "тридцать семи" to 37,
            "тридцать восемь" to 38, "тридцать восьми" to 38,
            "тридцать девять" to 39, "тридцать девяти" to 39,
            "сорок" to 40, "сорока" to 40,
            "сорок один" to 41, "сорок одного" to 41,
            "сорок два" to 42, "сорок две" to 42, "сорок двух" to 42,
            "сорок три" to 43, "сорок трёх" to 43, "сорок трех" to 43,
            "сорок четыре" to 44, "сорок четырёх" to 44, "сорок четырех" to 44,
            "сорок пять" to 45, "сорок пяти" to 45,
            "сорок шесть" to 46, "сорок шести" to 46,
            "сорок семь" to 47, "сорок семи" to 47,
            "сорок восемь" to 48, "сорок восьми" to 48,
            "сорок девять" to 49, "сорок девяти" to 49,
            "пятьдесят" to 50, "пятидесяти" to 50,
            "пятьдесят один" to 51, "пятьдесят одного" to 51,
            "пятьдесят два" to 52, "пятьдесят две" to 52, "пятьдесят двух" to 52,
            "пятьдесят три" to 53, "пятьдесят трёх" to 53, "пятьдесят трех" to 53,
            "пятьдесят четыре" to 54, "пятьдесят четырёх" to 54, "пятьдесят четырех" to 54,
            "пятьдесят пять" to 55, "пятьдесят пяти" to 55,
            "пятьдесят шесть" to 56, "пятьдесят шести" to 56,
            "пятьдесят семь" to 57, "пятьдесят семи" to 57,
            "пятьдесят восемь" to 58, "пятьдесят восьми" to 58,
            "пятьдесят девять" to 59, "пятьдесят девяти" to 59,
        )

        private val WORD_NUMBER_ALTERNATIVES = WORD_NUMBERS.keys
            .sortedByDescending { it.length }
            .joinToString("|") { Regex.escape(it) }

        private val TIME_WORD_HOUR = Regex(
            """${WB}(?:в\s+|к\s+|около\s+|примерно\s+(?:в\s+)?)($WORD_NUMBER_ALTERNATIVES)(?:\s+час(?:а|ов)?)?${WE}""",
        )

        private val RELATIVE_DELTA_WORDS = Regex(
            """через\s+($WORD_NUMBER_ALTERNATIVES)\s+(минуты|минуту|минут|мин|часа|часов|час|ч|месяца|месяцев|месяц|дня|дней|день)""",
        )

        private val DATE_ORDINAL_WORDS = Regex(
            """${WB}на\s+($ORDINAL_WORDS_FOR_DATE)(?:\s+числа)?${WE}""",
        )

        private val APPROX_PREFIXES = Regex(
            """${WB}(к|около|примерно)\s+${WE}""",
        )

        private val TIME_RANGE = Regex(
            """${WB}с\s+($WORD_NUMBER_ALTERNATIVES|\d{1,2})\s+до\s+($WORD_NUMBER_ALTERNATIVES|\d{1,2})(?:\s+час(?:а|ов)?)?${WE}""",
        )

        private val HOLIDAY_PLACEHOLDER = Regex(
            """${WB}(на\s+новый\s+год|на\s+день\s+рождения|на\s+день\s+рожденья)${WE}""",
        )
    }
}
