## MODIFIED Requirements

### Requirement: Прямой запуск AlarmActivity при включённом экране
При срабатывании напоминания с `deliveryMode` ALARM или VIBRATE система MUST запускать `AlarmActivity` напрямую через `startActivity` из `ReminderAlarmReceiver.onReceive` синхронно (до `goAsync`). Режим MUST браться из extras PendingIntent (`EXTRA_DELIVERY_MODE`), выставленных при `schedule`. Для NOTIFICATION и SILENT `startActivity` MUST NOT вызываться.

#### Scenario: Экран включён, разблокирован, обычное приложение
- **WHEN** напоминание ALARM срабатывает при включённом и разблокированном экране, пока пользователь в обычном приложении
- **THEN** `ReminderAlarmReceiver` синхронно в `onReceive` вызывает `context.startActivity(Intent(context, AlarmActivity::class.java))`
- **AND** `AlarmActivity` появляется поверх текущего приложения
- **AND** звук/вибрация запускаются корутиной ресивера после `goAsync`

#### Scenario: Immersive-приложение (игра/видео)
- **WHEN** напоминание ALARM срабатывает, пока foreground-приложение в immersive fullscreen-режиме
- **THEN** `AlarmActivity` всё равно появляется поверх immersive-приложения
- **AND** поведение аналогично системному «Будильнику»

#### Scenario: Режим VIBRATE
- **WHEN** напоминание VIBRATE срабатывает при включённом экране
- **THEN** `AlarmActivity` появляется поверх текущего приложения
- **AND** запускается только вибрация (без звука)

#### Scenario: Режимы NOTIFICATION и SILENT
- **WHEN** напоминание NOTIFICATION или SILENT срабатывает при включённом экране
- **THEN** `AlarmActivity` НЕ запускается напрямую
- **AND** система показывает heads-up (NOTIFICATION) или тихое уведомление (SILENT) как раньше
