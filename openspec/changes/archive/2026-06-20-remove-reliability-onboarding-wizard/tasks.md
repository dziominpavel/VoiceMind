## 1. ViewModel — удалить wizard state

- [x] 1.1 Удалить `onboardingCompleted` flow (привязка к `settings.onboardingCompleted`) из `VoiceMindViewModel.kt`.
- [x] 1.2 Удалить `_showOnboarding` / `showOnboarding` StateFlow и `consumeShowOnboarding()` (если есть).
- [x] 1.3 Удалить методы `openOnboarding()`, `dismissOnboarding()`, `completeOnboarding()` из `VoiceMindViewModel.kt`.
- [x] 1.4 Оставить `createTestReminder()` без изменений — будет вызываться из Settings.

## 2. SettingsRepository — удалить onboarding-ключ

- [x] 2.1 Удалить `onboardingCompletedKey` из `SettingsRepository.kt`.
- [x] 2.2 Удалить `onboardingCompleted: Flow<Boolean>`.
- [x] 2.3 Удалить `setOnboardingCompleted(completed: Boolean)`.

## 3. MainActivity — удалить wizard wiring

- [x] 3.1 Удалить `val showOnboarding by viewModel.showOnboarding.collectAsState()`.
- [x] 3.2 Удалить `val onboardingCompleted by viewModel.onboardingCompleted.collectAsState()`.
- [x] 3.3 Удалить `LaunchedEffect(Unit) { if (!onboardingCompleted) viewModel.openOnboarding(); ... }` — ВАЖНО: сохранить часть `notificationPermissionLauncher.launch(...)` если она в том же effect, вынести в отдельный `LaunchedEffect`.
- [x] 3.4 Удалить `AnimatedVisibility(visible = showOnboarding, ...) { ReliabilityOnboardingScreen(...) }` блок целиком.
- [x] 3.5 Удалить импорт `ReliabilityOnboardingScreen` и `AnimatedVisibility`/`slideInVertically`/`slideOutVertically` если больше не используются.

## 4. HomeScreen — переориентировать banner

- [x] 4.1 В `HomeScreen.kt` заменить параметр `onOpenReliabilityOnboarding: () -> Unit` на `onFixReliability: () -> Unit` (или переименовать callback).
- [x] 4.2 Кнопка «Исправить» в banner вызывает `onFixReliability` (логика выбора intent — в MainActivity, не в HomeScreen).
- [x] 4.3 Обновить вызов `HomeScreen` в `MainActivity`: `onOpenReliabilityOnboarding = { viewModel.openOnboarding() }` → `onFixReliability = { ... }` с логикой: если первый issue `NOTIFICATIONS_MISSING` → `notificationPermissionLauncher.launch(POST_NOTIFICATIONS)`, иначе `context.startActivity(ReminderPermissions.exactAlarmSettingsIntent(context))`.

## 5. SettingsScreen — убрать wizard-кнопку, добавить test reminder

- [x] 5.1 Удалить параметр `onOpenReliabilityOnboarding: () -> Unit` из сигнатуры `SettingsScreen` (если был).
- [x] 5.2 Удалить кнопку «Проверка надёжности» / `TextButton(onClick = onOpenReliabilityOnboarding)` из reliability-блока в SettingsScreen (строки ~117-122).
- [x] 5.3 Добавить кнопку «Проверить, что работает» в блок разрешений или новый блок «Диагностика» в SettingsScreen, вызов через новый параметр `onCreateTestReminder: () -> Unit`.
- [x] 5.4 Обновить вызов `SettingsScreen` в `MainActivity`: убрать `onOpenReliabilityOnboarding`, добавить `onCreateTestReminder = { viewModel.createTestReminder() }`.

## 6. Удалить ReliabilityOnboardingScreen

- [x] 6.1 Удалить файл `app/src/main/java/com/example/voicemind/ui/screens/ReliabilityOnboardingScreen.kt`.

## 7. Строковые ресурсы

- [x] 7.1 Удалить из `strings.xml`: `reliability_onboarding_title`, `reliability_onboarding_subtitle`, `reliability_action_skip`, `reliability_action_done`, `reliability_all_good_title`, `reliability_all_good_desc`, `reliability_action_test_reminder` (если не используется в новом месте Settings — если используется, переименовать/оставить).
- [x] 7.2 Проверить `grep reliability_onboarding|reliability_action_skip|reliability_action_done|reliability_all_good` — убедиться, что нет ссылок в коде.
- [x] 7.3 Оставить `reliability_banner_*`, `reliability_issue_*` (используются в banner и Settings).

## 8. Спека

- [x] 8.1 Убедиться, что delta spec содержит `## REMOVED Requirements` для «Мастер надёжности при первом запуске» и «Повторный вызов мастера из настроек».
- [x] 8.2 Убедиться, что delta spec содержит `## MODIFIED Requirements` для «Тестовое напоминание» и «Индикатор надёжности на главном экране».
- [x] 8.3 Запустить `openspec validate --all`.

## 9. Сборка и тесты

- [x] 9.1 Запустить `./gradlew assembleDebug` — убедиться, что компиляция проходит (проверка dangling references на удалённые символы).
- [x] 9.2 Запустить `./gradlew testDebugUnitTest` — убедиться, что unit-тесты проходят.
- [ ] 9.3 Ручной smoke (опционально): запустить приложение — должно открыться сразу на рабочем экране без wizard; проверить, что banner «Исправить» открывает системные настройки; проверить, что test reminder создаётся из Settings.
