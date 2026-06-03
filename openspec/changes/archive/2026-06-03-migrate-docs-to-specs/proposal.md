## Зачем

Документация VoiceMind сейчас разрознена: продуктовые документы лежат в `docs/`, а спецификации OpenSpec — в `openspec/specs/`. Это создаёт дублирование и риск рассинхронизации. Нужно перенести актуальные требования из `docs/` в `openspec/specs/`, превратив их в BDD-спеки, а `docs/` оставить как human-readable overview для новых разработчиков.

## Что изменится

1. Создать `openspec/specs/notification-delivery/spec.md` из `docs/NOTIFICATION_MODES.md`
2. Создать `openspec/specs/ui-screens/spec.md` из `docs/DESIGN_SYSTEM.md`
3. Создать `openspec/specs/widget/spec.md` из `docs/WIDGET_DESIGN.md`
4. Создать `openspec/specs/speech-recognition/spec.md` из `docs/REMINDER_PARSING.md` (pipeline STT)
5. Обновить `openspec/specs/reminder-parsing/spec.md` — дополнить из `docs/REMINDER_PARSING.md` недостающими контрактами и edge-cases

## Capability

### Новые capability
- `notification-delivery`: режимы оповещения, каналы уведомлений, действия на уведомлении
- `ui-screens`: дизайн-система CLEAR BELL, экраны, навигация
- `widget`: домашний виджет ReminderListWidget, RemoteViews, клики
- `speech-recognition`: STT pipeline, fallback, timeout

### Изменённые capability
- `reminder-parsing`: дополнить контракт `ParseResult`, warnings, candidate-based engine

## Влияние

- Новые spec-файлы в `openspec/specs/`
- `docs/` — без изменений (оставляем как overview)
- Ни один файл кода приложения не меняется
