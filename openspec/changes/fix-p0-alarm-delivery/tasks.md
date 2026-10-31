# Tasks

## 1. Каналы уведомлений

- [x] 1.1 Вынести в `NotificationChannels` списки `currentChannelIds` и `legacyChannelIds`; удалять только legacy-ID (`reminders_default`, `reminders_alarm`, `reminders_silent`, `reminders_vibrate`), не трогать текущие `_v2`/`_quiet_vibrate_v1`; убрать устаревшую проверку `SDK_INT < O`. Проверка: `NotificationChannelsTest` подтверждает, что списки не пересекаются.
- [x] 1.2 Убрать вызов `NotificationChannels.createAll` из `ReminderNotifier.show`; оставить единственный вызов в `VoiceMindApplication.onCreate`. Проверка: поиск по `createAll` показывает только определение и `Application`; `testDebugUnitTest` проходит.
- [x] 1.3 Добавить unit-тест на жизненный цикл каналов: текущие ID не входят в legacy-список, legacy-список содержит старые имена. Проверка: тест падает при добавлении текущего ID в legacy-список и проходит после исправления.
- [x] 1.4 Обновить раздел уведомлений в `docs/ARCHITECTURE.md`: каналы создаются один раз при старте, при показе уведомления не удаляются. Проверка: раздел соответствует коду и не упоминает удаление текущего `VIBRATE`.

## 2. Смена режима доставки

- [x] 2.1 Ввести интерфейс `ReminderAlarmScheduler` в `data/scheduling`; реализовать его текущим `ReminderScheduler`; внедрить интерфейс и DAO в `ReminderRepository` через конструктор, сохранив `getInstance` для продакшена. Проверка: `assembleDebug` и существующие тесты проходят.
- [x] 2.2 В `ReminderRepository.syncAllDeliveryModes` после `updateAllDeliveryModes` пересоздавать alarm для всех `PENDING`-напоминаний с новым `deliveryMode`. Проверка: unit-тест с fake DAO/scheduler фиксирует вызов `schedule` для каждой PENDING-записи с новым режимом.
- [x] 2.3 Добавить unit-тест `ReminderRepositorySyncDeliveryModeTest`: PENDING перепланируются, DONE/CANCELLED не перепланируются. Проверка: `testDebugUnitTest` проходит.
- [x] 2.4 Обновить раздел scheduling в `docs/ARCHITECTURE.md`: смена default deliveryMode синхронизирует Room и пересоздаёт alarm для PENDING. Проверка: описание совпадает с реализацией и spec `notification-delivery`.

## 3. AlarmActivity: back и действия

- [x] 3.1 Добавить в `ReminderIntents` действие `ACTION_COMPLETE`, extra `EXTRA_SNOOZE_MINUTES` и helper `actionBroadcast(...)`, возвращающий explicit `Intent` для `ReminderActionReceiver`. Проверка: unit-тест проверяет action, id и minutes в Intent.
- [x] 3.2 Обновить `ReminderActionReceiver`: обрабатывать `ACTION_COMPLETE`, брать минуты из `EXTRA_SNOOZE_MINUTES` с fallback на 10, обернуть тело в `try/catch/log` и всегда завершать `pendingResult`. Проверка: unit-тест на разбор extras и instrumented/manual-проверка действий.
- [x] 3.3 Заменить `AlarmActivity.onBackPressed()` на `OnBackPressedDispatcher`/`OnBackPressedCallback`: back отправляет `ACTION_CANCEL`, останавливает звук и закрывает Activity. Проверка: `lintDebug` больше не сообщает `GestureBackNavigation`.
- [x] 3.4 Перевести кнопки `AlarmActivity` на `actionBroadcast`: Done → `ACTION_COMPLETE`, Snooze → `ACTION_SNOOZE` с выбранными минутами, Cancel → `ACTION_CANCEL`; Activity закрывается сразу после отправки. Проверка: manual-тест подтверждает, что статус и alarm меняются после закрытия Activity.
- [x] 3.5 Добавить/обновить unit-тесты `AlarmActivityIntentExtrasTest` и `ReminderIntentsTest` на новые константы и helper. Проверка: `testDebugUnitTest` проходит.
- [x] 3.6 Обновить раздел полноэкранного UI в `docs/ARCHITECTURE.md`: back через dispatcher, действия через receiver, Activity не теряет операцию. Проверка: описание совпадает с `alarm-screen-wake` spec.

## 4. Виджеты вне main thread

- [x] 4.1 Обернуть `WidgetUpdater.updateAll` в `withContext(Dispatchers.IO)` и добавить `try/catch` на каждый виджет, чтобы сбой одного не блокировал остальные. Проверка: `WidgetUpdater` не содержит синхронных вызовов Room на main; StrictMode-проверка на устройстве.
- [x] 4.2 Сделать `ReminderListWidgetProvider.updateWidget` suspend, перевести `onUpdate` на `goAsync()` + `CoroutineScope(Dispatchers.IO)`, удалить `runBlocking`. Проверка: в файле не остаётся `runBlocking`; виджет обновляется после изменения напоминания.
- [x] 4.3 Перевести `WidgetToggleReceiver` на `goAsync()` + IO, брать статус из `reminder.status` после `getById`, убрать лишний `WidgetUpdater.updateAll` (репозиторий уже обновляет виджет). Проверка: `runBlocking` в файле отсутствует; тап по чекбоксу корректен при устаревшем intent.
- [x] 4.4 Удалить дублирующие `WidgetUpdater.updateAll` из `VoiceMindViewModel`; оставить обновление виджетов в `ReminderRepository` как единственной точке мутаций. Проверка: поиск по `WidgetUpdater` в ViewModel не находит вызовов; `testDebugUnitTest` проходит.
- [x] 4.5 Обновить раздел виджетов в `docs/ARCHITECTURE.md`: обновления выполняются вне main thread, receiver использует `goAsync`. Проверка: описание совпадает с кодом и spec `widget`.

## 5. Интеграционная проверка

- [x] 5.1 Запустить `openspec validate --all`; убедиться, что новый change и затронутые specs валидны. Проверка: exit 0, новых ошибок нет.
- [x] 5.2 Запустить `testDebugUnitTest` и `assembleDebug`; убедиться, что новые тесты проходят и APK собирается. Проверка: 0 падений, сборка успешна.
- [x] 5.3 Запустить `lintDebug`; устранить ошибки P0 (`GestureBackNavigation`), а для `StartActivityAndCollapseDeprecated` добавить обоснованный suppression или PendingIntent-вариант, чтобы lint не падал. Проверка: `lintDebug` завершается успешно; оставшиеся warnings не блокируют сборку.
- [ ] 5.4 Ручная проверка на устройстве: два уведомления подряд в VIBRATE сохраняют настройки канала; back на Android 16 отменяет напоминание; Done/Snooze/Cancel в `AlarmActivity` применяются после закрытия; смена delivery mode не даёт stale full-screen; виджет обновляется без jank. Проверка: на AVD API 36 прошли `AlarmDeliveryP0Test` (back, Done, Snooze) и `NotificationChannelsAndroidTest` (два VIBRATE-уведомления); осталась визуальная проверка widget jank.
- [x] 5.5 Добавить в `CHANGELOG.md` секцию `[Unreleased]`: пользовательский блок (**Исправлено**) и инженерные детали по change-id. Проверка: `python scripts/check-version.py` возвращает exit 0.
- [x] 5.6 Проверить, что версия не изменилась вне релиза: `version` остаётся прежней, бамп не выполняется. Проверка: `git diff -- version` пуст.

## Workflow follow-up

- Архивировать change после успешной ручной проверки и синхронизации specs.
- Проверить архивный результат и обновить `docs/FEATURE_PLAN.md` при необходимости.
