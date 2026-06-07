## Purpose

Режимы доставки напоминаний, каналы уведомлений, permissions и действия пользователя на уведомлении.
## Requirements
### Requirement: Режим оповещения NOTIFICATION
При deliveryMode NOTIFICATION система MUST показывать обычное уведомление через канал reminders_default с importance HIGH. Вибрация в уведомлении добавляется только если глобальная настройка `useVibration` включена.

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

### Requirement: Режим оповещения ALARM
При deliveryMode ALARM система MUST активировать три слоя доставки: (1) `AlarmSoundPlayer` — прямое управление ringtone через `STREAM_ALARM` и вибрацией через `Vibrator`; (2) `AlarmActivity` — full-screen intent поверх экрана блокировки; (3) `ReminderNotifier` — беззвучное anchor-уведомление в шторке с actions. Вибрация через `AlarmSoundPlayer` управляется настройкой `useVibration`, а вибрация в уведомлении — через `setVibrate()` того же флага.

#### Scenario: Будильник-напоминание с вибрацией
- **WHEN** напоминание с режимом ALARM срабатывает
- **AND** настройка `useVibration` установлена в `true`
- **THEN** система включает экран, если он выключен
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.play()` воспроизводит ringtone на `STREAM_ALARM` и запускает повторяющийся паттерн вибрации
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление MUST содержать паттерн вибрации `DEFAULT_VIBRATE_PATTERN`
- **AND** уведомление MUST NOT содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий

#### Scenario: Будильник-напоминание без вибрации
- **WHEN** напоминание с режимом ALARM срабатывает
- **AND** настройка `useVibration` установлена в `false`
- **THEN** система включает экран, если он выключен
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.play()` воспроизводит ringtone на `STREAM_ALARM` БЕЗ вибрации
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление MUST NOT содержать паттерна вибрации (`setVibrate(null)`)
- **AND** уведомление MUST NOT содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий

### Requirement: Режим оповещения VIBRATE
При deliveryMode VIBRATE система MUST активировать те же слои доставки, что и ALARM, за исключением звукового сигнала: (1) `FULL_WAKE_LOCK` с `ACQUIRE_CAUSES_WAKEUP` для включения экрана; (2) `AlarmActivity` через full-screen intent поверх экрана блокировки; (3) повторяющийся паттерн вибрации через `AlarmSoundPlayer`; (4) беззвучное anchor-уведомление в шторке.

#### Scenario: Вибрация как будильник без звука
- **WHEN** напоминание с режимом VIBRATE срабатывает
- **THEN** система включает экран, если он выключен (`FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE`)
- **AND** запускается full-screen intent (`AlarmActivity`) поверх экрана блокировки
- **AND** `AlarmSoundPlayer.playVibrationOnly()` воспроизводит повторяющийся паттерн вибрации
- **AND** звук не воспроизводится
- **AND** показывается высокоприоритетное anchor-уведомление с bypass DND
- **AND** уведомление MUST содержать паттерн вибрации `DEFAULT_VIBRATE_PATTERN`
- **AND** уведомление MUST NOT содержать собственного звука (`setSilent(true)`, `setSound(null)`)
- **AND** в центре экрана отображается `body` напоминания крупным шрифтом с кнопками действий

### Requirement: Режим оповещения NOTIFICATION
При deliveryMode NOTIFICATION система MUST показывать обычное уведомление через канал reminders_default с importance HIGH. Вибрация в уведомлении добавляется только если глобальная настройка `useVibration` включена.

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

### Requirement: Режим оповещения SILENT
При deliveryMode SILENT система MUST показывать уведомление без звука и вибрации через канал reminders_silent с importance LOW.

#### Scenario: Тихое уведомление
- **WHEN** напоминание с режимом SILENT срабатывает
- **THEN** в шторке появляется иконка и текст
- **AND** нет звука, нет вибрации

### Requirement: Действие "Готово" на уведомлении
Пользователь MUST иметь возможность отметить напоминание выполненным из уведомления.

#### Scenario: Отметить выполненным
- **WHEN** пользователь нажимает "Готово" на уведомлении
- **THEN** статус напоминания меняется на DISMISSED
- **AND** alarm отменяется

### Requirement: Действие "Отложить" на уведомлении
Пользователь MUST иметь возможность отложить напоминание на 10 минут.

#### Scenario: Отложить на 10 минут
- **WHEN** пользователь нажимает "Отложить 10 мин" на уведомлении
- **THEN** fireAt смещается на now + 10 минут
- **AND** статус меняется на SNOOZED → SCHEDULED
- **AND** alarm перепланируется через ReminderScheduler

### Requirement: Действие "Отменить" на уведомлении
Пользователь MUST иметь возможность отменить напоминание из уведомления.

#### Scenario: Отменить напоминание
- **WHEN** пользователь нажимает "Отменить" на уведомлении
- **THEN** статус меняется на CANCELLED
- **AND** alarm отменяется

### Requirement: Permission POST_NOTIFICATIONS
На API 33+ приложение MUST запрашивать POST_NOTIFICATIONS до первого показа уведомления.

#### Scenario: Запрос разрешения
- **WHEN** приложение впервые пытается показать уведомление на API 33+
- **THEN** система запрашивает у пользователя разрешение
- **AND** при отказе показывается Snackbar с переходом в настройки

### Requirement: Permission SCHEDULE_EXACT_ALARM
На API 31+ приложение MUST проверять canScheduleExactAlarms() и запрашивать разрешение.

#### Scenario: Нет разрешения на точный alarm
- **WHEN** пользователь создаёт напоминание без SCHEDULE_EXACT_ALARM
- **THEN** используется fallback setAndAllowWhileIdle
- **AND** показывается предупреждение о неточности

### Requirement: BootReceiver reschedule
При получении BOOT_COMPLETED или MY_PACKAGE_REPLACED система MUST перепланировать все SCHEDULED напоминания.

#### Scenario: Перезагрузка устройства
- **WHEN** устройство перезагрузилось
- **THEN** BootReceiver вызывает ReminderScheduler.rescheduleAll()
- **AND** все SCHEDULED напоминания вновь планируются в AlarmManager

### Requirement: Глобальный режим доставки из настроек
Система MUST определять режим доставки для любого напоминания исключительно из `settings.defaultDeliveryMode` при срабатывании, а не из поля `reminder.deliveryMode` в Room.

#### Scenario: Срабатывание с глобальным режимом
- **WHEN** напоминание срабатывает по alarm
- **THEN** `ReminderNotifier` и `ReminderAlarmReceiver` читают `settings.defaultDeliveryMode`
- **AND** поведение доставки соответствует глобальному режиму, независимо от значения `reminder.deliveryMode` в БД

#### Scenario: Смена режима в настройках до срабатывания
- **WHEN** пользователь меняет `defaultDeliveryMode` в настройках
- **AND** существуют напоминания с другим `deliveryMode` в БД
- **THEN** при следующем срабатывании любого напоминания применяется новый глобальный режим

### Requirement: Синхронизация deliveryMode в БД
Система MUST синхронизировать поле `reminder.deliveryMode` в Room с текущим `settings.defaultDeliveryMode` для всех записей без фильтра по статусу.

#### Scenario: Миграция при обновлении приложения
- **WHEN** пользователь обновляет приложение на версию с этим change
- **THEN** выполняется Room-миграция, обновляющая `deliveryMode` всех записей
- **AND** при первом запуске после миграции выполняется sync из актуального `settings.defaultDeliveryMode`

#### Scenario: Смена режима в настройках
- **WHEN** пользователь выбирает другой режим доставки в настройках
- **THEN** все записи в таблице `reminders` (любой статус: PENDING, TRIGGERED, DONE, CANCELLED) получают обновлённый `deliveryMode`
- **AND** sync выполняется до отображения Snackbar/закрытия экрана настроек

