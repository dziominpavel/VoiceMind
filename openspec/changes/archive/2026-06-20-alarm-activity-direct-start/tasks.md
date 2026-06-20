## 1. ReminderAlarmReceiver — прямой startActivity

- [x] 1.1 В `ReminderAlarmReceiver.onReceive` до `goAsync` добавить синхронный вызов `context.startActivity(Intent(context, AlarmActivity::class.java).apply { flags = FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP; putExtra(EXTRA_REMINDER_ID, reminderId) })` для всех срабатываний (решение о показе UI по режиму принимается в `AlarmActivity`).
- [x] 1.2 Обернуть `startActivity` в `try/catch` для обработки `ActivityNotFoundException` и логирования; при ошибке — полагаться на fallback notification.
- [x] 1.3 Убедиться, что после `startActivity` корутина `goAsync` продолжает выполнять WakeLock, `AlarmSoundPlayer.play`/`playVibrationOnly`, обновление статуса/рекуррентности и `ReminderNotifier.show` без изменений.
- [x] 1.4 Проверить, что для рекуррентных напоминаний `body`/`fireAt` для `AlarmActivity` берутся из оригинального `reminder` ДО вызова `RecurrenceCalculator.nextOccurrence` и `updateAndSchedule`.

## 2. AlarmActivity — загрузка данных через ViewModel

- [x] 2.1 Изменить `AlarmActivity.onCreate`: убрать чтение `EXTRA_REMINDER_BODY` и `EXTRA_REMINDER_FIRE_AT` из intent (оставить только `EXTRA_REMINDER_ID`); загружать `body`/`fireAt` через `VoiceMindViewModel` по `reminderId` (suspend-вызов, показать placeholder до загрузки).
- [x] 2.2 Добавить в `VoiceMindViewModel` метод `loadReminderForAlarm(reminderId: Long): Reminder?` (или использовать существующий `getById` через `safeDb`), возвращающий `Reminder` для `AlarmActivity`.
- [x] 2.3 В `AlarmActivity` использовать `produceState`/`LaunchedEffect` для асинхронной загрузки `Reminder` по `reminderId` и передачи в `AlarmScreen`.
- [x] 2.4 В `AlarmActivity.onCreate` через `ViewModel` определить `deliveryMode` текущего напоминания (или global default из `SettingsRepository`); если NOTIFICATION/SILENT — вызвать `finish()` сразу, не показывая UI (полагаемся на notification).
- [x] 2.5 Сохранить `EXTRA_REMINDER_BODY` и `EXTRA_REMINDER_FIRE_AT` в `companion object` для обратной совместимости с `ReminderNotifier.fullScreenIntent` (screen-off ветка использует extras из notification intent).

## 3. AlarmActivity — onNewIntent

- [x] 3.1 Переопределить `onNewIntent(intent: Intent?)` в `AlarmActivity`.
- [x] 3.2 В `onNewIntent` извлечь новый `EXTRA_REMINDER_ID`, обновить Compose-state (`mutableStateOf` для `reminderId`, перезапустить `LaunchedEffect` загрузки).
- [x] 3.3 В `onNewIntent` проверить `isFinishing` — если активность завершается, игнорировать новый intent.
- [x] 3.4 В `onNewIntent` перезапустить `AlarmSoundPlayer.play(...)` с новыми параметрами (без явного `stop`, т.к. `play` уже вызывает `stop` внутри).
- [x] 3.5 Убедиться, что `viewModel` в `AlarmActivity` — общий с `MainActivity` через `ViewModelProvider`/Activity scope, чтобы действия (dismiss/snooze/cancel) шли через ту же логику.

## 4. ReminderNotifier — fallback без структурных изменений

- [x] 4.1 Убедиться, что `ReminderNotifier.show()` для ALARM/VIBRATE продолжает создавать notification с `setFullScreenIntent` (для screen-off/locked кейса).
- [x] 4.2 Убедиться, что notification actions (Готово/Отложить/Отменить) остаются — это fallback для OEM-блокировок прямого `startActivity`.
- [x] 4.3 Не удалять `EXTRA_REMINDER_BODY`/`EXTRA_REMINDER_FIRE_AT` из `fullScreenIntent` extras — они используются, когда `AlarmActivity` запускается системой из notification (screen-off), а не из ресивера.

## 5. Согласование порядка операций с рекуррентностью

- [x] 5.1 В корутине `ReminderAlarmReceiver` убедиться, что `ReminderNotifier.show(reminder)` вызывается с **оригинальным** `reminder` (до `updateAndSchedule` для рекуррентных), чтобы notification и `AlarmActivity` показывали текущее срабатывание.
- [x] 5.2 Если `updateAndSchedule` уже сдвинул `reminder.fireAt` к моменту `show` — передавать в `show` копию оригинального reminder (snapshot до обновления).

## 6. Сборка и unit-тесты

- [x] 6.1 Запустить `./gradlew assembleDebug` — убедиться, что сборка проходит без ошибок.
- [x] 6.2 Запустить `./gradlew test` — убедиться, что существующие unit-тесты проходят.
- [x] 6.3 Добавить unit-test: `ReminderAlarmReceiver` формирует intent с `EXTRA_REMINDER_ID` для `AlarmActivity` (если тестируемо через Robolectric); иначе — manual.
- [x] 6.4 Добавить unit-test: `AlarmActivity.onNewIntent` обновляет `reminderId` state (если тестируемо); иначе — manual.

## 7. Manual-тестирование

- [ ] 7.1 ALARM при включённом и разблокированном экране, в обычном приложении (например, браузер) → `AlarmActivity` всплывает поверх, звук играет, кнопки работают.
- [ ] 7.2 ALARM при включённом экране в immersive-приложении (игра/YouTube fullscreen) → `AlarmActivity` всплывает поверх, аналогично системному будильнику.
- [ ] 7.3 ALARM при выключенном экране → экран включается, `AlarmActivity` поверх блокировки (поведение `fullScreenIntent` сохранено).
- [ ] 7.4 ALARM при заблокированном экране → UI поверх keyguard, действия без разблокировки.
- [ ] 7.5 VIBRATE при включённом экране → `AlarmActivity` всплывает, только вибрация (без звука).
- [ ] 7.6 NOTIFICATION при включённом экране → `AlarmActivity` НЕ появляется, heads-up в шторке.
- [ ] 7.7 SILENT при включённом экране → `AlarmActivity` НЕ появляется, тихое уведомление.
- [ ] 7.8 Повторное срабатывание: запустить два ALARM с разницей 30 секунд → второе срабатывание обновляет `body`/`fireAt` в уже открытой `AlarmActivity` через `onNewIntent`, звук перезапускается.
- [ ] 7.9 Рекуррентное напоминание «каждый день в 9:00» → `AlarmActivity` показывает «сегодня 09:00», а не «завтра 09:00».
- [ ] 7.10 Нажатие «Готово»/«Отложить»/«Отменить» на `AlarmActivity` → звук останавливается, статус меняется, активность закрывается.
- [ ] 7.11 Нажатие системной кнопки «Назад» → звук останавливается, статус CANCELLED, активность закрывается.
- [ ] 7.12 Сворачивание `AlarmActivity` кнопкой питания → `onPause`/`onDestroy` останавливают звук, notification остаётся в шторке с actions.

## 8. Финализация

- [x] 8.1 Запустить `openspec validate alarm-activity-direct-start` — убедиться, что change валиден.
- [x] 8.2 Проверить, что `openspec/specs/alarm-screen-wake/spec.md` после применения delta остаётся консистентным (нет конфликтов с существующими requirements).
- [x] 8.3 Обновить `docs/ARCHITECTURE.md` и `docs/NOTIFICATION_MODES.md`, если в них описан механизм запуска `AlarmActivity` (упомянуть прямой `startActivity` как основной механизм для включённого экрана).
