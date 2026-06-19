package com.example.voicemind.data.parse

enum class ParseWarning {
    TIME_AMBIGUOUS,
    DATE_MISSING_YEAR,
    NO_TIME_FOUND,
    BODY_EMPTY,
    PAST_TIME_ADJUSTED,
    APPROXIMATE_TIME,
    TIME_RANGE,
    CLARIFY_DATE,
}
