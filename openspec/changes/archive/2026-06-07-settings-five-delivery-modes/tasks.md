## 1. SettingsScreen — 5 режимов

- [x] 1.1 В `SettingsScreen.kt`, удалить глобальный Switch «Вибрация при срабатывании» и `isVibrateMode` логику.
- [x] 1.2 Добавить 5-ю карточку `DeliveryModeOption` для «Будильник без вибрации» с иконкой `Alarm` и tint `TextMuted` (визуально отличается от обычного будильника).
- [x] 1.3 Уменьшить padding внутри `DeliveryModeOption` с `Spacing.md` (16dp) до 12dp для компактности.
- [x] 1.4 Убрать subtitle из `DeliveryModeOption` или заменить на однострочный hint (labelSmall).
- [x] 1.5 Обновить mapping: ALARM + useVibration=true → «Будильник» (selected); ALARM + useVibration=false → «Будильник без вибрации».
- [x] 1.6 Обновить `onDefaultDeliveryModeChange`: выбор «Будильник» ставит `defaultDeliveryMode=ALARM` + `useVibration=true`; выбор «Будильник без вибрации» ставит `defaultDeliveryMode=ALARM` + `useVibration=false`.
- [ ] 1.7 Обновить `MainActivity.kt`: убрать передачу `useVibration` в `SettingsScreen` (больше не нужен).
- [ ] 1.8 Обновить `VoiceMindViewModel.kt`: убрать `useVibration` из параметров SettingsScreen (если передавался отдельно).

## 2. Strings

- [x] 2.1 В `strings.xml`, добавить `settings_mode_alarm_no_vibrate` = «Будильник без вибрации».
- [x] 2.2 В `strings.xml`, добавить `settings_mode_alarm_no_vibrate_hint` = «Громкий звонок, без вибрации».
- [ ] 2.3 Убрать `settings_use_vibration` и `settings_use_vibration_hint` (или оставить для экрана создания напоминания, если используется).

## 3. Верификация

- [x] 3.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.
- [ ] 3.2 Проверить, что 5 карточек влезают на экран без scroll на эмуляторе small/regular.
- [ ] 3.3 Проверить: ALARM + vibrate=true → карточка «Будильник» selected; ALARM + vibrate=false → «Будильник без вибрации» selected.
- [ ] 3.4 Проверить, что при выборе «Будильник без вибрации» alarm-настройки (мелодия, громкость) показываются.
