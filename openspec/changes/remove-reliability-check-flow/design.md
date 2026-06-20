## Context

После смены `2026-06-20-remove-reliability-onboarding-wizard` в приложении остались два артефакта flow надёжности:

1. **Persistent-баннер на Home** — `ReliabilityIssue` enum, `VoiceMindViewModel.reliabilityIssues: StateFlow<List<ReliabilityIssue>>`, `checkReliability()` (вызывается из `MainActivity.onResume()`), блок «Reliability Banner» в `HomeScreen` с кнопкой «Исправить», открывающей системные настройки разрешений.
2. **Карточка «Диагностика» в Settings** — кнопка «Проверить, что работает», вызывающая `VoiceMindViewModel.createTestReminder()`, которая через `repository.insertAndSchedule` создаёт напоминание на `now + 60_000ms` в обход ConfirmScreen.

Блок разрешений в Settings («Точные будильники», «Уведомления», «Полноэкранные intent») — отдельная, независимая часть UI: каждый пункт через `ReminderPermissions.*` проверяет состояние и даёт прямой переход в системные настройки. Этот блок остаётся.

Запрос `POST_NOTIFICATIONS` в момент первого сохранения напоминания также остаётся (в `ConfirmReminderScreen`/`saveReminder` path).

## Goals / Non-Goals

**Goals:**
- Удалить persistent-баннер «Надёжность» с Home и весь связанный flow (`ReliabilityIssue`, `reliabilityIssues`, `checkReliability`, вызов из `onResume`).
- Удалить карточку «Диагностика» и действие «Проверить, что работает» из Settings (`onCreateTestReminder`, `createTestReminder()`).
- Удалить неиспользуемые строки `reliability_*` и `test_reminder_body` из `strings.xml`.
- Удалить файл `viewmodel/ReliabilityIssue.kt`.
- Сохранить блок разрешений в Settings и запрос `POST_NOTIFICATIONS` при первом сохранении без изменений.
- После применения архивировать capability `reliability-onboarding` (в ней не остаётся requirements).

**Non-Goals:**
- Не менять блок разрешений в Settings и логику запроса `POST_NOTIFICATIONS`/`SCHEDULE_EXACT_ALARM`/`USE_FULL_SCREEN_INTENT`.
- Не менять `ReminderPermissions` (утилита используется блоком разрешений).
- Не вводить новый flow диагностики или онбординга.
- Не трогать `onboarding_completed` orphan-флаг в DataStore (уже безвреден).
- Не менять виджет, парсер, STT, AlarmManager.

## Decisions

### Решение 1: Удалить, а не скрывать за флагом
**Решение:** Полностью удалить код и ресурсы flow надёжности и тестового напоминания, без feature-flag.
**Альтернативы:** Скрыть баннер за настройкой «показывать предупреждения» — отказано, это оставляет мёртвый код и поверхность ошибок (race в `onResume`), ради чего? Пользователь, которому важна проверка, создаёт обычное напоминание на минуту вперёд.
**Почему:** Удаление — это меньше кода, меньше импортов, меньше состояний в `VoiceMindViewModel`, нет race condition при пересчёте в `onResume`. Соответствует философии MVP: меньше surface, больше фокус на основном сценарии.

### Решение 2: Блок разрешений в Settings остаётся как есть
**Решение:** Не трогать `PermissionCard` для exact alarm / notifications / full-screen intent в `SettingsScreen`.
**Почему:** Это прямой, не-modal доступ к системным настройкам каждого разрешения — не flow проверки и не нарушает guardrail «Confirm перед schedule». Пользователь, который понимает, что ему нужно разрешение, идёт туда сам. Это покрывает обнаружение и исправление проблем без отдельного индикатора на Home.

### Решение 3: Тестовое напоминание заменяется пользовательским сценарием
**Решение:** Не предоставлять программную замену «проверить срабатывание». Пользователь создаёт обычное напоминание на ~1 минуту вперёд (голосом или вручную) через стандартный ConfirmScreen.
**Почему:** Это тот же путь планирования (`repository.insertAndSchedule` → `ReminderScheduler.schedule`), та же доставка, но через явное согласие на ConfirmScreen. Соответствует guardrail «Confirm перед schedule». Не плодит параллельный путь создания напоминаний.

### Решение 4: Архивировать capability `reliability-onboarding` после применения
**Решение:** После `openspec archive` этой смены capability `reliability-onboarding` в `openspec/specs/` останется без единого requirement — её нужно архивировать (переместить в `openspec/specs/archive/` или удалить согласно конвенции проекта).
**Почему:** Пустая capability в `openspec/specs/` валидируется, но не несёт требований и сбивает с толку. В tasks.md — отдельная задача проверить после применения, что capability пуста, и архивировать её.

## Risks / Trade-offs

- **[Risk] Пользователь не поймёт, как проверить доставку** → Mitigation: основной сценарий «создать напоминание на минуту вперёд» самоочевиден и уже описан в `docs/PROJECT_OVERVIEW.md`. Блок разрешений в Settings даёт прямой доступ к настройкам, если доставка не работает.
- **[Risk] Пользователь не узнает, что разрешения не выданы, пока не создаст напоминание** → Mitigation: запрос `POST_NOTIFICATIONS` происходит в момент первого сохранения (в `ConfirmReminderScreen`/save path) — это и есть момент ценности. Блок разрешений в Settings доступен всегда для тех, кто хочет проверить заранее.
- **[Trade-off] Меньше «руководящей руки» для неопытных пользователей** → Принято: MVP фокусируется на основном сценарии, а не на edge-case онбординге. Прошлая смена уже убрала wizard по той же причине.
- **[Risk] Orphan-импорты/ссылки останутся после удаления** → Mitigation: tasks.md включает явный шаг `./gradlew assembleDebug` + `grep` по `reliability`/`createTestReminder`/`ReliabilityIssue` для проверки отсутствия ссылок.

## Migration Plan

1. Удалить код и ресурсы (см. tasks.md).
2. `./gradlew assembleDebug` — сборка должна проходить без ошибок.
3. `grep -ri "reliability\|ReliabilityIssue\|createTestReminder\|onCreateTestReminder" app/src` — пусто.
4. Ручная проверка: Home без баннера, Settings без карточки «Диагностика», блок разрешений на месте, создание напоминания на ~1 минуту вперёд доставляется.
5. `openspec validate --all` → `openspec archive remove-reliability-check-flow`.
6. После архивации проверить, что `openspec/specs/reliability-onboarding/` пуста (нет requirements), и архивировать capability согласно конвенции проекта.

Rollback: git revert коммита применения. Миграции данных нет, поэтому откат безопасен.
