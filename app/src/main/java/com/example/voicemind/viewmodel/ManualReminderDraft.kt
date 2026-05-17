package com.example.voicemind.viewmodel

import com.example.voicemind.data.DeliveryMode

/** Ручной ввод: дата/время + текст (без парсера). */
data class ManualReminderDraft(
    val body: String = "",
    val fireAtMillis: Long? = null,
    val deliveryMode: DeliveryMode = DeliveryMode.NOTIFICATION,
    /** Исходная фраза после голоса (для справки). */
    val rawPhrase: String? = null,
    val editingReminderId: Long? = null,
    /** Открыто после неудачного разбора голосовой фразы. */
    val fromVoiceParseFailure: Boolean = false,
)
