## Why

Сейчас при срабатывании будильника (deliveryMode ALARM) с выключенным экраном пользователь видит только уведомление в шторке после разблокировки. Это не соответствует поведению стандартного приложения «Будильник» на Android: экран должен включаться, всплывать поверх экрана блокировки и показывать текст напоминания крупно, с кнопками действий. Без этого режим ALARM теряет смысл для пробуждения пользователя.

## What Changes

- Добавить `USE_FULL_SCREEN_INTENT` в AndroidManifest (permission + activity flag).
- Добавить `WAKE_LOCK` permission и логику пробуждения экрана в `ReminderAlarmReceiver`.
- Создать отдельный `AlarmActivity` (прозрачный/overlay-стиль) для отображения поверх экрана блокировки с текстом напоминания и кнопками «Готово», «Отложить 10 мин», «Отменить».
- Настроить `fullScreenIntent` в `NotificationCompat.Builder` для канала `reminders_alarm`.
- Обновить `ReminderNotifier`: для ALARM устанавливать `fullScreenIntent` на запуск `AlarmActivity`.
- Обновить сценарий ALARM в `notification-delivery` spec: теперь это обязательное full-screen поведение, а не опциональное.
- Сохранить поведение для других режимов (NOTIFICATION, VIBRATE_ONLY, SILENT) без изменений.

## Capabilities

### New Capabilities
- `alarm-screen-wake`: Пробуждение экрана и показ полноэкранного UI при срабатывании ALARM с выключенным экраном или на экране блокировки.

### Modified Capabilities
- `notification-delivery`: Сценарий «Будильник-напоминание» теперь требует обязательного full-screen intent и пробуждения экрана вместо «full-screen intent (фаза 4) или высокоприоритетное уведомление».

## Impact

- `AndroidManifest.xml` — новые permissions и activity declaration.
- `ReminderAlarmReceiver.kt` — добавить WakeLock / PowerManager для включения экрана.
- `ReminderNotifier.kt` — добавить `setFullScreenIntent` для ALARM.
- `NotificationChannels.kt` — убедиться, что канал `reminders_alarm` поддерживает fullScreenIntent (bypass DND).
- Новый файл: `ui/screens/AlarmActivity.kt` (или `ui/alarm/AlarmActivity.kt`) — Compose UI для полноэкранного отображения.
- `VoiceMindViewModel` — новые методы для обработки действий из `AlarmActivity` (done/snooze/cancel).
- `strings.xml` — новые строки для UI будильника.
