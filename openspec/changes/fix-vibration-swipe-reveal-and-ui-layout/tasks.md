## 1. Исправление вибрации в ReminderNotifier

- [x] 1.1 В `ReminderNotifier.kt`, блок `ALARM`: добавить `setVibrate(DEFAULT_VIBRATE_PATTERN)` когда `useVibration == true`.
- [x] 1.2 В `ReminderNotifier.kt`, блок `VIBRATE_ONLY`: добавить `setVibrate(DEFAULT_VIBRATE_PATTERN)` (всегда, этот режим подразумевает вибрацию).
- [x] 1.3 Убедиться, что `NOTIFICATION` блок оставлен без изменений (вибрация через `useVibration`).
- [x] 1.4 Собрать и проверить: `./gradlew :app:assembleDebug`.

## 2. Swipe-to-Reveal компонент

- [x] 2.1 Создать `SwipeToRevealBox.kt` в `ui/components/`:
  - `Animatable(offsetX)` для горизонтального сдвига.
  - `pointerInput` + `detectHorizontalDragGestures` для обработки свайпа влево.
  - Логика залипания: offset > threshold → `animateTo(maxReveal)`, иначе `animateTo(0)`.
  - `maxReveal = 80.dp`.
  - Reveal-панель с фоном `errorContainer` и иконкой `Delete` (tint `error`).
  - Тап по панели → `onAction` (отмена).
  - Тап по карточке (offset == 0) → `onClick`.
- [x] 2.2 Добавить `rememberRevealState` или shared `MutableState<Long?>` в `ReminderListScreen` для radio-behavior (одна открытая панель за раз).
- [x] 2.3 Заменить `SwipeableUpcomingCard` (SwipeToDismissBox) на `SwipeToRevealBox` в `ReminderListScreen`.
- [x] 2.4 Удалить `SwipeableUpcomingCard` composable (или оставить как fallback, но не использовать).
- [x] 2.5 Проверить в Preview / сборке, что свайп залипает, иконка кликабельна, открытие одной панели закрывает другие.

## 3. Удаление чекбокса и перенос "Выполнить"

- [x] 3.1 В `UpcomingReminderCard`, убрать `Column` с иконкой режима и `Checkbox`.
- [x] 3.2 Оставить только иконку режима доставки (single `Icon`, без Column, выровненная по центру справа).
- [x] 3.3 В `ReminderDetailScreen`, `BottomAppBar`: добавить третью кнопку «Готово» (FilledTonalButton или Button с иконкой ✓) рядом с «Отменить» и «Отложить».
- [x] 3.4 Связать кнопку «Готово» с `onComplete` колбэком (уже есть в `ReminderDetailScreen` параметрах? Проверить и добавить если нужно).
- [x] 3.5 В `MainActivity.kt` (или где вызывается `ReminderDetailScreen`), передать колбэк на выполнение через `viewModel.completeReminder(reminder.id)`.

## 4. Исправление формата даты в списке

- [x] 4.1 В `FormatUtils.kt`, добавить `formatRelativeDateShort(epochMillis, nowMillis, zone)`:
  - diff < 0 → "просрочено"
  - daysDiff == 0 → "сегодня"
  - daysDiff == 1 → "завтра"
  - daysDiff == 2 → "послезавтра"
  - иначе → "d MMM" (только дата, без времени)
- [x] 4.2 В `UpcomingReminderCard`, заменить `formatRelativeFireAt` на `formatRelativeDateShort` для нижнего текста в time column.
- [x] 4.3 Верхний текст оставить `formatTime` (HH:mm).
- [x] 4.4 Убедиться, что оба текста влезают в 72dp колонку (maxLines=1, overflow=ellipsis).

## 5. Верификация

- [x] 5.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.
- [x] 5.2 Проверить на устройстве: вибрация ALARM, вибрация VIBRATE_ONLY, свайп-отмена, отсутствие чекбокса, кнопка "Готово" в деталях, compact-формат даты.

## 6. Синхронизация спеков

- [x] 6.1 Обновить `openspec/specs/notification-delivery/spec.md` — добавить вибрацию для ALARM и VIBRATE_ONLY.
- [x] 6.2 Обновить `openspec/specs/ui-screens/spec.md` — swipe-to-reveal, удаление чекбокса, кнопка "Готово", compact-формат даты.
