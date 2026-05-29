# План реализации VoiceMind

> **Напоминалка с голосовым вводом** — парсинг времени и текста, точное срабатывание, настраиваемый тип оповещения.
>
> Статус: **MVP реализован** (фазы 0–3 done). Фазы 4+ — в плане.
> Дата: 2026-05-29.
> Референс **стиля кода:** GymProgress. **UI и продукт** — по документам VoiceMind.

---

## Текущее состояние

| Компонент | Статус |
|-----------|--------|
| README, docs, AGENTS | ✅ |
| Структура пакетов | ✅ (частично, см. FOLDER_STRUCTURE) |
| Gradle / Kotlin / shell UI / тема | ✅ фаза 0 |
| STT + парсер + confirm + Room | ✅ фаза 1 |
| Alarm + уведомления + boot reschedule | ✅ фаза 2 |
| Списки / история / настройки | ✅ фаза 3 |

---

## Принципы

1. **Подтверждение перед alarm** — без автосохранения после парсинга.
2. **Точное время** — `setExactAndAllowWhileIdle`, reschedule после boot.
3. **Offline MVP** — STT on-device + rule-based parser; API — только fallback.
4. **Один ViewModel**, `safeDb`, ошибки в Snackbar.
5. **Не хранить аудио** в MVP (только `rawPhrase`).

---

## Дорожная карта

| Фаза | Результат | Размер | Риск |
|------|-----------|--------|------|
| **0** | Gradle, тема CLEAR BELL, shell, permissions skeleton | M | низкий |
| **1** | STT + парсер + Confirm + Room + ручной ввод | L | **высокий** (парсинг) |
| **2** | AlarmManager + уведомления + snooze/dismiss + boot | L | **высокий** (OEM/Doze) |
| **3** | Список предстоящих/история, редактирование, отмена | M | низкий |
| **4** | Режимы ALARM/full-screen, тихие часы, батарея | M | средний |
| **5** | Поиск, облачный fallback парсера, бэкап JSON | M | средний |
| **6** | Повторяющиеся напоминания | L | средний |
| **7** | Виджет, ярлык, Wear — опционально | M | средний |

**Критический путь:** 0 → 1 → 2 → 3. Фазы 4–7 — после рабочего MVP (можно 3 параллельно с 4).

---

## Фаза 0. Bootstrap

### Задачи

- [x] Gradle из GymProgress: `libs.versions.toml`, `version.properties`, patch bump.
- [x] `applicationId` `com.example.voicemind`, minSdk **26** (не 29, как в GymProgress).
- [x] `VoiceMindTheme` — палитра CLEAR BELL (бирюза + нейтрали), тёмная тема.
- [x] `MainActivity`, `VoiceMindViewModel`, `NavigationSuiteScaffold`: Главная / Список / Настройки.
- [x] `AndroidManifest`: receivers, permissions declared.
- [x] `.cursor/rules` — kotlin-android, compose-ui.

### Критерии готовности

- `assembleDebug` OK.
- Три таба, Snackbar для `errorMessage`.

### Оценка: **3–5 дней**

---

## Фаза 1. Голос → текст → парсинг → подтверждение

### 1.1 Room v1

- [x] Entity `Reminder`, DAO, `AppDatabase` v1.
- [x] Insert только со статусом `SCHEDULED` после confirm.

### 1.2 SpeechInputController

- [x] `SpeechRecognizer`, ru-RU.
- [x] UI: Listening / Processing / Error.
- [x] Permission `RECORD_AUDIO` + rationale.
- [x] Fallback на системный диалог распознавания (OEM-friendly).

### 1.3 ReminderParser (unit tests)

- [x] `ParseResult`, паттерны из [REMINDER_PARSING.md](REMINDER_PARSING.md).
- [x] ≥ 38 тестов с фиксированным `Instant now`.
- [x] Warnings + confidence.

### 1.4 ConfirmReminderScreen (overlay)

- [x] Показ `fireAt`, `body`, warnings.
- [x] DatePicker + TimePicker.
- [x] `DeliveryModePicker`.
- [x] Кнопка «Сохранить» → Room + `schedule()`.

### 1.5 Текстовый ввод

- [x] `OutlinedTextField` на Home → тот же parser → Confirm.
- [x] Ручное создание без парсинга (пустая форма с дефолтным временем).

### Критерии готовности

- Фраза «завтра в 9:00 позвонить соседу» → Confirm с правильными полями (на тестовом устройстве/эмуляторе с Google STT).
- Ручной picker при `NO_TIME_FOUND`.
- Тесты парсера green в CI/`test`.

### Оценка: **2–3 недели**

---

## Фаза 2. Планирование и срабатывание

### 2.1 ReminderScheduler

- [x] `schedule` / `cancel` / `rescheduleAll`.
- [x] `ReminderAlarmReceiver` → `ReminderNotifier`.

### 2.2 Notification channels + modes

- [x] `NOTIFICATION`, `VIBRATE_ONLY`, `SILENT`, `ALARM` — [NOTIFICATION_MODES.md](NOTIFICATION_MODES.md).
- [x] `ReminderActionReceiver`: Done, Snooze 10m, Cancel.

### 2.3 Permissions UX

- [x] `POST_NOTIFICATIONS` (33+) при старте.
- [x] `SCHEDULE_EXACT_ALARM` — проверка + кнопка в настройках.
- [x] При сохранении Confirm → `schedule()`.

### 2.4 BootReceiver

- [x] `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` → reschedule all.

### 2.5 Статусы

- [x] FIRED при показе; DISMISSED / CANCELLED / SNOOZED (snooze → снова SCHEDULED).

### Критерии готовности

- Напоминание через 2 мин (тест) срабатывает после сворачивания app.
- Snooze переносит `fireAt`.
- Reboot → срабатывание всё ещё приходит (manual test).

### Оценка: **1–2 недели**

---

## Фаза 3. Списки и управление

### 3.1 ReminderListScreen

- [x] Вкладки: Предстоящие (`fireAt ASC`) / История.
- [x] `ReminderCard`, swipe to cancel (опционально).

### 3.2 ReminderDetailScreen

- [x] Просмотр, изменение времени/текста/режима → reschedule.
- [x] Удалить / отменить.

### 3.3 Home — ближайшее

- [x] Карточка next reminder + относительное время («через 2 ч»).

### 3.4 Settings

- [x] `defaultDeliveryMode` в DataStore.
- [x] `confirmBeforeSchedule` (default on).
- [ ] (Фаза 4+) Тихие часы — `quietHoursStart` / `End`.
- [ ] (Фаза 4+) Запрос исключения из оптимизации батареи.

### Критерии готовности

- 10+ напоминаний в списке без лагов.
- Редактирование времени перепланирует alarm.

### Оценка: **1 неделя**

---

## Фаза 4. Усиленные оповещения и система

### 4.1 Режим ALARM

- [x] Канал high priority, vibration pattern.
- [ ] `fullScreenIntent` + `AlarmActivity` — крупный текст, Snooze / Done.

### 4.2 LOUD_ALARM (опционально внутри фазы)

- [ ] Отдельная Activity, keep screen on.

### 4.3 Тихие часы

- [ ] DataStore start/end → override mode при показе.

### 4.4 Батарея

- [ ] Экран с объяснением + Intent `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (не навязчиво).

### 4.5 Повтор при ALARM

- [ ] Если не dismissed за 5 мин — один повтор (настраиваемо).

### Оценка: **1–2 недели**

---

## Фаза 5. Улучшения

### 5.1 Поиск по `body` / `rawPhrase`

### 5.2 Облачный парсер (OpenRouter) как fallback

- [ ] Только при низком confidence + opt-in.
- [ ] `OPENROUTER_API_KEY` в `local.properties`.

### 5.3 Бэкап JSON + Auto Backup rules

- [ ] `schemaVersion: 1`, reschedule after import.

### Оценка: **1–2 недели**

---

## Фаза 6. Повторяющиеся напоминания

- [ ] `RecurrenceRule` (weekly, daily) — отдельная таблица или поля в Reminder.
- [ ] После FIRED → вычислить следующий `fireAt` и reschedule.
- [ ] Парсер: «каждый понедельник в 9» — расширение паттернов.

### Оценка: **2+ недели**

---

## Фаза 7. Виджет и быстрый доступ

- [ ] App shortcut → сразу listening.
- [ ] Glance widget — mic + next reminder.

---

## MVP (что считать «готово к себе»)

Фазы **0 + 1 + 2 + 3**:

- Сказал → подтвердил → в списке → в срок пришло уведомление → отложил или закрыл.

Всё остальное — после dogfooding.

---

## Риски и митигация

| Риск | Митигация |
|------|-----------|
| STT ошибся | Confirm + редактирование текста |
| Парсер ошибся | warnings + manual pickers |
| OEM убивает alarm | exact alarm permission + boot reschedule + docs пользователю |
| «Завтра в девять» неоднозначно | дефолт 09:00 + warning |

---

## Синхронизация документации

После каждой фазы:

- Обновить дату в `PROJECT_OVERVIEW.md` / `ARCHITECTURE.md`.
- В `README` пометка «MVP реализован» — done.

---

## Связанные документы

- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) — идеи add/remove
- [REMINDER_PARSING.md](REMINDER_PARSING.md)
- [NOTIFICATION_MODES.md](NOTIFICATION_MODES.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
