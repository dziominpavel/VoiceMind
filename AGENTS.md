# Руководство для агента (Cursor)

**VoiceMind** — голосовая напоминалка (парсинг времени + текст → alarm/уведомление).

**GymProgress** — референс только для **стиля кода** (Kotlin, Compose, Room, ViewModel, `safeDb`, документация). **UI и домен GymProgress не копировать.**

## Перед изменениями

1. `docs/PROJECT_OVERVIEW.md` — продукт.
2. `docs/FEATURE_PLAN.md` — фазы.
3. `docs/REMINDER_PARSING.md`, `docs/NOTIFICATION_MODES.md` — парсинг и оповещения.
4. `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`.
5. Детальные планы сессий — `.cursor/plans/`.

## Приоритеты

- **Confirm перед schedule** — never alarm without user confirm (настройка `confirmBeforeSchedule` только для power users).
- **Точные alarm** — `ReminderScheduler` единственная точка; после изменения `fireAt` — cancel + schedule.
- **Offline MVP** — parser + on-device STT без сети.
- **Один ViewModel** — `VoiceMindViewModel`.
- **Room** — миграции, не destructive в release.
- **Ошибки** — `safeDb`, Snackbar.

## Сортировка

| Список | ORDER BY |
|--------|----------|
| Предстоящие | `fireAt ASC, id ASC` |
| История | `fireAt DESC, id DESC` |

## Секреты

- `OPENROUTER_API_KEY` — только fallback-парсер (фаза 5), `local.properties` → `BuildConfig`.

## Стиль кода

- Как GymProgress: корутины, StateFlow, Compose.
- UI-тексты — русский; код — английский.
- UI-токены — `VoiceMindTheme`, не палитра GymProgress.
