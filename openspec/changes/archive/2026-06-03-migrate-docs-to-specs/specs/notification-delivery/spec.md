## ADDED Requirements

### Requirement: Режим оповещения NOTIFICATION
При deliveryMode NOTIFICATION система ДОЛЖНА показывать обычное уведомление через канал reminders_default с importance HIGH.

#### Scenario: Обычное уведомление
- **WHEN** напоминание с режимом NOTIFICATION срабатывает
- **THEN** показывается heads-up уведомление со звуком и текстом
- **AND** уведомление остаётся в шторке

### Requirement: Режим оповещения ALARM
При deliveryMode ALARM система ДОЛЖНА показывать уведомление через канал reminders_alarm с высоким приоритетом, длинной вибрацией и bypass DND.

#### Scenario: Будильник-напоминание
- **WHEN** напоминание с режимом ALARM срабатывает
- **THEN** показывается full-screen intent (фаза 4) или высокоприоритетное уведомление
- **AND** воспроизводится паттерн вибрации

### Requirement: Режим оповещения VIBRATE_ONLY
При deliveryMode VIBRATE_ONLY система ДОЛЖНА показывать уведомление без звука с паттерном вибрации через канал reminders_vibrate.

#### Scenario: Только вибрация
- **WHEN** напоминание с режимом VIBRATE_ONLY срабатывает
- **THEN** устройство вибрирует по заданному паттерну
- **AND** звук не воспроизводится

### Requirement: Режим оповещения SILENT
При deliveryMode SILENT система ДОЛЖНА показывать уведомление без звука и вибрации через канал reminders_silent с importance LOW.

#### Scenario: Тихое уведомление
- **WHEN** напоминание с режимом SILENT срабатывает
- **THEN** в шторке появляется иконка и текст
- **AND** нет звука, нет вибрации

### Requirement: Действие "Готово" на уведомлении
Пользователь ДОЛЖЕН иметь возможность отметить напоминание выполненным из уведомления.

#### Scenario: Отметить выполненным
- **WHEN** пользователь нажимает "Готово" на уведомлении
- **THEN** статус напоминания меняется на DISMISSED
- **AND** alarm отменяется

### Requirement: Действие "Отложить" на уведомлении
Пользователь ДОЛЖЕН иметь возможность отложить напоминание на 10 минут.

#### Scenario: Отложить на 10 минут
- **WHEN** пользователь нажимает "Отложить 10 мин" на уведомлении
- **THEN** fireAt смещается на now + 10 минут
- **AND** статус меняется на SNOOZED → SCHEDULED
- **AND** alarm перепланируется через ReminderScheduler

### Requirement: Действие "Отменить" на уведомлении
Пользователь ДОЛЖЕН иметь возможность отменить напоминание из уведомления.

#### Scenario: Отменить напоминание
- **WHEN** пользователь нажимает "Отменить" на уведомлении
- **THEN** статус меняется на CANCELLED
- **AND** alarm отменяется

### Requirement: Permission POST_NOTIFICATIONS
На API 33+ приложение ДОЛЖНО запрашивать POST_NOTIFICATIONS до первого показа уведомления.

#### Scenario: Запрос разрешения
- **WHEN** приложение впервые пытается показать уведомление на API 33+
- **THEN** система запрашивает у пользователя разрешение
- **AND** при отказе показывается Snackbar с переходом в настройки

### Requirement: Permission SCHEDULE_EXACT_ALARM
На API 31+ приложение ДОЛЖНО проверять canScheduleExactAlarms() и запрашивать разрешение.

#### Scenario: Нет разрешения на точный alarm
- **WHEN** пользователь создаёт напоминание без SCHEDULE_EXACT_ALARM
- **THEN** используется fallback setAndAllowWhileIdle
- **AND** показывается предупреждение о неточности

### Requirement: BootReceiver reschedule
При получении BOOT_COMPLETED или MY_PACKAGE_REPLACED система ДОЛЖНА перепланировать все SCHEDULED напоминания.

#### Scenario: Перезагрузка устройства
- **WHEN** устройство перезагрузилось
- **THEN** BootReceiver вызывает ReminderScheduler.rescheduleAll()
- **AND** все SCHEDULED напоминания вновь планируются в AlarmManager
