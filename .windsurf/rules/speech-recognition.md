---
title: Speech Recognition (STT)
description: Правила для SpeechRecognizer и fallback
globs: ["**/data/speech/**", "**/ui/screens/HomeScreen*"]
alwaysApply: false
---

# Speech Recognition (STT)

## Архитектура
- `SpeechInputController` — обёртка над `SpeechRecognizer` (on-device, locale `ru-RU`).
- `SpeechRecognition` (object) — утилиты / константы.
- Fallback: `RecognizerIntent` (системный диалог) если on-device не работает (OEM-friendly).

## Поведение
- Timeout: **10 сек**.
- Результат: одна строка `rawPhrase`.
- Ошибки: no network для offline-движка не должны ломать STT (on-device).
- Аудио на диск **не пишем** в MVP.

## ViewModel
- `listeningState`: Idle / Listening / Processing.
- `startListening()` → `SpeechInputController` → `onSpeechResult(rawPhrase)` → `ReminderParser.parse()`.
- После парсинга — `pendingConfirm` state → overlay `ConfirmReminderScreen`.

## Permissions
- `RECORD_AUDIO` — запрашивать при первом нажатии на микрофон.
- Объяснение: «Нужен доступ к микрофону для голосовых напоминаний».

## Fallback
- `fallbackToSystemSpeech` (DataStore, default false) — если true, сразу использовать `RecognizerIntent`.
