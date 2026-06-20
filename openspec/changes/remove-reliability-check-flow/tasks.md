## 1. ViewModel: удалить flow надёжности и тестовое напоминание

- [x] 1.1 В `viewmodel/VoiceMindViewModel.kt` удалить `_reliabilityIssues`, `reliabilityIssues: StateFlow<List<ReliabilityIssue>>`, `checkReliability()` и импорт `ReliabilityIssue`/`ReminderPermissions` (если `ReminderPermissions` больше не используется в файле — проверить). `ReminderPermissions` используется в `saveReminder` — импорт оставлен.
- [x] 1.2 В `viewmodel/VoiceMindViewModel.kt` удалить `createTestReminder()` и связанные импорты (`R`, `UUID`, `WidgetUpdater` — только если не используются другими методами; `duplicateReminder` использует `UUID`/`WidgetUpdater`, поэтому оставить).
- [x] 1.3 Удалить файл `viewmodel/ReliabilityIssue.kt`.

## 2. UI: убрать баннер с Home и карточку «Диагностика» из Settings

- [x] 2.1 В `ui/screens/HomeScreen.kt` удалить параметры `reliabilityIssues: List<ReliabilityIssue>` и `onFixReliability: () -> Unit` из сигнатуры `HomeScreen`, удалить блок «Reliability Banner» (Card + TextButton), удалить неиспользуемые импорты (`ReliabilityIssue`, `ErrorCoral`, `TextButton`, `reliability_*` strings).
- [x] 2.2 В `ui/screens/SettingsScreen.kt` удалить параметр `reliabilityIssues: List<ReliabilityIssue>` из сигнатуры `SettingsScreen`, удалить блок «Reliability Card» в начале Column, удалить карточку «Диагностика» (SettingsCard + Button + `onCreateTestReminder`), удалить параметр `onCreateTestReminder: () -> Unit`, удалить неиспользуемые импорты (`ReliabilityIssue`, `Button`, `reliability_*`/`test_reminder_body` strings).

## 3. MainActivity: убрать вызовы и прокидку параметров

- [x] 3.1 В `MainActivity.kt` удалить `viewModel.checkReliability()` в `onResume()`.
- [x] 3.2 В `MainActivity.kt` удалить `val reliabilityIssues by viewModel.reliabilityIssues.collectAsState()` и импорт `ReliabilityIssue`.
- [x] 3.3 В `MainActivity.kt` удалить передачу `reliabilityIssues`/`onFixReliability` в `HomeScreen` и всю ветку `onFixReliability` (логика с `ReliabilityIssue.NOTIFICATIONS_MISSING`/`EXACT_ALARM_MISSING`).
- [x] 3.4 В `MainActivity.kt` удалить передачу `reliabilityIssues` и `onCreateTestReminder = { viewModel.createTestReminder() }` в `SettingsScreen`.

## 4. Ресурсы: удалить неиспользуемые строки

- [x] 4.1 В `app/src/main/res/values/strings.xml` удалить строки `reliability_issue_notifications_title`, `reliability_issue_notifications_desc`, `reliability_issue_exact_alarm_title`, `reliability_issue_exact_alarm_desc`, `reliability_action_grant`, `reliability_action_test_reminder`, `reliability_banner_title`, `reliability_banner_desc`, `reliability_banner_action`, `test_reminder_body`.
- [x] 4.2 Проверить grep'ом, что удалённые строки не используются нигде в коде (`grep -ri "reliability_\|test_reminder_body" app/src`).

## 5. Сборка и проверка

- [x] 5.1 Запустить `./gradlew assembleDebug` — сборка должна проходить без ошибок и warning'ов об orphan-импортах.
- [x] 5.2 Запустить `./gradlew testDebugUnitTest` — unit-тесты должны проходить (парсер, рекурренция, AlarmActivityTest).
- [x] 5.3 `grep -ri "reliability\|ReliabilityIssue\|createTestReminder\|onCreateTestReminder\|onFixReliability" app/src` — должно быть пусто.
- [ ] 5.4 Ручная проверка на устройстве/эмуляторе: Home открывается без баннера, Settings без карточки «Диагностика», блок разрешений на месте, создание напоминания на ~1 минуту вперёд доставляется выбранным режимом.

## 6. OpenSpec: валидация и архивация

- [x] 6.1 `openspec validate --all` — должно проходить без ошибок.
- [ ] 6.2 Зафиксировать изменения в git (отдельный коммит применения).
- [ ] 6.3 `openspec archive remove-reliability-check-flow` — архивировать смену.
- [ ] 6.4 Проверить, что `openspec/specs/reliability-onboarding/spec.md` не содержит requirements (только `## Purpose`), и архивировать capability согласно конвенции проекта (переместить в `openspec/specs/archive/` или удалить).
