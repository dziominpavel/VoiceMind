## Why

После фикса парсера в `docs/voicemind-bugs-from-benchmark.md` остался бэклог надёжности alarms/scheduling (V-001). Критичные дыры подтверждены по текущему коду: `BootReceiver` с `exported="false"` может не получить reboot; fire/notify игнорируют per-reminder `deliveryMode`; scheduler/receiver имеют гонки и лишний flash AlarmActivity. Цель — закрыть подтверждённые баги минимальным набором правок и удалить бенчмарк-доку.

## What Changes

- `BootReceiver`: `android:exported="true"` для `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`.
- Fire-path: режим доставки брать из `reminder.deliveryMode` (с fallback на settings) в `ReminderAlarmReceiver` и `ReminderNotifier`.
- `ReminderScheduler`: перед `schedule` — `cancel`; убрать `pendingIntent.cancel()`; fallback `setAndAllowWhileIdle` в едином try/catch.
- `ReminderAlarmReceiver`: `startActivity(AlarmActivity)` только для ALARM/VIBRATE и только если reminder ещё `PENDING` (после быстрой загрузки id/status/mode; sync path сохранить где возможно).
- Atomic status: snooze/dismiss/fire не воскрешают CANCELLED/DONE; guard в `snoozeReminder` / markFired.
- `updateAndSchedule`: порядок `cancel → update → schedule`.
- `AlarmSoundPlayer`: безопасный `stop()` / сброс static state при ошибках (минимальный фикс утечки).
- Удалить `docs/voicemind-bugs-from-benchmark.md` после закрытия scope.

**Вне scope (упрощение):** OEM quick-boot, Direct Boot, requestCode-коллизии 2³¹, SupervisorJob-рефактор receivers, UX-баннеры exact-alarm/battery, шторм `fireOverdue` лимиты, FR/RR import helpers, confidence/LOW noise.

## Capabilities

### New Capabilities
*(none)*

### Modified Capabilities
- `notification-delivery`: fire и notification MUST использовать `reminder.deliveryMode`; snooze/dismiss MUST NOT воскрешать не-PENDING.
- `alarm-screen-wake`: AlarmActivity MUST запускаться только для ALARM/VIBRATE при валидном PENDING reminder.

## Impact

- `AndroidManifest.xml` — BootReceiver exported.
- `ReminderScheduler.kt`, `ReminderAlarmReceiver.kt`, `ReminderNotifier.kt`, `ReminderRepository.kt`, `AlarmSoundPlayer.kt`.
- Unit/instrumented тесты по возможности; ручная проверка reboot + delivery modes.
- Удаление `docs/voicemind-bugs-from-benchmark.md`.
