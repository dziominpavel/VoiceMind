# VoiceMind — UI-направление

> Дата: 2026-05-29

**Важно:** стиль **кода** ориентируем на GymProgress (Compose, токены, Material 3). **Визуальный стиль — отдельный**, не копируем «IRON CORE» / Volt.

## Направление: «CLEAR BELL»

Напоминалка должна считываться за секунду: **когда** и **что**. Акцент — на времени и кнопке микрофона, не на «атмосфере дневника».

- Тема: **тёмная** (светлая — позже, фаза 4+).
- Акцент: **бирюза** `#2EC4B6` — ассоциация «не пропусти», не тревожный красный.
- Фон: `Midnight` `#0A0C10`, поверхности: `Slate` `#141820` / `SlateVariant` `#1C2230`.
- Крупная типографика для `fireAt` на карточке (tabular nums).
- Микрофон — главная кнопка на Home.

Токены: `ui/theme/Color.kt`, `Theme.kt`, `Dimens.kt` (`Spacing`).

---

## Экраны (ключевые)

### Home

- Верх: «Ближайшее» — одна карточка или empty state.
- Центр: `MicButton` + состояния Listening (пульс кольца).
- Низ: «Ввести текстом».

### Confirm

- Крупно: дата и время (клик → pickers).
- Поле текста напоминания.
- `DeliveryModePicker` (иконки: колокол, вибро, без звука).
- Кнопки: Сохранить / Отмена.
- Блок предупреждений парсера (жёлтый `WarningAmber`).

### Список

- Карточка: время слева (колонка), текст справа, иконка режима.
- Просроченные scheduled — subtle error tint.

### ManualReminderScreen

- Пустая форма с дефолтным временем (завтра 9:00).
- DatePicker + TimePicker + OutlinedTextField для body.
- `DeliveryModePicker`.

### Settings

- Radio: режим по умолчанию с пояснением одной строкой.
- Toggle: `confirmBeforeSchedule`.
- Ссылки на системные: уведомления, точные будильники (фаза 4: батарея, тихие часы).

---

## Правила (как в GymProgress)

- Отступы: `Spacing`, размеры: `Dimens`.
- Цвета: `MaterialTheme` + `VoiceMindTheme.colors`, без inline hex в screens.
- Иконки: Material Symbols — `Mic`, `Notifications`, `Alarm`, `Vibration`, `Schedule`.

---

## Связанные документы

- [FEATURE_PLAN.md](FEATURE_PLAN.md) — фаза 0 (тема)
- GymProgress `docs/DESIGN_SYSTEM.md` — **только** как пример структуры документа, не палитры
