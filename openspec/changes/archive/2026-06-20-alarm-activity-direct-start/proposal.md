## Why

При срабатывании напоминания в режимах ALARM/VIBRATE пользователь видит только heads-up уведомление в шторке, если экран включён и разблокирован. Чтобы остановить будильник, приходится смахивать уведомление влево — это неудобно и не соответствует поведению системного «Будильника», который сразу открывает полноэкранное окно поверх любого приложения. Существующий `fullScreenIntent` в notification срабатывает только при выключенном/заблокированном экране — на включённом экране Android принципиально показывает heads-up, а не запускает Activity.

## What Changes

- `ReminderAlarmReceiver` для `deliveryMode ∈ {ALARM, VIBRATE}` синхронно в `onReceive` (до `goAsync`) запускает `AlarmActivity` напрямую через `context.startActivity(...)`, чтобы окно всплывало поверх любого приложения, включая immersive (игры/видео) — аналогично системному будильнику.
- `AlarmActivity` обрабатывает `onNewIntent`: при повторном срабатывании (другое напоминание, `launchMode="singleTask"`) обновляет `body`/`fireAt` и перезапускает звук, иначе второе напоминание покажет текст первого.
- Порядок операций в ресивере уточнён: extras для `AlarmActivity` (`body`, `fireAt`) формируются из **оригинального** reminder **до** обновления рекуррентного `nextFireAt`, чтобы UI показывал текущее срабатывание, а не следующее.
- `ReminderNotifier.show()` остаётся как fallback: notification с actions (Готово/Отложить/Отменить) и `fullScreenIntent` для screen-off/locked кейса, а также на случай, если прямой `startActivity` заблокирован OEM-ограничениями.
- Поведение для `NOTIFICATION` и `SILENT` не меняется — heads-up / тихое уведомление, без полноэкранного окна.
- Permission `USE_FULL_SCREEN_INTENT` остаётся нужен только для screen-off ветки (notification `fullScreenIntent`); прямой `startActivity` его не требует.

## Capabilities

### New Capabilities
<!-- Нет новых capabilities — расширяется существующая alarm-screen-wake. -->

### Modified Capabilities
- `alarm-screen-wake`: добавляется требование прямого запуска `AlarmActivity` из ресивера при включённом экране (а не только через `fullScreenIntent`); уточняется обработка `onNewIntent` для повторных срабатываний; уточняется требование, что extras формируются из текущего срабатывания до сдвига рекуррентного `fireAt`.

## Impact

- **Код**:
  - `data/scheduling/ReminderAlarmReceiver.kt` — добавление синхронного `startActivity` до `goAsync`; уточнение порядка операций с рекуррентными напоминаниями.
  - `ui/screens/AlarmActivity.kt` — реализация `onNewIntent`, обновление state (body/fireAt) и перезапуск `AlarmSoundPlayer`.
  - `data/notification/ReminderNotifier.kt` — без структурных изменений; остаётся как fallback.
- **Manifest**: без изменений (`AlarmActivity` уже объявлена с `showOnLockScreen`, `turnScreenOn`, `launchMode="singleTask"`, `excludeFromRecents`).
- **Permissions**: без новых permissions. `USE_FULL_SCREEN_INTENT` остаётся для screen-off ветки.
- **Тестирование**: manual-сценарии — ALARM при включённом экране в обычном приложении / в immersive (игра/видео) / при выключенном экране / при заблокированном; VIBRATE при включённом экране; повторное срабатывание (onNewIntent); рекуррентное напоминание (UI показывает текущее, а не следующее).
- **Риски**: на Android 10+ background-activity-start блок — митигируется вызовом `startActivity` синхронно в `onReceive` (ресивер в момент `onReceive` в allowlist'е). На некоторых OEM прямой старт активити из ресивера может блокироваться — fallback на notification с `fullScreenIntent` и heads-up.
