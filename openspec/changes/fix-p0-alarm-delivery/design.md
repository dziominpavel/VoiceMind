# Design

## Context

См. `proposal.md` — Why. Текущая доставка устроена так:

- `VoiceMindApplication.onCreate` вызывает `NotificationChannels.createAll`, но `ReminderNotifier.show` вызывает его повторно на каждое уведомление.
- `ReminderAlarmReceiver` синхронно запускает `AlarmActivity` и передаёт только `reminderId`; действия экрана идут через `VoiceMindViewModel`, который завершается вместе с Activity.
- `ReminderRepository.syncAllDeliveryModes` обновляет только Room; PendingIntent уже запланированных alarm сохраняет старый `EXTRA_DELIVERY_MODE`.
- `WidgetUpdater.updateAll` не переключает dispatcher, а `WidgetToggleReceiver` и `ReminderListWidgetProvider` используют `runBlocking` на main thread.

## Goals / Non-Goals

**Goals:**

- Убрать удаление активных каналов и повторное создание каналов при показе уведомления.
- Сделать back и действия `AlarmActivity` надёжными на Android 16+ и при уничтожении Activity.
- Держать `EXTRA_DELIVERY_MODE` запланированных alarm в соответствии с Room.
- Убрать синхронные операции с Room из main thread в виджетном пути.
- Сохранить текущее поведение UI и не менять схему Room.

**Non-Goals:**

- Не пересматривать модель повторяющихся напоминаний и не добавлять occurrence-таблицу.
- Не менять визуальный дизайн `AlarmActivity`, настройки и виджетов.
- Не добавлять Clean Architecture, DI-фреймворк или Navigation Compose.
- Не исправлять P1-замечания из ревью, не входящие в P0 (например, `safeDb`, `fireOverdue`, permission-refresh).

## Decisions

### 1. Каналы создаются один раз, текущие каналы не удаляются

`NotificationChannels.createAll` остаётся в `VoiceMindApplication.onCreate`. Вызов из `ReminderNotifier.show` удаляется. В `createAll`:

- убрать проверку `SDK_INT < O` (minSdk 26);
- удалять только legacy-ID: `reminders_default`, `reminders_alarm`, `reminders_silent`, `reminders_vibrate`;
- никогда не удалять текущие `reminders_default_v2`, `reminders_alarm_v2`, `reminders_vibrate_v2`, `reminders_silent_v2`, `reminders_quiet_vibrate_v1`;
- вынести `currentChannelIds` и `legacyChannelIds` во внутренние константы, чтобы unit-тест проверял их непересечение.

**Почему не оставить вызов в notifier:** даже без удаления текущего `VIBRATE` повторный `createAll` на каждое уведомление — лишняя работа и риск побочных эффектов при будущих изменениях каналов. Один вызов на процесс достаточен, потому что каналы живут в системе.

**Альтернатива:** создавать новый ID канала при каждом изменении настроек. Отклонено: это плодит каналы и сбрасывает пользовательские настройки.

### 2. Действия AlarmActivity идут через ReminderActionReceiver

`AlarmActivity` перестаёт вызывать `viewModel.completeReminder/snoozeReminder/cancelReminder` с последующим `finish()`. Вместо этого экран отправляет explicit broadcast в `ReminderActionReceiver` и закрывается. Receiver уже использует `goAsync` и переживает уничтожение Activity.

Добавляем:

- `ACTION_COMPLETE` — вызывает `ReminderRepository.completeReminder` (сохраняет текущую семантику `fireAt = now`, в отличие от `ACTION_DONE`);
- `EXTRA_SNOOZE_MINUTES` — чтобы `AlarmActivity` мог передать 5/10/15/60, а не фиксированные 10 минут;
- helper `ReminderIntents.actionBroadcast(...)`, возвращающий `Intent` для `sendBroadcast`.

`ReminderActionReceiver` оборачивает тело в `try/catch/log` и всегда вызывает `pendingResult.finish()`; при ошибке действие не считается выполненным, но процесс не падает.

**Почему не suspend-методы ViewModel с `finish()` после успеха:** это проще, но операция всё ещё может быть отменена вместе с Activity при повороте/уходе, а обработка ошибок остаётся в UI. Receiver — уже существующий надёжный путь для действий уведомления.

**Альтернатива:** application-scope `CoroutineScope` для команд. Отклонено: это новый компонент и дублирование пути receiver’а.

### 3. Back обрабатывается через OnBackPressedDispatcher

В `AlarmActivity.onCreate` регистрируется `OnBackPressedCallback`, который:

1. останавливает `AlarmSoundPlayer`;
2. отправляет `ACTION_CANCEL` в `ReminderActionReceiver`;
3. вызывает `finish()`.

Устаревший `override fun onBackPressed()` удаляется. Это работает и для кнопки, и для predictive back на Android 16+.

**Альтернатива:** Compose `BackHandler` внутри `AlarmScreen`. Допустимо, но диспетчер Activity надёжнее для случая, когда Compose-контент ещё не загрузился (placeholder/пустой экран).

### 4. Смена deliveryMode пересоздаёт alarm для PENDING

`ReminderRepository.syncAllDeliveryModes(mode)` после `dao.updateAllDeliveryModes`:

1. читает `dao.getAllScheduled()` (только PENDING);
2. для каждой записи вызывает `scheduler.schedule(reminder.copy(deliveryMode = mode.name))`;
3. обновляет виджеты.

`ReminderScheduler.schedule` уже идемпотентен: отменяет старый PendingIntent и создаёт новый с актуальным `EXTRA_DELIVERY_MODE`.

**Почему не `rescheduleAll()`:** он дополнительно пересчитывает `fireAt` для просроченных повторяющихся напоминаний. Смена режима не должна менять расписание.

**Тестируемость:** вводим интерфейс `ReminderAlarmScheduler` (реализация — текущий `ReminderScheduler`), чтобы unit-тест репозитория мог подставить fake и проверить, что `schedule` вызван для каждого PENDING с новым режимом.

### 5. Виджеты обновляются вне main thread

- `WidgetUpdater.updateAll` оборачивается в `withContext(Dispatchers.IO)` и обновляет каждый виджет в `try/catch`, чтобы падение одного не блокировало остальные.
- `ReminderListWidgetProvider.updateWidget` становится `suspend`; `onUpdate` использует `goAsync()` + `CoroutineScope(Dispatchers.IO)`, `runBlocking` удаляется.
- `WidgetToggleReceiver` переводится на `goAsync()` + IO; статус берётся из `reminder.status` после `getById`, а не из `EXTRA_CURRENT_STATUS`.
- Дублирующие `WidgetUpdater.updateAll` из `VoiceMindViewModel` удаляются: репозиторий уже обновляет виджеты после каждой мутации. `MainActivity.onResume` оставляем для обновления относительного времени, но теперь он безопасен.

**Почему не оставить `runBlocking`:** даже на IO это лишняя блокировка потока и скрытая синхронность; `suspend`-цепочка уже есть в `WidgetReminderProvider`.

**Альтернатива:** Glance/WorkManager для виджетов. Отклонено: текущие RemoteViews работают, замена не нужна для P0.

### 6. Проверки

- Unit: `NotificationChannels` — текущие и legacy ID не пересекаются; `ReminderRepository.syncAllDeliveryModes` — `schedule` вызван для PENDING с новым режимом (fake scheduler).
- Instrumented/manual: back на Android 16, действия AlarmActivity при немедленном finish, два уведомления подряд в VIBRATE, виджет при StrictMode.
- `openspec validate --all`, `testDebugUnitTest`, `assembleDebug`, `lintDebug`.

## Risks / Trade-offs

- **Broadcast вместо прямого вызова.** Действие доставляется асинхронно; при мгновенном убийстве процесса сразу после `finish()` теоретически может потеряться. → Receiver использует `goAsync`, а broadcast отправляется explicit intent в тот же процесс; риск ниже, чем отмена `viewModelScope`. Проверить на устройстве.
- **Пересоздание PENDING alarm.** Может на короткое время отменить и снова поставить alarm, если пользователь меняет режим ровно в момент срабатывания. → `schedule` идемпотентен; сценарий редкий, проверить.
- **Удаление legacy-каналов.** Пользовательские настройки старых каналов будут потеряны. → Удаляются только ID, которые приложение больше не использует; текущие каналы не трогаются.
- **Удаление дублирующих widget-update из ViewModel.** Если какой-то путь мутации не проходит через репозиторий, виджет может не обновиться. → Проверить все вызовы `WidgetUpdater` и оставить обновление в репозитории как единственной точке мутаций.
- **`ACTION_COMPLETE` vs `ACTION_DONE`.** Два близких действия могут запутать. → `ACTION_DONE` остаётся для уведомления (не перезаписывает `fireAt`), `ACTION_COMPLETE` — для полноэкранного UI (текущая семантика `completeReminder`); различие зафиксировано в коде и тестах.

## Migration Plan

- Схема Room не меняется, миграции не нужны.
- При обновлении приложения `createAll` удалит только legacy-каналы; текущие каналы сохранят настройки.
- Откат: вернуть предыдущую версию APK; данные и текущие каналы совместимы. Legacy-каналы останутся удалёнными, но приложение их не использует.

## Open Questions

Нет.
