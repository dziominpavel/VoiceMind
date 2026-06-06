package com.example.voicemind.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

object FormatUtils {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))

    fun formatFireAt(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        Instant.ofEpochMilli(epochMillis).atZone(zone).format(dateTimeFormatter)

    fun formatRelativeFireAt(
        epochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val diff = epochMillis - nowMillis
        return when {
            diff < 0 -> "просрочено"
            diff < 60_000L -> "меньше минуты"
            diff < 3_600_000L -> {
                val min = (diff / 60_000L).toInt().coerceAtLeast(1)
                "$min мин"
            }
            diff < 86_400_000L -> {
                val hours = diff / 3_600_000L
                val min = ((diff % 3_600_000L) / 60_000L).toInt()
                if (min > 0) "$hours ч $min мин" else "$hours ч"
            }
            else -> formatFireAt(epochMillis, zone)
        }
    }

    fun statusLabel(status: String): String = when (status) {
        "PENDING" -> "Ожидает"
        "TRIGGERED" -> "Сработало"
        "DONE" -> "Выполнено"
        "CANCELLED" -> "Отменено"
        else -> status
    }

    fun formatTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.forLanguageTag("ru"))
        return Instant.ofEpochMilli(epochMillis).atZone(zone).format(formatter)
    }

    fun formatRelativeDateShort(
        epochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val diff = epochMillis - nowMillis
        if (diff < 0) return "просрочено"
        return formatShortDate(epochMillis, nowMillis, zone)
    }

    fun formatShortDate(
        epochMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String {
        val date = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val daysDiff = ChronoUnit.DAYS.between(today, date)
        return when (daysDiff) {
            0L -> "сегодня"
            1L -> "завтра"
            2L -> "послезавтра"
            else -> {
                val formatter = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("ru"))
                date.format(formatter)
            }
        }
    }
}
