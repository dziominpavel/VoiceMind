## Context

В текущей реализации вибрация для режимов `ALARM` и `VIBRATE_ONLY` запускается двумя путями одновременно:
1. `ReminderNotifier` выставляет `setVibrate(DEFAULT_VIBRATE_PATTERN)` на `NotificationCompat.Builder` — это даёт один короткий цикл `300-150-300`.
2. `AlarmSoundPlayer.playVibrationOnly()` запускает `Vibrator` с паттерном `500-200-500`, но только один цикл (repeat=0 на `createWaveform` означает **бесконечный** цикл, однако на некоторых OEM-устройствах система обрезает длительность вибрации при блокировке экрана или Doze).

В результате пользователь ощущает либо одиночный короткий толчок, либо вибрацию, которая быстро прерывается. Необходимо единое управление вибрацией через `AlarmSoundPlayer`, гарантированную длительность и отключение дублирующей вибрации из уведомления.

## Goals / Non-Goals

**Goals:**
- Вибрация в режимах `ALARM` и `VIBRATE_ONLY` должна быть ощутимой на протяжении до 60 секунд.
- Использовать отчётливый повторяющийся паттерн (alarm-стиль).
- Избежать дублирования вибрации от уведомления и от `Vibrator`.
- Гарантировать остановку вибрации при любом пользовательском действии.

**Non-Goals:**
- Не добавляем пользовательские настройки паттерна вибрации (MVP).
- Не меняем звуковое поведение `ALARM` (ringtone, volume).

## Decisions

### 1. Единый источник вибрации — `AlarmSoundPlayer`
**Решение**: `ReminderNotifier` полностью убирает `setVibrate()` для `ALARM` и `VIBRATE_ONLY`. Вибрация идёт только через `AlarmSoundPlayer.startVibration()`.

**Рационал**: Два источника вибрации конкурируют и создают непредсказуемое поведение на разных OEM. Уведомление не может гарантировать длительную вибрацию; `Vibrator` API даёт полный контроль.

**Альтернатива**: Оставить `setVibrate()` и убрать `AlarmSoundPlayer`. Отклонено, потому что `NotificationCompat.Builder.setVibrate()` обрезается системой и не поддерживает бесконечный цикл надёжно.

### 2. Паттерн вибрации: alarm-стиль с repeat
**Решение**: Использовать `longArrayOf(0, 500, 200, 500, 200, 500, 200, 500)` с `repeat = 0` (бесконечный цикл).

**Рационал**: Этот паттерн близок к стандартному системному будильнику Android. `repeat = 0` в `createWaveform` означает повтор с индекса 0 (с начала массива), что даёт бесконечный цикл. Таймер `Handler.postDelayed(AUTO_STOP_MS)` обеспечит прерывание через 60 секунд.

**Альтернатива**: Использовать `VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)` серией. Отклонено, так как `createPredefined` не поддерживает длительное повторение.

### 3. Авто-остановка через 60 секунд
**Решение**: `AlarmSoundPlayer` уже содержит `scheduleAutoStop(context)` с таймаутом 60 секунд. Достаточно убедиться, что `stop()` вызывает `vibrator.cancel()`.

**Рационал**: Необходимо защитить батарею и избежать вечной вибрации, если пользователь не реагирует.

### 4. Гарантированный `stop()` при действиях
**Решение**: Все пути dismiss/snooze/cancel/открытие экрана уже ведут к `AlarmSoundPlayer.stop()`. Нужно проверить:
- `ReminderActionReceiver` вызывает `stop()` при обработке `ACTION_DONE`, `ACTION_SNOOZE`, `ACTION_CANCEL`, `ACTION_DISMISS`.
- `AlarmActivity.onCreate/onStop` вызывает `stop()`.

**Рационал**: Вибрация — глобальное состояние (singleton `AlarmSoundPlayer`), поэтому любое взаимодействие пользователя должно сбрасывать её.

### 5. Отключение вибрации в канале `reminders_vibrate_v2`
**Решение**: В `NotificationChannels.kt` для `vibrateChannel` установить `enableVibration(false)` и убрать `vibrationPattern`.

**Рационал**: Если канал сам вибрирует, уведомление будет вибрировать даже при `setVibrate(null)`. На API 26+ канал определяет поведение; `NotificationCompat` не может его переопределить.

### 6. Независимые настройки доставки
**Решение**: Три toggle (`useAlarmSound`, `usePushNotification`, `useVibration`) становятся независимыми. `getDefaultDeliveryMode()` выбирает режим по следующей логике:
- `useAlarmSound = true` → `ALARM` (независимо от остальных; вибрация внутри ALARM контролируется `useVibration`).
- `useAlarmSound = false, usePushNotification = true` → `NOTIFICATION` (вибрация внутри NOTIFICATION контролируется `useVibration`).
- `useAlarmSound = false, usePushNotification = false, useVibration = true` → `VIBRATE_ONLY`.
- Все false → `SILENT`.

При срабатывании:
- `ALARM`: `AlarmSoundPlayer.play(..., withVibration = useVibration)`.
- `NOTIFICATION`: если `useVibration = true` — `setVibrate(DEFAULT_VIBRATE_PATTERN)`; если `false` — `setVibrate(null)`.
- `VIBRATE_ONLY`: всегда `AlarmSoundPlayer.playVibrationOnly()` (игнорирует `useVibration`, потому что это explicit выбор режима).

**Рационал**: Пользователь хочет комбинировать. Например, ALARM без вибрации (только звук будильника) или NOTIFICATION с вибрацией. Настройка `useVibration` становится модификатором для ALARM/NOTIFICATION, а не определяющим фактором выбора режима.

**Альтернатива**: Создать 8 отдельных DeliveryMode для всех комбинаций. Отклонено — enum раздуется, UI усложняется, каналы размножаются.

## Risks / Trade-offs

- **[Risk]** Некоторые OEM (Samsung, Xiaomi) игнорируют длительные `Vibrator` вызовы в фоне или при Doze.
  → **Mitigation**: `setExactAndAllowWhileIdle` + `WakeLock` на 10 секунд в `ReminderAlarmReceiver` даёт окно для запуска вибрации. Для `VIBRATE_ONLY` WakeLock не используется, но `AlarmManager` должен доставить broadcast.

- **[Risk]** На API < 26 `vibrate(longArray, 0)` (deprecated) может вести себя иначе.
  → **Mitigation**: Код уже содержит fallback на deprecated API; тестировать на физическом устройстве.

- **[Risk]** Отключение вибрации в канале `reminders_vibrate_v2` приведёт к тому, что существующие уведомления в шторке (если есть) не вибрируют.
  → **Mitigation**: Это допустимо, так как активные уведомления уже сработали, а новые будут использовать новое поведение.

## Migration Plan

1. Обновить `NotificationChannels.kt` — отключить вибрацию в `reminders_vibrate_v2`.
2. Обновить `AlarmSoundPlayer.kt` — заменить паттерн, убедиться в `vibrator.cancel()` при `stop()`.
3. Обновить `ReminderNotifier.kt` — убрать `setVibrate()` для `ALARM` и `VIBRATE_ONLY`.
4. Проверить `ReminderActionReceiver.kt` и `AlarmActivity.kt` — вызов `AlarmSoundPlayer.stop()`.
5. Ручное тестирование на физическом устройстве для `ALARM` и `VIBRATE_ONLY`.
