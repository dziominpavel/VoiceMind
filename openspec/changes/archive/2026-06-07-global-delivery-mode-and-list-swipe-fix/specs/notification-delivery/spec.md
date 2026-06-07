## ADDED Requirements

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
