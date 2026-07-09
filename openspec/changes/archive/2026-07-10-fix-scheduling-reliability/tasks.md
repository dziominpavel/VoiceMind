## 1. Boot + deliveryMode

- [x] 1.1 `AndroidManifest`: `BootReceiver` → `android:exported="true"`
- [x] 1.2 Хелпер `Reminder.resolvedDeliveryMode(fallback)` (или локальная функция)
- [x] 1.3 `ReminderIntents.alarmIntent`: extras `EXTRA_DELIVERY_MODE` из reminder
- [x] 1.4 `ReminderAlarmReceiver`: sync `startActivity` только если mode ∈ {ALARM, VIBRATE}; звук/wake из `reminder.resolvedDeliveryMode`
- [x] 1.5 `ReminderNotifier.show`: канал/FSI/вибрация из `reminder.resolvedDeliveryMode`, не из settings default

## 2. Scheduler + repository

- [x] 2.1 `ReminderScheduler.schedule`: `alarmManager.cancel(pi)` перед setExact/setAndAllowWhileIdle; единый try/catch на fallback
- [x] 2.2 `ReminderScheduler.cancel`: убрать `pendingIntent.cancel()`
- [x] 2.3 `ReminderRepository.updateAndSchedule`: порядок `cancel` → `dao.update` → `schedule`
- [x] 2.4 `snoozeReminder`: no-op если status не PENDING и не TRIGGERED

## 3. AlarmSoundPlayer

- [x] 3.1 Идемпотентный `stop()` + сброс ringtone/handler/volume в finally при ошибках `play`

## 4. Проверка и cleanup

- [x] 4.1 Прогнать релевантные unit-тесты (если есть) / ручной чек-лист: reboot, NOTIFICATION vs ALARM, snooze на CANCELLED
  — compileDebugKotlin + unitTestKotlin SUCCESS; reboot/modes — ручной чек на устройстве
- [x] 4.2 `openspec validate --all`
- [x] 4.3 Удалить `docs/voicemind-bugs-from-benchmark.md`
