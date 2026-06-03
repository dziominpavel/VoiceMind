---
trigger: glob
description: Структура openspec/specs — capability = подсистема, не фича
globs:
  - "openspec/**/*"
---

# Структура openspec/specs/

**Capability = подсистема/домен, а не отдельная фича.**

## Правила группировки

- **Крупные capability** — одна папка на подсистему (`reminder-parsing/`, `notification-delivery/`, `speech-recognition/`)
- **Не мелкие capability** — не создавать `parse-half-past/`, `parse-weekend/`, `weekday-next/` и т.п.
- **Delta specs** при архивации сливаются в существующую папку подсистемы, а не создают новую

## Текущие capability VoiceMind

```
openspec/specs/
├── reminder-parsing/
│   └── spec.md          ← время, даты, относительные, части дня, выходные
├── speech-recognition/
│   └── spec.md          ← STT, fallback, timeout
├── notification-delivery/
│   └── spec.md          ← режимы, каналы, actions
├── reminder-scheduling/
│   └── spec.md          ← AlarmManager, BootReceiver, reschedule
├── settings/
│   └── spec.md          ← DataStore, defaultMode, confirmBeforeSchedule
└── ui-screens/
    └── spec.md          ← экраны, навигация, оверлеи
```

## Когда создавать новую папку

Только если change вводит **новую подсистему**, которой ещё нет в specs/. Например:
- `widget/` — если появляется домашний виджет
- `import-export/` — если появляется backup/sync

Не создавать новую папку для:
- нового regex в парсере → дописать в `reminder-parsing/spec.md`
- нового экрана → дописать в `ui-screens/spec.md`
- нового канала уведомлений → дописать в `notification-delivery/spec.md`
