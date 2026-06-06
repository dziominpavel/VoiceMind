## MODIFIED Requirements

### Requirement: Режим оповещения VIBRATE
При deliveryMode VIBRATE система ДОЛЖНА активировать те же слои доставки, что и ALARM, за исключением звукового сигнала: (1) `FULL_WAKE_LOCK` с `ACQUIRE_CAUSES_WAKEUP` для включения экрана; (2) `AlarmActivity` через full-screen intent поверх экрана блокировки; (3) повторяющийся паттерн вибрации через `AlarmSoundPlayer`; (4) беззвучное anchor-уведомление в шторке.

#### Scenario: Вибрация как будильник без звука
- **WHEN** напоминание с режимом VIBRATE срабатывает
- **THEN** система включает экран, если он выключен (`FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE`)
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.playVibrationOnly()` воспроизводит повторяющийся паттерн вибрации
- **AND** звук не воспроизводится
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление ДОЛЖНО содержать паттерн вибрации `DEFAULT_VIBRATE_PATTERN`
- **AND** уведомление НЕ ДОЛЖНО содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий
