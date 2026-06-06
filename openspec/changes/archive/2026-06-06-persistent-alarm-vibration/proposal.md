## Why

Пользователь не ощущает вибрацию напоминания в режимах `VIBRATE_ONLY` и `ALARM` — система выдаёт лишь один короткий вибро-импульс, который теряется в кармане или на шумной улице. Необходимо сделать вибрацию продолжительной (до 60 секунд или до явной остановки) с отчётливым повторяющимся паттерном, аналогичным системному будильнику.

Кроме того, текущие настройки `useAlarmSound`, `usePushNotification`, `useVibration` работают как приоритетная лестница (ALARM → NOTIFICATION → VIBRATE_ONLY → SILENT), что не позволяет пользователю комбинировать их. Например, нельзя выбрать ALARM с будильником, но без вибрации. Настройки должны стать тремя независимыми переключателями.

## What Changes

- Заменить короткий однократный паттерн вибрации на длительный повторяющийся (alarm-стиль): длинные импульсы 500 мс с паузами 200 мс, цикл до 60 секунд.
- Для `ALARM` и `VIBRATE_ONLY` использовать единый паттерн `longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)` с `repeat = 0` (бесконечный цикл через `VibrationEffect.createWaveform`), но ограничить автоматической остановкой через 60 секунд.
- Убрать вибрацию из `NotificationCompat.Builder.setVibrate()` для `VIBRATE_ONLY` и `ALARM`, чтобы избежать дублирования и конфликта с `AlarmSoundPlayer`/`Vibrator`.
- Обеспечить гарантированную остановку вибрации при любых действиях пользователя: «Готово», «Отложить», «Отменить» на уведомлении, а также при открытии `AlarmActivity` и нажатии кнопки «Стоп».
- Обновить канал `reminders_vibrate_v2`: отключить встроенную вибрацию канала (`enableVibration(false)`), так как управление берёт на себя `AlarmSoundPlayer`.
- Переписать `SettingsRepository.getDefaultDeliveryMode()` для поддержки комбинаций независимых настроек: ALARM может работать со звуком ±вибрацией, NOTIFICATION — с пушем ±вибрацией, VIBRATE_ONLY — всегда вибрация, SILENT — всегда тихо.
- При срабатывании `ALARM` учитывать `useVibration`: если выключено — только звук, без `startVibration()`. При срабатывании `NOTIFICATION` — если `useVibration = false`, не добавлять вибрацию в уведомление.

## Capabilities

### New Capabilities
- `persistent-vibration`: спецификация поведения длительной вибрации при срабатывании напоминания — паттерн, длительность, условия автоматической и ручной остановки.

### Modified Capabilities
- `notification-delivery`: изменение требований к вибрации для режимов `VIBRATE_ONLY` и `ALARM` — отказ от однократного канального паттерна в пользу явного `Vibrator` с повторяющимся рисунком.

## Impact

- `AlarmSoundPlayer.kt` — изменение паттерна, добавление таймера авто-остановки, гарантированный `cancel()` при `stop()`, параметр `withVibration` в `play()`.
- `ReminderNotifier.kt` — удаление `setVibrate()` для `VIBRATE_ONLY` и `ALARM`, оставить только для `NOTIFICATION`; условное `setVibrate()` для NOTIFICATION в зависимости от `useVibration`.
- `NotificationChannels.kt` — отключение `enableVibration()` и `vibrationPattern` для `reminders_vibrate_v2`.
- `ReminderAlarmReceiver.kt` — чтение `useVibration` из `SettingsRepository` и передача флага в `AlarmSoundPlayer.play()`.
- `SettingsRepository.kt` — переписать `getDefaultDeliveryMode()` для поддержки комбинаций независимых настроек.
- `ReminderActionReceiver.kt` / `AlarmActivity` — убедиться, что `AlarmSoundPlayer.stop()` вызывается при любом dismiss/snooze/cancel/открытии экрана.
