## 1. P0 — устойчивость и критичные фразы

- [x] 1.1 Обернуть `LocalDate.of` в try/catch (skip candidate) для `DATE_DAY_MONTH`, `DATE_ORDINAL`, `DATE_ORDINAL_DIGIT`, `DATE_ORDINAL_WORDS`
- [x] 1.2 Добавить гварды `h in 0..23` / `min in 0..59` перед `LocalTime.of` в `TIME_HOURS` и `TIME_HOURS_MIN`
- [x] 1.3 Исправить `stripPrefixes`: снимать префикс только если после него конец строки или whitespace
- [x] 1.4 Расширить `TIME_HOURS_PART` regex: опциональное `\s+час(?:а|ов)?` между числом/минутами и маркером суток; скорректировать resolve часа для «12 вечера»→00:00 / «12 утра»→12:00
- [x] 1.5 Поправить `TIME_MIDNIGHT_NOON`: токен с «вечера» → 00:00
- [x] 1.6 Unit-тесты P0: «31 апреля…», «в 25 часов», «в 9 часов 70 минут», «напомнить завтра в 10:00…», «напомни завтра…», «в 8 часов вечера фильм», «в 3 часа дня обед», «в 12 вечера…», «в 12 утра…»

## 2. P1 — body, weekday, ambiguous, holiday

- [x] 2.1 При сборке `spans` всегда добавлять span'ы всех кандидатов `PART_OF_DAY` (и `PART_PREFIX`); при необходимости расширить `TIME_PART_PREFIX` до минут `HH:mm` и score ≥ 90
- [x] 2.2 Добавить средний род в `NEXT_WEEKDAY`: `следующ(ий|ую|ее)`
- [x] 2.3 Включить `WORD_HOUR` (час 1..11 без маркера суток) в выдачу `TIME_AMBIGUOUS` + confidence ≤ 0.5
- [x] 2.4 В `isVoiceParseSuccessful()` возвращать `false` при `CLARIFY_DATE`; в `computeConfidence` поднять ветку `CLARIFY_DATE → 0.2f` выше `NO_TIME_FOUND`
- [x] 2.5 Unit-тесты P1: «вечером позвонить в 21:00», «утром в 9:00 зарядка», «в следующее воскресенье в 11:00…», «в девять позвонить», «на день рождения…» (`isVoiceParseSuccessful` false, conf 0.2)

## 3. P2 — согласованность эвристик

- [x] 3.1 Унифицировать PM-смещение для «примерно (в)» с «к»/«около» (часы 1..11); обновить тест `approx_primemernoVVosem`
- [x] 3.2 Поддержать «через два месяца» / слово-число + месяц (через `RELATIVE_DELTA_WORDS` или `wordNumberToInt` + `RELATIVE_MONTH`)
- [x] 3.3 Past-time adjust через `atZone(zone).plusDays(n)` вместо `Instant.plus(ChronoUnit.DAYS)`
- [x] 3.4 Early-return relative: включать span'ы date-кандидатов в remove-list (минимум: «через полчаса завтра…» без «завтра» в body)
- [x] 3.5 Unit-тесты P2: «примерно в восемь», «через два месяца…», relative+«завтра» body-clean

## 4. Регрессия и проверка

- [x] 4.1 Прогнать `:app:testDebugUnitTest --tests "com.example.voicemind.data.parse.ReminderParserTest"` — все зелёные
- [x] 4.2 `openspec validate --all` — pass
- [x] 4.3 Кратко отметить в PR/коммите ссылку на B1-* из `docs/voicemind-bugs-from-benchmark.md` (без правок доки, если не просили)
  — при коммите/PR упомянуть: B1-1, B1-2, B1-3, B1-4, B1-5, B1-6, B1-7, B1-8, B1-9, B1-14, B1-15, B1-21, B1-22 (+ explore: «в N часов вечера»)
