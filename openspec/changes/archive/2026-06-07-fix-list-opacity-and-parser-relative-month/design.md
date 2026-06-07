## Context

**UI:** `UpcomingReminderCard` задаёт `containerColor = bgTint.copy(alpha = 0.04–0.06)` для URGENT/CRITICAL/OVERDUE. Compose рисует это как полупрозрачный слой — сквозь него видна красная `errorContainer` полоса `SwipeToRevealBox`, отрисованная всегда (даже при `offset = 0`). Нормальные карточки используют непрозрачный `SurfaceElevated` — артефакта нет.

**Парсер:** `RELATIVE_WEEK` есть (`через неделю` → +7 дней). `RELATIVE_DELTA` поддерживает мин/час/день, но не месяц. `DEFAULT_MORNING = LocalTime.of(9, 0)` уже применяется при `hadExplicitDate && !hadExplicitTime`, но не документирован в spec и не покрыт «через месяц».

## Goals / Non-Goals

**Goals:**
- Все карточки списка предстоящих — непрозрачный фон; urgency выражается accent bar + цвет текста, не прозрачностью container.
- Delete-strip видна только при активном свайпе.
- Парсинг `через месяц`, `через 2 месяца`, `через 3 месяца` и т.д.
- Единый дефолт **09:00** для «дата есть, время нет» — в коде и в spec.

**Non-Goals:**
- Изменение порога confidence / логики `isVoiceParseSuccessful`.
- Новые urgency-цвета или анимации.
- Парсинг «через год» (можно позже).

## Decisions

### 1. Непрозрачный urgency-фон: blend, не alpha

**Решение:** В `UpcomingReminderCard` заменить `bgTint.copy(alpha = tintAlpha)` на функцию `blendSurfaceWithTint(SurfaceElevated, tint, fraction)` — линейная интерполяция RGB каналов, результат всегда `alpha = 1f`.

**Почему:** Сохраняет визуальный акцент (4–6% «примеси» цвета), но карточка остаётся непрозрачной. Соответствует требованию «как строка ниже».

**Альтернатива:** Убрать tint фона совсем, оставить только accent bar. **Отклонено:** теряется urgency на скриншотах/спеке ReminderCard.

### 2. Delete-strip по offset

**Решение:** В `SwipeToRevealBox` обернуть правую полосу в `Modifier.graphicsLayer { alpha = if (offsetX < -1f) 1f else 0f }` или `AnimatedVisibility(visible = offsetX < -1f)`.

**Почему:** Двойная защита вместе с opaque card; delete не «подглядывает» на просроченных карточках.

### 3. RELATIVE_MONTH regex

**Решение:** Добавить паттерны:
```
через месяц
через (\d+) месяц|месяца|месяцев
```
Создавать `DateCandidate` с `date = today.plusMonths(n)`, `score = 45` (как неделя), `relativeOnly = true`. Без указания числа — `n = 1`.

**Почему:** Симметрично `RELATIVE_WEEK`; покрывает пользовательские фразы из bug report.

### 4. Дефолтное время 09:00

**Решение:** Оставить существующий `DEFAULT_MORNING = LocalTime.of(9, 0)` без изменения кода; добавить normative requirement в `reminder-parsing` spec. При `hadExplicitDate && !hadExplicitTime` → `fireAt = date.atTime(09:00)` + warning `NO_TIME_FOUND` (как сейчас).

**Почему:** Уже работает для «завтра», «через неделю», «N числа» — нужна только формализация и покрытие месяца.

## Risks / Trade-offs

| Риск | Компенсация |
|------|-------------|
| Blend-цвет чуть отличается от текущего alpha-tint | Подобрать fraction 0.04/0.06; визуальная проверка на overdue/urgent |
| `plusMonths` на 31 января + 1 месяц | Стандартное поведение `LocalDate.plusMonths` (28 фев) — приемлемо |
| Скрытие delete до свайпа — менее discoverable | UX swipe-to-reveal уже задокументирован; пользователь свайпает |

## Migration Plan

Только code-level. Миграция данных не нужна. Откат — revert 3 файла.

## Open Questions

- Нет — пользователь подтвердил: месяц + N месяцев, дефолт 09:00, все карточки непрозрачные.
