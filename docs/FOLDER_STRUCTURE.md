# Структура папок VoiceMind

> 2026-05-17 · Напоминалка (не дневник)

```
VoiceMind/
├── .cursor/
│   ├── plans/
│   └── rules/
│       └── project-context.mdc
├── app/src/
│   ├── main/java/com/example/voicemind/
│   │   ├── data/
│   │   │   ├── parse/           # ReminderParser, ParseResult
│   │   │   ├── speech/          # SpeechInputController (STT)
│   │   │   ├── scheduling/      # AlarmManager, receivers
│   │   │   ├── notification/    # Channels, ReminderNotifier
│   │   │   └── backup/          # фаза 5
│   │   ├── viewmodel/
│   │   └── ui/
│   │       ├── components/      # MicButton, ReminderCard, …
│   │       ├── navigation/
│   │       ├── screens/
│   │       └── theme/
│   ├── main/res/xml/            # backup_rules (фаза 5)
│   ├── test/                    # ReminderParserTest, …
│   └── androidTest/
├── docs/
├── AGENTS.md
└── README.md
```

Устаревшие каталоги от черновика «дневника» (`data/audio`, `data/transcribe`, `service/` для записи аудио) **не используются** — при появлении кода ориентироваться на дерево выше.

Пустые каталоги помечены `.gitkeep`.

См. [ARCHITECTURE.md](ARCHITECTURE.md).
