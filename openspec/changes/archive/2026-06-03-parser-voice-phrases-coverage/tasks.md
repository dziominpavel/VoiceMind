## 1. Foundation & Prefixes

- [x] 1.1 Add new prefixes to `stripPrefixes`: "сделай напоминание", "поставь напоминалку", "напомни мне", "чтобы не забыть"
- [x] 1.2 Add `monthFromNominative()` helper mapping nominative month names (январь–декабрь)
- [x] 1.3 Update `DATE_DAY_MONTH` regex to accept both genitive and nominative month forms

## 2. Next Weekday

- [x] 2.1 Add `NEXT_WEEKDAY` regex matching "в следующ(ий|ую) (weekday)"
- [x] 2.2 Add `DateType.NEXT_WEEKDAY` with score 85
- [x] 2.3 Implement `nextWeekday()` helper: nearest weekday + 7 days
- [x] 2.4 Add extraction in `findAllCandidates()` before plain weekday check
- [x] 2.5 Add tests: next Monday, next Wednesday (feminine), next Friday with time

## 3. Weekend Phrases

- [x] 3.1 Add `WEEKEND` regex matching "на выходных"
- [x] 3.2 Add `DateType.WEEKEND` with score 75
- [x] 3.3 Implement resolution: nearest upcoming Saturday
- [x] 3.4 Add tests: "на выходных сходить в магазин", "на выходных в 11:00 бранч"

## 4. Compound Time Expressions

- [x] 4.1 Add `TIME_HALF_PAST` regex for "половина [часа] N" → (N-1):30
- [x] 4.2 Add `TIME_QUARTER_TO` regex for "без (четверти|пятнадцати) [часа] N" → (N-1):(60-M)
- [x] 4.3 Add `TIME_QUARTER_PAST` regex for "четверть [часа] N" → (N-1):15
- [x] 4.4 Add `TIME_HALF_WITH` regex for "N с половиной" → N:30
- [x] 4.5 Add corresponding `TimeType` entries with scores 85–90
- [x] 4.6 Add tests for all four compound patterns

## 5. Approximate Relative Time

- [x] 5.1 Add `RELATIVE_COUPLE_HOURS` regex: "через пару часов" → +2h
- [x] 5.2 Add `RELATIVE_FEW_MINUTES` regex: "через несколько минут" → +5min
- [x] 5.3 Add `RELATIVE_WEEK` regex: "через неделю" → +7d (date candidate, score 45)
- [x] 5.4 Add extraction logic in `findAllCandidates()`
- [x] 5.5 Add tests for all three approximate patterns

## 6. Digit Ordinal Day

- [x] 6.1 Add `DATE_ORDINAL_DIGIT` regex: "(\\d{1,2}) числа"
- [x] 6.2 Reuse `ordinalDayFromGroup()` logic with digit parsing
- [x] 6.3 Add tests: "9 числа", "5 числа" (current month), "31 числа в 14:00"

## 7. Time with Part-of-Day Prefix

- [x] 7.1 Add `TIME_PART_PREFIX` regex: "(утром|днём|вечером|ночью) в (\\d{1,2})"
- [x] 7.2 Resolve with part-of-day context removing TIME_AMBIGUOUS
- [x] 7.3 Add tests: "утром в 9", "вечером в 8", "днём в 3"

## 8. Tests & Documentation

- [x] 8.1 Run full test suite; fix regressions
- [x] 8.2 Update `docs/REMINDER_PARSING.md` with new supported patterns table
- [x] 8.3 Verify all 60+ new tests pass
- [x] 8.4 Update score/confidence documentation for new candidate types
