# VoiceMind — Comprehensive Redesign Specification
## Version 4.0 — Production Design System &amp; UX Blueprint
### Design System: NeoWave Voice

---

# 1. DESIGN VISION

## 1.1. Core Concept

VoiceMind — не приложение для напоминаний с микрофоном, а голосовой центр управления временем.

Ключевая идея: **"Speak. Verify. Trust."**

Пользователь должен ощущать, что:
- голос — основной способ взаимодействия;
- приложение понимает контекст;
- время отображается максимально надёжно;
- ошибки исправляются легко;
- интерфейс не отвлекает от задачи.

---

## 1.2. Product Personality

### Calm
Без агрессивных цветов и визуального шума.

### Intelligent
Интерфейс помогает принять решение, а не просто показывает данные.

### Trustworthy
Время, статус и режим доставки всегда очевидны.

### Premium Utility
Минимум декора, максимум качества исполнения.

---

## 1.3. Visual Principles

1. OLED-black фон как основа.
2. Мягкие tonal elevation вместо тяжёлых теней.
3. Бирюзовый акцент используется экономно.
4. Время — главный визуальный объект.
5. Голосовые состояния должны «оживлять» интерфейс.

---

# 2. DESIGN TOKENS

## 2.1. Color Tokens

### Background

| Token | Value |
|---|---|
| backgroundPrimary | #040608 |
| backgroundSecondary | #0A0E14 |
| backgroundTertiary | #10151D |

### Surface

| Token | Value |
|---|---|
| surface | #121923 |
| surfaceElevated | #182231 |
| surfaceFloating | #1D293A |
| surfaceHighlight | #243247 |

### Accent

| Token | Value |
|---|---|
| accentPrimary | #2EC4B6 |
| accentSoft | #1B6E65 |
| accentContainer | #103B37 |
| accentGlow | #53F2E1 |

### Status

| Token | Value |
|---|---|
| success | #4CAF50 |
| warning | #FFB020 |
| error | #EF5350 |
| info | #42A5F5 |

### Time State

| Token | Value |
|---|---|
| timeCritical | #FF5A5F |
| timeWarning | #F8B84E |
| timeSafe | #4CAF50 |

### Text

| Token | Value |
|---|---|
| textPrimary | #F6F8FA |
| textSecondary | #B0BAC8 |
| textMuted | #7E8997 |
| textDisabled | #5A6573 |

### Outline

| Token | Value |
|---|---|
| outline | #2B3442 |
| outlineStrong | #364153 |

---

## 2.2. Elevation Model

В тёмной теме elevation реализуется через tonal overlay:

| Level | Overlay |
|---|---|
| 0 | 0% |
| 1 | 2% accent overlay |
| 2 | 4% accent overlay |
| 3 | 6% accent overlay |
| 4 | 8% accent overlay |

Применение:
- background → level 0
- standard cards → level 1
- elevated cards → level 2
- bottom sheets → level 3
- modal overlays → level 4

---

# 3. TYPOGRAPHY

## 3.1. Font Stack

Primary: Inter  
Fallback: Roboto  
Numeric: Inter with tabular nums (`tnum`)

---

## 3.2. Type Scale

| Style | Size | Line Height | Weight |
|---|---|---|---|
| displayXL | 48sp | 56sp | SemiBold |
| displayLarge | 40sp | 48sp | SemiBold |
| displayMedium | 32sp | 40sp | SemiBold |
| headlineLarge | 28sp | 36sp | SemiBold |
| headlineMedium | 24sp | 32sp | SemiBold |
| titleLarge | 20sp | 28sp | SemiBold |
| titleMedium | 18sp | 26sp | Medium |
| bodyLarge | 16sp | 24sp | Regular |
| bodyMedium | 14sp | 20sp | Regular |
| labelLarge | 14sp | 20sp | Medium |
| labelMedium | 12sp | 16sp | Medium |

---

## 3.3. Special Styles

### TimeDisplay
- 48sp
- SemiBold
- tabular nums

### CountdownLabel
- 14sp
- Medium
- accentPrimary

### ReminderBody
- 16sp
- Regular

### Metadata
- 12sp
- Medium
- textMuted

---

# 4. SPACING &amp; LAYOUT

## 4.1. Grid

Base grid: 8dp

Allowed spacing:
4, 8, 12, 16, 24, 32, 40, 48, 64

---

## 4.2. Screen Padding

| Context | Horizontal |
|---|---|
| Compact phones | 16dp |
| Regular phones | 24dp |
| Tablets | 32dp |

Vertical safe area учитывается через WindowInsets.

---

## 4.3. Touch Targets

| Element | Minimum |
|---|---|
| Any tappable | 48dp |
| Primary actions | 56dp |
| Mic button | 112dp+ |

---

# 5. SHAPE LANGUAGE

| Token | Radius |
|---|---|
| shapeSmall | 10dp |
| shapeMedium | 18dp |
| shapeLarge | 28dp |
| shapeXL | 36dp |
| shapePill | 999dp |

Применение:
- chips → small
- cards → medium
- sheets/dialogs → large
- hero cards → XL
- mic button → pill

---

# 6. ICONOGRAPHY

## 6.1. Style

Material Symbols Rounded, weight 500.

## 6.2. Sizes

| Context | Size |
|---|---|
| metadata | 16dp |
| inline | 20dp |
| standard | 24dp |
| action button | 32dp |
| hero | 48dp |
| empty state | 96dp |

## 6.3. Animated Icons

| State | Animation |
|---|---|
| Mic idle → listening | morph + glow |
| Save success | check reveal |
| Snooze | subtle pulse |
| Alarm active | ringing motion (reduced-motion aware) |

---

# 7. COMPONENT LIBRARY

## 7.1. NeoMicButton

### Sizes
- Idle: 112dp
- Listening: 124dp
- Processing: 120dp

### States
1. Idle
2. Pressed
3. Listening
4. Processing
5. Error

### Listening Visuals
- outer glow ring
- wave ring
- waveform visualization
- live transcription below

### Motion
- scale 1 → 1.08
- 220ms
- emphasized easing

### Haptics
- medium impact on start
- medium impact on stop

### A11Y
- “Начать голосовой ввод”
- “Остановить запись”

---

## 7.2. Reminder Card

### Layout
```
┌─────────────────────────────────────┐
│ 09:00   Позвонить соседу            │
│ Сегодня  через 2 ч         🔔       │
└─────────────────────────────────────┘
```

### Structure
- time column: fixed width 72dp
- content column: flexible
- status column: 32dp

### Delivery Accent
- Notification → teal
- Alarm → amber
- Vibrate → purple
- Silent → gray

### States
- normal
- urgent (&lt;24h)
- critical (&lt;1h)
- overdue
- completed

### Animation
- 0.98 tap scale
- 150ms

---

## 7.3. Delivery Mode Selector

Grid of 4 cards.

Each card contains:
- icon
- title
- short description
- visual preview

Selected state:
- accentContainer background
- accentPrimary outline
- check icon

---

## 7.4. Confidence Indicator

Replaces large warning card.

| Confidence | Color |
|---|---|
| &gt;90% | success |
| 70–90% | accentPrimary |
| 40–70% | warning |
| &lt;40% | error |

Visual:
```
● Высокая уверенность
```

---

## 7.5. Date Time Chip

Selected:
- accentContainer background
- accentPrimary text

Unselected:
- surfaceElevated background
- textSecondary

Examples:
- Сегодня
- Завтра
- Через час
- Вечером
- Выбрать

---

# 8. SCREEN SPECIFICATIONS

## 8.1. Home Dashboard

### Layout

```
[Top App Bar]

┌─────────────────────────────┐
│ Ближайшее напоминание       │
│ 09:00                       │
│ Позвонить соседу            │
│ Через 2 часа                │
└─────────────────────────────┘

        [ Mic ]

[ Ввести текстом ]

[Сегодня]
  11:00 Купить продукты
  15:30 Позвонить

[Завтра]
  09:00 Встреча
```

### Hero Card
- height: 220dp
- XL radius
- subtle accent glow

### Voice Hub
- centered
- highest visual hierarchy
- 128dp diameter

### Empty State
Illustration: floating waveform.  
Title: “Скажите первое напоминание”.  
Subtitle: “Например: «Завтра в девять позвонить маме»”.

---

## 8.2. Listening Overlay

### Behavior
Activated immediately after mic tap.

### Layout
```
[Dimmed background]

        ~ waveform ~

     Слушаю…

"Завтра в девять..."

[Stop]
```

### Motion
- background fade: 180ms
- waveform pulse: 1200ms loop
- transcription crossfade

### A11Y
- announce “Распознавание речи запущено”

---

## 8.3. Confirm Screen

### Layout
```
[Top App Bar]

[Recognized Text]
"Завтра в девять позвонить соседу"

[Confidence]
● Высокая уверенность

[Date &amp; Time]

[Quick Chips]
[Сегодня] [Завтра] [Через час] [Вечером]

[Reminder Text Field]

[Delivery Mode Grid]

[Save Button]
```

### Interaction
- quick chips update date immediately
- date picker still available via “Выбрать” chip

### Save Flow
1. button morphs to check
2. success haptic
3. screen slides down

---

## 8.4. Manual Screen

### Layout
```
[Top App Bar]

[Time Card]
Завтра
09:00

[Reminder Text]

[Suggestions]
[Купить продукты]
[Позвонить]
[Оплатить]

[Save]
```

### Goal
Reduce blank-form anxiety.

---

## 8.5. Reminder List

### Grouping
- Сегодня
- Завтра
- На неделе
- Позже

### Urgency Rules
- &lt;1h → red left bar
- &lt;24h → amber left bar
- normal → neutral

### Swipe Actions
Right → Complete.  
Left → Snooze.  
Long swipe → Delete.

### Empty State
Icon: `schedule`.  
Title: “Нет запланированных напоминаний”.  
CTA: “Создать голосом”.

---

## 8.6. Detail Screen

### Layout
```
[Top App Bar]

09:00
Сегодня

Позвонить соседу

[Schedule Info]

[History Timeline]

[Actions]
[Snooze] [Edit]
[Duplicate] [Delete]
```

### Timeline
- Created
- Edited
- Snoozed
- Completed

### A11Y
Timeline items grouped semantically.

---

## 8.7. Alarm Fired Screen

### Visual
OLED black.  
No distractions.

### Layout
```
09:00

Позвонить соседу

[ВЫПОЛНЕНО]

[ОТЛОЖИТЬ]

[ОТМЕНИТЬ]
```

### Sizes
- time: 64sp
- body: 28sp
- primary button: 72dp height

### Motion
Subtle breathing glow behind primary button.

---

## 8.8. Settings

### Structure
Cards instead of plain sections.

### Permission Card
```
┌─────────────────────────────┐
│ ⏰ Точные будильники        │
│ Требуется разрешение        │
│ [Открыть настройки]         │
└─────────────────────────────┘
```

### Status Colors
- green = granted
- amber = recommended
- red = required

---

## 8.9. Widgets

### 2×1
Time + title.

### 2×2
Time + title + mic button.

### 4×2
Hero widget:
- next reminder
- countdown
- mic button

---

# 9. INTERACTION &amp; MOTION GUIDE

## 9.1. Principles
1. Motion must explain state change.
2. Motion must be interruptible.
3. Motion must respect reduced-motion settings.
4. Voice interactions deserve richer animation than list interactions.

---

## 9.2. Durations

| Type | Duration |
|---|---|
| Micro | 150ms |
| Standard | 220ms |
| Screen | 300ms |
| Sheet | 350ms |
| Hero | 450ms |

---

## 9.3. Micro Interactions

### Card Tap
- scale to 0.98
- 150ms

### Checkbox Complete
- check fill
- 1.0 → 1.12 → 1.0

### Swipe
- elastic resistance
- icon reveal

### Save
- button morph
- success pulse

### Mic
- glow expansion
- waveform activation
- transcription fade-in

---

## 9.4. Haptics

| Action | Feedback |
|---|---|
| Card tap | Light |
| Toggle | Light |
| Save reminder | Success |
| Start listening | Medium |
| Stop listening | Medium |
| Delete | Heavy |
| Alarm complete | Success |

---

# 10. ACCESSIBILITY SPECIFICATION

## 10.1. TalkBack
- all icons have contentDescription
- section headers marked as headings
- grouped cards use semantic containers

## 10.2. Touch Targets
- minimum 48dp
- primary actions 56dp+

## 10.3. Contrast
- all text passes WCAG AA
- large text minimum 3:1
- normal text minimum 4.5:1

## 10.4. Font Scaling
Layouts tested at 200% font scale.

## 10.5. Reduced Motion
When enabled:
- waveform becomes static bars
- glow pulse disabled
- scale animations replaced with fades

## 10.6. Screen Reader Announcements
- “Напоминание сохранено”
- “Распознавание речи запущено”
- “Распознавание речи остановлено”
- “Напоминание отложено на 10 минут”

---

# 11. DO &amp; DON&apos;T

## Do
- Use voice as the primary action.
- Make time visually dominant.
- Use tonal elevation for hierarchy.
- Keep surfaces clearly separated.
- Provide immediate feedback for every action.

## Don&apos;t
- Use flat stock Material cards everywhere.
- Overuse gradients.
- Hide important actions behind menus.
- Rely only on color for status.
- Use tiny icons or tiny touch targets.

---

# 12. IMPLEMENTATION NOTES FOR DEVELOPERS

## 12.1. Compose APIs

### Recommended
- `AnimatedVisibility`
- `AnimatedContent`
- `animateContentSize`
- `updateTransition`
- `rememberInfiniteTransition`
- `graphicsLayer`
- `drawBehind`
- `Canvas`
- `Path`

### Experimental
- `SharedTransitionLayout` for mic → confirm morph

---

## 12.2. Glow Implementation

Use `drawBehind` with radial gradient.

Pseudo-code:
```kotlin
drawCircle(
    brush = Brush.radialGradient(
        colors = listOf(accentGlow.copy(alpha = 0.35f), Color.Transparent),
        radius = size.minDimension * 0.7f
    )
)
```

---

## 12.3. Waveform

Use `Canvas` and animated bar heights or Path-based waveform.  
Throttle updates to ~30fps.

---

## 12.4. Insets

Use `WindowInsets.safeDrawing` for all top-level screens.  
Bottom bars must respect navigation bar insets.

---

# 13. PHASE RECOMMENDATIONS

## Phase 1 — Immediate
- new color tokens
- new typography
- new card styles
- improved list grouping
- confidence indicator

## Phase 2 — Dashboard
- hero card
- voice hub
- quick chips
- empty state redesign

## Phase 3 — Voice Experience
- listening overlay
- waveform visualization
- shared transitions
- haptic tuning

## Phase 4 — Alarm Experience
- full-screen alarm UI
- richer snooze flow
- widget redesign

## Phase 5 — Productivity
- search
- smart grouping
- bulk actions
- analytics

## Phase 6 — Expansion
- tablet layouts
- foldable layouts
- Wear OS companion

---

# 14. SUMMARY

NeoWave Voice превращает VoiceMind из обычного тёмного Material-приложения в современный voice-first utility продукт. Главные изменения:

1. Голос становится центральным визуальным элементом.
2. Время получает максимальную иерархию.
3. Карточки становятся информативными и различимыми.
4. Confirm-flow ускоряется.
5. Motion и haptics создают ощущение живого интерфейса.
6. Accessibility встроен в систему, а не добавлен постфактум.

Результат должен ощущаться как премиальный системный инструмент Android 2026 года: спокойный, быстрый, надёжный и ориентированный на голосовое взаимодействие.
