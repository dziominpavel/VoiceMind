## Зачем

Анализ текущей архитектуры режимов доставки выявил системные расхождения между UI настроек, логикой создания напоминаний и поведением при срабатывании:

1. **SettingsScreen вводит в заблуждение**: три независимых Switch («Звук будильника», «Пуш-уведомления», «Вибрация») создают впечатление, что пользователь может комбинировать функции. На самом деле `getDefaultDeliveryMode()` использует их как приоритетную цепочку — только один режим выбирается. Это radio-логика, замаскированная под чекбоксы.
2. **`useAlarmSound` — «мёртвая» настройка**: она влияет только на выбор режима по умолчанию для новых напоминаний, но не отключает звук у уже созданных ALARM. Реальный звук контролируется `alarmVolume` и `alarmRingtoneUri`.
3. **`ConfirmReminderScreen` игнорирует настройки**: режим доставки жёстко зашит `DeliveryMode.NOTIFICATION`, даже если пользователь настроил ALARM в настройках.
4. **`ManualReminderScreen` не позволяет выбрать режим**: отсутствует `DeliveryModePicker`.
5. **Спека `notification-delivery` не описывает архитектуру слоёв**: непонятно, где AlarmSoundPlayer (прямое железо), а где ReminderNotifier (системное уведомление). Нет сценария ALARM без вибрации.

## Что меняется

- **SettingsScreen**: заменить три независимых Switch на radio-группу из 4 режимов: «Будильник», «Уведомление», «Вибрация», «Тихий». Убрать `useAlarmSound` и `usePushNotification` из DataStore; оставить только `defaultDeliveryMode` (строка enum name) и `useVibration` (отдельный toggle, влияющий на поведение при срабатывании).
- **ConfirmReminderScreen**: использовать `settings.getDefaultDeliveryMode()` как начальное значение `selectedDeliveryMode` вместо хардкода `NOTIFICATION`.
- **ManualReminderScreen**: добавить `DeliveryModePicker` для выбора режима доставки при ручном создании.
- **Спека notification-delivery**: добавить requirement «Архитектура ALARM» с пояснением трёх слоёв (AlarmSoundPlayer, AlarmActivity, ReminderNotifier). Добавить сценарий «ALARM без вибрации».
- **Переименовать `VIBRATE_ONLY` → `VIBRATE`** в коде и спеках для единообразия.

## Capabilities

### Новые capabilities
- Нет.

### Изменённые capabilities
- `notification-delivery`: добавлено описание архитектуры ALARM (три слоя), сценарий ALARM без вибрации, переименование VIBRATE_ONLY → VIBRATE.
- `ui-screens`: SettingsScreen получает radio-группу режимов; ConfirmReminderScreen и ManualReminderScreen уважают default delivery mode.
- `datastore-settings`: упрощение ключей — удаление `useAlarmSound` и `usePushNotification`, добавление `defaultDeliveryMode`.

## Влияние

- `app/src/main/java/com/example/voicemind/ui/screens/SettingsScreen.kt` — radio-группа вместо трёх Switch.
- `app/src/main/java/com/example/voicemind/ui/screens/ConfirmReminderScreen.kt` — дефолтный режим из настроек.
- `app/src/main/java/com/example/voicemind/ui/screens/ManualReminderScreen.kt` — `DeliveryModePicker`.
- `app/src/main/java/com/example/voicemind/data/SettingsRepository.kt` — миграция ключей DataStore.
- `app/src/main/java/com/example/voicemind/viewmodel/VoiceMindViewModel.kt` — чтение новых ключей.
- `app/src/main/java/com/example/voicemind/data/DeliveryMode.kt` — переименование `VIBRATE_ONLY` → `VIBRATE`.
- `app/src/main/java/com/example/voicemind/data/notification/ReminderNotifier.kt`, `ReminderAlarmReceiver.kt` — обновление enum references.
- `app/src/main/res/values/strings.xml` — новые/обновлённые строки.
- **Миграция DataStore**: `useAlarmSound` и `usePushNotification` удаляются; `defaultDeliveryMode` вычисляется из старых значений при первом чтении.
