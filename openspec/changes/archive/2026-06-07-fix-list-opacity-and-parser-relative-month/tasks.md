## 1. UI — непрозрачные карточки списка

- [x] 1.1 Добавить helper `blendSurfaceWithTint(base, tint, fraction)` в theme или локально в `ReminderListScreen`
- [x] 1.2 `UpcomingReminderCard`: заменить `bgTint.copy(alpha)` на непрозрачный blend с `SurfaceElevated`
- [x] 1.3 Убедиться, что NORMAL/URGENT/CRITICAL/OVERDUE все имеют `alpha = 1.0` у `containerColor`

## 2. UI — delete-strip только при свайпе

- [x] 2.1 `SwipeToRevealBox`: скрывать правую полосу `errorContainer` при `offsetX == 0` (alpha или visibility)
- [x] 2.2 Проверить: в покое на просроченной карточке не видно 🗑️ и красной подложки

## 3. Парсер — относительные месяцы

- [x] 3.1 Добавить `RELATIVE_MONTH` regex: `через месяц`, `через (\d+) месяц(а|ев)?`
- [x] 3.2 В `findAllCandidates`: `DateCandidate` с `plusMonths(n)`, score 45, `relativeOnly = true`
- [x] 3.3 Обновить reference-секцию в main spec (при архивации) — `через месяц`, `через N месяцев`

## 4. Тесты парсера

- [x] 4.1 Тест: «через месяц напоминалка четыре» → body, fireAt +1 month 09:00, successful
- [x] 4.2 Тест: «через 2 месяца сдать отчёт» → +2 months, body «сдать отчёт»
- [x] 4.3 Регрессия: «через неделю» не сломан

## 5. Проверка

- [x] 5.1 `./gradlew :app:testDebugUnitTest` — pass
- [x] 5.2 `./gradlew :app:compileDebugKotlin` — pass
- [ ] 5.3 Ручной тест: список — просроченная и обычная карточки выглядят одинаково плотными; 🗑️ только при свайпе
- [ ] 5.4 Ручной тест: голос «через месяц напоминалка четыре» → ConfirmScreen, не ручной fallback
- [x] 5.5 `openspec validate fix-list-opacity-and-parser-relative-month` — pass
