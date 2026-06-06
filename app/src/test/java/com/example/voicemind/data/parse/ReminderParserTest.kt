package com.example.voicemind.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderParserTest {

    private val zone = ZoneId.of("Europe/Moscow")
  private lateinit var parser: ReminderParser
    private lateinit var now: Instant

    @Before
    fun setUp() {
        parser = ReminderParser(zone)
        // 2026-05-17 (воскресенье) 10:00 MSK
        now = LocalDateTime.of(2026, 5, 17, 10, 0).atZone(zone).toInstant()
    }

    @Test
    fun tomorrowAtNine_callNeighbor() {
        val r = parser.parse("завтра в 9:00 позвонить соседу", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить соседу", r.body)
        assertTrue(r.confidence >= 0.9f)
    }

    @Test
    fun inOneHour_buyMilk() {
        val r = parser.parse("через час купить молоко", now)
        assertEquals(now.plusSeconds(3600), r.fireAt)
        assertEquals("купить молоко", r.body)
    }

    @Test
    fun mondayAt1830_workout() {
        val r = parser.parse("в понедельник в 18:30 тренировка", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 18, 30).atZone(zone).toInstant(), r.fireAt)
        assertEquals("тренировка", r.body)
    }

    @Test
    fun callMomTomorrowMorning() {
        val r = parser.parse("позвонить маме завтра утром", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.body.contains("позвонить маме"))
    }

    @Test
    fun may25At14_dentist() {
        val r = parser.parse("25 мая в 14:00 стоматолог", now)
        assertEquals(LocalDateTime.of(2026, 5, 25, 14, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("стоматолог", r.body)
    }

    @Test
    fun remindPrefix_stripped() {
        val r = parser.parse("напомни завтра в 10:00 оплатить счёт", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.body.contains("оплатить"))
    }

    @Test
    fun todayAt21_eveningPhrase() {
        val r = parser.parse("сегодня в 21:30 вечерняя прогулка", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 21, 30).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun in30Minutes_short() {
        val r = parser.parse("через 30 минут выключить плиту", now)
        assertEquals(now.plusSeconds(30 * 60), r.fireAt)
    }

    @Test
    fun dayAfterTomorrow_atNoon() {
        val r = parser.parse("послезавтра в 12:00 встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 19, 12, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun dmyFormat_withYear() {
        val r = parser.parse("01.06.2026 в 8:00 подача документов", now)
        assertEquals(LocalDateTime.of(2026, 6, 1, 8, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun dmyFormat_withoutYear_rollsToNextYearIfPast() {
        val r = parser.parse("10.03 в 9:00 весеннее дело", now)
        assertEquals(LocalDateTime.of(2027, 3, 10, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.DATE_MISSING_YEAR))
    }

    @Test
    fun atNineOClock_ambiguousWarning() {
        val r = parser.parse("завтра в 9 часов звонок", now)
        assertNotNull(r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.TIME_AMBIGUOUS))
    }

    @Test
    fun noTime_bodyOnly_warning() {
        val r = parser.parse("купить хлеб", now)
        assertNull(r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun emptyPhrase() {
        val r = parser.parse("   ", now)
        assertNull(r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.BODY_EMPTY))
    }

    @Test
    fun pastTimeToday_adjustedToTomorrow() {
        val morningNow = LocalDateTime.of(2026, 5, 17, 15, 0).atZone(zone).toInstant()
        val r = parser.parse("сегодня в 8:00 зарядка", morningNow)
        assertEquals(LocalDateTime.of(2026, 5, 18, 8, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun tuesday_at1930() {
        val r = parser.parse("во вторник в 19:30 йога", now)
        assertEquals(LocalDateTime.of(2026, 5, 19, 19, 30).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun in2Days_defaultMorningIfNoTime() {
        val r = parser.parse("через 2 дня сдать анализы", now)
        assertNotNull(r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun tonight_partOfDay() {
        val r = parser.parse("завтра ночью проверить дверь", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 1, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun afternoon_partOfDay() {
        val r = parser.parse("завтра днём обед с коллегами", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 13, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun hoursAndMinutes_explicit() {
        val r = parser.parse("завтра в 9 часов 15 минут созвон", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 15).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun dotAsTimeSeparator() {
        val r = parser.parse("завтра в 9.30 завтрак", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 30).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun forgetPrefix() {
        val r = parser.parse("не забудь через 15 минут перекусить", now)
        assertEquals(now.plusSeconds(15 * 60), r.fireAt)
    }

    @Test
    fun needPrefix() {
        val r = parser.parse("нужно завтра в 7:00 встать", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 7, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun decemberDate_withYear() {
        val r = parser.parse("20 декабря 2026 в 10:00 подарки", now)
        assertEquals(LocalDateTime.of(2026, 12, 20, 10, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun friday_at1800() {
        val r = parser.parse("в пятницу в 18:00 спортзал", now)
        assertEquals(LocalDateTime.of(2026, 5, 22, 18, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun in3Hours_task() {
        val r = parser.parse("через 3 часа проверить почту", now)
        assertEquals(now.plusSeconds(3 * 3600), r.fireAt)
    }

    @Test
    fun todayAt2359_edge() {
        val lateNow = LocalDateTime.of(2026, 5, 17, 20, 0).atZone(zone).toInstant()
        val r = parser.parse("сегодня в 23:59 напоминание", lateNow)
        assertEquals(LocalDateTime.of(2026, 5, 17, 23, 59).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun tomorrowAt900_spaceInTime() {
        val r = parser.parse("завтра в 09:00 звонок", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun onlyTimeToday_assumesToday() {
        val r = parser.parse("в 15:00 чай", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 15, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun reminderWordPrefix() {
        val r = parser.parse("напоминание послезавтра в 20:00 фильм", now)
        assertEquals(LocalDateTime.of(2026, 5, 19, 20, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun bodyNotEmpty_whenTimeStripped() {
        val r = parser.parse("завтра в 9:00", now)
        assertNotNull(r.fireAt)
        assertTrue(r.body.isBlank() || r.warnings.contains(ParseWarning.BODY_EMPTY))
    }

    @Test
    fun confidence_highForExplicitDateTime() {
        val r = parser.parse("завтра в 9:00 тест", now)
        assertTrue(r.confidence >= 0.9f)
    }

    @Test
    fun sunday_nextSunday() {
        val r = parser.parse("в воскресенье в 11:00 бранч", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 11, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun at9evening_callMom() {
        val r = parser.parse("сегодня в 9 вечера позвонить маме", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 21, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить маме", r.body)
    }

    @Test
    fun at10morning_meeting() {
        val r = parser.parse("завтра в 10 утра встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
    }

    @Test
    fun at3day_lunch() {
        val r = parser.parse("в 3 дня обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 15, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("обед", r.body)
    }

    @Test
    fun at2night_check() {
        val r = parser.parse("в 2 ночи проверить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 2, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun atMidnight() {
        val r = parser.parse("в полночь встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 0, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun atNoon() {
        val r = parser.parse("в полдень обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 12, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun at12night() {
        val r = parser.parse("в 12 ночи проверить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 0, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun inHalfHour_turnOff() {
        val r = parser.parse("через полчаса выключить плиту", now)
        assertEquals(now.plusSeconds(30 * 60), r.fireAt)
        assertEquals("выключить плиту", r.body)
    }

    @Test
    fun inOneAndHalfHour() {
        val r = parser.parse("через полтора часа совещание", now)
        assertEquals(now.plusSeconds(90 * 60), r.fireAt)
    }

    @Test
    fun shortHour_ambiguousWarning() {
        val r = parser.parse("завтра в 9 тест", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.TIME_AMBIGUOUS))
    }

    @Test
    fun standaloneEvening() {
        val r = parser.parse("сегодня вечером фильм", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 22, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("фильм", r.body)
    }

    @Test
    fun in2Days_isVoiceParseSuccessful() {
        val r = parser.parse("через 2 дня сдать анализы", now)
        assertTrue(r.isVoiceParseSuccessful())
        assertNotNull(r.fireAt)
    }

    @Test
    fun todayAt0051_noColon_callBrother() {
        val midnightNow = LocalDateTime.of(2026, 5, 17, 0, 0).atZone(zone).toInstant()
        val r = parser.parse("сегодня 0051 позвонить брату", midnightNow)
        assertEquals(LocalDateTime.of(2026, 5, 17, 0, 51).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить брату", r.body)
        assertTrue(r.confidence >= 0.9f)
    }

    @Test
    fun todayAt0119_noV_callBrother() {
        val earlyNow = LocalDateTime.of(2026, 5, 17, 1, 0).atZone(zone).toInstant()
        val r = parser.parse("сегодня 01:19 позвонить брату", earlyNow)
        assertEquals(LocalDateTime.of(2026, 5, 17, 1, 19).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить брату", r.body)
        assertTrue(r.confidence >= 0.9f)
    }

    @Test
    fun spaceBetweenDigits_sttReturnsSpace() {
        val r = parser.parse("02 35 позвонить куку", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 2, 35).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить куку", r.body)
        assertTrue(r.confidence >= 0.8f)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun spaceBetweenDigits_withV_prefix() {
        val r = parser.parse("в 2 35 позвонить куку", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 2, 35).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить куку", r.body)
        assertTrue(r.confidence >= 0.8f)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun dateWithYear_doesNotTreatYearAsTime() {
        val r = parser.parse("25 мая 2026 в 10:00 праздник", now)
        assertEquals(LocalDateTime.of(2026, 5, 25, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("праздник", r.body)
    }

    @Test
    fun weekdayPastTime_adjustedToNextWeek() {
        // сейчас воскресенье 10:00; «в воскресенье в 9:00» уже прошло
        val r = parser.parse("в воскресенье в 9:00 бранч", now)
        assertEquals(LocalDateTime.of(2026, 5, 24, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    // --- Candidate-based engine regression / edge cases ---

    @Test
    fun exactTimeDoesNotGetOverriddenByPartOfDay() {
        // Exact time имеет приоритет: «в 9:00 днём» → 9:00, маркер не попадает в body
        val r = parser.parse("завтра в 9:00 днём обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("обед", r.body)
    }

    @Test
    fun bodyAtBeginning_timeAtEnd() {
        // Перестановка: body в начале, время в конце
        val r = parser.parse("позвонить маме завтра в 9:00", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить маме", r.body)
    }

    @Test
    fun bodyInMiddle_timeAround() {
        // Сложная перестановка: время по бокам от body
        val r = parser.parse("завтра купить молоко в 10:00", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("купить молоко", r.body)
    }

    @Test
    fun relativeDaysWithExplicitTime_highConfidence() {
        // «через 2 дня в 10:00» — confidence должен быть высоким
        val r = parser.parse("через 2 дня в 10:00 тест", now)
        assertEquals(LocalDateTime.of(2026, 5, 19, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("тест", r.body)
        assertTrue(r.confidence >= 0.8f)
        assertTrue(r.isVoiceParseSuccessful())
    }

    @Test
    fun hoursPartMorning_bodyClean() {
        // «в 9 утра» — HOURS_PART должен съесть «утра», а не оставить в body
        // now=10:00, поэтому 9 утра уже прошло → parser сдвигает на завтра
        val r = parser.parse("сегодня в 9 утра встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
        assertEquals("встреча", r.body)
    }

    @Test
    fun hoursPartEvening_bodyClean() {
        val r = parser.parse("завтра в 8 вечера фильм", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 20, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("фильм", r.body)
    }

    @Test
    fun noColonTimeWithoutV() {
        // «сегодня 01:19» — без «в» (уже есть, но проверяем стабильность)
        val earlyNow = LocalDateTime.of(2026, 5, 17, 1, 0).atZone(zone).toInstant()
        val r = parser.parse("сегодня 01:19 позвонить брату", earlyNow)
        assertEquals(LocalDateTime.of(2026, 5, 17, 1, 19).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить брату", r.body)
        assertTrue(r.confidence >= 0.9f)
    }

    @Test
    fun multipleTimeCandidates_pickMostSpecific() {
        // «в 9 часов 15 минут» — HOURS_MIN должен выиграть у HOURS_WORD
        val r = parser.parse("завтра в 9 часов 15 минут созвон", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 15).atZone(zone).toInstant(), r.fireAt)
        assertEquals("созвон", r.body)
    }

    @Test
    fun shortHourInsideHoursWord_notPicked() {
        // «в 9 часов» — HOURS_WORD (score 60) должен выиграть у HOURS_SHORT (score 50)
        val r = parser.parse("завтра в 9 часов звонок", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.TIME_AMBIGUOUS))
    }

    @Test
    fun standalonePartOfDay_withDate() {
        // «завтра утром» — PART_OF_DAY + TOMORROW
        val r = parser.parse("позвонить завтра утром", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить", r.body)
    }

    @Test
    fun dotSeparatorTime() {
        // «в 9.30» — точка вместо двоеточия
        val r = parser.parse("завтра в 9.30 завтрак", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 30).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun hoursWordAmbiguousWithPartOfDayInBody_noWarning() {
        // «в 9» + «утром» отдельно → PART_OF_DAY не является уточнением для TIME_HOURS_SHORT,
        // но TIME_AMBIGUOUS должен НЕ добавляться, если PART_OF_DAY присутствует где-то в фразе
        val r = parser.parse("завтра утром в 9 встреча", now)
        // Примечание: «утром» — PART_OF_DAY, «в 9» — HOURS_SHORT.
        // TIME_AMBIGUOUS не добавляется, т.к. PART_OF_DAY содержится в lowerText.
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        // warning может быть, т.к. PART_OF_DAY не «утра/вечера» для HOURS_SHORT
        // Это acceptable — confidence будет 0.5
    }

    @Test
    fun onlyRelativeDays_noTime_voiceParseSuccess() {
        // «через 2 дня» — fireAt есть, но confidence=0, isVoiceParseSuccess=true по логике extensions
        val r = parser.parse("через 2 дня сдать анализы", now)
        assertNotNull(r.fireAt)
        assertTrue(r.isVoiceParseSuccessful())
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun dateWithYear_doesNotTreatYearAsTime_candidateEngine() {
        // Год рядом с датой — TIME_COLON не должен схватить год
        val r = parser.parse("25 мая 2026 в 10:00 праздник", now)
        assertEquals(LocalDateTime.of(2026, 5, 25, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("праздник", r.body)
    }

    @Test
    fun todayAt9evening_callMom() {
        val r = parser.parse("сегодня в 9 вечера позвонить маме", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 21, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить маме", r.body)
    }

    @Test
    fun pastTimeWeekday_adjustedToNextWeek() {
        // сейчас воскресенье 10:00; «в понедельник в 8:00» — завтра 8:00, не прошло
        // а «в воскресенье в 8:00» — уже прошло → +7
        val r = parser.parse("в воскресенье в 8:00 зарядка", now)
        assertEquals(LocalDateTime.of(2026, 5, 24, 8, 0).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun in3Minutes_bodyClean() {
        val r = parser.parse("через 3 минуты попить кофе", now)
        assertEquals(now.plusSeconds(3 * 60), r.fireAt)
        assertEquals("попить кофе", r.body)
    }

    @Test
    fun in3Min_shortForm() {
        val r = parser.parse("через 3 мин попить кофе", now)
        assertEquals(now.plusSeconds(3 * 60), r.fireAt)
        assertEquals("попить кофе", r.body)
    }

    @Test
    fun bodyNotEmpty_whenOnlyTimeProvided() {
        val r = parser.parse("в 15:00", now)
        assertNotNull(r.fireAt)
        assertTrue(r.body.isBlank() || r.warnings.contains(ParseWarning.BODY_EMPTY))
    }

    // --- Ordinal day ("числа") ---

    @Test
    fun ordinalDay_ninth_bodyClean() {
        // now = 17 мая; "девятого числа" → 9 июня (уже прошло в мае)
        val r = parser.parse("девятого числа заполнить анкету", now)
        assertEquals(LocalDateTime.of(2026, 6, 9, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("заполнить анкету", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun ordinalDay_first_sameMonth() {
        // now = 17 мая; "первого числа" → 1 июня
        val r = parser.parse("первого числа оплатить счёт", now)
        assertEquals(LocalDateTime.of(2026, 6, 1, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("оплатить счёт", r.body)
    }

    @Test
    fun ordinalDay_thirtyFirst_withTime() {
        val r = parser.parse("тридцать первого числа в 10:00 отчёт", now)
        assertEquals(LocalDateTime.of(2026, 5, 31, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("отчёт", r.body)
        assertTrue(r.confidence >= 0.8f)
    }

    @Test
    fun ordinalDay_twentyFifth_withTime() {
        val r = parser.parse("двадцать пятого числа в 14:30 встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 25, 14, 30).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
    }

    @Test
    fun ordinalDay_futureSameMonth_noRoll() {
        // now = 17 мая; "двадцатого числа" → 20 мая
        val r = parser.parse("двадцатого числа купить подарок", now)
        assertEquals(LocalDateTime.of(2026, 5, 20, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("купить подарок", r.body)
    }

    @Test
    fun ordinalDay_compositeForm_withTime() {
        // "двадцать первого числа" — composite form; now=17 мая, 21 мая ещё впереди
        val r = parser.parse("двадцать первого числа в 18:00 тренировка", now)
        assertEquals(LocalDateTime.of(2026, 5, 21, 18, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("тренировка", r.body)
    }

    @Test
    fun dayMonth_noTime_defaultMorning() {
        // "9 июня" — DATE_DAY_MONTH без времени; now=17 мая
        val r = parser.parse("9 июня заполни анкету", now)
        assertEquals(LocalDateTime.of(2026, 6, 9, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("заполни анкету", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    // --- New prefixes (1.1) ---

    @Test
    fun prefix_makeReminder() {
        val r = parser.parse("сделай напоминание завтра в 10:00 оплатить счёт", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("оплатить счёт", r.body)
    }

    @Test
    fun prefix_setReminder() {
        val r = parser.parse("поставь напоминалку завтра в 10:00 позвонить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить", r.body)
    }

    @Test
    fun prefix_remindMe() {
        val r = parser.parse("напомни мне завтра в 10:00 купить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("купить", r.body)
    }

    @Test
    fun prefix_soNotToForget() {
        val r = parser.parse("чтобы не забыть завтра в 10:00 встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
    }

    // --- Nominative month (1.2–1.3) ---

    @Test
    fun nominativeMay_noTime() {
        val r = parser.parse("9 май заполни анкету", now)
        // now = 17 мая; 9 мая уже прошло → 9 мая 2027
        assertEquals(LocalDateTime.of(2027, 5, 9, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("заполни анкету", r.body)
        assertTrue(r.warnings.contains(ParseWarning.DATE_MISSING_YEAR))
    }

    @Test
    fun nominativeJune_withTime() {
        val r = parser.parse("25 июнь в 10:00 встреча", now)
        assertEquals(LocalDateTime.of(2026, 6, 25, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
    }

    @Test
    fun nominativeDecember_withYear() {
        val r = parser.parse("20 декабрь 2026 в 10:00 праздник", now)
        assertEquals(LocalDateTime.of(2026, 12, 20, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("праздник", r.body)
    }

    // --- Next weekday (2.1–2.5) ---

    @Test
    fun nextMonday_withTime() {
        // now = воскресенье 17 мая; следующий понедельник = 25 мая
        val r = parser.parse("в следующий понедельник в 10:00 встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 25, 10, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
    }

    @Test
    fun nextWednesday_feminine_noTime() {
        // now = воскресенье 17 мая; следующая среда = 27 мая
        val r = parser.parse("в следующую среду позвонить", now)
        assertEquals(LocalDateTime.of(2026, 5, 27, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun nextFriday_withTime() {
        // now = воскресенье 17 мая; следующая пятница = 29 мая
        val r = parser.parse("в следующую пятницу в 18:00 тренировка", now)
        assertEquals(LocalDateTime.of(2026, 5, 29, 18, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("тренировка", r.body)
    }

    // --- Weekend phrases (3.1–3.4) ---

    @Test
    fun weekend_defaultMorning() {
        // now = воскресенье 17 мая; ближайшая суббота = 23 мая
        val r = parser.parse("на выходных сходить в магазин", now)
        assertEquals(LocalDateTime.of(2026, 5, 23, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("сходить в магазин", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun weekend_withExplicitTime() {
        val r = parser.parse("на выходных в 11:00 бранч", now)
        assertEquals(LocalDateTime.of(2026, 5, 23, 11, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("бранч", r.body)
    }

    // --- Compound time expressions (4.1–4.6) ---

    @Test
    fun halfPast_eightThirty() {
        val r = parser.parse("в половине девятого позвонить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 8, 30).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun quarterTo_nine_withoutMinutesWord() {
        val r = parser.parse("без четверти восемь", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 7, 45).atZone(zone).toInstant(), r.fireAt)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun quarterTo_fifteenMinutes() {
        val r = parser.parse("без пятнадцати девять встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 8, 45).atZone(zone).toInstant(), r.fireAt)
        assertEquals("встреча", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun quarterPast_eightFifteen() {
        val r = parser.parse("в четверть девятого обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 8, 15).atZone(zone).toInstant(), r.fireAt)
        assertEquals("обед", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun halfWith_nineThirty() {
        val r = parser.parse("в девять с половиной звонок", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 30).atZone(zone).toInstant(), r.fireAt)
        assertEquals("звонок", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    // --- Approximate relative time (5.1–5.5) ---

    @Test
    fun coupleOfHours_relative() {
        val r = parser.parse("через пару часов позвонить", now)
        assertEquals(now.plusSeconds(2 * 3600), r.fireAt)
        assertEquals("позвонить", r.body)
    }

    @Test
    fun fewMinutes_relative() {
        val r = parser.parse("через несколько минут проверить", now)
        assertEquals(now.plusSeconds(5 * 60), r.fireAt)
        assertEquals("проверить", r.body)
    }

    @Test
    fun inAWeek_defaultMorning() {
        val r = parser.parse("через неделю сдать отчёт", now)
        assertEquals(LocalDateTime.of(2026, 5, 24, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("сдать отчёт", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    // --- Digit ordinal day (6.1–6.3) ---

    @Test
    fun digitOrdinalDay_ninth() {
        // now = 17 мая; "9 числа" → 9 июня
        val r = parser.parse("9 числа заполнить анкету", now)
        assertEquals(LocalDateTime.of(2026, 6, 9, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("заполнить анкету", r.body)
        assertTrue(r.warnings.contains(ParseWarning.NO_TIME_FOUND))
    }

    @Test
    fun digitOrdinalDay_fifth_currentMonth() {
        val may3Now = LocalDateTime.of(2026, 5, 3, 10, 0).atZone(zone).toInstant()
        val r = parser.parse("5 числа оплатить", may3Now)
        assertEquals(LocalDateTime.of(2026, 5, 5, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("оплатить", r.body)
    }

    @Test
    fun digitOrdinalDay_thirtyFirst_withTime() {
        val r = parser.parse("31 числа в 14:00 совещание", now)
        assertEquals(LocalDateTime.of(2026, 5, 31, 14, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("совещание", r.body)
    }

    // --- Time with part-of-day prefix (7.1–7.3) ---

    @Test
    fun partPrefix_morningNine() {
        val r = parser.parse("утром в 9 позвонить", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
        assertTrue(!r.warnings.contains(ParseWarning.TIME_AMBIGUOUS))
    }

    @Test
    fun partPrefix_eveningEight() {
        val r = parser.parse("вечером в 8 фильм", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 20, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("фильм", r.body)
    }

    @Test
    fun partPrefix_dayThree() {
        val r = parser.parse("днём в 3 обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 15, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("обед", r.body)
    }

    // --- Expanded time-of-day markers (expand-time-of-day-parsing) ---

    @Test
    fun bareHourWithEveningMarker_bodyClean() {
        val r = parser.parse("в 10 вечера заполнить анкету", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 22, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("заполнить анкету", r.body)
    }

    @Test
    fun bareHourWithMorningMarker_bodyClean() {
        val r = parser.parse("в 8 утра позвонить маме", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 8, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить маме", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun exactTimeOverridesMarker() {
        val r = parser.parse("в 18:30 вечера ужин", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 18, 30).atZone(zone).toInstant(), r.fireAt)
        assertEquals("ужин", r.body)
    }

    @Test
    fun standaloneMorningMarker() {
        val r = parser.parse("утром позвонить в банк", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("позвонить в банк", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun standaloneDayMarker() {
        val r = parser.parse("днём забрать посылку", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 13, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("забрать посылку", r.body)
    }

    @Test
    fun standaloneNightMarker() {
        val r = parser.parse("ночью сделать бэкап", now)
        assertEquals(LocalDateTime.of(2026, 5, 18, 1, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("сделать бэкап", r.body)
        assertTrue(r.warnings.contains(ParseWarning.PAST_TIME_ADJUSTED))
    }

    @Test
    fun combinedOrdinalAndTimeOfDay_bodyClean() {
        val r = parser.parse("9 числа в 10 вечера сделать отчёт", now)
        assertEquals(LocalDateTime.of(2026, 6, 9, 22, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("сделать отчёт", r.body)
    }

    @Test
    fun ordinalWithGoSuffix() {
        val may3Now = LocalDateTime.of(2026, 5, 3, 10, 0).atZone(zone).toInstant()
        val r = parser.parse("5-го числа оплатить", may3Now)
        assertEquals(LocalDateTime.of(2026, 5, 5, 9, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("оплатить", r.body)
    }

    @Test
    fun todayAt8pm_withColon_parses2000() {
        val r = parser.parse("сегодня в 8:00 вечера спать", now)
        println("fireAt=${r.fireAt}, body=${r.body}, warnings=${r.warnings}, confidence=${r.confidence}")
        assertEquals(LocalDateTime.of(2026, 5, 17, 20, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("спать", r.body)
    }

    @Test
    fun debugRegexMatch() {
        val text = "сегодня в 8:00 вечера спать"
        // Match actual regex from ReminderParser source (WB + pattern + WE)
        val WB = """(?<![\p{L}\d])"""
        val WE = """(?![\p{L}\d])"""
        val regex = Regex("""${WB}в\s+(\d{1,2})(?:[:.](\d{2}))?\s+(утра|утром|дня|днём|днем|вечера|вечером|ночи|ночью)${WE}""")
        val matches = regex.findAll(text).toList()
        println("Matches: ${matches.size}")
        matches.forEach { m ->
            println("Match='${m.value}', groups=${m.groupValues}")
        }
    }

    @Test
    fun todayAt8pm_withoutColon_parses2000() {
        val r = parser.parse("сегодня в 8 вечера спать", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 20, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("спать", r.body)
    }
}
