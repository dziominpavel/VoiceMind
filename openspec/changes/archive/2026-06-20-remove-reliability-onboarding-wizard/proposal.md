## Why

Мастер надёжности (ReliabilityOnboardingScreen) выскакивает при каждом запуске приложения из-за race condition: `LaunchedEffect(Unit)` в `MainActivity` читает `onboardingCompleted` до того, как DataStore успевает вернуть сохранённое `true`, и зовёт `openOnboarding()`. Даже если починить race condition, сам концепт обязательного first-launch wizard — это тяжёлое upfront-трение: пользователь ещё не понял ценность приложения, а ему уже показывают экран с разрешениями. Запрос разрешений должен происходить в момент ценности (первое сохранение напоминания), а не как ритуал при запуске. Persistent banner на Home + блок разрешений в Settings уже покрывают обнаружение проблем — wizard как отдельный full-screen flow избыточен.

## What Changes

- **Удалить** экран `ReliabilityOnboardingScreen.kt` целиком.
- **Удалить** state и методы wizard во `VoiceMindViewModel`: `showOnboarding`, `openOnboarding()`, `dismissOnboarding()`, `completeOnboarding()`, `onboardingCompleted` flow.
- **Удалить** `LaunchedEffect(Unit)` в `MainActivity`, который автоматически открывает wizard при запуске.
- **Удалить** `AnimatedVisibility { ReliabilityOnboardingScreen(...) }` блок в `MainActivity`.
- **Удалить** ключ `onboarding_completed` и метод `setOnboardingCompleted` из `SettingsRepository` (DataStore).
- **Удалить** Requirement «Мастер надёжности при первом запуске» и Requirement «Повторный вызов мастера из настроек» из spec `reliability-onboarding`.
- **Изменить** persistent banner на Home: кнопка «Исправить» вместо открытия wizard открывает системные настройки разрешений напрямую (через `ReminderPermissions.exactAlarmSettingsIntent` / `notificationPermissionLauncher`).
- **Изменить** блок надёжности в Settings: убрать кнопку «Проверка надёжности» (которая открывала wizard), оставить только карточки разрешений (точные будильники, уведомления, full-screen intent).
- **Сохранить** `createTestReminder()` и Requirement «Тестовое напоминание» — действие «проверить срабатывание» переносится в Settings как отдельная кнопка (не часть wizard).
- **Сохранить** persistent banner на Home и Requirement «Индикатор надёжности на главном экране» — он ненавязчивый и показывает только когда реально что-то не так.
- **Удалить** строковые ресурсы, относящиеся только к wizard: `reliability_onboarding_title`, `reliability_onboarding_subtitle`, `reliability_action_skip`, `reliability_action_done`, `reliability_all_good_title`, `reliability_all_good_desc`, `reliability_action_test_reminder`.

## Capabilities

### New Capabilities
<!-- Нет новых capabilities. -->

### Modified Capabilities
- `reliability-onboarding`: удаляются Requirement «Мастер надёжности при первом запуске» и Requirement «Повторный вызов мастера из настроек»; MODIFIED Requirement «Тестовое напоминание» — действие переносится из wizard в Settings; MODIFIED Requirement «Индикатор надёжности на главном экране» — кнопка «Исправить» открывает системные настройки разрешений напрямую, а не wizard.

## Impact

- **Код**: `ReliabilityOnboardingScreen.kt` (удаление), `MainActivity.kt`, `VoiceMindViewModel.kt`, `SettingsRepository.kt`, `HomeScreen.kt`, `SettingsScreen.kt`.
- **DataStore**: удаляется ключ `onboarding_completed`; миграция не нужна (флаг просто перестаёт читаться).
- **Ресурсы**: `strings.xml` теряет ~7 строк wizard-текстов; `reliability_banner_*` и `reliability_issue_*` остаются.
- **Спека**: `openspec/specs/reliability-onboarding/spec.md` — 2 REMOVED + 2 MODIFIED Requirements.
- **Пользователи**: приложение открывается сразу на рабочий экран без модальных ритуалов; разрешения запрашиваются в момент сохранения напоминания или исправляются через banner/Settings.
- **Архитектура**: упрощается state graph `VoiceMindViewModel` (минус `showOnboarding` flow и 3 метода).
