## Зачем

После предыдущего фикса налезания иконок в списке предстоящих (ui-polish-and-parser-fixes) проблема частично сохранилась. Визуальный overlay воспроизводится на реальном устройстве по нескольким причинам:

1. **Недостаточный отступ в карточке**: `Spacer(4.dp)` между иконкой режима доставки и `Checkbox` не учитывает, что Material 3 `Checkbox` расширяется до 48.dp через `minimumInteractiveComponentSize`. В результате элементы всё ещё перекрываются.
2. **Список прилипает к BottomAppBar**: `LazyColumn` не имеет `contentPadding` снизу, поэтому последняя карточка визуально "наезжает" на нижнюю панель с кнопками Mic/Add.
3. **Прозрачный фон при свайпе**: `SwipeToDismissBox.backgroundContent` — это просто `Row` с иконкой удаления на прозрачном фоне. При свайпе иконка удаления накладывается поверх иконки режима доставки, создавая эффект наложения кнопок.
4. **Дублирующий SnackbarHost**: в `MainActivity` два `SnackbarHost` на одном `SnackbarHostState` — один внутри `Scaffold`, другой поверх `Box`. Это может приводить к наложению снекбаров поверх нижних кнопок.
5. **Клавиатура перекрывает кнопку на ConfirmScreen**: при редактировании `body` в `ConfirmReminderScreen` клавиатура может закрыть `BottomAppBar` с кнопкой "Сохранить", потому что `Scaffold` не использует `imePadding`.

## Что меняется

- **ReminderListScreen / UpcomingReminderCard**: увеличить вертикальный padding колонки действий и заменить `Spacer(4.dp)` на `Spacing.sm` (12.dp). Добавить `contentPadding(bottom = Spacing.md)` в `LazyColumn`.
- **ReminderListScreen / SwipeableUpcomingCard**: обернуть `backgroundContent` в `Box` с фоном `MaterialTheme.colorScheme.errorContainer`.
- **MainActivity**: удалить дублирующий `SnackbarHost` вне `Scaffold`.
- **ConfirmReminderScreen**: добавить `.imePadding()` к `Scaffold`.

## Capabilities

### Новые capabilities
- Нет.

### Изменённые capabilities
- `ui-screens`: карточка предстоящего напоминания получила корректные отступы и фон при свайпе. `LazyColumn` больше не прилипает к `BottomAppBar`. `ConfirmReminderScreen` корректно обрабатывает IME.

## Влияние

- `app/src/main/java/com/example/voicemind/ui/screens/ReminderListScreen.kt` — отступы, padding, фон свайпа.
- `app/src/main/java/com/example/voicemind/ui/screens/ConfirmReminderScreen.kt` — `imePadding()`.
- `app/src/main/java/com/example/voicemind/MainActivity.kt` — удаление дублирующего `SnackbarHost`.
- **Без изменений схемы БД.**
- **Без изменений API.**
- **Без новых разрешений.**
