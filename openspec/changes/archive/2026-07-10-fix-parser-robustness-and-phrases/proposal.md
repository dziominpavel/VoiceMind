## Why

`ReminderParser` падает на невалидных датах/часах и систематически ошибается на частых русских фразах: «напомнить завтра…» режет body до «ть…», «в 8 часов вечера» даёт 08:00 вместо 20:00, «12 вечера» — полдень вместо полуночи, маркеры «утром/вечером» утекают в body рядом с точным временем. Это блокирует доверие к голосовому вводу и подтверждено ревью кода + бенчмарком `docs/voicemind-bugs-from-benchmark.md` (B1-1…B1-9, B1-14/15/6/8).

## What Changes

- Сделать парсер устойчивым к мусорным датам и часам: невалидный кандидат MUST пропускаться без `DateTimeException` / crash.
- Исправить `stripPrefixes`: префиксы («напомни», «нужно», …) снимаются только по границе слова — «напомнить» больше не превращается в «ть».
- Распознавать «в N час(а|ов) утра/дня/вечера/ночи» как час + маркер суток (не голый `TIME_HOURS`).
- Исправить «12 вечера» / «12 утра» (и связанные midnight/noon эвристики) на канонические 00:00 / 12:00.
- Не оставлять проигравшие `PART_OF_DAY` / `PART_PREFIX` маркеры в body, когда выбрано точное `HH:mm`.
- Поддержать «в следующее воскресенье» (средний род в `NEXT_WEEKDAY`).
- Помечать голое слово-час («в девять») как `TIME_AMBIGUOUS` с пониженным confidence.
- Не считать holiday-placeholder (`CLARIFY_DATE`) успешным голосовым разбором для confirm-flow.
- (P2, в том же change) выровнять «примерно в восемь» с PM-эвристикой «к/около»; «через два месяца» словами; DST-safe сдвиг дня; early-return relative не игнорирует соседние date-маркеры вроде «завтра».

## Capabilities

### New Capabilities
*(none)*

### Modified Capabilities
- `reminder-parsing`: Ужесточаются требования к устойчивости (skip invalid date/time), границам префиксов, паттерну «N часов + маркер суток», полуночи/полудню для «12 вечера/утра», очистке body от маркеров суток, среднему роду «следующее», `TIME_AMBIGUOUS` для словесных часов, гейту `isVoiceParseSuccessful` при `CLARIFY_DATE`, плюс согласованность приблизительного времени и относительных месяцев словами.

## Impact

- `app/src/main/java/com/example/voicemind/data/parse/ReminderParser.kt` — regex, валидация кандидатов, spans, past-adjust, confidence.
- `app/src/main/java/com/example/voicemind/data/parse/ParseResultExtensions.kt` — гейт `CLARIFY_DATE`.
- `app/src/test/java/com/example/voicemind/data/parse/ReminderParserTest.kt` — регрессии на жизненные фразы.
- UI confirm-flow косвенно: holiday-фразы уйдут в ручной ввод вместо ложного confirm.
- Источник находок: explore-ревью + `docs/voicemind-bugs-from-benchmark.md` (актуальные B1-*).
