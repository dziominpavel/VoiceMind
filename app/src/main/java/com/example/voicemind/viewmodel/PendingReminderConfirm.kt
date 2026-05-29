package com.example.voicemind.viewmodel

import com.example.voicemind.data.parse.ParseWarning

data class PendingReminderConfirm(
    val rawPhrase: String,
    val body: String,
    val fireAtMillis: Long?,
    val confidence: Float,
    val warnings: List<ParseWarning>,
)
