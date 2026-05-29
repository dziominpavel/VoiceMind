---
title: Architecture Guardrails
description: Запреты и must-have архитектуры VoiceMind
globs: ["**/*"]
alwaysApply: true
---

# Architecture Guardrails

## Запрещено
- **Несколько ViewModel** на экран — только `VoiceMindViewModel`.
- **Ставить alarm без подтверждения** — всегда preview → confirm → schedule.
- **Alarm scheduling вне ReminderScheduler** — единственная точка входа.
- **Destructive migration** в release-сборке.
- **Хранить аудиозаписи** в MVP — только текст `rawPhrase`.
- **Копировать UI GymProgress** — свой дизайн CLEAR BELL.
- **Копировать домен GymProgress** — никаких тренировок/упражнений.

## Обязательно
- **Парсер unit-tested** — `ReminderParser` покрыт тестами.
- **BootReceiver** — reschedule всех SCHEDULED при `BOOT_COMPLETED` и `MY_PACKAGE_REPLACED`.
- **Exact alarm permission** — проверка + fallback на `setAndAllowWhileIdle`.
- **Snooze** — откладывает на 10 мин, перепланирует через `ReminderScheduler`.

## Модель данных (Reminder)
- `fireAt` — epoch millis, локальная TZ.
- `status` — SCHEDULED, FIRED, DISMISSED, CANCELLED, SNOOZED.
- `deliveryMode` — NOTIFICATION, ALARM, VIBRATE_ONLY, SILENT.
- `alarmRequestCode` — стабильный id для PendingIntent.
