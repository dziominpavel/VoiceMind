---
trigger: glob
description: Визуальный стиль VoiceMind — тёмная тема, токены, иконки
globs: ["**/ui/**", "**/theme/**"]
---

# UI & Theme — CLEAR BELL

## Направление
- **Тёмная тема** (светлая — фаза 4+).
- Акцент: **бирюза** `#2EC4B6` — «не пропусти», не тревожный красный.
- Фон: `Midnight` `#0A0C10`, поверхности: `Slate` `#141820` / `SlateVariant` `#1C2230`.
- Крупная типографика для `fireAt` на карточке (tabular nums).

## Токены
- `ui/theme/Color.kt` — палитра CLEAR BELL.
- `ui/theme/Theme.kt` — `VoiceMindTheme` (Material 3 + custom colors).
- `ui/theme/Dimens.kt` — `Spacing`, размеры.
- **Без inline hex** в `@Composable` функциях экранов — только через `MaterialTheme.colorScheme` / `VoiceMindTheme.colors`.

## Иконки
- Material Symbols: `Mic`, `Notifications`, `Alarm`, `Vibration`, `Schedule`.
- Режимы в `DeliveryModePicker`: иконка + текст.

## Экраны
- **Home**: сверху «Ближайшее» карточка, центр `MicButton` (пульс при Listening), низ «Ввести текстом».
- **Confirm**: крупно дата/время (клик → pickers), поле body, `DeliveryModePicker`, блок warnings жёлтым (`WarningAmber`), кнопки Сохранить/Отмена.
- **List**: время слева (колонка), текст справа, иконка режима. Просроченные — subtle error tint.
- **Manual**: пустая форма, default завтра 9:00, DatePicker + TimePicker + OutlinedTextField + `DeliveryModePicker`.

## Запрещено
- Копировать UI GymProgress (Volt, Obsidian, IRON CORE).
- Использовать `LiveData` — только `StateFlow`.
- Добавлять зависимости типа Lottie/Splash без обсуждения.
