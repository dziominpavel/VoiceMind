---
description: Работа с ReminderParser — добавление/исправление парсинга времени
---

Правила работы с `ReminderParser` (candidate-based engine):

1. Прочитать текущие тесты: `data/parse/ReminderParserTest.kt` и `docs/REMINDER_PARSING.md`.
2. Добавить новый **test-case** с замороженным `Instant now` — **до** реализации (TDD).
3. Запустить тест — убедиться, что он падает (red).
4. Реализовать паттерн в `ReminderParser.kt`:
   - regex `findAll` → `DateCandidate` / `TimeCandidate` со `score`.
   - `resolveBestDate` / `resolveBestTime` выбирают по max score (при равенстве — раньше в строке).
   - Часть дня (утром/вечером) **не переопределяет** точное время `HH:mm`.
   - `body` строится только из spans выбранных кандидатов.
5. Запустить тест — green.
6. Проверить edge-кейсы: полночь, полдень, «в 12 ночи», «через полчаса», «завтра» на 23:59.
7. Обновить `docs/REMINDER_PARSING.md` при изменении поддерживаемых конструкций.
8. Проверить `confidence` и `warning` для нового паттерна.
// turbo
9. Запустить полный набор тестов парсера:
   ```powershell
   Start-Process -FilePath "gradlew.bat" -ArgumentList ":app:testDebugUnitTest", "--tests", "com.example.voicemind.data.parse.ReminderParserTest", "--no-daemon" -WorkingDirectory "." -Wait -NoNewWindow
   ```
10. Если confidence-логика изменилась — проверить отображение warning на `ConfirmReminderScreen`.
