package com.example.voicemind.data.parse

/** Голосовой разбор считается успешным — можно показать подтверждение без ручного ввода. */
fun ParseResult.isVoiceParseSuccessful(): Boolean {
    if (fireAt == null || body.isBlank()) return false
    if (warnings.contains(ParseWarning.NO_TIME_FOUND)) return false
    return true
}
