# Code review: backlog

Итоги код-ревью VoiceMind от 2026-10-31. P0-дефекты вынесены в change
`fix-p0-alarm-delivery`; ниже — предложения, которые остались за его пределами.

## Уже закрыто в `fix-p0-alarm-delivery`

- [x] Каналы уведомлений создаются один раз; текущие каналы не удаляются при показе.
- [x] Системный back в `AlarmActivity` обрабатывается через `OnBackPressedDispatcher`.
- [x] Смена `defaultDeliveryMode` пересоздаёт alarm для `PENDING`.
- [x] Обновление виджетов вынесено с main thread; `WidgetToggleReceiver` и `ReminderListWidgetProvider` используют `goAsync`.
- [x] Действия `AlarmActivity` идут через `ReminderActionReceiver` и не теряются при закрытии Activity.
- [x] Убраны дублирующие `WidgetUpdater.updateAll` из `VoiceMindViewModel`.

## P1 — надёжность и производительность

- [ ] **Обработка ошибок во всех receiver'ах.** `ReminderAlarmReceiver`, `BootReceiver`, `ScreenOnReceiver` и `VoiceMindApplication.appScope` всё ещё не ловят исключения. Добавить общий helper `runReceiverAction` с `goAsync`, `try/catch/log` и обязательным `finish()`.
- [ ] **`safeDb` не должен показывать `e.message`.** Всегда использовать локализованный fallback, отдельно логировать исключение и не перехватывать `CancellationException`.
- [ ] **`fireOverdue` без N+1.** Один запрос `getOverduePending`, batch-обновление статусов, один снимок quiet hours, одно обновление виджетов и один проход уведомлений.
- [ ] **Обновление статусов разрешений в настройках.** Перечитывать `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM` и `USE_FULL_SCREEN_INTENT` при `ON_RESUME`, иначе карточки показывают устаревшее состояние после возврата из системных настроек.
- [ ] **Единая семантика DONE.** `dismissReminder` и `completeReminder` по-разному меняют `fireAt`; добавить `completedAt` или зафиксировать одно поведение для уведомления и UI.
- [ ] **Повторяющиеся напоминания.** Одна строка на серию не позволяет одновременно хранить текущее срабатывание и следующее. Рассмотреть `ReminderOccurrence`/`lastFiredAt` + `nextFireAt`, чтобы `AlarmActivity` не показывал следующее время, а Done/Snooze не теряли серию.
- [ ] **Слои data → ui.** `ReminderRepository` импортирует `ui.widget.WidgetUpdater`, `ReminderAlarmReceiver` — `ui.screens.AlarmActivity`, ViewModel — `WidgetActions`. Вынести widget-обновления в application-scope observer, а intent-константы — в нейтральный пакет.

## P2 — архитектура и тесты

- [ ] **`TimeProvider`/`Clock`.** Заменить разбросанные `System.currentTimeMillis()`/`Instant.now()` на инъекцию времени; это откроет unit-тесты для snooze, overdue и quiet hours.
- [ ] **Единый settings-flow.** Сейчас `SettingsRepository` создаёт отдельный flow на каждую настройку. Собрать `AppSettings` и один `StateFlow`, а поля сделать производными.
- [ ] **Room schema и migration-тесты.** Включить `exportSchema = true`, настроить `room.schemaLocation`, добавить `androidx.room:room-testing` и тесты миграций 1→7.
- [ ] **Типы вместо строк.** Заменить `Reminder.status`/`deliveryMode` на enum с `TypeConverter`; убрать `valueOf`/`name` по коду.
- [ ] **`collectAsStateWithLifecycle`.** Добавить `lifecycle-runtime-compose` и перейти на lifecycle-aware collection; сгруппировать состояние в screen-level `UiState`.
- [ ] **Тесты core-логики.** Нет тестов на `ReminderRepository` (кроме нового sync), `ReminderNotifier`, `ReminderActionReceiver`, `VoiceMindViewModel`, `SettingsRepository` и миграции.
- [ ] **Lint в CI.** `lintDebug` содержит 2 ошибки и 112 предупреждений; после P0-исправлений включить lint в CI и не пропускать ошибки.
- [ ] **Unused resources/deps.** Удалить 57 неиспользуемых ресурсов, `androidx.startup`, glance-записи в version catalog и `ExampleUnitTest`.

## P3 — уменьшение и структура кода

- [ ] **`FormatUtils`.** Вынести форматтеры в `val`, объединить `formatHistoryDate`/`formatShortDate`, убрать повторное создание `DateTimeFormatter`.
- [ ] **`SettingsScreen`.** Сделать список режимов доставки data-driven (сейчас 5 повторяющихся `DeliveryModeOption`), вынести permission-строки в ресурсы, разбить карточки на компоненты.
- [ ] **`ReminderListScreen`.** Убрать неиспользуемые `onComplete`/`onMicPermissionDenied`, вынести `UpcomingReminderCard`/`HistoryReminderCard` в отдельные файлы, заменить хардкод заголовков групп на ресурсы.
- [ ] **`MainActivity`.** Вынести `VoiceMindApp` в `ui/VoiceMindApp.kt`, оставив Activity только launchers/permissions.
- [ ] **`ReminderParser`.** При расширении грамматики разделить на `DateExtractor`, `TimeExtractor`, `RecurrenceExtractor`; убрать дубли `nextOrSameWeekday`/`nextWeekday`, вынести `Regex("\\s+")` в константу.
- [ ] **`ReminderRepository`.** Удалить неиспользуемые `reschedule` и `stopRecurrence` (если не появятся вызовы), рассмотреть batch-обновление в `rescheduleAll`.

## Приоритет

1. P1 — ошибки receiver'ов, `safeDb`, `fireOverdue`, permission-refresh.
2. P2 — `TimeProvider`, Room schema/тесты, типы enum.
3. P3 — форматтеры, UI-разбиение, парсер.
4. Повторяющиеся напоминания — отдельный change с миграцией и продуктовым решением по Done/Snooze.
