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
        val r = parser.parse("сегодня ночью проверить дверь", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 22, 0).atZone(zone).toInstant(), r.fireAt)
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
        assertEquals(LocalDateTime.of(2026, 5, 17, 2, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun atMidnight() {
        val r = parser.parse("в полночь встреча", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 0, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun atNoon() {
        val r = parser.parse("в полдень обед", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 12, 0).atZone(zone).toInstant(), r.fireAt)
    }

    @Test
    fun at12night() {
        val r = parser.parse("в 12 ночи проверить", now)
        assertEquals(LocalDateTime.of(2026, 5, 17, 0, 0).atZone(zone).toInstant(), r.fireAt)
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
        assertEquals(LocalDateTime.of(2026, 5, 17, 19, 0).atZone(zone).toInstant(), r.fireAt)
        assertEquals("фильм", r.body)
    }
}
