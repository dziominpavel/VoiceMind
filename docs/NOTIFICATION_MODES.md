# Режимы оповещения

> Дата: 2026-05-29

Каждое напоминание хранит `deliveryMode`. Если не задано при создании — берётся `defaultDeliveryMode` из DataStore.

## Enum DeliveryMode

| Значение | UI (ru) | Поведение |
|----------|---------|-----------|
| `NOTIFICATION` | Уведомление | Канал с звуком, heads-up, текст в шторке |
| `ALARM` | Как будильник | Отдельный канал, высокий приоритет, длинная вибрация; `fullScreenIntent` — фаза 4 |
| `VIBRATE_ONLY` | Только вибрация | `setSound(null)`, паттерн вибрации |
| `SILENT` | Тихое | Только иконка и текст, без звука и вибрации |
| `LOUD_ALARM` | (фаза 4) | Отдельная `AlarmActivity` поверх lock screen, keep screen on |

В настройках — **radio / dropdown** с кратким описанием каждого режима.

---

## Реализация (Android)

### Планирование

- `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, fireAt, pendingIntent)`
- `PendingIntent` → `ReminderAlarmReceiver` → показ уведомления / запуск Activity
- `alarmRequestCode` = стабильный hash от `reminder.id` (или id напрямую)

### Notification channels (создать при первом запуске)

| Channel ID | Режим | Importance |
|------------|-------|------------|
| `reminders_default` | NOTIFICATION | HIGH |
| `reminders_alarm` | ALARM, LOUD_ALARM | HIGH + bypass DND (запросить разрешение) |
| `reminders_silent` | SILENT | LOW |
| `reminders_vibrate` | VIBRATE_ONLY | DEFAULT, no sound |

### Действия на уведомлении

| Action | Эффект |
|--------|--------|
| Готово | `status = DISMISSED`, отменить alarm |
| Отложить 10 мин | `fireAt = now + 10 мин`, `status = SNOOZED` → `SCHEDULED`, перепланировать |
| Отменить | `status = CANCELLED` |

`BroadcastReceiver` для action — `ReminderActionReceiver`.

---

## Разрешения

| Permission | Когда |
|------------|--------|
| `POST_NOTIFICATIONS` | API 33+, до первого напоминания |
| `SCHEDULE_EXACT_ALARM` | API 31+, проверка `canScheduleExactAlarms()` |
| `USE_FULL_SCREEN_INTENT` | API 34+ для ALARM / LOUD_ALARM |
| `VIBRATE` | manifest |
| `RECEIVE_BOOT_COMPLETED` | перепланирование |
| `RECORD_AUDIO` | только для STT |

---

## Тихие часы (фаза 4)

Если `fireAt` попадает в интервал quiet hours:

- принудительно показать как `SILENT` или `VIBRATE_ONLY` (настройка);
- на ConfirmScreen при создании — предупреждение: «Сработает тихо из‑за тихих часов».

> Не реализовано в MVP: нет полей `quietHoursStart` / `End` в DataStore.

---

## Надёжность

| Риск | Митигация |
|------|-----------|
| Doze | `setExactAndAllowWhileIdle` |
| Reboot | `BootReceiver` → `ReminderScheduler.rescheduleAll()` |
| Kill app | alarm в системе, не в процессе |
| OEM агрессивный | экран в настройках «Не оптимизировать батарею» (опционально) |

---

## Связанные документы

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [FEATURE_PLAN.md](FEATURE_PLAN.md) — фазы 2–4
