## 1. DataStore — миграция ключей

- [x] 1.1 В `SettingsRepository.kt`, удалить `useAlarmSoundKey` и `usePushNotificationKey`.
- [x] 1.2 Добавить `defaultDeliveryModeKey` (stringPreferencesKey, default = "NOTIFICATION").
- [x] 1.3 Добавить миграцию: при первом чтении `defaultDeliveryMode` проверить `useAlarmSound`/`usePushNotification`; если есть — вычислить режим по старой цепочке, сохранить, удалить старые ключи.
- [x] 1.4 Обновить `getDefaultDeliveryMode()`: читать строку из DataStore, парсить в `DeliveryMode`.
- [x] 1.5 Добавить `setDefaultDeliveryMode(mode: DeliveryMode)`.
- [x] 1.6 Убрать `useAlarmSound`, `usePushNotification` Flow и setters.

## 2. Переименование VIBRATE_ONLY → VIBRATE

- [x] 2.1 В `DeliveryMode.kt`, переименовать `VIBRATE_ONLY` → `VIBRATE`.
- [x] 2.2 В `ReminderNotifier.kt`, обновить все `DeliveryMode.VIBRATE_ONLY` → `VIBRATE`.
- [x] 2.3 В `ReminderAlarmReceiver.kt`, обновить enum reference.
- [x] 2.4 В `NotificationChannels.kt`, обновить channel mapping.
- [x] 2.5 Добавить Room-миграцию: `UPDATE reminders SET deliveryMode = 'VIBRATE' WHERE deliveryMode = 'VIBRATE_ONLY'`.
- [x] 2.6 Обновить `AppDatabase.kt` — bump version, добавить миграцию.

## 3. SettingsScreen — radio-группа режимов

- [x] 3.1 Убрать три `NotificationToggle` для `useAlarmSound` и `usePushNotification`.
- [x] 3.2 Добавить radio-группу из 4 режимов с иконками в карточке "Режим по умолчанию".
- [x] 3.3 Оставить Switch «Вибрация при срабатывании» (`useVibration`) отдельно.
- [x] 3.4 Скрывать/disable'ить Switch вибрации когда выбран режим VIBRATE.
- [x] 3.5 В `VoiceMindViewModel`: заменить `useAlarmSound`/`usePushNotification` на `defaultDeliveryMode`.
- [x] 3.6 В `MainActivity.kt`: передать `defaultDeliveryMode` в `SettingsScreen`.

## 4. ConfirmReminderScreen — default из Settings

- [x] 4.1 В `ConfirmReminderScreen`, читать `settings.defaultDeliveryMode` при инициализации.
- [x] 4.2 Использовать его как начальное значение `selectedDeliveryMode` вместо хардкода `NOTIFICATION`.
- [x] 4.3 Сохранить возможность ручного переопределения через `DeliveryModeGrid`.

## 5. ManualReminderScreen — DeliveryModePicker

- [x] 5.1 Добавить `DeliveryModeGrid` в `ManualReminderScreen` под полем body.
- [x] 5.2 Начальное значение — `settings.defaultDeliveryMode`.
- [x] 5.3 Передавать выбранный режим при сохранении напоминания.
- [x] 5.4 Убедиться, что `VoiceMindViewModel.saveManualReminder` принимает `deliveryMode`.

## 6. Спеки — синхронизация

- [x] 6.1 Обновить `openspec/specs/notification-delivery/spec.md` — ALARM с двумя сценариями (с/без вибрации), переименование VIBRATE_ONLY → VIBRATE, архитектура ALARM.
- [x] 6.2 Обновить `openspec/specs/ui-screens/spec.md` — radio-группа в настройках, DeliveryModePicker в ManualReminderScreen, default в ConfirmReminderScreen.

## 7. Верификация

- [x] 7.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.
- [x] 7.2 Проверить миграцию DataStore: старые ключи удаляются, defaultDeliveryMode корректно вычисляется.
- [x] 7.3 Проверить миграцию Room: записи с `VIBRATE_ONLY` преобразуются в `VIBRATE`.
- [x] 7.4 Проверить UI: radio-группа в настройках, Confirm и Manual уважают default.
