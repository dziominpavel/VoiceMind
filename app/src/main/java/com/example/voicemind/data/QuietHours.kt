package com.example.voicemind.data

import java.time.LocalTime

enum class QuietHoursMode {
    SILENT,
    VIBRATE,
}

/**
 * Настройки тихих часов: интервал в локальном времени устройства.
 *
 * Границы интервала — минуты от полуночи, интервал полуоткрытый: [start, end).
 * Если начало и конец совпадают, тихие часы считаются выключенными.
 */
data class QuietHoursSettings(
    val enabled: Boolean = false,
    val startMinute: Int = DEFAULT_START_MINUTE,
    val endMinute: Int = DEFAULT_END_MINUTE,
    val mode: QuietHoursMode = QuietHoursMode.SILENT,
) {
    fun isActive(now: LocalTime = LocalTime.now()): Boolean =
        enabled && isWithin(now.hour * MINUTES_PER_HOUR + now.minute)

    fun isWithin(nowMinute: Int): Boolean =
        isWithinRange(nowMinute, startMinute, endMinute)

    companion object {
        const val MINUTES_PER_HOUR = 60
        const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
        const val DEFAULT_START_MINUTE = 22 * MINUTES_PER_HOUR
        const val DEFAULT_END_MINUTE = 7 * MINUTES_PER_HOUR

        fun normalizeMinute(minute: Int): Int =
            minute.coerceIn(0, MINUTES_PER_DAY - 1)

        fun isWithinRange(nowMinute: Int, startMinute: Int, endMinute: Int): Boolean {
            val now = normalizeMinute(nowMinute)
            val start = normalizeMinute(startMinute)
            val end = normalizeMinute(endMinute)
            if (start == end) return false
            return if (start < end) {
                now >= start && now < end
            } else {
                now >= start || now < end
            }
        }
    }
}
