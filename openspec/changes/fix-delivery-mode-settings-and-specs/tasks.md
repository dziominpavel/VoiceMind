## 1. DataStore — миграция ключей

- [ ] 1.1 В `SettingsRepository.kt`, удалить `useAlarmSoundKey` и `usePushNotificationKey`.
- [ ] 1.2 Добавить `defaultDeliveryModeKey` (stringPreferencesKey, default = "NOTIFICATION").
- [ ] 1.3 Добавить миграцию: при первом чтении `defaultDeliveryMode` проверить `useAlarmSound`/`usePushNotification`; если есть — вычислить режим по старой цепочке, сохранить, удалить старые ключи.
- [ ] 1.4 Обновить `getDefaultDeliveryMode()`: читать строку из DataStore, парсить в `DeliveryMode`.
- [ ] 1.5 Добавить `setDefaultDeliveryMode(mode: DeliveryMode)`.
- [ ] 1.6 Убрать `useAlarmSound`, `usePushNotification` Flow и setters.

## 2. Переименование VIBRATE_ONLY → VIBRATE

- [ ] 2.1 В `DeliveryMode.kt`, переименовать `VIBRATE_ONLY` → `VIBRATE`.
- [ ] 2.2 В `ReminderNotifier.kt`, обновить все `DeliveryMode.VIBRATE_ONLY` → `VIBRATE`.
- [ ] 2.3 В `ReminderAlarmReceiver.kt`, обновить enum reference.
- [ ] 2.4 В `NotificationChannels.kt`, обновить channel mapping.
- [ ] 2.5 Добавить Room-миграцию: `UPDATE reminders SET deliveryMode = 'VIBRATE' WHERE deliveryMode = 'VIBRATE_ONLY'`.
- [ ] 2.6 Обновить `AppDatabase.kt` — bump version, добавить миграцию.

## 3. SettingsScreen — radio-группа режимов

- [ ] 3.1 Убрать три `NotificationToggle` для `useAlarmSound` и `usePushNotification`.
- [ ] 3.2 Добавить radio-группу из 4 режимов с иконками в карточке "Режим по умолчанию".
- [ ] 3.3 Оставить Switch «Вибрация при срабатывании» (`useVibration`) отдельно.
- [ ] 3.4 Скрывать/disable'ить Switch вибрации когда выбран режим VIBRATE.
- [ ] 3.5 В `VoiceMindViewModel`: заменить `useAlarmSound`/`usePushNotification` на `defaultDeliveryMode`.
- [ ] 3.6 В `MainActivity.kt`: передать `defaultDeliveryMode` в `SettingsScreen`.

## 4. ConfirmReminderScreen — default из Settings

- [ ] 4.1 В `ConfirmReminderScreen`, читать `settings.defaultDeliveryMode` при инициализации.
- [ ] 4.2 Использовать его как начальное значение `selectedDeliveryMode` вместо хардкода `NOTIFICATION`.
- [ ] 4.3 Сохранить возможность ручного переопределения через `DeliveryModeGrid`.

## 5. ManualReminderScreen — DeliveryModePicker

- [ ] 5.1 Добавить `DeliveryModeGrid` в `ManualReminderScreen` под полем body.
- [ ] 5.2 Начальное значение — `settings.defaultDeliveryMode`.
- [ ] 5.3 Передавать выбранный режим при сохранении напоминания.
- [ ] 5.4 Убедиться, что `VoiceMindViewModel.saveManualReminder` принимает `deliveryMode`.

## 6. Спеки — синхронизация

- [ ] 6.1 Обновить `openspec/specs/notification-delivery/spec.md` — ALARM с двумя сценариями (с/без вибрации), переименование VIBRATE_ONLY → VIBRATE, архитектура ALARM.
- [ ] 6.2 Обновить `openspec/specs/ui-screens/spec.md` — radio-группа в настройках, DeliveryModePicker в ManualReminderScreen, default в ConfirmReminderScreen.

## 7. Верификация

- [ ] 7.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.
- [ ] 7.2 Проверить миграцию DataStore: старые ключи удаляются, defaultDeliveryMode корректно вычисляется.
- [ ] 7.3 Проверить миграцию Room: записи с `VIBRATE_ONLY` преобразуются в `VIBRATE`.
- [ ] 7.4 Проверить UI: radio-группа в настройках, Confirm и Manual уважают default.
