---
title: Testing Rules
description: Правила для unit, instrumented и manual тестов
globs: ["**/test/**", "**/androidTest/**", "**/*Test.kt"]
alwaysApply: false
---

# Testing

## Unit-тесты
- **ReminderParser** — приоритет №1. Замороженный `Instant now` для детерминизма.
- **ReminderScheduler** — fake `AlarmManager` (robolectric опционально).
- Новый паттерн парсера → сначала **test-case**, потом реализация.

## Instrumented
- DAO: insert / query / `ORDER BY` проверки.
- Receivers: `BootReceiver` + reschedule после reboot.
- Migration: установка поверх предыдущей версии БД.

## Manual
- Reboot + reschedule.
- Snooze (10 мин).
- Exact alarm permission denied (Android 12+).
- Doze / battery optimization.

## Стиль
- Именование: `methodName_condition_expected()`.
- Парсер: 38+ кейсов, edge (полночь, полдень, «через полчаса»).
- Не использовать `Thread.sleep()` в unit-тестах.

## Запуск
- `./gradlew test` — unit.
- `./gradlew connectedAndroidTest` — instrumented (эмулятор/девайс).
