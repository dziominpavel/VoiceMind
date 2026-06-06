## Why

Пользователь выбирает режим «Вибрация» и ожидает поведение идентичное будильнику — включение экрана, `AlarmActivity` поверх блокировки, длинная вибрация — но без звука. В текущей реализации VIBRATE работает как «тихая вибрация в кармане»: только `PARTIAL_WAKE_LOCK`, нет full-screen intent, нет `AlarmActivity`. Это не соответствует ожиданиям.

Также в `ReminderListScreen` accent bar (цветная полоса слева) из-за ошибочного `fillMaxWidth()` растягивается на всю карточку, создавая визуальное наслоение.

## What Changes

- Переопределить поведение `DeliveryMode.VIBRATE` в `ReminderAlarmReceiver`: использовать `FULL_WAKE_LOCK` + `ACQUIRE_CAUSES_WAKEUP`, запускать `AlarmActivity` через full-screen intent, вибрация через `AlarmSoundPlayer.playVibrationOnly()`
- Обновить `ReminderNotifier` для VIBRATE: `fullScreenIntent` + `PRIORITY_MAX` + `CATEGORY_ALARM` (как ALARM, но `setSilent(true)` без ringtone)
- Исправить UI-баг в `ReminderListScreen`: заменить `fillMaxWidth()` на `fillMaxHeight()` в accent bar `Box`

## Capabilities

### New Capabilities
*(none)*

### Modified Capabilities
- `notification-delivery`: Требование к режиму VIBRATE — теперь это ALARM без звука (full-screen intent, wake lock, AlarmActivity, длинная вибрация)
- `ui-screens`: Требование к карточке списка — accent bar имеет фиксированную ширину и высоту карточки, не перекрывает содержимое

## Impact

- `ReminderAlarmReceiver.kt` — логика wake lock и ветвления ALARM/VIBRATE
- `ReminderNotifier.kt` — логика full-screen intent для VIBRATE
- `ReminderListScreen.kt` — один модификатор в accent bar
- Спеки `notification-delivery` и `ui-screens` — дельта-обновления
