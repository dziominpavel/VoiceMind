## Why

Запрос исключения из оптимизации батареи (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) не приносит реальной пользы для MVP: напоминания планируются через `AlarmManager.setExactAndAllowWhileIdle`, который уже обходит Doze для одиночных срабатываний, а recurring перепланируется внутри `ReminderAlarmReceiver` через тот же путь. Между срабатываниями приложению не нужно работать в фоне. При этом permission является flagged в Google Play и требует обоснования, а wizard с этим шагом увеличивает трение при первом запуске без повышения надёжности. Эмпирически без exemption напоминания срабатывали стабильно.

## What Changes

- **Удалить** permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` из `AndroidManifest.xml`.
- **Удалить** методы `isIgnoringBatteryOptimizations`, `requestIgnoreBatteryOptimizationsIntent`, `batteryOptimizationSettingsIntent` из `ReminderPermissions`.
- **Удалить** значение `BATTERY_OPTIMIZATION_NOT_IGNORED` из enum `ReliabilityIssue` и соответствующую проверку в `VoiceMindViewModel.checkReliability()`.
- **Удалить** шаг battery optimization из `ReliabilityOnboardingScreen` и callback `onRequestBatteryOptimization` из его сигнатуры.
- **Удалить** блок battery optimization из `SettingsScreen` и wiring `onRequestBatteryOptimization` в `MainActivity`.
- **Удалить** строковые ресурсы `reliability_issue_battery_*`.
- **Удалить** Requirement «Запрос исключения из оптимизации батареи» из spec `reliability-onboarding`.

## Capabilities

### New Capabilities
<!-- Нет новых capabilities. -->

### Modified Capabilities
- `reliability-onboarding`: удаляется Requirement «Запрос исключения из оптимизации батареи» и связанные сценарии; мастер надёжности и индикатор на главном экране больше не учитывают состояние battery optimization.

## Impact

- **Код**: `ReminderPermissions.kt`, `VoiceMindViewModel.kt`, `ReliabilityOnboardingScreen.kt`, `SettingsScreen.kt`, `MainActivity.kt`.
- **Манифест**: `AndroidManifest.xml` теряет одно permission.
- **Ресурсы**: `strings.xml` теряет несколько строк `reliability_issue_battery_*`.
- **Спека**: `openspec/specs/reliability-onboarding/spec.md` — REMOVED Requirement.
- **Зависимости/API**: никаких внешних API не затрагивается; публичная поверхность приложения сужается (минус flagged permission).
- **Пользователи**: первый запуск становится короче (минус один шаг в wizard), настройки — проще (минус один блок). Надёжность доставки не ухудшается.
- **Play Store**: снижается риск ревью/отклонения из-за flagged permission.
