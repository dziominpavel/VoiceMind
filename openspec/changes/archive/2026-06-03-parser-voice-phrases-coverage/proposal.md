## Why

The ReminderParser currently handles ~55 common Russian voice phrases, but real-world STT output includes many more variations: colloquialisms, alternative word order, compound time expressions, and edge cases that reduce parse confidence to 0 or produce wrong `fireAt`. Expanding coverage will improve the rate of single-shot successful parses and reduce the need for manual correction on ConfirmScreen.

## What Changes

- Add **12+ new regex patterns** for unhandled date/time constructions (weekend phrases, "next week", "half past", "quarter to", digit ordinals + числа, etc.)
- Add **new DateType/TimeType candidates** with appropriate scores for colloquial and compound expressions
- Extend **stripPrefixes** to cover more STT-introduced filler phrases
- Add **60+ new unit tests** covering edge cases and boundary phrases
- Handle **nominative month names** from STT ("9 май" vs "9 мая")
- Support **"next" qualifiers** for weekdays ("в следующий понедельник")
- Support **weekend phrases** ("на выходных")
- Support **approximate relative time** ("через пару часов", "через несколько минут")
- Support **compound hour expressions** ("половина девятого", "без четверти девять", "в девять с половиной")

## Capabilities

### New Capabilities
- `next-weekday`: Parsing "следующий/следующую + weekday" (e.g., "в следующий понедельник")
- `weekend-phrases`: Parsing "на выходных" / "в субботу или воскресенье"
- `compound-time`: Parsing "половина девятого", "без пятнадцати девять", "четверть девятого"
- `approximate-relative`: Parsing "через пару часов", "через несколько минут", "через неделю"
- `digit-ordinal-day`: Parsing "5 числа", "9 числа" (digit + "числа")
- `nominative-month`: Parsing "9 май", "25 июнь" (STT may return nominative instead of genitive)
- `time-with-part-of-day-prefix`: Parsing "утром в 9", "вечером в 8" (part of day before time)

### Modified Capabilities
- `ordinal-day`: Extend to support digit form "9 числа" in addition to word form "девятого числа"

## Impact

- `ReminderParser.kt` — new regexes, candidate extraction, resolution logic
- `ReminderParserTest.kt` — 60+ new tests (no breaking changes to existing tests)
- `docs/REMINDER_PARSING.md` — documentation update for new patterns
