---
title: DataStore & AppSettings
description: Настройки приложения через DataStore
globs: ["**/data/SettingsRepository*", "**/SettingsScreen*", "**/viewmodel/**"]
alwaysApply: false
---

# DataStore & AppSettings

## Ключи (Preferences DataStore)
| Ключ | Тип | Default | Описание |
|------|-----|---------|----------|
| `defaultDeliveryMode` | String | `NOTIFICATION` | Режим оповещения по умолчанию |
| `confirmBeforeSchedule` | Boolean | `true` | Всегда показывать Confirm перед alarm |
| `keepRawAudio` | Boolean | `false` | Хранить аудиозапись (MVP — false) |
| `fallbackToSystemSpeech` | Boolean | `false` | Fallback на системный RecognizerIntent |

## Правила
- Все ключи — `stringPreferencesKey` / `booleanPreferencesKey`, константы в `SettingsRepository`.
- `defaultDeliveryMode` хранится как строка (enum name), парсится обратно в `DeliveryMode`.
- UI `SettingsScreen` — radio для режима, toggle для остальных.
- Значения читаются в `VoiceMindViewModel` при старте, передаются в ConfirmScreen как default.

## Запрещено
- Хранить `keepRawAudio = true` в MVP — аудио на диск не пишем.
- Использовать `SharedPreferences` — только DataStore.
