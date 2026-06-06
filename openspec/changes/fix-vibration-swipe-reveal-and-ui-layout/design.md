## Контекст

VoiceMind MVP имеет три UI/UX дефекта, выявленных при реальном использовании:

1. **Вибрация не работает**: `ReminderNotifier.show()` для `ALARM` использует `setSilent(true)` без явного `setVibrate()`, а `VIBRATE_ONLY` вообще не вызывает вибрацию. Спека `notification-delivery` требует вибрацию через `AlarmSoundPlayer`, но на уровне уведомления она тоже должна быть явной.
2. **Случайное удаление через свайп**: `SwipeToDismissBox` с `confirmValueChange = true` отменяет напоминание сразу при пересечении threshold (~25% ширины). Пользователь не имеет шанса передумать. Кроме того, прозрачный `backgroundContent` создаёт красные артефакты по краям скруглённой карточки.
3. **Случайное выполнение через чекбокс**: `Checkbox` в `UpcomingReminderCard` легко нажимается случайно при прокрутке или тапе по карточке.

## Goals / Non-Goals

**Goals:**
- Все режимы доставки (`ALARM`, `VIBRATE_ONLY`, `NOTIFICATION`) корректно управляют вибрацией согласно спеке.
- Swipe-to-Reveal заменяет SwipeToDismissBox: свайп открывает панель с кликабельной иконкой "Отменить" (двухфакторное подтверждение).
- Чекбокс удалён из списка предстоящих; выполнение перенесено в `ReminderDetailScreen`.
- `formatRelativeFireAt` не переполняет 72dp колонку в списке.

**Non-Goals:**
- Переделка навигации, архитектуры или схемы БД.
- Добавление новых режимов доставки.
- Изменение `AlarmSoundPlayer` (вибрация через уведомление — отдельный слой).

## Decisions

### 1. Swipe-to-Reveal через кастомный `Animatable` offset
**Почему:** Material 3 `SwipeToDismissBox` не поддерживает sticky reveal (залипание на фиксированной ширине). Единственная альтернатива из Compose Foundation — кастомный `Modifier.pointerInput` + `Animatable`.
**Реализация:**
- `SwipeToRevealBox` — обёртка вокруг карточки с `Animatable(offsetX, Float)`.
- `detectHorizontalDragGestures` — отслеживает свайп влево. При отпускании: если offset > threshold (50% maxReveal), залипает на `maxReveal` (80dp); иначе — анимация отскока на 0.
- Панель действий — `Box` фиксированной ширины (`maxReveal`), расположенный за карточкой (z-order ниже). Фон панели — `errorContainer`, иконка — `Delete`, tint — `error`.
- Тап по панели вызывает `onCancel`. Тап по самой карточке (когда offset == 0) — `onClick`.
- Radio-behavior: при открытии панели на одной карточке, остальные схлопываются (через shared `MutableState<Long?>` с ID открытой карточки, передаваемый в `LazyColumn`).

**Альтернатива:** `confirmValueChange = false` + AlertDialog при свайпе. **Отклонено:** свайп + диалог = два шага, но пользователь теряет визуальную связь с карточкой. Reveal-панель интуитивнее.

### 2. Вибрация в `ReminderNotifier`
**ALARM:** `setSilent(true)` оставляем (не нужен звук уведомления, т.к. `AlarmSoundPlayer` управляет звуком), но добавляем `setVibrate(...)` если `useVibration == true`.
**VIBRATE_ONLY:** добавляем `setVibrate(DEFAULT_VIBRATE_PATTERN)` — ранее был только `setPriority(DEFAULT)` без вибрации.
**NOTIFICATION:** оставляем как есть — вибрация через `setVibrate()` при `useVibration == true`.

### 3. Удаление чекбокса и перенос "Выполнить"
- Из `UpcomingReminderCard` убираем `Checkbox` и весь блок Column с иконкой режима + чекбокс.
- Иконка режима доставки остаётся (информативная), но размещается в единственной `Icon` без Column.
- В `ReminderDetailScreen` добавляем третью кнопку "Готово" (или иконку ✓) в `BottomAppBar` рядом с "Отменить" и "Отложить".

### 4. Compact-формат даты в списке
- `formatRelativeFireAt` для `diff >= 86_400_000` возвращает `formatFireAt` (полная дата + время), что не влезает в 72dp.
- Новый хелпер `formatRelativeDateShort(epochMillis)` — возвращает только относительную дату: "сегодня", "завтра", "послезавтра" или "d MMM". Время отдельно через `formatTime`.
- `UpcomingReminderCard` показывает `formatTime` (titleMedium) и `formatRelativeDateShort` (labelSmall, TextMuted) в Column 72dp.

## Risks / Trade-offs

| Риск | Компенсация |
|------|-------------|
| **Кастомный свайп может конфликтовать с NestedScroll LazyColumn** | Используем `pointerInput` на уровне элемента, а не списка; `detectHorizontalDragGestures` не перехватывает вертикальные жесты. |
| **Radio-behavior добавляет состояние в список** | `MutableState<Long?>` с `rememberSaveable` на уровне `ReminderListScreen`; при изменении вкладки или scroll — сброс. |
| **Удаление чекбокса увеличивает время на выполнение** | Пользователь открывает детали одним тапом (уже делал для просмотра) и жмёт "Готово" — один дополнительный тап, но зато без случайных отмен. |
| **Вибрация в ALARM через уведомление дублирует Vibrator** | `AlarmSoundPlayer` использует `Vibrator` напрямую; `setVibrate()` в уведомлении — fallback для случаев, когда `AlarmSoundPlayer` не успел запуститься (OEM kill). Дублирование безопасно. |

## План миграции

Миграция данных не требуется. Все изменения — code-level. Откат:
- `ReminderNotifier.kt` — revert 3 строки.
- `ReminderListScreen.kt` — заменить `SwipeToRevealBox` на `SwipeToDismissBox`, вернуть `Checkbox`.
- `ReminderDetailScreen.kt` — убрать кнопку "Готово".
- `FormatUtils.kt` — удалить `formatRelativeDateShort`.

## Открытые вопросы

- Нет.
