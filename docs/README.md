# Документация VoiceMind

**VoiceMind** — Android-напоминалка с голосовым вводом: распознавание фразы, извлечение даты/времени и текста, срабатывание в срок выбранным способом (пуш, будильник, вибро и т.д.).

> Дата ревизии: 2026-05-29 · Код реализован: фазы 0–3 (MVP). Фазы 4+ — в плане.

## Файлы

| Файл | Назначение |
|------|------------|
| **[PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md)** | Продукт, сценарии, модель данных, что добавить/убрать |
| **[FEATURE_PLAN.md](FEATURE_PLAN.md)** | **План реализации по фазам** (оценки, критерии готовности) |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | Пакеты, ViewModel, scheduler, receivers |
| **[FOLDER_STRUCTURE.md](FOLDER_STRUCTURE.md)** | Дерево каталогов |

> Режимы оповещения, правила парсинга и требования к UI больше **не** ведутся отдельными md-файлами — они перенесены в формальные спецификации `../openspec/specs/` (см. ниже).

**Формальные спецификации (BDD):** `../openspec/specs/`
- `reminder-parsing/spec.md` — правила парсинга русских фраз, тесты
- `ui-screens/spec.md` — требования к экранам
- `speech-recognition/spec.md` — распознавание речи
- `notification-delivery/spec.md` — уведомления и будильник
- `alarm-screen-wake/spec.md` — экран пробуждения
- `widget/spec.md` — виджет

## Принципы

- Источник истины — **код**; после bootstrap обновлять docs в том же PR.
- **GymProgress** — референс структуры docs и стиля кода, не продуктовых требований.
- Roadmap — в `FEATURE_PLAN.md`.
- Планы сессий в Cursor — `.cursor/plans/`; в Windsurf — `.windsurf/workflows/`.

## Корень репозитория

- `README.md` — краткое описание
- `AGENTS.md` — инструкции для AI
