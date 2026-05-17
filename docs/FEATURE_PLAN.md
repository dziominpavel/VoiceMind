# План реализации VoiceMind

> **Напоминалка с голосовым вводом** — парсинг времени и текста, точное срабатывание, настраиваемый тип оповещения.
>
> Статус: **план, код не начат** (кроме docs и структуры папок).
> Дата: 2026-05-17.
> Референс **стиля кода:** GymProgress. **UI и продукт** — по документам VoiceMind.

---

## Текущее состояние

| Компонент | Статус |
|-----------|--------|
| README, docs, AGENTS | ✅ |
| Структура пакетов | ✅ (частично, см. FOLDER_STRUCTURE) |
| Gradle / Kotlin / shell UI | ✅ фаза 0 (2026-05-17) |
| STT + парсер + alarm | ⏳ фазы 1–2 |

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

- [ ] Gradle из GymProgress: `libs.versions.toml`, `version.properties`, patch bump.
- [x] `applicationId` `com.example.voicemind`, minSdk **26** (не 29, как в GymProgress).
- [ ] `VoiceMindTheme` — палитра CLEAR BELL (бирюза + нейтрали).
- [ ] `MainActivity`, `VoiceMindViewModel` (пустой), `NavigationSuiteScaffold`: Главная / Список / Настройки.
- [ ] `AndroidManifest`: receivers заглушки, permissions declared.
- [ ] `.cursor/rules` — kotlin-android, compose-ui (адаптация).

### Критерии готовности

- `assembleDebug` OK.
- Три таба, Snackbar для `errorMessage`.

### Оценка: **3–5 дней**

---

## Фаза 1. Голос → текст → парсинг → подтверждение

### 1.1 Room v1

- [ ] Entity `Reminder`, DAO, `AppDatabase` v1.
- [ ] Insert только со статусом `SCHEDULED` после confirm.

### 1.2 SpeechInputController

- [ ] `SpeechRecognizer`, ru-RU.
- [ ] UI: Listening / Processing / Error.
- [ ] Permission `RECORD_AUDIO` + rationale.

### 1.3 ReminderParser (unit tests)

- [ ] `ParseResult`, паттерны из [REMINDER_PARSING.md](REMINDER_PARSING.md).
- [ ] ≥ 30 тестов с фиксированным `Clock`.
- [ ] Warnings + confidence.

### 1.4 ConfirmReminderScreen (overlay)

- [ ] Показ `fireAt`, `body`, warnings.
- [ ] DatePicker + TimePicker.
- [ ] `DeliveryModePicker` (значения пока сохраняются, alarm — фаза 2).
- [ ] Кнопка «Сохранить» → Room **без** schedule (или schedule stub log).

### 1.5 Текстовый ввод

- [ ] `OutlinedTextField` на Home → тот же parser → Confirm.

### Критерии готовности

- Фраза «завтра в 9:00 позвонить соседу» → Confirm с правильными полями (на тестовом устройстве/эмуляторе с Google STT).
- Ручной picker при `NO_TIME_FOUND`.
- Тесты парсера green в CI/`test`.

### Оценка: **2–3 недели**

---

## Фаза 2. Планирование и срабатывание

### 2.1 ReminderScheduler

- [ ] `schedule` / `cancel` / `rescheduleAll`.
- [ ] `ReminderAlarmReceiver` → `ReminderNotifier`.

### 2.2 Notification channels + modes

- [ ] Реализовать `NOTIFICATION`, `VIBRATE_ONLY`, `SILENT` — [NOTIFICATION_MODES.md](NOTIFICATION_MODES.md).
- [ ] `ReminderActionReceiver`: Done, Snooze 10m, Cancel.

### 2.3 Permissions UX

- [ ] `POST_NOTIFICATIONS` (33+).
- [ ] `SCHEDULE_EXACT_ALARM` — проверка + Intent в настройки.
- [ ] При сохранении Confirm → `schedule()`.

### 2.4 BootReceiver

- [ ] `BOOT_COMPLETED` → reload SCHEDULED from DB → schedule all.

### 2.5 Статусы

- [ ] FIRED при показе; DISMISSED / CANCELLED / SNOOZED по действиям.

### Критерии готовности

- Напоминание через 2 мин (тест) срабатывает после сворачивания app.
- Snooze переносит `fireAt`.
- Reboot → срабатывание всё ещё приходит (manual test).

### Оценка: **1–2 недели**

---

## Фаза 3. Списки и управление

### 3.1 ReminderListScreen

- [ ] Вкладки: Предстоящие (`fireAt ASC`) / История.
- [ ] `ReminderCard`, swipe to cancel (опционально).

### 3.2 ReminderDetailScreen

- [ ] Просмотр, изменение времени/текста/режима → reschedule.
- [ ] Удалить / отменить.

### 3.3 Home — ближайшее

- [ ] Карточка next reminder + относительное время («через 2 ч»).

### 3.4 Settings

- [ ] `defaultDeliveryMode` в DataStore.
- [ ] `confirmBeforeSchedule` (default on).

### Критерии готовности

- 10+ напоминаний в списке без лагов.
- Редактирование времени перепланирует alarm.

### Оценка: **1 неделя**

---

## Фаза 4. Усиленные оповещения и система

### 4.1 Режим ALARM

- [ ] Канал high priority, fullScreenIntent.
- [ ] `AlarmActivity` — крупный текст, Snooze / Done.

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
- Завести `docs/TECH_DEBT.md` при появлении известных багов.
- В `README` снять пометку «инициализация», когда есть MVP.

---

## Связанные документы

- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md) — идеи add/remove
- [REMINDER_PARSING.md](REMINDER_PARSING.md)
- [NOTIFICATION_MODES.md](NOTIFICATION_MODES.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
