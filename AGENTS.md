# VoiceMind — руководство для агента

> Этот файл — quick reference. Детальные правила для Windsurf/Cascade: `.windsurf/rules/`.

**VoiceMind** — голосовая напоминалка (парсинг времени + текст → alarm/уведомление).

**GymProgress** — референс только для **стиля кода** (Kotlin, Compose, Room, ViewModel, `safeDb`, документация). **UI и домен GymProgress не копировать.**

## Перед изменениями

1. `docs/PROJECT_OVERVIEW.md` — продукт.
2. `docs/FEATURE_PLAN.md` — фазы.
3. `docs/REMINDER_PARSING.md`, `docs/NOTIFICATION_MODES.md` — парсинг и оповещения.
4. `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`.
5. `.windsurf/rules/` — автоматические правила для Windsurf/Cascade.
6. `.windsurf/workflows/` — повторяющиеся сценарии (`/implement-feature`, `/parser-work`, `/db-migration`, `/code-review`).
7. `.cursor/rules/` и `.cursor/plans/` — контекст и планы для Cursor (см. `project-context.mdc`).

## Приоритеты (кратко)

- **Confirm перед schedule** — never alarm без явного confirm.
- **Точные alarm** — `ReminderScheduler` единственная точка; после `fireAt` — cancel + schedule.
- **Offline MVP** — parser + on-device STT без сети.
- **Один ViewModel** — `VoiceMindViewModel`.
- **Room** — миграции, не destructive в release.
- **Ошибки** — `safeDb`, Snackbar.
- **Сортировка** — предстоящие `fireAt ASC`, история `fireAt DESC`.
