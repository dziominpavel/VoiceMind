## 1. Подготовка каналов и уведомлений

- [x] 1.1 В `NotificationChannels.kt` отключить `enableVibration(false)` и убрать `vibrationPattern` для `reminders_vibrate_v2`.
- [x] 1.2 В `ReminderNotifier.kt` убрать `builder.setVibrate(...)` для `ALARM` и `VIBRATE_ONLY` (оставить только для `NOTIFICATION` при `useVibration = true`).
- [x] 1.3 Убедиться, что для `ALARM` в `ReminderNotifier` установлен `setSilent(true)` и `setDefaults(0)` (уже есть, проверить, что `setVibrate` не добавляется).
- [x] 1.4 В `ReminderNotifier.kt` для `NOTIFICATION` сделать `setVibrate(null)` при `useVibration = false`.

## 2. Обновление AlarmSoundPlayer

- [x] 2.1 Заменить `VIBRATE_PATTERN` на `longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)`.
- [x] 2.2 Убедиться, что `startVibration()` использует `repeat = 0` (бесконечный цикл) в `VibrationEffect.createWaveform(pattern, 0)`.
- [x] 2.3 Проверить, что `stop()` вызывает `vibrator?.cancel()` до и после остальных операций.
- [x] 2.4 Убедиться, что `scheduleAutoStop()` по-прежнему ограничивает вибрацию 60 секундами.
- [x] 2.5 Добавить параметр `withVibration: Boolean = true` в `AlarmSoundPlayer.play()`. При `false` не вызывать `startVibration()`.

## 3. Независимые настройки доставки

- [x] 3.1 Переписать `SettingsRepository.getDefaultDeliveryMode()`: ALARM если `useAlarmSound`, иначе NOTIFICATION если `usePushNotification`, иначе VIBRATE_ONLY если `useVibration`, иначе SILENT.
- [x] 3.2 В `ReminderAlarmReceiver.kt` при `ALARM` читать `settings.useVibration.first()` и передавать в `AlarmSoundPlayer.play(..., withVibration)`.
- [x] 3.3 В `ReminderNotifier.kt` при `NOTIFICATION` читать `settings.useVibration.first()` и условно добавлять/убирать `setVibrate()`.
- [x] 3.4 Проверить, что `ConfirmReminderScreen` / `DeliveryModeGrid` не требует изменений (режимы остаются теми же 4, настройки влияют только на default и runtime behavior).

## 4. Гарантированная остановка вибрации

- [x] 4.1 В `ReminderActionReceiver.kt` проверить, что `AlarmSoundPlayer.stop(context)` вызывается при обработке `ACTION_DONE`, `ACTION_DISMISS`, `ACTION_CANCEL` и `ACTION_SNOOZE`.
- [x] 4.2 В `AlarmActivity.kt` проверить, что `AlarmSoundPlayer.stop(this)` вызывается в `onCreate` (при открытии) и/или при нажатии кнопки «Стоп».
- [x] 4.3 При необходимости добавить недостающие вызовы `AlarmSoundPlayer.stop()` в `ReminderActionReceiver` или `AlarmActivity`.

## 5. Тестирование и верификация

- [ ] 5.1 Ручной тест: напоминание `VIBRATE_ONLY` — вибрация продолжается ~60 секунд с отчётливым паттерном.
- [ ] 5.2 Ручной тест: напоминание `ALARM` с `useVibration=true` — звук + вибрация.
- [ ] 5.3 Ручной тест: напоминание `ALARM` с `useVibration=false` — только звук, вибрации нет.
- [ ] 5.4 Ручной тест: действия «Готово», «Отложить», «Отменить» на уведомлении немедленно останавливают вибрацию.
- [ ] 5.5 Ручной тест: смахивание уведомления останавливает вибрацию.
- [ ] 5.6 Ручной тест: режим `NOTIFICATION` с `useVibration=true` — короткая вибрация канала.
- [ ] 5.7 Ручной тест: режим `NOTIFICATION` с `useVibration=false` — пуш без вибрации.
