package com.example.voicemind.data.parse

import java.time.Instant

data class ParseResult(
    val fireAt: Instant?,
    val body: String,
    val confidence: Float,
    val warnings: List<ParseWarning>,
    val matchedTimeSpan: IntRange?,
    val rawPhrase: String,
)
