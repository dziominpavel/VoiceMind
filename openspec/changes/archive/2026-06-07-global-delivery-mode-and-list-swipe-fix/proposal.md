## Why

Сейчас режим оповещения хранится per-reminder в Room (`deliveryMode`), а на экранах Confirm/Manual пользователь может выбрать режим отдельно для каждой записи. Это противоречит продуктовой модели: один глобальный профиль оповещения в настройках для всех напоминаний. На скриншоте списка видны разные иконки (вибро vs будильник) на соседних карточках — следствие per-reminder хранения.

Параллельно swipe-to-reveal в списке предстоящих не доведён до конца: красная подложка `errorContainer` просвечивает по скруглённым углам карточки, а при открытой reveal-панели иконка удаления наезжает на иконку режима доставки. Предыдущий change `fix-vibration-swipe-reveal-and-ui-layout` внедрил паттерн, но визуальные дефекты остались.

## What Changes

- **Глобальный режим оповещения**: единственный источник правды — `settings.defaultDeliveryMode` (+ `useVibration` для ALARM/NOTIFICATION). При срабатывании читаются настройки, а не `reminder.deliveryMode`.
- **Миграция БД**: при обновлении приложения все записи в `reminders` (любой статус) получают `deliveryMode` = текущий `defaultDeliveryMode` из DataStore.
- **Смена режима в настройках**: при изменении `defaultDeliveryMode` все напоминания в БД (любой статус) немедленно обновляются на новый режим.
- **Убрать per-reminder выбор**: удалить `DeliveryModeGrid` с `ConfirmReminderScreen` и `ManualReminderScreen`; убрать передачу `deliveryMode` при сохранении.
- **Список и Home**: иконка режима доставки берётся из текущих настроек (одинаковая для всех карточек), а не из поля записи.
- **Исправить SwipeToRevealBox**: красный фон только в полосе действия (80dp), clip скруглённой формы на foreground, скрытие иконки режима при открытом reveal — устранить красные углы и наложение иконок.
- **BREAKING**: колонка `deliveryMode` в Room остаётся для совместимости, но перестаёт быть источником правды; поведение при срабатывании меняется для уже созданных напоминаний.

## Capabilities

### New Capabilities

- Нет.

### Modified Capabilities

- `notification-delivery`: режим доставки определяется глобальными настройками; миграция и синхронизация всех записей при смене настроек.
- `ui-screens`: убрать per-reminder `DeliveryModeGrid` с Confirm/Manual; иконка режима в списке из настроек; доработать swipe-to-reveal без визуальных артефактов.

## Impact

- `SettingsRepository` / `VoiceMindViewModel` — синхронизация `deliveryMode` всех reminders при смене настроек.
- `AppDatabase` — миграция Room: UPDATE всех `deliveryMode` на текущий default.
- `ReminderNotifier`, `ReminderAlarmReceiver` — читать режим из настроек вместо `reminder.deliveryMode`.
- `ConfirmReminderScreen`, `ManualReminderScreen` — убрать `DeliveryModeGrid`.
- `ReminderListScreen`, `HomeScreen` — иконка из настроек; скрытие при reveal.
- `SwipeToRevealBox` — исправление layout/clip фона reveal-панели.
- `VoiceMindViewModel.saveReminder` — не принимать per-reminder `deliveryMode`.
- Спеки `notification-delivery`, `ui-screens` — обновить требования.
