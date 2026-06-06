## Зачем

В ходе ежедневного использования VoiceMind MVP выявилось три независимых, но заметных дефекта: вибрация не работает ни в одном режиме доставки, карточки напоминаний в списке имеют визуальные артефакты и перекрывающиеся элементы, а свайп для отмены срабатывает мгновенно без подтверждения, что приводит к случайным потерям напоминаний.

## Что меняется

- **Вибрация**: исправить `ReminderNotifier.show()` так, чтобы `ALARM` и `VIBRATE_ONLY` режимы корректно запрашивали вибрацию через `NotificationCompat.Builder.setVibrate()`. Ранее `ALARM` использовал `setSilent(true)` без явного `setVibrate()`, а `VIBRATE_ONLY` вообще не вызывал вибрацию.
- **Swipe-to-Reveal**: заменить стандартный `SwipeToDismissBox` на кастомный reveal-паттерн как в Gmail. Свайп залипает на фиксированную ширину (~80dp), открывая кликабельную красную иконку «Отменить». Тап по иконке отменяет напоминание (двухфакторное подтверждение: свайп + тап). Убирает проблему случайного удаления и красных артефактов по краям карточек.
- **Убрать чекбокс из списка**: `Checkbox` в `UpcomingReminderCard` подвержен случайным нажатиям. Убираем его из списка; функция «Отметить как выполненное» переносится в `ReminderDetailScreen`.
- **Добавить "Выполнить" в детали**: в `ReminderDetailScreen` добавить кнопку «Готово» (или "Выполнить") рядом с "Отменить" и "Отложить", чтобы завершить напоминание из экрана деталей.
- **Исправить переполнение time column**: `formatRelativeFireAt` для разницы ≥ 1 суток возвращает полную дату `formatFireAt`, которая не влезает в 72dp колонку. Добавить отдельный compact-формат `formatRelativeDateOnly` для списка.

## Capabilities

### Новые capabilities
- Нет.

### Изменённые capabilities
- `notification-delivery`: добавлено требование вибрации для `ALARM` и `VIBRATE_ONLY` режимов.
- `ui-screens`: замена `SwipeToDismissBox` на `SwipeToReveal`; удаление чекбокса из списка; добавление кнопки "Выполнить" в `ReminderDetailScreen`; исправление формата даты в `UpcomingReminderCard`.

## Влияние

- `app/src/main/java/com/example/voicemind/data/notification/ReminderNotifier.kt` — логика вибрации для ALARM/VIBRATE_ONLY.
- `app/src/main/java/com/example/voicemind/ui/screens/ReminderListScreen.kt` — замена свайпа на reveal, удаление чекбокса, формат даты.
- `app/src/main/java/com/example/voicemind/ui/screens/ReminderDetailScreen.kt` — добавление кнопки "Выполнить".
- `app/src/main/java/com/example/voicemind/data/FormatUtils.kt` — новый compact-формат даты.
- **Без изменений схемы БД.**
- **Без новых разрешений.**
