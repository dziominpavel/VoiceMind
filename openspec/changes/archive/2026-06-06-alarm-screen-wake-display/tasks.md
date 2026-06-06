## 1. Permissions и Manifest

- [x] 1.1 Добавить `WAKE_LOCK` permission в `AndroidManifest.xml`.
- [x] 1.2 Добавить `USE_FULL_SCREEN_INTENT` permission в `AndroidManifest.xml`.
- [x] 1.3 Объявить `AlarmActivity` в `AndroidManifest.xml` с `showOnLockScreen`, `turnScreenOn`, `excludeFromRecents`, `launchMode="singleTask"`, `taskAffinity=""`.
- [x] 1.4 Добавить `android:theme="@style/Theme.VoiceMind.Alarm"` для `AlarmActivity`.
- [x] 1.5 Создать/обновить тему `Theme.VoiceMind.Alarm` в `themes.xml` (fullscreen, no action bar, keep screen on, transparent status/nav).

## 2. AlarmActivity — полноэкранный UI

- [x] 2.1 Создать `AlarmActivity.kt` (ComponentActivity) с `setShowWhenLocked(true)` и `setTurnScreenOn(true)` для API 27+.
- [x] 2.2 Добавить `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON` для всех API.
- [x] 2.3 Создать composable `AlarmScreen(body: String, fireAt: Long, onDone, onSnooze, onCancel)`.
- [x] 2.4 В `AlarmScreen` отобразить `body` крупным шрифтом (`headlineLarge`), время срабатывания под ним, и три кнопки действий.
- [x] 2.5 При любом действии или `onPause`/`onDestroy` вызывать `AlarmSoundPlayer.stop()`.
- [x] 2.6 При нажатии системной кнопки «Назад» — остановить звук и закрыть активность (эквивалент «Отменить»).

## 3. Звук и пробуждение

- [x] 3.1 В `ReminderAlarmReceiver` для ALARM: запросить WakeLock (`PowerManager.ACQUIRE_CAUSES_WAKEUP` + `ON_AFTER_RELEASE`) на 10 секунд перед вызовом `AlarmSoundPlayer.play`.
- [x] 3.2 Убедиться, что WakeLock освобождается в `finally` блоке.
- [x] 3.3 Убедиться, что `AlarmSoundPlayer.stop()` вызывается при старте `AlarmActivity` и при её закрытии.

## 4. Full-screen intent в Notification

- [x] 4.1 В `ReminderNotifier.show()` для ALARM: создать `PendingIntent.getActivity` на `AlarmActivity` (с extras: reminderId, body, fireAt).
- [x] 4.2 Добавить `builder.setFullScreenIntent(fullScreenPendingIntent, true)` для ALARM.
- [x] 4.3 Убедиться, что канал `reminders_alarm` имеет `setBypassDnd(true)` и высокий приоритет.
- [x] 4.4 Убедиться, что `USE_FULL_SCREEN_INTENT` запрашивается на API 34+ (runtime permission) при необходимости; добавить в `ReminderPermissions` проверку.

## 5. ViewModel и обработка действий

- [x] 5.1 Добавить в `VoiceMindViewModel` методы: `dismissAlarm(reminderId)`, `snoozeAlarm(reminderId)`, `cancelAlarm(reminderId)`.
- [x] 5.2 Реализовать `dismissAlarm` → `repo.markDismissed()` + `AlarmSoundPlayer.stop()` + `ReminderScheduler.cancel()`.
- [x] 5.3 Реализовать `snoozeAlarm` → `repo.snooze(reminderId, +10min)` + `AlarmSoundPlayer.stop()` + `ReminderScheduler.schedule()`.
- [x] 5.4 Реализовать `cancelAlarm` → `repo.markCancelled()` + `AlarmSoundPlayer.stop()` + `ReminderScheduler.cancel()`.
- [x] 5.5 `AlarmActivity` передаёт reminderId через intent extras, извлекает его и подключается к `VoiceMindViewModel`.

## 6. Strings и локализация

- [x] 6.1 Добавить строки в `strings.xml`: `alarm_screen_title`, `alarm_screen_done`, `alarm_screen_snooze`, `alarm_screen_cancel`, `alarm_screen_back_press_cancel`.

## 7. Тестирование

- [x] 7.1 Unit-test: `AlarmActivity` получает правильные extras из Intent.
- [x] 7.2 Manual test: ALARM с выключенным экраном → экран включается, текст виден, кнопки работают.
- [x] 7.3 Manual test: ALARM на заблокированном экране → UI поверх keyguard, действия без разблокировки.
- [x] 7.4 Manual test: Нажатие кнопки питания → звук останавливается, notification остаётся.
- [x] 7.5 Manual test: NOTIFICATION / VIBRATE_ONLY / SILENT — поведение не изменилось.
