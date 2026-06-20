## Purpose

Пробуждение экрана и полноэкранный UI при срабатывании напоминания в режиме ALARM.

## Requirements

### Requirement: Пробуждение экрана при ALARM
При срабатывании напоминания с deliveryMode ALARM система MUST включать экран устройства, если он выключен или заблокирован.

#### Scenario: Экран выключен
- **WHEN** напоминание ALARM срабатывает, а экран устройства выключен
- **THEN** экран включается
- **AND** отображается полноэкранный UI с текстом напоминания

#### Scenario: Экран заблокирован
- **WHEN** напоминание ALARM срабатывает на заблокированном экране
- **THEN** UI отображается поверх экрана блокировки (keyguard)
- **AND** пользователь может взаимодействовать с кнопками без разблокировки

### Requirement: Полноэкранный UI текста напоминания
При срабатывании ALARM система MUST отображать крупный текст напоминания и время срабатывания на весь экран.

#### Scenario: Отображение текста
- **WHEN** запускается полноэкранный UI ALARM
- **THEN** в центре экрана отображается `body` напоминания крупным шрифтом
- **AND** под текстом показывается время срабатывания (`FormatUtils.formatFireAt`)
- **AND** UI использует тёмную тему CLEAR BELL

### Requirement: Действия на полноэкранном UI
На полноэкранном UI ALARM MUST быть доступны три действия: «Готово», «Отложить 10 мин», «Отменить».

#### Scenario: Нажатие «Готово»
- **WHEN** пользователь нажимает «Готово» на полноэкранном UI
- **THEN** статус напоминания меняется на DISMISSED
- **AND** alarm звук останавливается
- **AND** полноэкранный UI закрывается

#### Scenario: Нажатие «Отложить»
- **WHEN** пользователь нажимает «Отложить 10 мин» на полноэкранном UI
- **THEN** напоминание перепланируется на now + 10 минут
- **AND** статус меняется на SNOOZED, затем SCHEDULED
- **AND** alarm звук останавливается
- **AND** полноэкранный UI закрывается

#### Scenario: Нажатие «Отменить»
- **WHEN** пользователь нажимает «Отменить» на полноэкранном UI
- **THEN** статус напоминания меняется на CANCELLED
- **AND** alarm звук останавливается
- **AND** полноэкранный UI закрывается

### Requirement: Full-screen intent notification
Для `deliveryMode` ALARM и VIBRATE система MUST устанавливать `fullScreenIntent` в notification как fallback для кейса выключенного/заблокированного экрана и для OEM-сборок, блокирующих прямой `startActivity` из ресивера. Прямой `startActivity` из `ReminderAlarmReceiver.onReceive` — основной механизм запуска `AlarmActivity` при включённом экране; `fullScreenIntent` — дополнительный для screen-off/locked.

#### Scenario: System full-screen intent при выключенном экране
- **WHEN** AlarmManager доставляет intent для ALARM, а экран выключен или заблокирован
- **THEN** `ReminderNotifier` создаёт уведомление с `setFullScreenIntent`
- **AND** система запускает `AlarmActivity` поверх экрана блокировки
- **AND** прямой `startActivity` из ресивера также срабатывает (дублирующий запуск подавляется `launchMode="singleTask"` + `onNewIntent`)

#### Scenario: Fallback при блокировке прямого startActivity
- **WHEN** OEM-ограничение блокирует прямой `startActivity` из `ReminderAlarmReceiver` при включённом экране
- **THEN** notification с actions (Готово/Отложить/Отменить) остаётся в шторке
- **AND** пользователь может остановить звук через actions в шторке

### Requirement: Permissions для пробуждения
Приложение MUST иметь все необходимые permissions для пробуждения экрана и full-screen intent.

#### Scenario: Permissions в манифесте
- **WHEN** приложение установлено
- **THEN** AndroidManifest содержит `WAKE_LOCK`
- **AND** AndroidManifest содержит `USE_FULL_SCREEN_INTENT`
- **AND** `AlarmActivity` объявлена с `android:showOnLockScreen="true"` и соответствующими flags

### Requirement: Прямой запуск AlarmActivity при включённом экране
При срабатывании напоминания с `deliveryMode` ALARM или VIBRATE система MUST запускать `AlarmActivity` напрямую через `startActivity` из `ReminderAlarmReceiver.onReceive` синхронно (до `goAsync`), чтобы полноэкранный UI появлялся поверх любого приложения, включая immersive (игры/видео), а не только через `fullScreenIntent` notification.

#### Scenario: Экран включён, разблокирован, обычное приложение
- **WHEN** напоминание ALARM срабатывает при включённом и разблокированном экране, пока пользователь в обычном приложении
- **THEN** `ReminderAlarmReceiver` синхронно в `onReceive` вызывает `context.startActivity(Intent(context, AlarmActivity::class.java))`
- **AND** `AlarmActivity` появляется поверх текущего приложения
- **AND** звук/вибрация запускаются корутиной ресивера после `goAsync`

#### Scenario: Immersive-приложение (игра/видео)
- **WHEN** напоминание ALARM срабатывает, пока foreground-приложение в immersive fullscreen-режиме
- **THEN** `AlarmActivity` всё равно появляется поверх immersive-приложения
- **AND** поведение аналогично системному «Будильнику»

#### Scenario: Режим VIBRATE
- **WHEN** напоминание VIBRATE срабатывает при включённом экране
- **THEN** `AlarmActivity` появляется поверх текущего приложения
- **AND** запускается только вибрация (без звука)

#### Scenario: Режимы NOTIFICATION и SILENT
- **WHEN** напоминание NOTIFICATION или SILENT срабатывает при включённом экране
- **THEN** `AlarmActivity` НЕ запускается напрямую
- **AND** система показывает heads-up (NOTIFICATION) или тихое уведомление (SILENT) как раньше

### Requirement: Обработка onNewIntent в AlarmActivity
`AlarmActivity` (с `launchMode="singleTask"`) MUST обрабатывать `onNewIntent`: при повторном срабатывании другого напоминания обновлять отображаемые `body` и `fireAt` и перезапускать звук/вибрацию с новыми параметрами.

#### Scenario: Второе срабатывание во время показа первого
- **WHEN** `AlarmActivity` уже на экране и срабатывает второе напоминание ALARM
- **THEN** система доносит новый intent в `onNewIntent` (а не создаёт новую активность)
- **AND** `AlarmActivity` обновляет отображаемые `body` и `fireAt` на данные второго напоминания
- **AND** `AlarmSoundPlayer` перезапускается с параметрами второго напоминания

#### Scenario: onNewIntent во время завершения активности
- **WHEN** `onNewIntent` приходит, пока `AlarmActivity` завершается (`isFinishing == true`) после нажатия «Готово»/«Отменить»/«Отложить»
- **THEN** `AlarmActivity` игнорирует новый intent
- **AND** звук и статус второго напоминания обрабатываются корутиной ресивера и notification с actions

### Requirement: Extras из текущего срабатывания для рекуррентных напоминаний
Для рекуррентных напоминаний `AlarmActivity` MUST отображать `body` и `fireAt` текущего срабатывания (которое только что сработало), а не следующее запланированное. Ресивер MUST формировать данные для `AlarmActivity` из оригинального reminder ДО вычисления `nextFireAt` и обновления записи.

#### Scenario: Рекуррентное напоминание «каждый день в 9:00»
- **WHEN** срабатывает рекуррентное напоминание с `fireAt` = сегодня 09:00 и `recurrenceRule` = daily
- **THEN** `AlarmActivity` показывает `body` и время «сегодня 09:00»
- **AND** корутина ресивера параллельно считает `nextFireAt` = завтра 09:00 и обновляет запись
- **AND** пользователь видит текущее срабатывание, а не завтрашнее
