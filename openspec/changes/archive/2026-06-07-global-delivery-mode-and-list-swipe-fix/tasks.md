## 1. Data layer — глобальный режим

- [x] 1.1 Добавить в `ReminderDao` метод `updateAllDeliveryModes(mode: String)` без фильтра по статусу
- [x] 1.2 Добавить в `ReminderRepository` метод `syncAllDeliveryModes(mode: DeliveryMode)`
- [x] 1.3 Добавить Room migration `5 → 6` в `AppDatabase` (UPDATE всех `deliveryMode`)
- [x] 1.4 Добавить one-shot startup sync: DataStore-флаг `delivery_mode_synced_v6` + вызов `syncAllDeliveryModes` при первом запуске после upgrade

## 2. Runtime — чтение режима из Settings

- [x] 2.1 `ReminderNotifier.show()` — получать `deliveryMode` из `SettingsRepository.getDefaultDeliveryMode()`, не из `reminder.deliveryMode`
- [x] 2.2 `ReminderAlarmReceiver` — аналогично читать режим из Settings
- [x] 2.3 `VoiceMindViewModel.setDefaultDeliveryMode()` — после записи в DataStore вызывать `repository.syncAllDeliveryModes(mode)`

## 3. ViewModel и save-пути — убрать per-reminder mode

- [x] 3.1 Убрать параметр `deliveryMode` из `saveReminder()` / `saveManualReminder()` / `ManualReminderDraft`
- [x] 3.2 При insert/update записывать `deliveryMode = settings.getDefaultDeliveryMode().name` (синхронизация с глобальным)
- [x] 3.3 `duplicateReminder()` — использовать `settings.getDefaultDeliveryMode()`, не `reminder.deliveryMode`

## 4. UI — убрать DeliveryModeGrid

- [x] 4.1 `ConfirmReminderScreen` — удалить `DeliveryModeGrid`, `selectedDeliveryMode`, параметр `defaultDeliveryMode`
- [x] 4.2 `ManualReminderScreen` — удалить `DeliveryModeGrid` и передачу `deliveryMode` в `onSave`
- [x] 4.3 `MainActivity` — убрать передачу `defaultDeliveryMode` в Confirm; обновить сигнатуры колбэков
- [x] 4.4 Удалить неиспользуемый `DeliveryModeGrid` composable (если нигде не остался) или оставить только если нужен в Settings

## 5. UI — иконка режима из настроек

- [x] 5.1 `ReminderListScreen` — принимать `currentDeliveryMode: DeliveryMode`; иконка из настроек, не из `reminder.deliveryMode`
- [x] 5.2 `HomeScreen` — аналогично для превью ближайших напоминаний
- [x] 5.3 `MainActivity` — передать `defaultDeliveryMode` Flow в List и Home

## 6. UI — исправить SwipeToRevealBox

- [x] 6.1 Заменить `matchParentSize()` красный фон на `Box` шириной 80dp, `align(CenterEnd)`, фон `errorContainer`
- [x] 6.2 Добавить `Modifier.clip(MaterialTheme.shapes.medium)` на foreground Box с карточкой
- [x] 6.3 Передавать `isRevealed` в `UpcomingReminderCard`; скрывать иконку режима при открытом reveal
- [x] 6.4 Визуально проверить: нет красных углов в покое; нет наложения delete + delivery icon при свайпе

## 7. Проверка

- [x] 7.1 Unit/instrumented: `updateAllDeliveryModes` обновляет записи всех статусов
- [x] 7.2 Ручной тест: два напоминания с разными иконками → смена режима в настройках → обе карточки показывают одну иконку
- [x] 7.3 Ручной тест: свайп reveal — красные углы не видны; иконки не наезжают
- [x] 7.4 `openspec validate --all` — pass
