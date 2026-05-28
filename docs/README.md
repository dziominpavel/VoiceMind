# Документация VoiceMind

**VoiceMind** — Android-напоминалка с голосовым вводом: распознавание фразы, извлечение даты/времени и текста, срабатывание в срок выбранным способом (пуш, будильник, вибро и т.д.).

> Дата ревизии: 2026-05-29 · Код реализован: фазы 0–3 (MVP). Фазы 4+ — в плане.

## Файлы

| Файл | Назначение |
|------|------------|
| **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)** | Продукт, сценарии, модель данных, что добавить/убрать |
| **[FEATURE_PLAN.md](FEATURE_PLAN.md)** | **План реализации по фазам** (оценки, критерии готовности) |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Пакеты, ViewModel, scheduler, receivers |
| **[REMINDER_PARSING.md](REMINDER_PARSING.md)** | Правила парсинга русских фраз, тесты |
| **[NOTIFICATION_MODES.md](NOTIFICATION_MODES.md)** | Режимы оповещения и разрешения Android |
| **[DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)** | UI «CLEAR BELL» (не GymProgress) |
| **[FOLDER_STRUCTURE.md](FOLDER_STRUCTURE.md)** | Дерево каталогов |

## Принципы

- Источник истины — **код**; после bootstrap обновлять docs в том же PR.
- **GymProgress** — референс структуры docs и стиля кода, не продуктовых требований.
- Планы сессий в `.cursor/plans/`, roadmap — в `FEATURE_PLAN.md`.

## Корень репозитория

- `README.md` — краткое описание
- `AGENTS.md` — инструкции для AI
