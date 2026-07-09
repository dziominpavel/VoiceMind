## 1. Строки и лейблы статусов

- [x] 1.1 В `strings.xml` заменить «Готово» на «Выполнено» для `notification_action_done`, `alarm_screen_done`, `detail_done` (и связанных contentDescription, если есть)
- [x] 1.2 В `FormatUtils.statusLabel` заменить TRIGGERED → «Без ответа» (DONE/CANCELLED/PENDING без изменений смысла)
- [x] 1.3 Добавить `FormatUtils.formatHistoryDate` (сегодня / вчера / d MMM) без ветки «просрочено»
- [x] 1.4 В `ReminderDetailScreen.StatusBadge` синхронизировать лейбл TRIGGERED с «Без ответа»

## 2. Карточка Истории

- [x] 2.1 В `HistoryReminderCard` убрать вызов `formatRelativeFireAt`; показывать статус + `formatHistoryDate`
- [x] 2.2 Закрепить визуал: DONE — зелёный + зачёркивание; CANCELLED — серый без зачёркивания; TRIGGERED — TimeWarning без зачёркивания
- [x] 2.3 Убедиться, что «просрочено» не появляется ни на одной карточке Истории

## 3. Предстоящие (регрессия)

- [x] 3.1 Проверить, что overdue PENDING по-прежнему показывает «просрочено» и пульсирующую полосу
- [x] 3.2 Urgency-цвета (NORMAL/URGENT/CRITICAL/OVERDUE) не затронуты изменениями Истории

## 4. Уведомление и Alarm UI

- [x] 4.1 Проверить, что `ReminderNotifier` берёт строку «Выполнено» из resources
- [x] 4.2 Проверить, что `AlarmActivity` / alarm screen показывает «Выполнено»
- [x] 4.3 Семантика действий без изменений кода ресивера: Выполнено→DONE, Отменить→CANCELLED, +10→PENDING

## 5. Проверка

- [x] 5.1 Unit-тесты на `FormatUtils` (statusLabel, formatHistoryDate, overdue только для прошлого fireAt в upcoming-хелпере)
- [x] 5.2 Ручной сценарий: срабатывание NOTIFICATION → три кнопки с новыми подписями → История с корректными лейблами
- [x] 5.3 `openspec validate --all`
