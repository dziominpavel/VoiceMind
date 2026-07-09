## MODIFIED Requirements

### Requirement: BootReceiver reschedule
При получении BOOT_COMPLETED или MY_PACKAGE_REPLACED система MUST перепланировать все PENDING напоминания. `BootReceiver` MUST быть объявлен с `android:exported="true"`, иначе система не доставит implicit boot broadcast.

#### Scenario: Перезагрузка устройства
- **WHEN** устройство перезагрузилось
- **THEN** BootReceiver получает `BOOT_COMPLETED`
- **AND** вызывает `ReminderRepository.rescheduleAll()` (и при необходимости `fireOverdue`)
- **AND** все PENDING напоминания вновь планируются в AlarmManager

#### Scenario: BootReceiver экспортирован
- **WHEN** приложение установлено на API 26+
- **THEN** в AndroidManifest у `BootReceiver` стоит `android:exported="true"`
- **AND** intent-filter содержит `BOOT_COMPLETED` и `MY_PACKAGE_REPLACED`

### Requirement: Глобальный режим доставки из настроек
Система MUST использовать `reminder.deliveryMode` как источник истины при срабатывании alarm и показе уведомления. `settings.defaultDeliveryMode` MUST применяться только как fallback, если поле reminder пустое или невалидное. Смена дефолта в настройках MUST NOT менять поведение уже созданных напоминаний, пока их `deliveryMode` не синхронизирован явно (см. sync requirement).

#### Scenario: Срабатывание с per-reminder режимом
- **WHEN** напоминание с `deliveryMode=NOTIFICATION` срабатывает, а в настройках default = ALARM
- **THEN** `ReminderNotifier` и `ReminderAlarmReceiver` используют NOTIFICATION
- **AND** полноэкранный ALARM UI / alarm-звук не запускаются

#### Scenario: Fallback на settings
- **WHEN** `reminder.deliveryMode` невалиден
- **THEN** используется `settings.defaultDeliveryMode`

## ADDED Requirements

### Requirement: Snooze не воскрешает отменённые
`snoozeReminder` MUST быть no-op, если статус напоминания не PENDING и не TRIGGERED (в частности CANCELLED/DONE).

#### Scenario: Stale snooze на отменённом
- **WHEN** пользователь нажимает «Отложить» на устаревшем уведомлении CANCELLED/DONE напоминания
- **THEN** статус и alarm не меняются

### Requirement: ReminderScheduler идемпотентен
`ReminderScheduler.schedule` MUST отменять предыдущий alarm для того же id перед постановкой нового. `cancel` MUST вызывать только `AlarmManager.cancel(pendingIntent)` и MUST NOT вызывать `PendingIntent.cancel()`, чтобы не уничтожать PI для параллельного schedule.

#### Scenario: Повторный schedule
- **WHEN** для одного reminder вызывается `schedule` дважды с разным `fireAt`
- **THEN** в системе остаётся один alarm на новое время

#### Scenario: Cancel не убивает PI навсегда
- **WHEN** вызывается `cancel(id)`, затем сразу `schedule` для того же id
- **THEN** новый alarm успешно ставится

### Requirement: Порядок updateAndSchedule
При изменении напоминания система MUST выполнять `scheduler.cancel` до записи в БД, затем `schedule` если статус PENDING и `fireAt` в будущем.

#### Scenario: Смена fireAt перед старым alarm
- **WHEN** пользователь меняет время напоминания незадолго до старого `fireAt`
- **THEN** старый alarm отменяется до обновления записи так, чтобы не сработать на устаревшие данные
