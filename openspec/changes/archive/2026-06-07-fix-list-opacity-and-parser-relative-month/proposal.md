## Why

После change `global-delivery-mode-and-list-swipe-fix` swipe-to-reveal улучшился, но на просроченных/срочных карточках списка всё ещё просвечивает красная полоса удаления — фон urgency-состояний полупрозрачный (`alpha 4–6%`), а delete-strip всегда лежит под карточкой. Обычные карточки (`SurfaceElevated`) выглядят корректно — нужна единая непрозрачность для всех состояний.

Парсер не распознаёт «через месяц» и аналоги («через 2 месяца»): фраза «через месяц напоминалка четыре» уходит в ручной fallback с дефолтной датой завтра 09:00, хотя дата в фразе есть. В справочнике паттернов есть `через неделю`, но нет месячных относительных дат; единое дефолтное время при «дата есть, время нет» не зафиксировано в spec.

## What Changes

- **Список — непрозрачный фон всех карточек**: urgency tint (urgent/critical/overdue) накладывается на непрозрачный `SurfaceElevated`, а не через `Color.copy(alpha)`. Визуально все карточки одинаково плотные, как нормальная строка.
- **Swipe-to-reveal**: красная полоса удаления и иконка 🗑️ отображаются только при сдвиге карточки (`offset < 0`), в покое полностью скрыты.
- **Парсер — относительные месяцы**: `через месяц`, `через N месяц/месяца/месяцев` → `LocalDate.plusMonths(N)`; span вырезается из body.
- **Парсер — единое дефолтное время**: если дата найдена явно, а время нет — `fireAt` = дата в **09:00** (`DEFAULT_MORNING`); правило едино для всех таких случаев (завтра, через неделю, через месяц, N числа и т.д.); зафиксировать в `reminder-parsing` spec.
- **Тесты**: unit-тесты для «через месяц», «через 2 месяца» + регрессия body extraction.

## Capabilities

### New Capabilities

- Нет.

### Modified Capabilities

- `ui-screens`: непрозрачный фон карточек списка во всех urgency-состояниях; reveal-панель удаления только при свайпе.
- `reminder-parsing`: относительные месяцы; формальное требование дефолтного времени 09:00 при явной дате без времени.

## Impact

- `ReminderListScreen.kt` — opaque containerColor для urgency states.
- `SwipeToRevealBox.kt` — visibility delete-strip по offset.
- `ReminderParser.kt` — `RELATIVE_MONTH` regex + candidate; reference в spec.
- `ReminderParserTest.kt` — новые сценарии.
- `openspec/specs/reminder-parsing/spec.md`, `openspec/specs/ui-screens/spec.md` — delta merge при архивации.
