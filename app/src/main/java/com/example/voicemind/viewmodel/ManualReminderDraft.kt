package com.example.voicemind.viewmodel

/** Ручной ввод: дата/время + текст (без парсера). */
data class ManualReminderDraft(
    val body: String = "",
    val fireAtMillis: Long? = null,
    val deliveryMode: String? = null,
    /** Исходная фраза после голоса (для справки). */
    val rawPhrase: String? = null,
    val editingReminderId: Long? = null,
    /** Открыто после неудачного разбора голосовой фразы. */
    val fromVoiceParseFailure: Boolean = false,
)
