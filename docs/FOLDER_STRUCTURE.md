# Структура папок VoiceMind

> 2026-05-29 · Напоминалка (MVP фазы 0–3)

```
VoiceMind/
├── .cursor/
│   └── rules/
│       └── project-context.mdc
├── app/src/
│   ├── main/java/com/example/voicemind/
│   │   ├── data/
│   │   │   ├── parse/           # ReminderParser, ParseResult, ParseWarning, ParseResultExtensions
│   │   │   ├── speech/          # SpeechInputController, SpeechRecognition
│   │   │   ├── scheduling/      # ReminderScheduler, ReminderIntents, receivers
│   │   │   └── notification/    # Channels, ReminderNotifier
│   │   ├── viewmodel/           # VoiceMindViewModel + state classes
│   │   ├── ui/
│   │   │   ├── components/      # DeliveryModePicker
│   │   │   ├── navigation/      # AppDestination
│   │   │   ├── screens/         # Home, Confirm, Manual, List, Detail, Settings
│   │   │   └── theme/           # Color, Theme, Dimens
│   │   ├── util/                # ReminderPermissions
│   │   ├── MainActivity.kt
│   │   ├── VoiceMindApplication.kt
│   │   └── AppDestinations.kt
│   ├── test/                    # ReminderParserTest (38+)
│   └── androidTest/
├── docs/
├── AGENTS.md
└── README.md
```

См. [ARCHITECTURE.md](ARCHITECTURE.md).
