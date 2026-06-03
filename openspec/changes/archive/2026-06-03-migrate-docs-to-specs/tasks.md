## 1. Создать specs из docs/NOTIFICATION_MODES.md

- [x] 1.1 Создать `openspec/specs/notification-delivery/spec.md` с BDD-спеками режимов оповещения
- [x] 1.2 Перевести таблицу DeliveryMode в требования WHEN/THEN
- [x] 1.3 Добавить спеки для actions на уведомлении (Готово, Отложить, Отменить)
- [x] 1.4 Добавить спеки для permissions (POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM)
- [x] 1.5 Добавить спеки для BootReceiver reschedule

## 2. Создать specs из docs/DESIGN_SYSTEM.md

- [x] 2.1 Создать `openspec/specs/ui-screens/spec.md` с BDD-спеками экранов
- [x] 2.2 Добавить спеки для темы CLEAR BELL и цветов
- [x] 2.3 Добавить спеки для главного экрана (ближайшее напоминание, empty state)
- [x] 2.4 Добавить спеки для ConfirmScreen (редактирование, предупреждения)
- [x] 2.5 Добавить спеки для списка напоминаний (вкладки, сортировка)
- [x] 2.6 Добавить спеки для ManualReminderScreen и SettingsScreen

## 3. Создать specs из docs/WIDGET_DESIGN.md

- [x] 3.1 Создать `openspec/specs/widget/spec.md` с BDD-спеками виджета
- [x] 3.2 Добавить спеки для layout и размеров виджета
- [x] 3.3 Добавить спеки для строк (выполненные + предстоящие)
- [x] 3.4 Добавить спеки для кликов (body, checkbox, microphone)
- [x] 3.5 Добавить спеки для цветов и RemoteViews ограничений

## 4. Создать specs из docs/REMINDER_PARSING.md (STT pipeline)

- [x] 4.1 Создать `openspec/specs/speech-recognition/spec.md` с BDD-спеками STT
- [x] 4.2 Добавить спеки для on-device SpeechRecognizer
- [x] 4.3 Добавить спеки для fallback на RecognizerIntent
- [x] 4.4 Добавить спеки для timeout 10 секунд
- [x] 4.5 Добавить спеки для обработки ошибок STT

## 5. Дополнить specs/reminder-parsing/spec.md

- [x] 5.1 Добавить спеки для контракта ParseResult
- [x] 5.2 Добавить спеки для ParseWarning (TIME_AMBIGUOUS, NO_TIME_FOUND, BODY_EMPTY, PAST_TIME_ADJUSTED)
- [x] 5.3 Добавить спеки для candidate-based engine
- [x] 5.4 Добавить спеки для часового пояса
- [x] 5.5 Проверить, что существующие спеки не дублируются

## 6. Финализация

- [x] 6.1 Проверить, что все specs на русском языке (openspec-language.md)
- [x] 6.2 Проверить структуру capability (openspec-specs-structure.md)
- [x] 6.3 Убедиться, что docs/ не изменены (они остаются human-readable)
