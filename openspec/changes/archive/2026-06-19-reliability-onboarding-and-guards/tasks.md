## 1. Разрешения и хелперы

- [x] 1.1 Добавить `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` в `AndroidManifest.xml`.
- [x] 1.2 В `ReminderPermissions` добавить `isIgnoringBatteryOptimizations(context)` и `requestIgnoreBatteryOptimizationsIntent(context)` (+ fallback `batteryOptimizationSettingsIntent()`).

## 2. Состояние и флаги

- [x] 2.1 В `SettingsRepository` добавить флаг `onboarding_completed` (read/flow + setter).
- [x] 2.2 Во `VoiceMindViewModel` добавить состояние шагов мастера и производное `reliabilityIssues` (нет уведомлений / нет exact alarm / батарея не исключена).
- [x] 2.3 Добавить события/флаги показа мастера (первый запуск + явный вызов) и потребление в UI.

## 3. UI мастера и баннер

- [x] 3.1 Создать оверлей мастера надёжности (Compose), шаги динамические по API и статусу.
- [x] 3.2 Привязать шаги к системным launcher'ам в `MainActivity` (notifications, exact alarm, battery).
- [x] 3.3 Добавить баннер надёжности в `HomeScreen` с кнопкой «Исправить» → открыть мастер.
- [x] 3.4 Пересчитывать `reliabilityIssues` на `onResume`.
- [x] 3.5 Добавить точку входа «Проверка надёжности» в `SettingsScreen`.

## 4. Тестовое напоминание

- [x] 4.1 Добавить во ViewModel `createTestReminder()` → `ReminderRepository.insertAndSchedule` с `now + 60s`.
- [x] 4.2 Кнопка «Проверить, что работает» в мастере и/или настройках.

## 5. Строки и первый запуск

- [x] 5.1 Добавить строковые ресурсы (объяснения шагов, баннер, тестовое напоминание).
- [x] 5.2 Показ мастера при первом запуске (флаг не выставлен), пропускаемый.

## 6. Проверка

- [x] 6.1 `openspec validate --all` — без ошибок.
- [x] 6.2 Сборка `:app:assembleDebug` и `:app:testDebugUnitTest` — успешно.
- [ ] 6.3 Ручной чек на устройстве с включённой оптимизацией батареи: мастер ведёт через шаги; баннер исчезает после выдачи; тестовое напоминание реально срабатывает через ~1 мин.
