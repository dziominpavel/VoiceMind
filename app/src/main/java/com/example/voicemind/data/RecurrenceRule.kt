package com.example.voicemind.data

enum class RecurrenceType {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    WEEKLY,
    MONTHLY,
}

data class RecurrenceRule(
    val type: RecurrenceType,
    val interval: Int = 1,
    val dayOfWeek: Int? = null,
    val dayOfMonth: Int? = null,
) {
    fun toLabel(): String {
        return when (type) {
            RecurrenceType.DAILY -> if (interval == 1) "ежедневно" else "каждые $interval дн."
            RecurrenceType.WEEKDAYS -> "по будням"
            RecurrenceType.WEEKENDS -> "по выходным"
            RecurrenceType.WEEKLY -> {
                val dayName = when (dayOfWeek) {
                    1 -> "пн"; 2 -> "вт"; 3 -> "ср"; 4 -> "чт"
                    5 -> "пт"; 6 -> "сб"; 7 -> "вс"
                    else -> ""
                }
                if (interval == 1) "еженедельно ($dayName)" else "каждые $interval нед. ($dayName)"
            }
            RecurrenceType.MONTHLY -> if (interval == 1) "ежемесячно ($dayOfMonth)" else "каждые $interval мес. ($dayOfMonth)"
        }
    }

    fun serialize(): String {
        return when (type) {
            RecurrenceType.DAILY -> "DAILY:$interval"
            RecurrenceType.WEEKDAYS -> "WEEKDAYS"
            RecurrenceType.WEEKENDS -> "WEEKENDS"
            RecurrenceType.WEEKLY -> "WEEKLY:${dayOfWeek ?: 1}:$interval"
            RecurrenceType.MONTHLY -> "MONTHLY:${dayOfMonth ?: 1}:$interval"
        }
    }

    companion object {
        fun parse(serialized: String?): RecurrenceRule? {
            if (serialized.isNullOrBlank()) return null
            val parts = serialized.split(":")
            return when (parts[0]) {
                "DAILY" -> RecurrenceRule(
                    type = RecurrenceType.DAILY,
                    interval = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                )
                "WEEKDAYS" -> RecurrenceRule(type = RecurrenceType.WEEKDAYS)
                "WEEKENDS" -> RecurrenceRule(type = RecurrenceType.WEEKENDS)
                "WEEKLY" -> RecurrenceRule(
                    type = RecurrenceType.WEEKLY,
                    dayOfWeek = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    interval = parts.getOrNull(2)?.toIntOrNull() ?: 1,
                )
                "MONTHLY" -> RecurrenceRule(
                    type = RecurrenceType.MONTHLY,
                    dayOfMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    interval = parts.getOrNull(2)?.toIntOrNull() ?: 1,
                )
                else -> null
            }
        }
    }
}
