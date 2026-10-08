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
- В changelog попадают **только глобальные доработки** — новая функция, заметное
  изменение поведения, фикс, который пользователь реально видит. Пункт пишется
  в секцию `## [Unreleased]` в момент работы, а не перед релизом. Мелочь
  (косметика UI: отступы, цвета, выравнивание, подпись элемента, и локальные
  правки без заметного эффекта), как и чистые доки, спеки, тесты, CI,
  внутренний рефактор, в changelog не пишется.
- Релиз (где есть конвейер): `release.ps1 -Prepare` → сборка артефактов в `dist/`
  → `release.ps1`. Порядок не меняется.
- Секция changelog начинается с **пользовательского саммари** — 2–5 строк о том,
  что изменится для пользователя, — **выше первой строки** `###`; под категориями
  (`### Добавлено`, `### Исправлено` и аналоги) идут инженерские детали: change-id,
  состав изменений, тесты, метрики. Подготовка релиза откажет, если саммари нет.
- Заметки GitHub-Release берутся из этого блока автоматически — отдельное
  описание релиза писать не нужно и нельзя дублировать.
- Сверка перед работой и после: `python scripts/check-version.py`
  (exit 0 — версия и changelog согласованы, exit 1 — рассинхрон).
- Полные правила и запреты: `docs/versioning.md`.
<!-- versioning:end -->

<!-- commit-hygiene:begin -->
## Коммиты: без ИИ-трейлеров (обязательно)

- **ЗАПРЕЩЕНО** добавлять в сообщение коммита строки `Generated with [...]`,
  `Co-Authored-By` и любые другие авторские/агентские трейлеры.
- Перед коммитом проверь сообщение: в нём не должно быть строк, начинающихся
  с `Generated with` или `Co-Authored-By:`.
- Нарушение даёт только **начало строки**: обсуждение самого запрета, цитата
  правила или название файла внутри предложения трейлером не считаются.
<!-- commit-hygiene:end -->
