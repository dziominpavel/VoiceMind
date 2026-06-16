# VoiceMind — голосовая напоминалка

## Продукт

Пользователь говорит фразу с временем → STT → парсер → экран подтверждения → Room + AlarmManager → уведомление/будильник.

**Пример:** «завтра в 9:00 позвонить соседу» → fireAt завтра 09:00, body «Позвонить соседу».

## Референс

- **GymProgress** — только стиль кода: один ViewModel, `safeDb`, Compose, Room, DataStore, overlay-навигация.
- **Не копировать** UI (Volt/Obsidian) и домен (тренировки).

## Стек

- Compose, Material 3, Room, DataStore
- SpeechRecognizer (ru-RU), `ReminderParser` (unit-tested)
- AlarmManager, NotificationCompat, BootReceiver
- Min SDK 26, targetSdk 36

## Критичные правила

- **Confirm перед schedule** — never alarm без явного согласия пользователя (`confirmBeforeSchedule` default true).
- **ReminderScheduler** — единственное место планирования/отмены alarm.
- **После изменения `fireAt`** → cancel + reschedule.
- **MVP без хранения аудио** — только `rawPhrase` текстом.

## Документация (читать при изменениях)

1. `docs/PROJECT_OVERVIEW.md` — продукт и сценарии.
2. `docs/FEATURE_PLAN.md` — фазы разработки.
3. `openspec/specs/reminder-parsing/spec.md` — парсинг времени (формальные требования + справочник паттернов).
4. `docs/ARCHITECTURE.md` — архитектура.
5. `docs/DESIGN_SYSTEM.md` — токены CLEAR BELL (свои, не GymProgress).

## Сортировка

- Предстоящие: `fireAt ASC, id ASC`
- История: `fireAt DESC, id DESC`

## Структура пакетов (быстрый поиск)

- `data/parse/` — `ReminderParser`, `ParseResult`, `ParseWarning`.
- `data/speech/` — `SpeechInputController`, `SpeechRecognition`.
- `data/scheduling/` — `ReminderScheduler`, `ReminderAlarmReceiver`, `BootReceiver`.
- `data/notification/` — `ReminderNotifier`, `NotificationChannels`.
- `data/` — `Reminder.kt`, `ReminderDao.kt`, `ReminderRepository.kt`, `SettingsRepository.kt`.
- `viewmodel/` — `VoiceMindViewModel` + state classes.
- `ui/screens/` — `HomeScreen`, `ConfirmReminderScreen`, `ManualReminderScreen`, `ReminderListScreen`, `SettingsScreen`.
- `ui/theme/` — `Color.kt`, `Theme.kt`, `Dimens.kt` (CLEAR BELL).

## Специализированные rules (читать при работе с модулем)

- `.cursor/rules/kotlin-compose-style.mdc` — Kotlin / Compose / Room.
- `.cursor/rules/architecture-guardrails.mdc` — запреты и must-have.
- `.cursor/rules/notification-channels.mdc` — AlarmManager, каналы, permissions, receivers.
- `.cursor/rules/parser-rules.mdc` — `ReminderParser`, confidence, warnings, тесты.
- `.cursor/rules/datastore-settings.mdc` — DataStore ключи и default values.
- `.cursor/rules/ui-theme-rules.mdc` — CLEAR BELL, токены, экраны.
- `.cursor/rules/speech-recognition.mdc` — STT, fallback, timeout.
- `.cursor/rules/testing-rules.mdc` — unit / instrumented / manual тесты.
- `.cursor/rules/openspec-language.mdc` — язык openspec (русский текст + MUST/SHALL в requirements).
- `.cursor/rules/openspec-specs-structure.mdc` — структура openspec/specs (capability = подсистема).

## Секреты

- `OPENROUTER_API_KEY` — только fallback-парсер (фаза 5), хранить в `local.properties` → `BuildConfig`.

## Язык

- UI-тексты — **русский**.
- Код — **английский**.
