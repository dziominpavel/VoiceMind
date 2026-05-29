---
title: Kotlin / Compose Style
description: Стиль кода VoiceMind (Kotlin, Compose, Coroutines)
globs: ["**/*.kt"]
alwaysApply: false
---

# Kotlin / Compose — стиль VoiceMind

## Корутины и Flow
- ViewModel: `viewModelScope`, **один** `VoiceMindViewModel`.
- UI-state: `StateFlow` + `data class` экранов.
- Не использовать `LiveData`.

## Compose
- `@Composable` функции — `PascalCase`, параметры называть семантически.
- Тема: `VoiceMindTheme`, не палитра GymProgress (Volt/Obsidian).
- Preview-функции рядом с composable.

## Room
- Миграции — **не destructive** в release.
- `@Dao` интерфейсы чистые, логика во ViewModel/UseCase.
- `safeDb { dao.operation() }` — обёртка для обработки ошибок.

## Ошибки
- DB/IO — `safeDb` + `Snackbar` в UI.
- Не пробрасывать `Exception` наверх без обработки.

## AlarmManager
- `setExactAndAllowWhileIdle` — основной метод.
- Проверять `canScheduleExactAlarms()` (Android 12+).
- После reboot — перепланировать все SCHEDULED.

## Тексты
- Строковые ресурсы — `strings.xml` (русский).
- Логи release — **не** печатать `body` и `rawPhrase`.
