package com.example.voicemind.data.parse

/** Голосовой разбор считается успешным — можно показать подтверждение без ручного ввода. */
fun ParseResult.isVoiceParseSuccessful(): Boolean {
    if (fireAt == null || body.isBlank()) return false
    // "через N дней" вычисляет дату, но не время — fireAt != null, это ок.
    if (warnings.contains(ParseWarning.BODY_EMPTY)) return false
    if (warnings.contains(ParseWarning.CLARIFY_DATE)) return false
    return true
}
