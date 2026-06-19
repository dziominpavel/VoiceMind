## 1. Манифест

- [x] 1.1 Удалить `<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />` из `app/src/main/AndroidManifest.xml`.

## 2. ReminderPermissions

- [x] 2.1 Удалить метод `isIgnoringBatteryOptimizations(context)` из `app/src/main/java/com/example/voicemind/util/ReminderPermissions.kt`.
- [x] 2.2 Удалить метод `requestIgnoreBatteryOptimizationsIntent(context)`.
- [x] 2.3 Удалить метод `batteryOptimizationSettingsIntent()`.
- [x] 2.4 Удалить неиспользуемые импорты (`PowerManager`, `Settings` — если больше не нужны).

## 3. ViewModel и enum ReliabilityIssue

- [x] 3.1 Удалить значение `BATTERY_OPTIMIZATION_NOT_IGNORED` из enum `ReliabilityIssue` (найти файл объявления).
- [x] 3.2 Удалить проверку `isIgnoringBatteryOptimizations` и добавление `BATTERY_OPTIMIZATION_NOT_IGNORED` из `VoiceMindViewModel.checkReliability()`.

## 4. UI — ReliabilityOnboardingScreen

- [x] 4.1 Удалить параметр `onRequestBatteryOptimization: () -> Unit` из сигнатуры `ReliabilityOnboardingScreen`.
- [x] 4.2 Удалить `when`-ветку `ReliabilityIssue.BATTERY_OPTIMIZATION_NOT_IGNORED` (карточка IssueCard с `BatteryAlert`).
- [x] 4.3 Удалить неиспользуемый импорт `Icons.Default.BatteryAlert` (если остался).

## 5. UI — SettingsScreen

- [x] 5.1 Удалить блок battery optimization из `SettingsScreen.kt` (проверка `isIgnoringBatteryOptimizations`, кнопка перехода к `requestIgnoreBatteryOptimizationsIntent`).
- [x] 5.2 Удалить параметр `onRequestBatteryOptimization` из сигнатуры `SettingsScreen`, если он там был.
- [x] 5.3 Убрать неиспользуемые импорты.

## 6. MainActivity — wiring

- [x] 6.1 Удалить wiring `onRequestBatteryOptimization = { ... }` из вызова `ReliabilityOnboardingScreen` в `MainActivity.kt`.
- [x] 6.2 Удалить wiring `onRequestBatteryOptimization` из вызова `SettingsScreen` (если передавался).
- [x] 6.3 Удалить неиспользуемые импорты (`Settings`, `Uri` для battery intent — проверить, что используются elsewhere).

## 7. Строковые ресурсы

- [x] 7.1 Удалить строки `reliability_issue_battery_title`, `reliability_issue_battery_desc` и любые другие `reliability_*battery*` из `app/src/main/res/values/strings.xml`.
- [x] 7.2 Проверить, что на удалённые строки нет ссылок в коде (`grep reliability_issue_battery`).

## 8. Спека

- [x] 8.1 Убедиться, что delta spec `openspec/changes/remove-battery-optimization-exemption/specs/reliability-onboarding/spec.md` содержит `## REMOVED Requirements` для requirement «Запрос исключения из оптимизации батареи».
- [x] 8.2 Запустить `openspec validate --all` — убедиться, что валидация проходит.

## 9. Сборка и тесты

- [x] 9.1 Запустить `./gradlew assembleDebug` — убедиться, что компиляция проходит без ошибок (проверка dangling references).
- [x] 9.2 Запустить `./gradlew testDebugUnitTest` — убедиться, что unit-тесты проходят (включая `ReminderParserTest` и любые тесты, связанные с reliability).
- [ ] 9.3 Ручной smoke (опционально): запустить приложение, пройти wizard — убедиться, что шаг battery отсутствует; проверить, что индикатор надёжности на Home не показывается при наличии notifications + exact alarm.
