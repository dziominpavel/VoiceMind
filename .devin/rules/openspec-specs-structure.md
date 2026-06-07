# Структура openspec/specs (Devin)

> Зеркало `.cursor/rules/openspec-specs-structure.mdc`.

**Capability = подсистема**, не отдельная фича. Формат MUST/SHALL — см. `openspec-language.md`.

## Capability VoiceMind

- `reminder-parsing/` — парсинг дат и времени
- `speech-recognition/` — STT
- `notification-delivery/` — режимы оповещения, каналы, actions
- `ui-screens/` — экраны и UI
- `alarm-screen-wake/` — пробуждение экрана ALARM
- `persistent-vibration/` — вибрация
- `widget/` — домашний виджет

## Правила

- Новый regex/экран/канал → дописать в существующую capability, не создавать новую папку под фичу
- При архивации change: delta → merge в main spec (`## Purpose` + `## Requirements`)
- После merge: `openspec validate --all`
