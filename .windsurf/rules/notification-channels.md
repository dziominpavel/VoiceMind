---
title: Notifications & Alarms
description: Каналы, AlarmManager, permissions, receivers для напоминаний
globs: ["**/data/scheduling/**", "**/data/notification/**", "**/util/**", "**/AndroidManifest.xml"]
alwaysApply: true
---

# Notifications & Alarms

## AlarmManager
- **Единственная точка:** `ReminderScheduler.schedule()`, `cancel()`, `rescheduleAll()`.
- Основной метод: `setExactAndAllowWhileIdle(RTC_WAKEUP, fireAt, pendingIntent)`.
- Fallback: `setAndAllowWhileIdle` если нет `SCHEDULE_EXACT_ALARM`.
- `alarmRequestCode` — стабильный hash от `reminder.id` (или id напрямую), не `Random`.

## Permissions (AndroidManifest + runtime)
| Permission | Когда запрашивать |
|------------|-------------------|
| `POST_NOTIFICATIONS` | API 33+, до первого напоминания |
| `SCHEDULE_EXACT_ALARM` | API 31+, проверка `canScheduleExactAlarms()` |
| `USE_FULL_SCREEN_INTENT` | API 34+ для ALARM / LOUD_ALARM |
| `VIBRATE` | manifest |
| `RECEIVE_BOOT_COMPLETED` | manifest, BootReceiver reschedule |
| `RECORD_AUDIO` | при первом использовании микрофона |

## Notification Channels (создать при первом запуске)
| Channel ID | Режим | Importance |
|------------|-------|------------|
| `reminders_default` | NOTIFICATION | HIGH |
| `reminders_alarm` | ALARM, LOUD_ALARM | HIGH + bypass DND |
| `reminders_silent` | SILENT | LOW |
| `reminders_vibrate` | VIBRATE_ONLY | DEFAULT, no sound |

- Для ALARM — высокий приоритет + vibration pattern; `fullScreenIntent` только фаза 4.
- Действия на уведомлении: Готово (DISMISSED), Отложить 10 мин (SNOOZED→SCHEDULED), Отменить (CANCELLED).
- `ReminderActionReceiver` обрабатывает actions.

## Receivers
- `BootReceiver` — `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` → `ReminderScheduler.rescheduleAll()`.
- `ReminderAlarmReceiver` — триггер от AlarmManager → `ReminderNotifier.show()`.

## Надёжность
- Doze: `setExactAndAllowWhileIdle`.
- Kill app: alarm в системе, не в процессе.
- OEM: экран настроек «Не оптимизировать батарею» — фаза 4.

## Запрещено
- Ставить alarm без ConfirmScreen.
- Alarm scheduling вне `ReminderScheduler`.
- `NotificationManager.notify()` напрямую из ViewModel/UI — только через `ReminderNotifier`.
