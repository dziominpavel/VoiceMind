## 1. Полировка карточки «О приложении» в настройках

- [x] 1.1 Обновить `strings.xml`: изменить `settings_version` на «Версия %1$s» (убрать `%2$s` / тип сборки).
- [x] 1.2 Обновить `strings.xml`: удалить строку `settings_test_hint` (или перестать на неё ссылаться).
- [x] 1.3 Обновить `strings.xml`: изменить `settings_developer_name` на «Дёмин Павел»; добавить `settings_developer_label` = «Разработчик».
- [x] 1.4 Обновить `SettingsScreen.kt`, карточку «О приложении`: выводить версию только с `VERSION_NAME`; добавить строку разработчика; убрать тестовую подсказку.

## 2. Дата в превью ближайших напоминаний на главном экране

- [x] 2.1 Добавить `formatShortDate(epochMillis, nowMillis, zone)` в `FormatUtils.kt`: возвращает «сегодня», «завтра», «послезавтра» или `d MMM`.
- [x] 2.2 Обновить `HomeScreen.kt`, `UpcomingPreviewItem`: заменить один `Text` с временем на `Column`, содержащий `formatTime` (titleMedium) и `formatShortDate` (labelSmall, TextMuted).
- [x] 2.3 Убедиться, что высота строки остаётся ~64 dp и текст не переносится; задать `maxLines = 1` для метки даты.

## 3. Исправление приоритета «вечера» в парсере

- [x] 3.1 В `ReminderParser.kt` повысить score кандидата `TIME_HOURS_PART` с `80` до `100` (должен превосходить `TIME_COLON` 90 и `TIME_4DIGIT` 95).
- [x] 3.2 Проверить, что regex `TIME_HOURS_PART` уже покрывает «вечера» / «вечером» через логику `hour + 12`.
- [x] 3.3 Добавить unit-тест в `ReminderParserTest.kt`: `todayAt8pm_withColon_parses2000()` — фраза «сегодня в 8:00 вечера спать» должна дать fireAt в 20:00.
- [x] 3.4 Добавить unit-тест: `todayAt8pm_withoutColon_parses2000()` — фраза «сегодня в 8 вечера спать» как защита от регрессии.
- [x] 3.5 Запустить тесты парсера: `gradlew :app:testDebugUnitTest --tests ReminderParserTest`.

## 4. Исправление налезания иконки и чекбокса в списке предстоящих

- [x] 4.1 В `ReminderListScreen.kt`, `UpcomingReminderCard`, найти `Column`, содержащую иконку режима доставки и `Checkbox`.
- [x] 4.2 Вставить `Spacer(modifier = Modifier.height(4.dp))` между `Icon` и `Checkbox`.
- [x] 4.3 Проверить в превью / сборке, что вертикальный отступ устраняет перекрытие.

## 5. Верификация

- [x] 5.1 Собрать `./gradlew :app:assembleDebug` — без ошибок.
- [x] 5.2 Запустить `./gradlew :app:testDebugUnitTest --tests ReminderParserTest` — все тесты проходят.

## 6. Синхронизация спеков

- [x] 6.1 Создать delta spec `specs/ui-screens/spec.md` для изменений: About, дата в превью, отступ в списке.
- [x] 6.2 Создать delta spec `specs/reminder-parsing/spec.md` для изменений: приоритет TIME_HOURS_PART (score 100 для вечера/ночи).
- [x] 6.3 Синхронизировать `openspec/specs/ui-screens/spec.md` с delta spec.
- [x] 6.4 Синхронизировать `openspec/specs/reminder-parsing/spec.md` с delta spec.
