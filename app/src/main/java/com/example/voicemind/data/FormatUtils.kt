package com.example.voicemind.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object FormatUtils {

    private val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale("ru"))

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
        "SCHEDULED" -> "Запланировано"
        "SNOOZED" -> "Отложено"
        "FIRED" -> "Сработало"
        "DISMISSED" -> "Выполнено"
        "CANCELLED" -> "Отменено"
        else -> status
    }
}
