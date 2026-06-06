---
trigger: always_on
description: Базовый контекст проекта VoiceMind для Cascade
---

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
3. `docs/REMINDER_PARSING.md` — парсинг времени.
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
- `kotlin-compose-style.md` — Kotlin / Compose / Room.
- `architecture-guardrails.md` — запреты и must-have.
- `notification-channels.md` — AlarmManager, каналы, permissions, receivers.
- `parser-rules.md` — `ReminderParser`, confidence, warnings, тесты.
- `datastore-settings.md` — DataStore ключи и default values.
- `ui-theme-rules.md` — CLEAR BELL, токены, экраны.
- `speech-recognition.md` — STT, fallback, timeout.
- `testing-rules.md` — unit / instrumented / manual тесты.
- `openspec-language.md` — язык openspec-артефактов (русский).

## Секреты
- `OPENROUTER_API_KEY` — только fallback-парсер (фаза 5), хранить в `local.properties` → `BuildConfig`.

## Язык
- UI-тексты — **русский**.
- Код — **английский**.
