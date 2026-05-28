# План улучшения UI/UX VoiceMind

> Дата: 2026-05-29
> Статус: план, не начато

---

## Контекст

MVP (фазы 0–3) реализован. Интерфейс работает, но имеет узкие места в usability: формы сырые, детали read-only, primary action (микрофон) не выделен визуально, отсутствуют empty states и micro-interactions.

---

## Фаза 0. Дизайн-фундамент (1–2 дня)

### Задачи

- [ ] Расширить `Dimens.kt`: `xxs=4`, `sm=12`, `xl=32`, `xxl=48`, `xxxl=64`.
- [ ] Создать `Type.kt` (`VoiceMindTypography`) с `tabular nums` для времени; крупный `displayMedium` для `fireAt` на карточках.
- [ ] Добавить `Shapes.kt`: `Rounded24`, `Rounded16`, `Rounded12`, `Rounded8` — применить к Card / Button / BottomSheet.
- [ ] Добавить `Color.kt`: `WarningAmber`, `WarningContainer`, `SuccessGreen` для статусов/предупреждений.
- [ ] Создать reusable `EmptyState` component (иконка + заголовок + подзаголовок + CTA).
- [ ] Создать reusable `WarningCard` (amber container + иконка + текст).

### Критерии

- `assembleDebug` OK.
- Все существующие экраны компилируются (новые токены пока не применены или применены без ошибок).

---

## Фаза 1. HomeScreen (2–3 дня)

### Задачи

- [ ] **`MicButton`**:
  - Увеличить до 96.dp (или 120.dp).
  - Добавить `animateFloat` пульс кольца при `Listening`.
  - Использовать `IconButton` с custom `containerColor = Material.colorScheme.primary` и `contentColor = OnPrimary`.
- [ ] **NextReminderCard**:
  - Добавить иконку `DeliveryMode` (trailing).
  - Relative time крупнее (`titleMedium`), абсолютное — `labelMedium`.
  - Тап по карточке → `ReminderDetailScreen`.
- [ ] **Быстрый текстовый ввод**:
  - Добавить `OutlinedTextField` под микрофоном с кнопкой «→» (send) или FAB.
  - Ввод сразу идёт в `ReminderParser` → `ConfirmReminderScreen`.
- [ ] **Empty state**:
  - Если `nextReminder == null` — `EmptyState` с иконкой `Schedule` и текстом «Скажите или введите первое напоминание».
- [ ] **FAB** (опционально):
  - `FloatingActionButton` с иконкой `Add` для перехода в `ManualReminderScreen`.
  - При наличии quick input FAB можно отложить.

### Что убрать

- Раздел «Вручную» (Card с subtitle + кнопка). Заменить на FAB или compact row.

### Критерии

- Микрофон заметен на первый взгляд.
- Текстовый ввод с Home работает без лишних экранов.
- Empty state красивый и понятный.

---

## Фаза 2. Формы — Confirm / Manual (3–4 дня)

### Задачи

- [ ] **Единый блок даты/времени**:
  - Карточка `DateTimeField` с иконками `CalendarToday` / `AccessTime`.
  - Тап открывает `DatePicker` / `TimePicker`.
  - Убрать две раздельные кнопки «Дата» / «Время».
- [ ] **Presets (chips)**:
  - Ряд под `DateTimeField`: «через 15 мин», «через 1 ч», «завтра 9:00», «через неделю».
  - Тап сразу устанавливает `fireAtMillis`.
- [ ] **DeliveryMode — сегмент/чипсы**:
  - Заменить `RadioButton` на `SingleChoiceSegmentedButtonRow` или 4 `FilterChip` с иконками.
  - Иконки: `Notifications`, `Alarm`, `Vibration`, `DoNotDisturb`.
- [ ] **WarningCard**:
  - Заменить `ParseWarningsBlock` на `WarningCard` (amber surface + иконка `Warning`).
- [ ] **Sticky Save**:
  - Кнопка «Сохранить» внутри `BottomAppBar` или `Surface` с `shadowElevation`, чтобы она не уезжала при скролле.
- [ ] **BottomSheet для Confirm**:
  - Перевести `ConfirmReminderScreen` из full-screen overlay в `ModalBottomSheet` (быстрее, не отрывает от контекста).
  - `ManualReminderScreen` оставить full-screen (там больше полей).

### Критерии

- Создание напоминания быстрее на 2–3 тапа за счёт presets.
- Режим оповещения выбирается за 1 тап.
- Warnings визуально заметны.

---

## Фаза 3. Список и детали (3–4 дня)

### Задачи

- [ ] **ReminderCard polish**:
  - Добавить trailing иконку `DeliveryMode`.
  - Добавить relative time («через 2 ч») рядом с абсолютным.
  - Просроченные (`fireAt < now && status == SCHEDULED`) — subtle error tint border или error container.
  - Анимация появления списка (`AnimatedVisibility` / `animateItemPlacement`).
- [ ] **Убрать кнопку «Отменить»** с карточки (swipe-to-dismiss уже есть).
- [ ] **Добавить SwipeAction «Отложить»**:
  - Swipe влево = Cancel, swipe вправо = Snooze (+10 мин).
  - Или две иконки в background content.
- [ ] **Empty states**:
  - «Предстоящие» — `EmptyState` с CTA «Создать».
  - «История» — `EmptyState` с иконкой `History`.
- [ ] **SearchBar (skeleton)**:
  - Добавить `SearchBar` наверху `ReminderListScreen` (inactive state, реальный поиск — фаза 5).
  - Пока placeholder без логики.

### ReminderDetailScreen — критичный редизайн

- [ ] Добавить actions:
  - **Редактировать** → открывает `ManualReminderScreen` с `editingReminderId`.
  - **Удалить** → `AlertDialog` подтверждение.
  - **Отменить** (если scheduled) → `viewModel.cancelReminder()`.
  - **Отложить** → bottom sheet с preset (+10 мин / +1 ч / custom).
  - **Повторить** → создаёт копию с `fireAt = now + 1h` (или picker).
- [ ] Отображать `rawPhrase` более компактно (если отличается от `body`).
- [ ] Иконка режима крупно рядом со статусом.

### Критерии

- Из деталей можно редактировать, откладывать и удалять.
- Карточки в списке содержат иконку режима + relative time.
- Swipe-actions интуитивны.

---

## Фаза 4. Settings (1–2 дня)

### Задачи

- [ ] **Группировка секций**:
  - «Поведение» (default mode, confirmBeforeSchedule).
  - «Разрешения» (notifications, exact alarm) с **кнопками запроса**.
  - «О приложении» (version, test hint).
- [ ] **DeliveryMode default**:
  - Заменить радио на compact chips/segment (тот же компонент, что в Confirm).
- [ ] **Permissions UX**:
  - Если `POST_NOTIFICATIONS` не granted — показать `WarningCard` + кнопка «Разрешить».
  - Если exact alarm denied — `WarningCard` + кнопка «Открыть настройки».
- [ ] **Настройки тихих часов (UI-only)**:
  - Два `TimePicker` (start / end). Логика — фаза 4 FEATURE_PLAN.

### Критерии

- Настройки читаются как сгруппированный список.
- Пользователь видит, каких разрешений не хватает, и может исправить за 1 тап.

---

## Фаза 5. Анимации и микро-взаимодействия (2–3 дня)

### Задачи

- [ ] **Mic pulse** — `Listening` анимация (scale + alpha ring).
- [ ] **Overlay transitions** — `AnimatedContent` / `AnimatedVisibility` для Confirm / Manual / Detail.
- [ ] **List item animations** — `LazyColumn` `animateItemPlacement` при insert/remove.
- [ ] **Haptics** — лёгкая вибрация при successful save, cancel, swipe-to-dismiss.
- [ ] **Snackbar** — anchored к `BottomAppBar` / FAB, а не к низу экрана.

### Критерии

- Приложение ощущается «живым» — каждый экшн сопровождается обратной связью.

---

## Оценки

| Фаза | Срок |
|------|------|
| 0. Фундамент | 1–2 дня |
| 1. Home | 2–3 дня |
| 2. Формы | 3–4 дня |
| 3. Список + Детали | 3–4 дня |
| 4. Settings | 1–2 дня |
| 5. Анимации | 2–3 дня |
| **Итого** | **12–18 дней** |

---

## Связанные документы

- [DESIGN_SYSTEM.md](DESIGN_SYSTEM.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [FEATURE_PLAN.md](FEATURE_PLAN.md)
