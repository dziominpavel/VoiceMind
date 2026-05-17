package com.example.voicemind.viewmodel

import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.parse.ParseWarning

data class PendingReminderConfirm(
    val rawPhrase: String,
    val body: String,
    val fireAtMillis: Long?,
    val deliveryMode: DeliveryMode,
    val confidence: Float,
    val warnings: List<ParseWarning>,
)
