## Контекст

Предыдущий change (ui-polish-and-parser-fixes) добавил `Spacer(4.dp)` между иконкой режима доставки и `Checkbox` в `UpcomingReminderCard`. Однако на реальном устройстве overlay сохранился. Анализ показал, что проблема комплексная: недостаточный зазор, отсутствие padding у списка, прозрачный фон свайпа и дублирующий SnackbarHost.

## Цели / Out of scope

**Цели:**
- Устранить визуальное перекрытие иконки режима доставки и чекбокса в `UpcomingReminderCard`.
- Обеспечить отступ между последней карточкой списка и `BottomAppBar`.
- Добавить цветной фон к `backgroundContent` свайпа, чтобы иконка удаления не накладывалась на содержимое карточки.
- Удалить дублирующий `SnackbarHost` в `MainActivity`.
- Добавить `imePadding()` к `ConfirmReminderScreen`, чтобы клавиатура не перекрывала кнопку сохранения.

**Out of scope:**
- Переделка экранов или навигации.
- Изменения схемы БД.
- Новые разрешения.
- Изменения логики планирования будильников.

## Решения

### 1. Отступы в UpcomingReminderCard
**Почему:** `Checkbox` в Material 3 автоматически применяет `minimumInteractiveComponentSize() = 48.dp`, игнорируя переданный `size(32.dp)`. Визуально он занимает больше места, чем ожидалось.
**Реализация:**
- Обернуть Column с иконкой и чекбоксом в `Modifier.padding(vertical = Spacing.xs)` (8.dp).
- Заменить `Spacer(4.dp)` на `Spacing.sm` (12.dp).

### 2. Content padding в LazyColumn
**Почему:** без нижнего padding последний элемент списка вплотную прилегает к `BottomAppBar`.
**Реализация:** добавить `contentPadding = PaddingValues(bottom = Spacing.md)` в `LazyColumn`.

### 3. Фон при свайпе
**Почему:** `SwipeToDismissBox.backgroundContent` рисуется на прозрачном фоне поверх содержимого карточки.
**Реализация:** обернуть содержимое `backgroundContent` в `Box` с `Modifier.background(MaterialTheme.colorScheme.errorContainer)`.

### 4. Удаление дублирующего SnackbarHost
**Почему:** в `MainActivity` `Box` содержит `Scaffold` (со своим `SnackbarHost`) и ещё один `SnackbarHost` снаружи на том же `SnackbarHostState`. Это создаёт дублирование и потенциальное наложение.
**Реализация:** удалить внешний `SnackbarHost`. `Scaffold` уже корректно управляет своим хостом с `padding(bottom = 80.dp)`.

### 5. IME padding в ConfirmReminderScreen
**Почему:** при открытии клавиатуры для редактирования `body` `BottomAppBar` с кнопкой "Сохранить" остаётся за клавиатурой.
**Реализация:** добавить `.imePadding()` к modifier `Scaffold`.

## Риски / Компромиссы

| Риск | Компенсация |
|------|-------------|
| **Удаление внешнего SnackbarHost может сломать позиционирование снекбаров** | Внутренний `SnackbarHost` в `Scaffold` уже имеет `padding(bottom = 80.dp)`, что корректно сдвигает его над `BottomAppBar`. Внешний хост был избыточным. |
| **Увеличенный отступ может сделать карточку слишком высокой** | `cardMinHeight = 72.dp` остаётся неизменным; Column выравнивается по `CenterVertically`, поэтому дополнительный padding просто уменьшает визуальную плотность, но не ломает layout. |

## План миграции

Миграция данных не требуется. Все изменения — layout-level. Откат возможен файл за файлом.

## Открытые вопросы

- Нет.
