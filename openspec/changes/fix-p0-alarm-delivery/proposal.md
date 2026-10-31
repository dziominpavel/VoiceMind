## Why

Код-ревью выявило пять дефектов уровня P0 в доставке напоминаний: удаление активного канала уведомлений при каждом показе, неработающий системный back в `AlarmActivity` на Android 16, устаревший `deliveryMode` в уже запланированных alarm, обновление виджетов на main thread и потеря действий пользователя при закрытии `AlarmActivity`. Эти дефекты ломают надёжность будильника и могут приводить к ANR или потере статуса напоминания.

## What Changes

- **Каналы уведомлений:** создавать каналы один раз при старте приложения; при показе уведомления MUST NOT удалять и пересоздавать текущие каналы. Удаление старых ID допускается только как одноразовая миграция.
- **`AlarmActivity` и системный back:** обрабатывать back через `OnBackPressedDispatcher`/`BackHandler`, а не через устаревший `onBackPressed`; back MUST отменять напоминание, останавливать звук и закрывать экран.
- **Действия `AlarmActivity`:** «Выполнено», «Отложить» и «Отменить» MUST завершать изменение БД и перепланирование alarm до закрытия Activity либо выполняться в scope, который переживает Activity.
- **Смена режима доставки:** после синхронизации `deliveryMode` в Room пересоздавать alarm для всех `PENDING`-напоминаний, чтобы `EXTRA_DELIVERY_MODE` в PendingIntent соответствовал БД.
- **Виджеты:** обновление виджетов MUST выполняться вне main thread; broadcast-receiver виджета MUST использовать `goAsync`, а не `runBlocking`; дублирующие обновления из ViewModel убираются.
- **Проверки:** добавить unit/instrumented-тесты на жизненный цикл каналов, пересоздание alarm при смене режима и обработку back/действий `AlarmActivity`.

## Capabilities

### New Capabilities

(нет)

### Modified Capabilities

- `notification-delivery`: жизненный цикл каналов уведомлений и пересоздание alarm после синхронизации `deliveryMode`.
- `alarm-screen-wake`: обработка системного back и гарантия выполнения действий при закрытии полноэкранного UI.
- `widget`: обновление виджета не блокирует main thread и не приводит к ANR.

## Impact

- **Код:** `NotificationChannels.kt`, `ReminderNotifier.kt`, `VoiceMindApplication.kt`, `ReminderRepository.kt`, `VoiceMindViewModel.kt`, `AlarmActivity.kt`, `WidgetUpdater.kt`, `WidgetToggleReceiver.kt`, `ReminderListWidgetProvider.kt`, `MainActivity.kt`, `ReminderAlarmReceiver.kt`.
- **Данные:** схема Room не меняется, миграции не требуются.
- **Поведение:** UI и сценарии использования не меняются; исправляется надёжность доставки, back-навигации и обновления виджетов.
- **Риски:** изменение порядка обновления виджетов и жизненного цикла каналов требует проверки на устройстве; действия `AlarmActivity` должны остаться мгновенными для пользователя.
