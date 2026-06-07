## Зачем

Текущий SettingsScreen имеет 4 radio-карточки режимов + отдельный глобальный Switch вибрации. Пользовательский анализ показал, что естественным является выбор из **5 профилей**, а не 4 + toggle:

1. Будильник — ringtone + full-screen + вибрация (default)
2. Будильник (без вибрации) — ringtone + full-screen
3. Вибрация — только вибро
4. Пуш-уведомление — системный звук + короткая вибрация
5. Тихий — ничего

Глобальный toggle вибрации создаёт лишний уровень абстракции и путает: для режима VIBRATE toggle disabled (вибрация всегда), для ALARM/NOTIFICATION — вкл/выкл. Проще показать 5 готовых профилей.

## Что меняется

- **SettingsScreen**: заменить 4 radio-карточки + глобальный Switch на 5 компактных radio-карточек (Тихий, Пуш, Вибрация, Будильник, Будильник без вибро).
- **Убрать** глобальный Switch «Вибрация при срабатывании».
- **DataStore**: `defaultDeliveryMode` + `useVibration` остаются. UI маппит 5 визуальных профилей на ту же пару настроек.
- **Alarm-карточка** (ringtone + volume): показывается только при выборе «Будильник» или «Будильник без вибро».
- **ConfirmReminderScreen / ManualReminderScreen**: `DeliveryModeGrid` остаётся с 4 enum-значениями (ALARM/NOTIFICATION/VIBRATE/SILENT). Toggle vibrate появляется только при ALARM.

## Capabilities

### Изменённые
- `ui-screens`: SettingsScreen — 5 режимов, компактный layout, убран глобальный toggle vibrate.

### Нетронутые
- `notification-delivery`, `datastore-settings` — логика доставки и хранения не меняется.

## Влияние

- `app/src/main/java/com/example/voicemind/ui/screens/SettingsScreen.kt` — переработка UI.
- `app/src/main/res/values/strings.xml` — 2 новые строки (режимы будильника).
- `app/src/main/java/com/example/voicemind/viewmodel/VoiceMindViewModel.kt` — minor: убрать `useVibration` из SettingsScreen параметров.
- `app/src/main/java/com/example/voicemind/MainActivity.kt` — minor: не передавать `useVibration` в SettingsScreen.
