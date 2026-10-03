# VoiceMind — руководство для агента

> Этот файл — quick reference. Детальные правила для Windsurf/Cascade: `.windsurf/rules/`.

**VoiceMind** — голосовая напоминалка (парсинг времени + текст → alarm/уведомление).

**GymProgress** — референс только для **стиля кода** (Kotlin, Compose, Room, ViewModel, `safeDb`, документация). **UI и домен GymProgress не копировать.**

## Перед изменениями

1. `docs/PROJECT_OVERVIEW.md` — продукт.
2. `docs/FEATURE_PLAN.md` — фазы.
3. `openspec/specs/reminder-parsing/spec.md` — **формальные требования парсера** (BDD-сценарии + справочник паттернов).
4. `docs/NOTIFICATION_MODES.md` — режимы оповещения.
5. `docs/ARCHITECTURE.md`, `docs/DESIGN_SYSTEM.md`.
6. `.cursor/rules/` и `.cursor/plans/` — контекст и планы для Cursor (см. `project-context.mdc`).
7. `.devin/rules/` — те же openspec-правила для Devin.

## OpenSpec

- Артефакты на **русском**, normative keywords — **MUST/SHALL/MUST NOT** (не «ДОЛЖЕН»).
- Main specs: `## Purpose` + `## Requirements`; delta: `## ADDED/MODIFIED/REMOVED`.
- Проверка: `openspec validate --all`. Workflows: `/opsx:propose`, `/opsx:apply`, `/opsx:archive`.

## Приоритеты (кратко)

- **Confirm перед schedule** — never alarm без явного confirm.
- **Точные alarm** — `ReminderScheduler` единственная точка; после `fireAt` — cancel + schedule.
- **Offline MVP** — parser + on-device STT без сети.
- **Один ViewModel** — `VoiceMindViewModel`.
- **Room** — миграции, не destructive в release.
- **Ошибки** — `safeDb`, Snackbar.
- **Сортировка** — предстоящие `fireAt ASC`, история `fireAt DESC`.

<!-- versioning:begin -->
## Версии и changelog (обязательно)

- Версия проекта меняется **только в момент релиза**: бамп вне релиза запрещён,
  между релизами номер остаётся номером последнего релиза. У статического трека
  версия заморожена и не меняется вовсе.
- Каждая пользовательская правка сопровождается пунктом в секции `## [Unreleased]`
  файла `CHANGELOG.md` — в момент работы, а не перед релизом. Чистые доки, спеки,
  тесты, CI и внутренний рефактор в changelog не пишутся.
- Релиз (где есть конвейер): `release.ps1 -Prepare` → сборка артефактов в `dist/`
  → `release.ps1`. Порядок не меняется.
- Сверка перед работой и после: `python scripts/check-version.py`
  (exit 0 — версия и changelog согласованы, exit 1 — рассинхрон).
- Полные правила и запреты: `docs/versioning.md`.
<!-- versioning:end -->
