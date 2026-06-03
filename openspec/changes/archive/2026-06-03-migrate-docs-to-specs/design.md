## Контекст

В `docs/` лежат 9 markdown-файлов (PROJECT_OVERVIEW, REMINDER_PARSING, NOTIFICATION_MODES, DESIGN_SYSTEM, WIDGET_DESIGN, FEATURE_PLAN, ARCHITECTURE, FOLDER_STRUCTURE, README). Часть из них содержат требования, которые должны жить в `openspec/specs/` как BDD-спеки. Часть — продуктовый overview и roadmap, которые остаются в `docs/`.

## Цели / Не-цели

**Цели:**
- Перенести требования из `docs/` в `openspec/specs/` в формате BDD (Given-When-Then)
- Группировать спеки по подсистемам (capability), а не по отдельным фичам
- Оставить `docs/` как human-readable слой для новых разработчиков

**Не-цели:**
- Удалять или изменять `docs/` файлы
- Менять код приложения
- Создавать specs для roadmap / плановых фич (FEATURE_PLAN остаётся в docs)

## Решения

1. **Capability = подсистема**, не фича. `notification-delivery/` покрывает все режимы оповещения, каналы и actions.
2. **Delta-спеки** при будущих changes сливаются в существующие папки.
3. **Структура specs зеркалит архитектуру** (data/parse, data/notification, data/speech, ui/screens).

## Риски

- Дублирование между `docs/` и `openspec/specs/` — принято, это два слоя: human-readable vs machine-readable
- Забыть обновить specs при изменении docs — нужно будет следить вручную или через change-цикл
