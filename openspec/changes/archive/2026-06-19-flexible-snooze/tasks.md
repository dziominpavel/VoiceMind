## 1. Семантика «от now» (баг просроченного snooze)

- [x] 1.1 Исправить `VoiceMindViewModel.snoozeReminder` — считать `now + minutes` вместо `reminder.fireAt + minutes`.
- [x] 1.2 Проверить, что путь уведомления (`ReminderRepository.snoozeReminder`) уже считает от `now` (да) — оставить без изменений.

## 2. «Завтра утром» и абсолютный snooze

- [x] 2.1 Добавить helper вычисления «завтра утром» (ближайшее 09:00 в будущем, локальная TZ).
- [x] 2.2 Добавить во `VoiceMindViewModel` `snoozeUntil(id, fireAtMillis)` через `updateAndSchedule`.

## 3. Пресеты UI

- [x] 3.1 В `ReminderDetailScreen` заменить пресеты на 5 / 15 / 60 + «завтра утром».
- [x] 3.2 На ALARM-экране добавить пресеты (минимально), сохранив быстрый дефолт.
- [x] 3.3 Добавить строковые ресурсы для новых пресетов.

## 4. Проверка

- [x] 4.1 `openspec validate --all` — без ошибок.
- [x] 4.2 Сборка `:app:assembleDebug` и `:app:testDebugUnitTest` — успешно.
- [ ] 4.3 Ручной чек: отложить просроченное напоминание → реально срабатывает; «завтра утром» планирует корректное время.
