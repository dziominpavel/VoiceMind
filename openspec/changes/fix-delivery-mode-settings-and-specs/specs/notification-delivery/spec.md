## MODIFIED Requirements

### Requirement: Режим оповещения ALARM
При deliveryMode ALARM система ДОЛЖНА активировать три слоя доставки: (1) `AlarmSoundPlayer` — прямое управление ringtone через `STREAM_ALARM` и вибрацией через `Vibrator`; (2) `AlarmActivity` — full-screen intent поверх экрана блокировки; (3) `ReminderNotifier` — беззвучное anchor-уведомление в шторке с actions. Вибрация через `AlarmSoundPlayer` управляется настройкой `useVibration`, а вибрация в уведомлении — через `setVibrate()` того же флага.

#### Scenario: Будильник-напоминание с вибрацией
- **WHEN** напоминание с режимом ALARM срабатывает
- **AND** настройка `useVibration` установлена в `true`
- **THEN** система включает экран, если он выключен
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.play()` воспроизводит ringtone на `STREAM_ALARM` и запускает повторяющийся паттерн вибрации
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление ДОЛЖНО содержать паттерн вибрации `DEFAULT_VIBRATE_PATTERN`
- **AND** уведомление НЕ ДОЛЖНО содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий

#### Scenario: Будильник-напоминание без вибрации
- **WHEN** напоминание с режимом ALARM срабатывает
- **AND** настройка `useVibration` установлена в `false`
- **THEN** система включает экран, если он выключен
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.play()` воспроизводит ringtone на `STREAM_ALARM` БЕЗ вибрации
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление НЕ ДОЛЖНО содержать паттерна вибрации (`setVibrate(null)`)
- **AND** уведомление НЕ ДОЛЖНО содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий

### Requirement: Режим оповещения VIBRATE
При deliveryMode VIBRATE система ДОЛЖНА показывать уведомление без звука через канал reminders_vibrate. Вибрация управляется явно через Vibrator с повторяющимся паттерном.

#### Scenario: Только вибрация
- **WHEN** напоминание с режимом VIBRATE срабатывает
- **THEN** уведомление показывается без звука через канал reminders_vibrate
- **AND** уведомление ДОЛЖНО содержать паттерн вибрации `DEFAULT_VIBRATE_PATTERN`
- **AND** воспроизводится повторяющийся паттерн вибрации через `AlarmSoundPlayer`
- **AND** звук не воспроизводится

### Requirement: Режим оповещения NOTIFICATION
При deliveryMode NOTIFICATION система ДОЛЖНА показывать обычное уведомление через канал reminders_default с importance HIGH. Вибрация в уведомлении добавляется только если глобальная настройка `useVibration` включена.

#### Scenario: Пуш-уведомление с вибрацией
- **WHEN** напоминание с режимом NOTIFICATION срабатывает
- **AND** настройка `useVibration` установлена в `true`
- **THEN** показывается heads-up уведомление со звуком и текстом
- **AND** уведомление содержит паттерн вибрации `DEFAULT_VIBRATE_PATTERN`

#### Scenario: Пуш-уведомление без вибрации
- **WHEN** напоминание с режимом NOTIFICATION срабатывает
- **AND** настройка `useVibration` установлена в `false`
- **THEN** показывается heads-up уведомление со звуком и текстом
- **AND** уведомление НЕ содержит паттерна вибрации (`setVibrate(null)`)

## RENAMED Requirements

### Requirement: Режим оповещения VIBRATE_ONLY
**FROM:** `VIBRATE_ONLY`  
**TO:** `VIBRATE`  
**Reason:** Суффикс `_ONLY` избыточен в контексте enum `DeliveryMode`. Переименование упрощает чтение и устраняет путаницу.
