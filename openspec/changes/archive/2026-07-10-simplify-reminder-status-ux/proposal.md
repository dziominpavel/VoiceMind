## Why

Вкладка «История» и кнопки уведомления путают пользователя: подпись «просрочено» показывается почти у всех завершённых напоминаний (это не статус, а `fireAt` в прошлом), «Готово» и «Отменить» выглядят одинаково «убери уведомление», а цвета/зачёркивание карточек не подписаны. Нужна единая понятная модель статусов и их отображения.

## What Changes

- Зафиксировать пользовательскую модель из 4 статусов: `PENDING` / `TRIGGERED` / `DONE` / `CANCELLED` с ясными русскими лейблами.
- В Истории **запретить** подпись «просрочено»; показывать статус + короткую дату (`вчера`, `3 июн`).
- «Просрочено» оставить только для живых `PENDING` во вкладке Предстоящие.
- Переименовать действие уведомления/alarm «Готово» → «Выполнено» (семантика DONE).
- На карточке Истории явно показывать статус текстом; визуал упростить до трёх сигналов: выполнено / отменено / без ответа.
- Синхронизировать specs с реальными именами статусов (`PENDING`/`TRIGGERED`/`DONE`, не устаревшие `SCHEDULED`/`FIRED`/`DISMISSED` в UI-требованиях).

## Capabilities

### New Capabilities

<!-- нет — меняем существующие UI и delivery -->

### Modified Capabilities

- `ui-screens`: подписи и визуал карточек Предстоящих/Истории; urgency vs terminal-статусы; лейблы статусов.
- `notification-delivery`: переименование действия «Готово» → «Выполнено»; уточнение семантики DONE vs CANCELLED; актуальные имена статусов в сценариях.
- `alarm-screen-wake`: те же лейблы кнопок на полноэкранном UI.

## Impact

- UI: `ReminderListScreen` (HistoryReminderCard), `FormatUtils`, `ReminderDetailScreen` (StatusBadge), strings.
- Notification/Alarm: `ReminderNotifier`, `AlarmActivity`, `strings.xml` (`notification_action_*`, `alarm_screen_*`, `detail_done`).
- Specs: `ui-screens`, `notification-delivery`, `alarm-screen-wake` — выравнивание имён статусов.
- Модель данных `ReminderStatus` и переходы статусов **не меняются** (без миграции Room).
