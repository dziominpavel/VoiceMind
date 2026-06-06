## 1. Исправление отступов в UpcomingReminderCard

- [x] 1.1 В `ReminderListScreen.kt`, `UpcomingReminderCard`, найти `Column` с иконкой режима и `Checkbox`.
- [x] 1.2 Добавить `Modifier.padding(vertical = Spacing.xs)` к этой `Column`.
- [x] 1.3 Заменить `Spacer(modifier = Modifier.height(4.dp))` на `Spacer(modifier = Modifier.height(Spacing.sm))`.
- [x] 1.4 Проверить в Preview / сборке, что элементы больше не перекрываются.

## 2. Content padding в LazyColumn

- [x] 2.1 В `ReminderListScreen.kt`, найти `LazyColumn`.
- [x] 2.2 Добавить `contentPadding = PaddingValues(bottom = Spacing.md)`.
- [x] 2.3 Убедиться, что последняя карточка не прилипает к `BottomAppBar`.

## 3. Фон при свайпе

- [x] 3.1 В `ReminderListScreen.kt`, `SwipeableUpcomingCard`.
- [x] 3.2 Обернуть `backgroundContent` в `Box` с `Modifier.background(MaterialTheme.colorScheme.errorContainer)`.
- [x] 3.3 Сохранить внутренний `Row` с иконкой удаления и padding.

## 4. Удаление дублирующего SnackbarHost

- [x] 4.1 В `MainActivity.kt`, найти внешний `SnackbarHost` в `Box`.
- [x] 4.2 Удалить его. `Scaffold` уже содержит корректный `SnackbarHost` с `padding(bottom = 80.dp)`.

## 5. IME padding в ConfirmReminderScreen

- [x] 5.1 В `ConfirmReminderScreen.kt`, добавить `.imePadding()` к modifier `Scaffold`.
- [x] 5.2 Добавить `import androidx.compose.foundation.layout.imePadding`.
- [x] 5.3 Проверить, что при открытии клавиатуры кнопка "Сохранить" остаётся видимой.

## 6. Верификация

- [x] 6.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.

## 7. Синхронизация спеков

- [x] 7.1 Создать delta spec `specs/ui-screens/spec.md` для изменений: отступы, свайп-фон, IME, SnackbarHost.
- [x] 7.2 Синхронизировать `openspec/specs/ui-screens/spec.md` с delta spec.
