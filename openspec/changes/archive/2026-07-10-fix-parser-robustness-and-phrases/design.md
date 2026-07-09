## Context

`ReminderParser` — rule-based candidate engine (date/time regex → score → best → body via span removal). После серии фиксов (маркеры суток, ordinal days, evening numbers) остались дыры устойчивости и живых фраз, подтверждённые code-ревью и `docs/voicemind-bugs-from-benchmark.md`.

Текущие болевые точки в коде:
- `LocalDate.of` / `LocalTime.of` без гвардов в части хендлеров → crash.
- `stripPrefixes` через `startsWith` без границы слова.
- `TIME_HOURS_PART` не допускает «час(а|ов)» между числом и маркером.
- «12 вечера» в `TIME_HOURS_PART` / `TIME_MIDNIGHT_NOON` → 12:00.
- В `spans` попадают только best date/time (+ пересекающийся HOURS_PART); проигравший PART_OF_DAY / PART_PREFIX остаётся в body.
- `NEXT_WEEKDAY` без среднего рода `ее`.
- `TIME_AMBIGUOUS` только для `HOURS_WORD`/`HOURS_SHORT`, не для `WORD_HOUR`.
- `isVoiceParseSuccessful()` игнорирует `CLARIFY_DATE`.

## Goals / Non-Goals

**Goals:**
- Парсер MUST NOT падать на невалидных датах/часах — skip candidate.
- Корректный body и fireAt на топ жизненных фразах из explore + B1-*.
- Confirm-flow не принимает holiday-placeholder как успех.
- Unit-тесты на каждый фикс; регрессии существующих тестов зелёные.

**Non-Goals:**
- LLM / OpenRouter fallback-парсер.
- Новые типы recurrence / UI confirm redesign.
- Авто-ролл явной даты **с годом** на +1 год (B1-16) — осознанно out of scope: пользователь сказал «2026», не угадываем.
- Полный NLP / морфология префиксов — только граница слова.
- Исправление всех LOW confidence-расхождений со спекой (B1-13/18/19) — только holiday 0.2 и то, что нужно для P0/P1.

## Decisions

### D1. Invalid date/time → skip, не crash
Обернуть `LocalDate.of` в try/catch (как уже в `DATE_DMY`) для `DATE_DAY_MONTH`, `DATE_ORDINAL`, `DATE_ORDINAL_DIGIT`, `DATE_ORDINAL_WORDS`. Для `TIME_HOURS` / `TIME_HOURS_MIN` — гварды `h in 0..23`, `min in 0..59` перед `LocalTime.of`.

**Альтернатива:** coerce day to last day of month — отвергнуто: молчаливая подмена «31 апреля» → 30 апреля хуже, чем NO_TIME / ручной ввод.

### D2. Prefix strip только на границе слова
После `startsWith(prefix)` требовать `after.isEmpty() || after.first().isWhitespace()`.

**Альтернатива:** добавить «напомнить» в PREFIXES — отвергнуто: не чинит «нужное», «напоминание» уже есть, граница слова чинит класс багов.

### D3. «в N час(а|ов)? + маркер» через расширение TIME_HOURS_PART
Расширить regex:

```
в\s+(\d{1,2})(?:[:.](\d{2}))?(?:\s+час(?:а|ов)?)?\s+(утра|утром|дня|…)
```

Score как у текущего HOURS_PART (80/100). Тогда «в 8 часов вечера» выигрывает у голого TIME_HOURS (60).

**Альтернатива:** отдельный TimeType — лишняя сложность при том же span/resolve.

### D4. «12 вечера» → 00:00, «12 утра» → 12:00
В `TIME_HOURS_PART`:
- вечер + hour==12 → 0
- утро + hour==12 → 12
- день + hour==12 → 12
- ночь + hour==12 → 0 (уже есть)

В `TIME_MIDNIGHT_NOON`: токен с «вечера» → 00:00; «утра»/«дня» → 12:00.

### D5. Все PART_OF_DAY / PART_PREFIX spans всегда в remove-list
При сборке `spans` добавлять span'ы **всех** кандидатов типов `PART_OF_DAY` и `PART_PREFIX` (и при необходимости расширить `TIME_PART_PREFIX` до `(\d{1,2})(?:[:.](\d{2}))?` + score ≥ HH_MM, чтобы «утром в 9:00» выбирался одним кандидатом).

Минимальный фикс body-leak: всегда strip PART_OF_DAY spans даже если bestTime = HH_MM.

### D6. NEXT_WEEKDAY: `следующ(ий|ую|ее)`
Одна правка regex.

### D7. TIME_AMBIGUOUS для WORD_HOUR
Если bestTime.type == WORD_HOUR, hour in 1..11, нет смежного маркера суток — добавить TIME_AMBIGUOUS (confidence ≤ 0.5). Глобальный `PART_OF_DAY.containsMatchIn` оставить на P1 как есть; ужесточение смежности (B1-17) — P2.

### D8. Holiday gate
`isVoiceParseSuccessful()`: `if (warnings.contains(CLARIFY_DATE)) return false`.  
В `computeConfidence`: ветка `CLARIFY_DATE → 0.2f` **выше** `NO_TIME_FOUND → 0f`.

### D9. P2 (в том же change, отдельные tasks)
- «примерно» получает то же PM-смещение, что «к»/«около» для 1..11.
- `RELATIVE_DELTA_WORDS` + единицы месяца **или** отдельный путь через существующий `RELATIVE_MONTH` со словами через `wordNumberToInt`.
- Past adjust: `atZone(zone).plusDays(n)` вместо `Instant.plus(1, DAYS)`.
- Relative early-return: если есть dateCandidates («завтра»), не early-return только по relativeInstant **или** мержить date spans в body removal и не игнорировать дату, если relative — минуты/часы без явного конфликта. Предпочтение: при наличии date-кандидата с score ≥ 50 и relativeInstant — **не** early-return, а обычный resolve (дата + время из relative как? сложно). Проще P2-минимум: в early-return добавить spans всех dateCandidates в remove-list + warning, fireAt остаётся relative (документировать: «через час завтра» = now+1h, «завтра» только из body убрать). Полный семантический merge — follow-up.

## Risks / Trade-offs

- [Риск] Расширение TIME_HOURS_PART съест «в 8 часов дня рождения» → Mitigation: маркер только из фиксированного списка утра/дня/вечера/ночи; «дня рождения» не матчится целиком (нужен WE после маркера) — «дня» + WE перед «рождения» может сматчить «дня». Проверить фразу «в 3 часа дня рождения» в тестах; при ложном срабатывании требовать, что после маркера не идёт «рождения» / ужесточить lookahead.
- [Риск] Всегда strip PART_OF_DAY удалит «вечером» из body даже когда это часть задачи («купить вечером крем» без другого времени) — но тогда PART_OF_DAY и есть bestTime, span и так удаляется. Риск только при двух маркерах — редко.
- [Риск] PM для «примерно в восемь» изменит существующий тест `approx_primemernoVVosem` (hour=8) — обновить тест осознанно.
- [Риск] Большой change — Mitigation: tasks по P0 → P1 → P2, каждый блок с тестами.

## Migration Plan

Только код + unit-тесты. Миграций БД нет. Откат — revert change.

## Open Questions

- Нужен ли follow-up на семантику «через час завтра» (relative vs date), или достаточно убрать «завтра» из body?
- «к двенадцати» = 00:00 или 12:00? В этом change не трогаем (B1-20 out of scope), пока нет продуктового решения.
