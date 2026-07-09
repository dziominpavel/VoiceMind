## Context

Бенчмарк-док `docs/voicemind-bugs-from-benchmark.md` содержит ~45 V-находок по scheduling. После code-ревью актуальны ~10–12; остальное — OEM-noise, дубликаты, теория коллизий. Change закрывает только подтверждённые CRITICAL/HIGH (+ минимальный MEDIUM для deliveryMode/snooze), чтобы док можно было удалить.

Текущие факты в коде:
- `BootReceiver` `exported="false"` + `BOOT_COMPLETED` filter.
- `ReminderAlarmReceiver` / `ReminderNotifier` читают `settings.getDefaultDeliveryMode()`.
- `schedule()` без предварительного `cancel`; `cancel()` зовёт `pendingIntent.cancel()`.
- `startActivity(AlarmActivity)` всегда sync до status/mode check.
- `snoozeReminder` без status guard; `updateAndSchedule` = update→cancel→schedule.
- `AlarmSoundPlayer` — object со static ringtone.

## Goals / Non-Goals

**Goals:**
- Reminder переживает reboot (BootReceiver получает broadcast).
- Fire/notify уважают per-reminder `deliveryMode`.
- Нет лишнего flash AlarmActivity для NOTIFICATION/SILENT / уже отменённых.
- Scheduler idempotent; нет уничтожения PI через `pendingIntent.cancel()`.
- Snooze/fire не воскрешают CANCELLED/DONE.
- Удалить бенчмарк-док.

**Non-Goals:**
- OEM quick boot / LOCKED_BOOT_COMPLETED / battery optimization UI.
- Exact-alarm permission banner / setAlarmClock.
- requestCode 2³¹ collisions.
- SupervisorJob refactor всех receivers.
- Лимит шторма `fireOverdue`.
- Import/export FR helpers.
- Полный lifecycle-рефактор AlarmSoundPlayer (только stop-on-error / clear state).

## Decisions

### D1. BootReceiver `exported="true"`
Системный implicit `BOOT_COMPLETED` требует exported receiver. Альтернатива (JobScheduler-only) — out of scope.

### D2. Единый resolve deliveryMode
```kotlin
fun Reminder.resolvedDeliveryMode(fallback: DeliveryMode): DeliveryMode =
    runCatching { DeliveryMode.valueOf(deliveryMode) }.getOrDefault(fallback)
```
Использовать в receiver (звук/wake) и notifier (канал/FSI). Settings default — только fallback.

### D3. startActivity только для ALARM/VIBRATE (sync, без DB)
При `schedule` класть `EXTRA_DELIVERY_MODE` в alarm PendingIntent. В `onReceive` синхронно (до `goAsync`, как требует alarm-screen-wake): если mode ∈ {ALARM, VIBRATE} → `startActivity`; иначе не запускать. Звук/канал после load — из `reminder.deliveryMode`. Редкий race «отменили за секунду до fire» → Activity сама `finish()` при non-PENDING.

Это чинит flash для NOTIFICATION/SILENT и сохраняет sync-path для immersive.

### D4. Scheduler: cancel-then-set; no PI.cancel()
`schedule`: `alarmManager.cancel(pi)` затем setExact…  
`cancel`: только `alarmManager.cancel(pi)` — без `pendingIntent.cancel()`.

### D5. Status guards
`snoozeReminder`: early return если status ∉ {PENDING, TRIGGERED} (или только PENDING — выбрать PENDING+TRIGGERED, т.к. snooze с notification после fire).  
Практично: разрешить snooze только если status == PENDING || TRIGGERED; CANCELLED/DONE — no-op.  
`markFired` уже guard PENDING — оставить.

### D6. updateAndSchedule order
`cancel` → `dao.update` → `schedule` если PENDING и future.

### D7. AlarmSoundPlayer
В `play`/`stop`: try/finally сброс ringtone/handler; `stop` идемпотентен. Без Application.onTerminate.

### D8. Удаление доки
После зелёных правок — delete `docs/voicemind-bugs-from-benchmark.md` (task в конце).

## Risks / Trade-offs

- [Риск] Mode в PendingIntent extras устареет, если deliveryMode сменили без reschedule → Mitigation: при syncAllDeliveryModes / updateAndSchedule всегда пересоздавать alarm через schedule (уже cancel+schedule).
- [Риск] exported BootReceiver — теоретический attack surface → Mitigation: стандарт для BOOT_COMPLETED; filter только boot actions.
- [Риск] snooze с TRIGGERED vs только PENDING → Mitigation: разрешить оба (после fire notification ещё висит).

## Migration Plan

Только код + манифест. Откат — revert. Док удаляется в том же change.

## Open Questions

*(none — scope зафиксирован упрощением)*
