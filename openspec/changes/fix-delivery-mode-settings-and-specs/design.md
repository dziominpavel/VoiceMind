## Контекст

Текущая система режимов доставки имеет архитектурное рассогласование между UI настроек, логикой создания напоминаний и поведением при срабатывании.

**SettingsScreen** показывает три независимых Switch:
- `useAlarmSound` (default false)
- `usePushNotification` (default true)
- `useVibration` (default false)

Пользователь видит три опции и считает их независимыми. Но `SettingsRepository.getDefaultDeliveryMode()` реализует приоритетную цепочку:
```
if useAlarmSound → ALARM
else if usePushNotification → NOTIFICATION
else if useVibration → VIBRATE_ONLY
else → SILENT
```
Это radio-логика, замаскированная под чекбоксы. При включении всех трёх выбирается только ALARM.

**ConfirmReminderScreen** жёстко задаёт `selectedDeliveryMode = DeliveryMode.NOTIFICATION`, игнорируя настройки пользователя.

**ManualReminderScreen** не имеет `DeliveryModePicker` вообще.

**Спека `notification-delivery`** не описывает разделение слоёв: `AlarmSoundPlayer` (прямое управление ringtone + Vibrator) vs `ReminderNotifier` (системное уведомление). Пользователь ALARM получает оба слоя одновременно, но это не документировано.

## Goals / Non-Goals

**Goals:**
- Устранить обманчивый UX настроек — radio-группа вместо трёх Switch.
- Сохранить единственный toggle `useVibration`, который ВЛИЯЕТ на поведение при срабатывании (единственный из трёх, кто это делает).
- ConfirmReminderScreen и ManualReminderScreen должны уважать режим по умолчанию.
- Документировать архитектуру ALARM (AlarmSoundPlayer + AlarmActivity + ReminderNotifier).
- Переименовать `VIBRATE_ONLY` → `VIBRATE`.

**Non-Goals:**
- Изменение `AlarmSoundPlayer` или каналов уведомлений.
- Изменение схемы БД Room.
- Добавление новых режимов доставки.
- Изменение поведения BootReceiver или ReminderScheduler.

## Decisions

### 1. SettingsScreen: radio-группа + отдельный toggle вибрации
**Решение:** Заменить три Switch на:
- Radio-группу из 4 режимов: ALARM, NOTIFICATION, VIBRATE, SILENT (единственный выбор).
- Отдельный Switch «Вибрация при срабатывании» — включает/выключает вибрацию для ALARM и NOTIFICATION. Для VIBRATE этот toggle скрыт (вибрация всегда).

**Почему:** radio-группа честно отражает приоритетную цепочку `getDefaultDeliveryMode`. Отдельный `useVibration` — потому что это единственная настройка, которая реально влияет на runtime-поведение (передаётся в `AlarmSoundPlayer.play(..., useVibration)` и в `setVibrate()` уведомления).

**Альтернатива:** оставить три Switch, но добавить мгновенный preview выбранного режима. **Отклонено:** всё равно неочевидно, что происходит при комбинации.

### 2. DataStore: `defaultDeliveryMode` + `useVibration`
**Решение:** Удалить `useAlarmSoundKey` и `usePushNotificationKey`. Добавить `defaultDeliveryModeKey` (string, хранит enum name).
`useVibrationKey` оставить.

**Миграция:** при первом чтении `defaultDeliveryMode` — если старые ключи существуют, вычислить режим по старой приоритетной цепочке и сохранить в новый ключ. Старые ключи удалить.

**Почему:** `useAlarmSound` и `usePushNotification` — не behavior-настройки, а selectors режима. Они не влияют на уже созданные напоминания.

### 3. ConfirmReminderScreen: default из Settings
**Решение:** При открытии `ConfirmReminderScreen` читать `settings.defaultDeliveryMode` и использовать его как начальное значение `selectedDeliveryMode`.

### 4. ManualReminderScreen: DeliveryModePicker
**Решение:** Добавить `DeliveryModeGrid` (уже есть в ConfirmReminderScreen) в `ManualReminderScreen` с начальным значением из `settings.defaultDeliveryMode`.

### 5. Переименование VIBRATE_ONLY → VIBRATE
**Решение:** Переименовать enum value и все references в коде. Спеки обновить.
**Почему:** `_ONLY` добавляет ненужный шум. В контексте enum `DeliveryMode.VIBRATE` однозначно.

## Risks / Trade-offs

| Риск | Компенсация |
|------|-------------|
| **Миграция DataStore сломает старые значения** | При первом запуске после обновления вычисляем `defaultDeliveryMode` из старых ключей. Старые ключи `useAlarmSound`/`usePushNotification` удаляются после миграции. |
| **Пользователь привык к трём Switch и не поймёт radio** | Новый UI интуитивнее — radio показывает честный выбор одного из четырёх. Плюс preview выбранного режима в реальном времени. |
| **Переименование enum ломает старые записи в БД** | Room хранит enum как строку (`name`). Если в БД есть `VIBRATE_ONLY`, после переименования `DeliveryMode.valueOf()` упадёт. Нужна миграция БД. |

## План миграции

### DataStore
1. При первом чтении `defaultDeliveryMode`: проверить наличие `useAlarmSound`/`usePushNotification`.
2. Если есть — вычислить режим по старой логике, сохранить в `defaultDeliveryMode`, удалить старые ключи.
3. Если нет — использовать `defaultDeliveryMode` как есть (default = `NOTIFICATION`).

### Room (enum rename)
1. Создать миграцию `AppDatabase` версии N → N+1.
2. В миграции: `UPDATE reminders SET deliveryMode = 'VIBRATE' WHERE deliveryMode = 'VIBRATE_ONLY'`.
3. Enum value в Kotlin: `VIBRATE_ONLY` → `VIBRATE`.

### Откат
- DataStore: невозможен без резервной копии (но значения тривиальны).
- Room: миграция обратима — `UPDATE reminders SET deliveryMode = 'VIBRATE_ONLY' WHERE deliveryMode = 'VIBRATE'`.

## Открытые вопросы

- Нет.
