# Архитектура VoiceMind

> Дата: 2026-05-29 · Напоминалка с голосовым парсингом (MVP фазы 0–3)

## Поддерживаемые версии Android

| Параметр | Значение | Зачем |
|----------|----------|--------|
| `minSdk` | **26** (Android 8.0) | Установка на старые телефоны; каналы уведомлений из коробки |
| `targetSdk` | 36 | Требования Google Play, новое поведение разрешений |
| `compileSdk` | 36 | Сборка против актуальных API |

GymProgress с `minSdk 29` **не ставится** на Android 9 и ниже — здесь порог ниже.

При реализации alarm/уведомлений — ветки по API (`SCHEDULE_EXACT_ALARM` с 31, `POST_NOTIFICATIONS` с 33), не поднимать `minSdk` без причины.

## Модули

```
:root
└── :app
```

## Пакеты

```
com.example.voicemind/
├── MainActivity.kt
├── VoiceMindApplication.kt
├── AppDestinations.kt
├── data/
│   ├── AppDatabase.kt
│   ├── Reminder.kt
│   ├── ReminderDao.kt
│   ├── ReminderRepository.kt
│   ├── ReminderStatus.kt
│   ├── DeliveryMode.kt
│   ├── SettingsRepository.kt
│   ├── FormatUtils.kt
│   ├── parse/
│   │   ├── ReminderParser.kt
│   │   ├── ParseResult.kt
│   │   ├── ParseWarning.kt
│   │   └── ParseResultExtensions.kt
│   ├── speech/
│   │   ├── SpeechInputController.kt
│   │   └── SpeechRecognition.kt
│   ├── scheduling/
│   │   ├── ReminderScheduler.kt
│   │   ├── ReminderIntents.kt
│   │   ├── ReminderAlarmReceiver.kt
│   │   ├── ReminderActionReceiver.kt
│   │   └── BootReceiver.kt
│   └── notification/
│       ├── ReminderNotifier.kt
│       └── NotificationChannels.kt
├── viewmodel/
│   ├── VoiceMindViewModel.kt
│   ├── ListeningState.kt
│   ├── PendingReminderConfirm.kt
│   ├── ManualReminderDraft.kt
│   └── ReminderListTab.kt
├── ui/
│   ├── navigation/
│   │   └── AppDestination.kt
│   ├── screens/
│   │   ├── HomeScreen.kt
│   │   ├── ConfirmReminderScreen.kt
│   │   ├── ManualReminderScreen.kt
│   │   ├── ReminderListScreen.kt
│   │   ├── ReminderDetailScreen.kt
│   │   ├── ReminderDateTimeDialogs.kt
│   │   └── SettingsScreen.kt
│   ├── components/
│   │   ├── DateTimeField.kt
│   │   ├── EmptyState.kt
│   │   ├── MicButton.kt
│   │   ├── SwipeToRevealBox.kt
│   │   └── WarningCard.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Dimens.kt
└── util/
    └── ReminderPermissions.kt
```

Каталоги `data/parse`, `data/speech`, `data/scheduling`, `data/notification` — в репозитории (см. FOLDER_STRUCTURE).

## Поток создания напоминания

```mermaid
sequenceDiagram
    participant U as User
    participant H as HomeScreen
    participant VM as VoiceMindViewModel
    participant STT as SpeechInputController
    participant P as ReminderParser
    participant C as ConfirmScreen
    participant DB as Room
    participant S as ReminderScheduler

    U->>H: Говорит фразу
    H->>VM: startListening()
    VM->>STT: SpeechRecognizer
    STT-->>VM: rawPhrase
    VM->>P: parse(rawPhrase, now)
    P-->>VM: ParseResult
    VM->>C: show confirm
    U->>C: Правит / Сохранить
    C->>VM: confirm(fireAt, body, mode)
    VM->>DB: insert Reminder
    VM->>S: schedule(reminder)
```

## VoiceMindViewModel

**StateFlow:**

- `upcomingReminders` — из DAO `fireAt ASC` где `status = SCHEDULED`
- `historyReminders` — `FIRED | DISMISSED | CANCELLED`, `fireAt DESC`
- `nextReminder` — первый из upcoming
- `listeningState` — Idle / Listening / Processing
- `pendingConfirm` — для Confirm overlay (голос)
- `manualDraft` — ручной ввод / fallback
- `detailReminder` — просмотр/редактирование из списка
- `listTab` — Upcoming / History
- `defaultDeliveryMode` — из DataStore (**глобальный** режим доставки, единый для всех)
- `useVibration` / `alarmRingtoneUri` / `alarmVolume` / `dismissBehavior` — из DataStore
- `confirmBeforeSchedule` — из DataStore
- `fallbackToSystemSpeech` — флаг fallback на системный диалог
- `requestNotificationsPermission` — сигнал UI запросить разрешение на уведомления
- `errorMessage`

**Методы:**

- `startListening()` / `stopListening()` / `onSpeechResult(String)`
- `confirmVoiceReminder()` / `saveManualReminder(...)` — insert + schedule
- `cancelReminder(id)` / `openReminderForEdit(id)` / `openReminderDetail(reminder)`
- `updatePending(...)` — правка полей перед confirm
- `safeDb { }` — как в GymProgress

Бизнес-логику парсинга **не** держать в Composable.

## Навигация

| Таб | Экран |
|-----|--------|
| Главная | `HomeScreen` — микрофон, ближайшее напоминание |
| Список | `ReminderListScreen` — предстоящие / история |
| Настройки | `SettingsScreen` |

Overlay:

- `ConfirmReminderScreen` — после STT/ввода
- `ReminderDetailScreen` — просмотр/редактирование/отмена

Паттерн overlay + `BackHandler` — как GymProgress.

## ReminderScheduler

```kotlin
interface ReminderAlarmScheduler {
    fun schedule(reminder: Reminder)
    fun cancel(reminderId: Long)
    fun rescheduleAll(reminders: List<Reminder>)
}

class ReminderScheduler(context: Context) : ReminderAlarmScheduler
```

Единственное место работы с `AlarmManager`. Fallback на `setAndAllowWhileIdle` при отсутствии `SCHEDULE_EXACT_ALARM`.

При update напоминания: `cancel` + `schedule` с новым `fireAt`.

При смене `settings.defaultDeliveryMode` репозиторий обновляет `deliveryMode` в Room и пересоздаёт alarm для всех `PENDING`-напоминаний, чтобы `EXTRA_DELIVERY_MODE` в PendingIntent не устаревал. Интерфейс `ReminderAlarmScheduler` позволяет подменять планировщик в unit-тестах.

## Сортировка списков

| Список | ORDER BY |
|--------|----------|
| Предстоящие | `fireAt ASC, id ASC` |
| История | `fireAt DESC, id DESC` |

## Room

- DB: `voice_mind_db`, v1: таблица `reminders`.
- Индекс: `(fireAt)`, `(status)`.
- Миграции явные; destructive только debug.

## STT

`SpeechInputController` + `SpeechRecognition` (object):

- обёртка над `SpeechRecognizer`;
- fallback на системный диалог `RecognizerIntent` (OEM-friendly);
- поток результатов в ViewModel;
- обработка ошибок (нет сети для offline-движка не должно ломать — on-device);
- timeout 10 с.

Аудио на диск **не пишем** в MVP.

## Парсер

- `ReminderParser` — без Android SDK, тесты в `test/`.
- Зависимость от `Clock` / передаваемого `Instant now` для тестов.

## Уведомления

`ReminderNotifier.show(reminder, quietMode = null)`:

- строит `NotificationCompat` по mode;
- для `ALARM` — высокий приоритет + vibration pattern; `fullScreenIntent` как fallback для screen-off/locked;
- при активных тихих часах использует `SILENT` или канал `reminders_quiet_vibrate_v1` без звука и без `fullScreenIntent`.

`NotificationChannels.createAll` вызывается один раз в `VoiceMindApplication.onCreate`. При показе уведомления каналы не удаляются и не пересоздаются; удаляются только legacy-ID (`reminders_default`, `reminders_alarm`, `reminders_silent`, `reminders_vibrate`), текущие `_v2`-каналы сохраняют пользовательские настройки.

## Тихие часы

- `QuietHoursSettings` — интервал `[start, end)` в локальном времени; переход через полночь поддержан, `start == end` выключает тишину.
- Настройки читаются в момент срабатывания, а не при планировании: менять `fireAt` при смене тихих часов не нужно.
- `ReminderAlarmReceiver` синхронно читает настройки перед `startActivity`; при активных тихих часах не запускает `AlarmActivity`, не берёт wake lock и не играет звук.
- `ReminderNotifier` в тихие часы показывает уведомление в канале `SILENT` или `reminders_quiet_vibrate_v1` (короткая вибрация), сохраняя действия и `deleteIntent`.

## Виджеты

- `WidgetUpdater.updateAll` работает в `Dispatchers.IO`, поэтому вызовы из `MainActivity.onResume` и репозитория не блокируют main thread.
- `WidgetToggleReceiver` использует `goAsync` + IO; статус напоминания читается из Room, а не из extras устаревшего intent.
- `ReminderListWidgetProvider.onUpdate` использует `goAsync`; `updateWidget` — `suspend`, без `runBlocking`.
- Обновление виджетов после мутаций остаётся в `ReminderRepository`; ViewModel не дублирует эти вызовы.

## Полноэкранный UI при срабатывании (ALARM/VIBRATE)

При срабатывании напоминания `ReminderAlarmReceiver`:

1. **Синхронно в `onReceive`** (до `goAsync`) вызывает `startActivity(AlarmActivity)` — окно всплывает поверх любого приложения, включая immersive (игры/видео).
2. **В корутине** (после `goAsync`): WakeLock, `AlarmSoundPlayer.play`, `ReminderNotifier.show` (notification с `fullScreenIntent` как fallback для screen-off и OEM-блокировок), обновление статуса/рекуррентности.

`AlarmActivity` (`launchMode="singleTask"`, `showOnLockScreen`, `turnScreenOn`):

- загружает `Reminder` через `VoiceMindViewModel` по `EXTRA_REMINDER_ID`;
- если `deliveryMode` NOTIFICATION/SILENT — `finish()` без показа UI;
- если ALARM/VIBRATE — полноэкранный UI с кнопками Готово/Отложить/Отменить;
- системный back обрабатывается через `OnBackPressedDispatcher`: отменяет напоминание, останавливает звук и закрывает Activity;
- действия экрана отправляют explicit broadcast в `ReminderActionReceiver` (`ACTION_COMPLETE`, `ACTION_SNOOZE` с минутами, `ACTION_CANCEL`) и закрываются, поэтому операция не теряется вместе с `viewModelScope`;
- `onNewIntent` обновляет `reminderId` при повторном срабатывании (звук перезапускается корутиной ресивера).

## Референс GymProgress (только код)

| Брать | Не брать |
|-------|----------|
| `safeDb`, StateFlow, один ViewModel | Экраны журнала тренировок |
| `version.properties`, gradle structure | IRON CORE / Volt UI |
| overlay navigation | Scoring, exercises, Room schema |
| DataStore для настроек | OpenRouter для «тренера» |

## Тестирование

| Уровень | Что |
|---------|-----|
| Unit | `ReminderParser`, `ImportMerger` (позже) |
| Unit | `ReminderScheduler` с fake AlarmManager (robolectric опционально) |
| Instrumented | DAO, receiver + reschedule |
| Manual | reboot, snooze, exact alarm permission denied |

## Связанные документы

- `openspec/specs/reminder-parsing/spec.md`
- `openspec/specs/notification-delivery/spec.md`
- [FEATURE_PLAN.md](FEATURE_PLAN.md)
