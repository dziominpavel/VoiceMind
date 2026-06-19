## Context

Текущая подсистема `reliability-onboarding` состоит из четырёх частей:

1. **ReliabilityOnboardingScreen** — full-screen wizard с карточками разрешений и тестовым напоминанием. Показывается автоматически при первом запуске через `LaunchedEffect(Unit)` в `MainActivity`, который проверяет `onboardingCompleted` (DataStore). Из-за асинхронности DataStore проверка срабатывает до загрузки сохранённого `true`, и wizard открывается при каждом запуске.
2. **Persistent banner на HomeScreen** — показывает «напоминания могут не сработать» + кнопку «Исправить», если `reliabilityIssues` не пуст. Кнопка открывает wizard.
3. **Блок разрешений в SettingsScreen** — карточки «Точные будильники», «Уведомления», «Полноэкранные intent» с переходами к системным настройкам.
4. **`createTestReminder()`** во ViewModel — создаёт напоминание на `now + 60s` через стандартный путь планирования. Доступен только из wizard (кнопка «Проверить, что работает» в `AllGoodCard`).

Запрос разрешений в момент первого сохранения напоминания уже частично существует: `requestNotificationsPermission` flow во ViewModel срабатывает при сохранении без разрешения. `SCHEDULE_EXACT_ALARM` проверяется в `ReminderScheduler` с fallback на `setAndAllowWhileIdle`.

## Goals / Non-Goals

**Goals:**
- Убрать автоматическое открытие wizard при запуске приложения.
- Удалить экран `ReliabilityOnboardingScreen` и весь связанный state во ViewModel.
- Сохранить persistent banner на Home как индикатор проблем, но переориентировать кнопку «Исправить» на прямое открытие системных настроек разрешений.
- Сохранить блок разрешений в Settings.
- Сохранить test reminder как действие в Settings, а не в wizard.
- Обновить spec `reliability-onboarding` (REMOVED + MODIFIED Requirements).

**Non-Goals:**
- Не добавлять новый onboarding flow взамен wizard — приложение открывается сразу на рабочий экран.
- Не менять `ReminderScheduler`, `BootReceiver`, `ReminderAlarmReceiver`.
- Не добавлять контекстный запрос `SCHEDULE_EXACT_ALARM` в момент сохранения (уже есть fallback в scheduler; полный contextual flow — отдельная change если понадобится).
- Не трогать `ReliabilityIssue` enum (остаются `NOTIFICATIONS_MISSING`, `EXACT_ALARM_MISSING`).
- Не трогать `checkReliability()` во ViewModel — он кормит banner и Settings.

## Decisions

### Decision 1: Banner «Исправить» открывает системные настройки напрямую, не экран

**Решение:** Кнопка «Исправить» на Home banner открывает системный intent `ReminderPermissions.exactAlarmSettingsIntent(context)` — это самое критичное разрешение (без него alarm не сработает точно). Для `POST_NOTIFICATIONS` запрос уже происходит в момент сохранения напоминания через `requestNotificationsPermission` flow.

**Альтернативы:**
- *Открывать SettingsScreen на вкладке разрешений.* Отклонено: лишний переход, пользователь уже на Home, проще дать прямой доступ к самому критичному разрешению. К тому же NavigationSuiteScaffold не даёт программно переключить tab без дополнительного state.
- *Показывать bottom sheet с списком проблем.* Отклонено: это по сути новый мини-wizard, против которого мы и боремся.
- *Убрать кнопку совсем, оставить только текст-предупреждение.* Отклонено: баннер без действия бесполезен и раздражает.

### Decision 2: Test reminder переезжает в Settings, не удаляется

**Решение:** Перенести `createTestReminder()` из wizard в SettingsScreen как отдельную кнопку в блоке разрешений или в новом блоке «Диагностика». Функция во ViewModel остаётся без изменений.

**Альтернативы:**
- *Удалить test reminder целиком.* Отклонено: это единственный способ пользователю проверить, что доставка работает, без создания реального напоминания вручную. Полезная фича, просто жила не там.
- *Оставить только в wizard.* Невозможно — wizard удаляется.

### Decision 3: Удалить `onboarding_completed` из DataStore без миграции

**Решение:** Удалить ключ `onboardingCompletedKey` и методы `onboardingCompleted` flow / `setOnboardingCompleted` из `SettingsRepository`. Не вводить миграцию — ключ просто перестаёт читаться, DataStore проигнорирует orphan-запись при следующем write (не удаляет автоматически, но это не влияет на функциональность).

**Альтернативы:**
- *Сбросить ключ явным migration step.* Отклонено: избыточно, orphan boolean в DataStore не вызывает ошибок и не занимает значимого места.
- *Оставить ключ, просто перестать использовать.* Отклонено: оставляет мёртвый код в `SettingsRepository`, что затрудняет понимание.

### Decision 4: Spec-операции — 2 REMOVED + 2 MODIFIED

**Решение:**
- `## REMOVED Requirements`: «Мастер надёжности при первом запуске», «Повторный вызов мастера из настроек».
- `## MODIFIED Requirements`: «Тестовое напоминание» (действие переносится из wizard в Settings), «Индикатор надёжности на главном экране» (кнопка «Исправить» открывает системные настройки разрешений напрямую).

**Альтернативы:**
- *Только REMOVED всех 4 requirements и ADDED новых.* Отклонено: MODIFIED сохраняет историю и scenario structure для требований, которые меняют точку входа, но не суть.

## Risks / Trade-offs

- **[Risk] Пользователь не узнает о необходимости разрешений без wizard.** → Mitigation: persistent banner на Home показывает проблему сразу при запуске; блок разрешений в Settings доступен всегда; `POST_NOTIFICATIONS` запрашивается в момент сохранения.
- **[Risk] Banner «Исправить» открывает только exact alarm settings, а проблема может быть в notifications.** → Mitigation: если `reliabilityIssues` содержит только `NOTIFICATIONS_MISSING`, кнопка открывает `notificationPermissionLauncher` вместо exact alarm. Логика выбора intent — по первому issue в списке.
- **[Trade-off] Теряем «объясняющий» UX wizard (каждый шаг с описанием зачем).** → Принято: описания есть в карточках разрешений SettingsScreen; banner даёт краткое сообщение. Пользователь, которому нужно подробное объяснение, найдёт его в Settings.
- **[Risk] Orphan ключ `onboarding_completed` в DataStore у существующих пользователей.** → Mitigation: безвредно — DataStore не падает на неиспользуемых ключах; при следующем write ключ не трогается. Если когда-то захочется очистить — отдельный migration step.

## Migration Plan

1. Удалить `ReliabilityOnboardingScreen.kt`.
2. Удалить wizard state из `VoiceMindViewModel` (`showOnboarding`, `openOnboarding`, `dismissOnboarding`, `completeOnboarding`, `onboardingCompleted`).
3. Удалить `onboardingCompleted` из `SettingsRepository`.
4. Удалить `LaunchedEffect(Unit)` auto-open и `AnimatedVisibility { ReliabilityOnboardingScreen }` из `MainActivity`.
5. Изменить banner на Home: `onOpenReliabilityOnboarding` → `onFixReliability` callback, который открывает системные настройки по первому issue.
6. Перенести test reminder в Settings: добавить кнопку в `SettingsScreen`, wiring к `viewModel.createTestReminder()`.
7. Удалить wizard-строки из `strings.xml`.
8. Обновить spec (REMOVED + MODIFIED).
9. `./gradlew assembleDebug` + `./gradlew testDebugUnitTest`.
10. Smoke: запустить приложение — должно открыться сразу на рабочем экране без wizard.

**Rollback:** `git revert` коммита change. DataStore не модифицировался структурно (orphan ключ остаётся, но безвреден).

## Open Questions

Нет. Решение полностью определено в рамках proposal.
