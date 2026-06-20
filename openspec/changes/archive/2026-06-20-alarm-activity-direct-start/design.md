## Context

Текущая реализация ALARM/VIBRATE в `ReminderAlarmReceiver` и `ReminderNotifier` использует только `NotificationCompat.Builder.setFullScreenIntent()`. Согласно документации Android, `fullScreenIntent` запускает Activity **только если экран выключен или заблокирован**. При включённом и разблокированном экране система показывает heads-up уведомление, а не запускает Activity — это сознательное поведение платформы, чтобы приложения не перехватывали экран во время активного использования.

В результате пользователь, у которого сработал будильник VoiceMind во время использования телефона, видит только heads-up в шторке. Чтобы остановить звук, нужно смахнуть уведомление — это неудобно и не соответствует поведению системного «Будильника», который сразу открывает полноэкранное окно поверх любого приложения.

Существующая инфраструктура уже готова:
- `AlarmActivity` (`launchMode="singleTask"`, `showOnLockScreen`, `turnScreenOn`, `FLAG_KEEP_SCREEN_ON`) с Compose-экраном и тремя действиями.
- `USE_FULL_SCREEN_INTENT` permission в манифесте + runtime-запрос на API 34+.
- `AlarmSoundPlayer` с автостопом через 60 секунд.
- Спецификация `openspec/specs/alarm-screen-wake/spec.md`.

Ключевые ограничения платформы:
- Android 10+ (API 29+): background-activity-start блок — `startActivity` из `BroadcastReceiver` разрешён только синхронно в `onReceive` (ресивер в этот момент в allowlist'е). После `goAsync()` и перехода в корутину старт активити классифицируется как background и может быть заблокирован.
- `USE_FULL_SCREEN_INTENT` — не нужен для прямого `startActivity` (это запуск своей собственной Activity, не overlay). Нужен только для notification-варианта при screen-off.
- `SYSTEM_ALERT_WINDOW` — не нужен по той же причине.

## Goals / Non-Goals

**Goals:**
- При срабатывании ALARM или VIBRATE `AlarmActivity` всплывает поверх любого приложения, включая immersive (игры/видео), аналогично системному будильнику.
- Прямой `startActivity` из ресивера работает на API 26+ (minSdk).
- `AlarmActivity` корректно обрабатывает повторные срабатывания (`onNewIntent`): обновляет `body`/`fireAt`, перезапускает звук.
- Для рекуррентных напоминаний UI показывает текущее срабатывание, а не следующее.
- `ReminderNotifier.show()` остаётся как fallback (notification с actions + `fullScreenIntent` для screen-off).
- Поведение для NOTIFICATION и SILENT не меняется.

**Non-Goals:**
- Не запрашиваем `RoleManager.ROLE_ALARM_CLOCK` — VoiceMind остаётся напоминалкой, а не системным будильником по умолчанию (это нарушило бы UX пользователей, использующих отдельное app-будильник).
- Не реализуем обнаружение immersive-режима foreground-приложения для выборочного подавления окна — это требует `PACKAGE_USAGE_STATS` (special access) и не оправдано. Окно всплывает всегда при ALARM/VIBRATE, как системный будильник.
- Не меняем `AlarmActivity` визуально (тема, кнопки, layout остаются).
- Не добавляем выбор мелодии из `AlarmActivity`.
- Не меняем поведение для NOTIFICATION / SILENT.

## Decisions

### 1. Путь A: прямой `startActivity` из ресивера, без `ROLE_ALARM_CLOCK`

- **Выбор**: В `ReminderAlarmReceiver.onReceive` для `deliveryMode ∈ {ALARM, VIBRATE}` синхронно (до `goAsync`) вызывать `context.startActivity(Intent(context, AlarmActivity::class.java).addFlags(NEW_TASK | CLEAR_TOP).putExtras(...))`.
- **Почему**: Решает ровно проблему «окно не всплывает при включённом экране» — без новых permissions, без системных диалогов, без конфликта с настоящим будильником. `USE_FULL_SCREEN_INTENT` и `SYSTEM_ALERT_WINDOW` не нужны для этого пути.
- **Альтернатива B — `RoleManager.ROLE_ALARM_CLOCK`**: Отвергнуто. Роль делает VoiceMind «будильником по умолчанию» в системе — голосовой ассистент начнёт открывать VoiceMind вместо Часов, системный будильник потеряет роль, появится системный диалог «Сделать VoiceMind приложением для будильников?». Семантически VoiceMind — напоминалка, не будильник; забирать системную роль — UX-насилие.
- **Альтернатива C — A+B**: Отвергнуто. Если пользователь откажется от роли (что вероятно, т.к. у него уже есть будильник), всё равно падаем в путь A. Зачем тогда спрашивать роль.

### 2. `startActivity` синхронно в `onReceive`, до `goAsync`

- **Выбор**: Старт активити выполняется в самом `onReceive` до вызова `goAsync()`. После старта активити `onReceive` продолжает: вызывает `goAsync()`, запускает корутину для WakeLock, `AlarmSoundPlayer.play`, обновления статуса/рекуррентности, `ReminderNotifier.show`.
- **Почему**: На Android 10+ background-activity-start блок. Ресивер в момент `onReceive` находится в allowlist'е для старта активити. После `goAsync()` и перехода в корутину на `Dispatchers.IO` старт классифицируется как background и может быть заблокирован системой.
- **Альтернатива**: Старт из корутины после `goAsync`. Отвергнуто: нестабильно на Android 10+, особенно на OEM-сборках (Xiaomi, Huawei).

### 3.Extras формируются из оригинального reminder до сдвига рекуррентности

- **Выбор**: В `onReceive` сначала загружается `reminder` из репозитория, из него формируются extras (`reminderId`, `reminder.body`, `reminder.fireAt`) и стартует `AlarmActivity`. Только **после** этого корутина считает `nextFireAt` для рекуррентных и обновляет запись.
- **Почему**: Сейчас в ресивере для рекуррентных сразу считается `nextFireAt` и `updateAndSchedule`, статус остаётся PENDING. Если extras формировать после обновления — UI покажет следующее срабатывание, а не текущее. Это критично: пользователь увидит «завтра 9:00» вместо «сегодня 9:00», которое только что сработало.
- **Альтернатива**: Формировать extras из `reminder.fireAt` после `updateAndSchedule`. Отвергнуто: показывает будущее событие вместо текущего.

### 4. `AlarmActivity.onNewIntent` — обновление state и перезапуск звука

- **Выбор**: `AlarmActivity` (`launchMode="singleTask"`) переопределяет `onNewIntent(intent)`: извлекает новые `EXTRA_REMINDER_ID`/`EXTRA_REMINDER_BODY`/`EXTRA_REMINDER_FIRE_AT`, обновляет Compose-state (через `mutableStateOf` поля активности или `SnapshotStateList`), вызывает `AlarmSoundPlayer.stop(this)` затем `AlarmSoundPlayer.play(...)` для перезапуска звука с новыми параметрами.
- **Почему**: При `singleTask` второе срабатывание не создаёт новую активность, а доносит новый intent в `onNewIntent`. Без обработки UI останется со старым `body`/`fireAt`, а пользователь не поймёт, какое напоминание сработало.
- **Альтернатива**: `launchMode="standard"` — каждая активность своя. Отвергнуто: множит активности в backstack, конфликтует с `excludeFromRecents` и `taskAffinity=""`.

### 5. `ReminderNotifier.show()` остаётся как fallback

- **Выбор**: `ReminderNotifier.show()` не удаляется и не меняется структурно. Notification с actions (Готово/Отложить/Отменить) и `fullScreenIntent` остаётся.
- **Почему**: Покрывает screen-off/locked кейс (там `fullScreenIntent` действительно запускает Activity). Служит fallback, если прямой `startActivity` заблокирован OEM-ограничениями (некоторые OEM-сборки агрессивно глушат старт активити из ресивера). Notification с actions также остаётся в шторке после закрытия `AlarmActivity` — пользователь может вернуться к нему.
- **Альтернатива**: Удалить `fullScreenIntent` из notification, оставить только прямой старт. Отвергнуто: теряется screen-off ветка и fallback.

### 6. Порядок операций в `onReceive`

```
   ReminderAlarmReceiver.onReceive(context, intent):
   ┌──────────────────────────────────────────────────────────────┐
   │ 1. reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1)   │
   │ 2. if (reminderId < 0) return                                │
   │ 3. reminder = repo.getById(reminderId)  // синхронно?        │
   │    → НЕТ: getById suspend. Решение: старт активити с extras   │
   │      из intent ресивера (reminderId уже есть), body/fireAt   │
   │      подгружаются в AlarmActivity из ViewModel.               │
   │ 4. deliveryMode — тоже suspend; решение: стартовать активити  │
   │     всегда для ALARM/VIBRATE-канала, определять режим внутри  │
   │     AlarmActivity через ViewModel.                            │
   │      → Уточнение: см. Open Question 1.                        │
   │ 5. context.startActivity(AlarmActivity, NEW_TASK|CLEAR_TOP,  │
   │      extras: reminderId)                                     │
   │ 6. goAsync() → корутина:                                      │
   │      a. WakeLock (для ALARM/VIBRATE)                          │
   │      b. AlarmSoundPlayer.play / playVibrationOnly            │
   │      c. рекуррентность: nextFireAt + updateAndSchedule        │
   │         ИЛИ markFiredAndShow                                  │
   │      d. ReminderNotifier.show (notification + fullScreenIntent)│
   │ 7. finally: release WakeLock, pendingResult.finish()         │
   └──────────────────────────────────────────────────────────────┘
```

**Уточнение по шагу 3-4**: `ReminderRepository.getById` и `SettingsRepository.getDefaultDeliveryMode` — suspend-функции. В синхронной части `onReceive` их звать нельзя. Решение: `AlarmActivity` принимает только `reminderId` через extras, а `body`/`fireAt`/`deliveryMode` загружает внутри `onCreate`/`onNewIntent` через `VoiceMindViewModel`. Это развязывает синхронный старт активити с асинхронной загрузкой данных.

## Risks / Trade-offs

- **[Risk]** Background-activity-start блок на Android 10+ при старте из корутины. → **Mitigation**: `startActivity` зовётся синхронно в `onReceive` до `goAsync`. Ресивер в этот момент в allowlist'е.
- **[Risk]** OEM-ограничения (Xiaomi, Huawei, OPPO) могут блокировать прямой старт активити из ресивера даже синхронно. → **Mitigation**: `ReminderNotifier.show()` остаётся как fallback — notification с `fullScreenIntent` (для screen-off) и heads-up (для screen-on) с actions. Пользователь может остановить звук через actions в шторке.
- **[Risk]** Immersive-приложения (игры, видео) будут перебиваться полноэкранным окном. → **Mitigation**: Не митигируется — это сознательное поведение, аналогичное системному будильнику. Пользователь явно выбрал режим ALARM/VIBRATE, ожидая настойчивое оповещение.
- **[Risk]** `onNewIntent` может прийти во время анимации закрытия `AlarmActivity` после нажатия «Готово» — состояние гонки. → **Mitigation**: В `onNewIntent` проверять, не завершается ли активность (`isFinishing`); если завершается — игнорировать новый intent (звук и статус обработает корутина ресивера, notification остаётся).
- **[Risk]** Звук может не перезапуститься в `onNewIntent`, если `AlarmSoundPlayer.stop` и `play` вызываются слишком быстро. → **Mitigation**: `AlarmSoundPlayer.play` уже вызывает `stop` в начале; в `onNewIntent` вызывать только `play` с новыми параметрами, без явного `stop`.
- **[Trade-off]** `AlarmActivity` теперь загружает `body`/`fireAt` из ViewModel вместо intent extras — лишний запрос к БД при старте. → **Mitigation**: Запрос по primary key, миллисекунды. Зато развязывает синхронный старт с suspend-загрузкой.
- **[Trade-off]** Для рекуррентных напоминаний `AlarmActivity` покажет `body`/`fireAt` текущего срабатывания, но в БД запись уже сдвинута на `nextFireAt`. Если пользователь откроет список напоминаний во время показа `AlarmActivity` — увидит следующее срабатывание. → **Mitigation**: Это ожидаемое поведение; `AlarmActivity` показывает «что сработало», список показывает «что запланировано».

## Migration Plan

Нет destructive migration. Нет изменений схемы БД. Нет новых permissions. Изменения чисто кодовые в `ReminderAlarmReceiver` и `AlarmActivity`. Приложение продолжит работать на старых версиях без миграции.

**Rollback**: revert изменений в двух файлах. `fullScreenIntent` в notification продолжит работать для screen-off кейса.

## Open Questions

1. **Загрузка `body`/`fireAt` в `AlarmActivity`**: Сейчас `AlarmActivity` принимает `body`/`fireAt` через intent extras (синхронно в `onCreate`). Если стартовать активити синхронно в `onReceive` с только `reminderId`, то `body`/`fireAt` надо грузить через `VoiceMindViewModel` асинхронно — будет короткий flash пустого экрана. **Решение для proposal**: Передавать в extras то, что уже есть в intent ресивера (`reminderId`), а `body`/`fireAt` грузить через ViewModel. Альтернатива — расширить intent ресивера, чтобы `ReminderScheduler` клал `body`/`fireAt` в extras при планировании (тогда они доступны синхронно). Это решение принимается на этапе tasks — оба варианта реализуемы.

2. **Определение `deliveryMode` в синхронной части `onReceive`**: `SettingsRepository.getDefaultDeliveryMode()` — suspend. Чтобы решить, стартовать ли `AlarmActivity`, нужно знать режим синхронно. **Решение для proposal**: Стартовать `AlarmActivity` всегда при срабатывании (для всех режимов), а внутри активити через ViewModel решить — показывать полноэкранный UI (ALARM/VIBRATE) или сразу `finish()` и положиться на notification (NOTIFICATION/SILENT). Альтернатива — кэшировать `deliveryMode` в non-suspend кэше `SettingsRepository`. Решение принимается на этапе tasks.
