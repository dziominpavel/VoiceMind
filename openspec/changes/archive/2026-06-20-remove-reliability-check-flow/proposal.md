## Why

Текущая реализация надёжности (persistent-баннер на Home + кнопка «Проверить, что работает» в Settings) дублирует то, что пользователь может сделать сам за секунды: создать обычное напоминание на ~1 минуту вперёд и увидеть доставку. Баннер на Home шумит, когда разрешения уже выданы или когда пользователь осознанно не хочет их давать, а отдельная «диагностика» в Settings — это параллельный путь создания напоминания в обход ConfirmScreen, что противоречит guardrail «Confirm перед schedule». Запрос критичных разрешений уже происходит в момент ценности (первое сохранение напоминания), а прямой доступ к системным настройкам остаётся в блоке разрешений Settings. Отдельный flow «проверка надёжности» больше не нужен.

## What Changes

- **BREAKING** Удаляется persistent-баннер «Надёжность» с главного экрана (`HomeScreen`) и связанный flow: `ReliabilityIssue`, `VoiceMindViewModel.reliabilityIssues`, `VoiceMindViewModel.checkReliability()`, вызов `checkReliability()` в `MainActivity.onResume()`, параметр `reliabilityIssues`/`onFixReliability` у `HomeScreen` и `SettingsScreen`.
- **BREAKING** Удаляется карточка «Диагностика» в `SettingsScreen` вместе с действием «Проверить, что работает»: параметр `onCreateTestReminder`, метод `VoiceMindViewModel.createTestReminder()`, передача колбэка из `MainActivity`.
- Удаляются строки `reliability_*` и `test_reminder_body` из `strings.xml` (если больше не используются).
- Блок разрешений в Settings («Точные будильники», «Уведомления», «Полноэкранные intent») остаётся без изменений — это прямой доступ к системным настройкам, не flow проверки.
- Запрос `POST_NOTIFICATIONS` в момент первого сохранения напоминания остаётся без изменений.

## Capabilities

### New Capabilities
<!-- Нет новых capability. -->

### Modified Capabilities
- `reliability-onboarding`: удаляются оба оставшихся requirement — «Тестовое напоминание» и «Индикатор надёжности на главном экране». После применения capability не содержит requirements и подлежит архивации (см. tasks.md).

## Impact

- **Код**:
  - `viewmodel/ReliabilityIssue.kt` — удалить файл.
  - `viewmodel/VoiceMindViewModel.kt` — удалить `_reliabilityIssues`, `reliabilityIssues`, `checkReliability()`, `createTestReminder()` и связанные импорты.
  - `ui/screens/HomeScreen.kt` — удалить параметры `reliabilityIssues`, `onFixReliability` и блок «Reliability Banner».
  - `ui/screens/SettingsScreen.kt` — удалить параметр `reliabilityIssues`, карточку «Диагностика», параметр `onCreateTestReminder`, неиспользуемые импорты (`ReliabilityIssue`, `Button`, `reliability_*` strings).
  - `MainActivity.kt` — удалить `viewModel.checkReliability()` в `onResume`, `reliabilityIssues` collect, передачу `reliabilityIssues`/`onFixReliability`/`onCreateTestReminder` в экраны, ветку `onFixReliability`.
- **Ресурсы**: `strings.xml` — удалить `reliability_*` и `test_reminder_body` (после проверки, что не используются).
- **Specs**: `openspec/specs/reliability-onboarding/spec.md` — после применения остаётся без requirements; capability архивируется.
- **Тесты**: интеграционных/UI-тестов на баннер и тестовое напоминание нет — удалять нечего.
- **Миграция данных**: не требуется. Orphan-флаг `onboarding_completed` в DataStore уже безвреден с прошлой смены.
