# VoiceMind — Дизайн виджетов

> Дата: 2026-05-31
> Этот документ — источник правды для всех виджетов. Перед правками перечитать.

## Виджет: ReminderListWidget

**Один виджет.** Размер 4x1 (min 250x55dp), растягивается по горизонтали до 5x1 (max 320x72dp).

### Структура

```
┌───────────────────────────────────────────┐
│ ~~Купить молоко~~  выполнено  [x] │ 🎤 │  ← 4x1
│ Купить хлеб        через 2ч   [ ] │    │
│ Позвонить маме     через 5ч   [ ] │    │
└───────────────────────────────────────────┘
```

- **Слева (weight=3):** 3 строки напоминаний
  - Строка 1 — последнее выполненное (done). Body ~~перечёркнут~~, time = "выполнено" (обычный текст, не перечёркнут), чекбокс ☑.
  - Строки 2–3 — ближайшие предстоящие (upcoming). Body обычный, time = относительное время (через Xч), чекбокс ☐.
  - Пустой список: строка 1 показывает "Нет напоминаний", time и checkbox скрыты.
- **Справа (weight=1):** кнопка микрофона
  - Внешний FrameLayout — прозрачный, padding 12dp. Кликабельная область 1x1.
  - Внутренний FrameLayout — бирюзовый `#FF2EC4B6`, скругление 16dp. Визуально меньше за счёт padding внешнего.
  - Иконка mic — белая `#FFFFFF`, 20dp (крупная, хорошо видна).
  - Клик → открывает голосовой ввод (`ACTION_START_VOICE`).

### Цвета

- **Фон виджета** — прозрачный (`@android:color/transparent`)
- **Текст body** — белый `#FFFFFF`, 12sp
- **Текст time** — полупрозрачный белый `#B3FFFFFF`, 10sp
- **Done body** — `Paint.STRIKE_THRU_TEXT_FLAG` (перечёркивание)
- **Фон mic** — бирюза `#FF2EC4B6` (внутренний, уменьшенный)

### Клики

- **Body строки** → открывает напоминание в приложении (`ACTION_OPEN_REMINDER`)
- **Чекбокс строки** → переключает статус (`WidgetToggleReceiver`)
- **Микрофон** → открывает голосовой ввод (`ACTION_START_VOICE`)

### Логика выборки (WidgetReminderProvider)

```kotlin
when {
    upcoming.isNotEmpty() && recentDone.isNotEmpty() ->
        (listOf(recentDone.first()) + upcoming).take(3)  // 1 done + 2 upcoming
    upcoming.isNotEmpty() -> upcoming.take(3)              // 3 upcoming
    else -> recentDone.take(3)                             // 3 done
}
```

## Технические ограничения

- **RemoteViews** — статический layout. 3 строки с уникальными ID (`body_1`, `body_2`, `body_3`, `time_1`…, `checkbox_1`…). Нет `addView()`.
- **Strike-through** — через `RemoteViews.setInt(id, "setPaintFlags", Paint.STRIKE_THRU_TEXT_FLAG)`.
- **Время** — done: "выполнено", upcoming: `FormatUtils.formatRelativeFireAt`.

## Файлы

| Файл | Назначение |
|------|------------|
| `widget_reminder_list.xml` | Корневой layout: горизонтальный LL, слева 3 строки (body+time+checkbox), справа микрофон |
| `widget_list_provider.xml` | Meta: minWidth=250, minHeight=55, maxResizeWidth=320, maxResizeHeight=72 |
| `widget_mic_background.xml` | Фигура: бирюза + скругление (для внутреннего mic area) |
| `ic_mic.xml` | Иконка микрофона |
| `ic_checkbox_checked.xml` / `ic_checkbox_unchecked.xml` | Иконки чекбоксов |
