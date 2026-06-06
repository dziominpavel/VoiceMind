# VoiceMind — NeoWave Voice: Production Appendix

> Этот документ дополняет `VoiceMind_Comprehensive_Redesign_v4.md` недостающими production-деталями, необходимыми для прямой реализации в Compose.
> Правило: если значение не указано в этом appendix, берётся из v4. При конфликте — этот appendix приоритетнее.

---

# 1. PRODUCTION LAYOUT SPECIFICATIONS

## 1.1. Global Insets & Safe Areas

All top-level screens use `WindowInsets.safeDrawing`:
```kotlin
Modifier
    .fillMaxSize()
    .padding(WindowInsets.safeDrawing.asPaddingValues())
```

| Inset | Behavior |
|-------|----------|
| Status bar | transparent, content draws behind (enableEdgeToEdge) |
| Navigation bar | bottom padding respected, bottom nav sits above it |
| Keyboard | `imePadding()` on screens with text input (Confirm, Manual) |
| Display cutout | `safeDrawing` handles automatically |

---

## 1.2. Navigation Architecture (exact)

### Bottom Navigation (`NavigationSuiteScaffold`)
- **Height:** 80dp (includes nav bar safe area; content area 64dp)
- **Background:** `backgroundSecondary` with tonal elevation level 1
- **Selected item:** `accentPrimary` icon + text
- **Unselected:** `textMuted` icon + text
- **Active indicator:** pill-shaped container, `accentContainer` bg, 32dp height
- **Icon size:** 24dp, weight 500 (Material Symbols Rounded)
- **Label:** `labelMedium` (12sp, Medium)
- **Animation:** crossfade color, `tween(150ms, easing = FastOutSlowInEasing)`

### Tabs (ReminderListScreen)
- **Container height:** 48dp
- **Background:** same as screen background (transparent, seamless)
- **Selected indicator:** 3dp height, `accentPrimary`, width = text width + 24dp
- **Indicator animation:** `spring(stiffness = 380f, dampingRatio = 0.9f)`
- **Text:** `titleMedium` for selected, `bodyMedium` for unselected
- **Divider:** 1dp `outline` below TabRow

---

## 1.3. TopAppBar Spec

### Standard TopAppBar
- **Container height:** 64dp
- **Background:** transparent (scrolls away with content on List screen; fixed on Confirm/Manual/Detail)
- **Title:** `titleLarge` (20sp, SemiBold), `textPrimary`
- **Navigation icon:** 24dp, `textSecondary`, touch target 48dp
- **Action icons:** 24dp, touch target 48dp each
- **Elevation on scroll:** 0dp (we use tonal elevation, not shadow)

### TopAppBar on scroll (List screen)
When content scrolls > 8dp:
- Background transitions to `backgroundSecondary` (tonal elevation level 1)
- Color transition: `tween(200ms, easing = FastOutSlowInEasing)`

---

## 1.4. Screen-by-Screen Layout Grids

### 1.4.1. Home Dashboard

```
+----------------------------------------------+
| Status bar (transparent)                     |
+----------------------------------------------+
|                                              |
|  [Hero Card]                                 |
|  height: 220dp                               |
|  horizontal padding: 24dp                    |
|  radius: shapeXL (36dp)                      |
|                                              |
|  16dp gap                                    |
|                                              |
|        [NeoMicButton]                        |
|        size: 128dp (listening: 124dp)        |
|                                              |
|  16dp gap                                    |
|                                              |
|  [Text button: "Vvesti tekstom"]             |
|  height: 48dp, shapePill                     |
|                                              |
|  24dp gap                                    |
|                                              |
|  [Upcoming Preview]                          |
|  "Blizhayshie" label + list                  |
|  padding horizontal: 24dp                    |
|                                              |
+----------------------------------------------+
| [Bottom Nav] 80dp                            |
+----------------------------------------------+
| Navigation bar (system)                      |
+----------------------------------------------+
```

**Hero Card internal layout:**
- Padding: 24dp all sides
- Time: `displayXL` (48sp), top-aligned
- Body: `bodyLarge` (16sp), below time, 8dp gap
- Countdown: `CountdownLabel` (14sp, `accentPrimary`), below body, 8dp gap
- Delivery icon: 24dp, bottom-right
- Accent glow: `drawBehind` radial gradient, radius 140dp, alpha 0.12, positioned bottom-right

**Upcoming Preview section:**
- Section title: "Blizhayshie" — `labelLarge` (14sp, Medium), `textSecondary`
- Max 3 items, each 64dp height
- Each item: time (64dp width, `titleMedium`) + body (flexible, `bodyMedium`) + delivery icon (24dp)
- Divider: 1dp `outline` between items
- "Vse napominaniya ->" link at bottom: `labelMedium`, `accentPrimary`

### 1.4.2. Confirm Screen (Overlay)

Full-screen overlay, enters with slide from bottom.

```
+----------------------------------------------+
| [TopAppBar] 64dp                             |
| Title: "Podtverdit"                          |
| Back arrow: left                             |
+----------------------------------------------+
|                                              |
|  padding horizontal: 24dp                    |
|  padding top: 16dp                           |
|                                              |
|  [Recognized phrase]                         |
|  style: bodyMedium, textMuted                |
|  max 2 lines, ellipsize end                  |
|                                              |
|  8dp                                         |
|                                              |
|  [Confidence Indicator]                      |
|  height: 24dp                                |
|                                              |
|  16dp                                        |
|                                              |
|  [Time Display]                              |
|  "Zavtra, 09:00"                             |
|  style: TimeDisplay (48sp, SemiBold, tnum) |
|                                              |
|  12dp                                        |
|                                              |
|  [Quick Chips Row]                           |
|  horizontal scroll if overflow             |
|  chip height: 40dp, shapeSmall (10dp)        |
|  gap between chips: 8dp                      |
|                                              |
|  24dp                                        |
|                                              |
|  [Body Text Field]                           |
|  OutlinedTextField, minLines: 2              |
|  shape: shapeMedium (18dp)                   |
|                                              |
|  24dp                                        |
|                                              |
|  [Delivery Mode Grid]                        |
|  2x2 grid, gap: 12dp                         |
|  each card: 80dp height                      |
|                                              |
|  Spacer(modifier = Modifier.weight(1f))      |
|                                              |
+----------------------------------------------+
| [BottomAppBar]                               |
| height: 64dp + nav bar inset                 |
| [Save] Button, full width, height 56dp       |
| shape: shapePill                             |
| text: "Sohranit"                             |
+----------------------------------------------+
| Navigation bar                               |
+----------------------------------------------+
```

**Delivery Mode Grid Card internal:**
- Padding: 12dp
- Icon: 32dp, centered top
- Title: `labelLarge`, below icon, 8dp gap
- Subtitle: `labelMedium`, `textMuted`, below title, 4dp gap
- Selected: `accentContainer` bg + 2dp `accentPrimary` outline
- Unselected: `surfaceElevated` bg + 1dp `outline` border
- Press: scale 0.97, `tween(100ms)`

### 1.4.3. Manual Screen

Identical layout to Confirm Screen, differences:
- Title: "Sozdat napominanie" (or "Redaktirovat")
- No recognized phrase block
- No confidence indicator
- Default time: "Zavtra, 09:00" pre-filled
- **No Suggestions chips** (see Scope Creep Audit below)

### 1.4.4. Reminder List Screen

```
+----------------------------------------------+
| [TopAppBar] 64dp                             |
| Title: "Napominaniya"                        |
| (scrolls with content, becomes tonal bg)     |
+----------------------------------------------+
| [TabRow] 48dp                                |
| "Predstoyashchie" | "Istoriya"               |
+----------------------------------------------+
|                                              |
|  [Grouped List]                              |
|  padding horizontal: 16dp (compact)          |
|                    or 24dp (regular)         |
|  vertical padding: 8dp top, 16dp bottom      |
|                                              |
|  [Section Header]                            |
|  "Segodnya"                                  |
|  style: labelLarge, textMuted                |
|  padding: 8dp top, 4dp bottom                |
|                                              |
|  [Reminder Card]                             |
|  height: min 72dp                            |
|                                              |
|  8dp gap between cards                       |
|                                              |
|  [Section Header]                            |
|  "Zavtra"                                    |
|                                              |
|  [Reminder Card]...                          |
|                                              |
+----------------------------------------------+
| [BottomAppBar]                               |
| Mic button (64dp) + Add button (48dp)        |
+----------------------------------------------+
| Bottom Nav 80dp                              |
+----------------------------------------------+
| Navigation bar                               |
+----------------------------------------------+
```

**Group headers:**
- Text: `labelLarge` (14sp, Medium), `textMuted`
- Padding: 16dp left, 8dp top, 4dp bottom
- Sticky header on scroll (Compose `LazyList` `stickyHeader`)

### 1.4.5. Reminder Detail Screen

```
+----------------------------------------------+
| [TopAppBar] 64dp                             |
| Title: "Napominanie"                         |
| Actions: Edit (24dp), Delete (24dp, error)  |
+----------------------------------------------+
|                                              |
|  padding: 24dp                               |
|                                              |
|  [Time Hero]                                 |
|  "09:00"                                     |
|  style: displayLarge (40sp)                  |
|                                              |
|  [Date Label]                                |
|  "Zavtra, 6 iyunya"                          |
|  style: titleMedium, textSecondary           |
|                                              |
|  24dp                                        |
|                                              |
|  [Body Text]                                 |
|  "Pozvonit sosedu"                           |
|  style: headlineSmall (24sp)               |
|                                              |
|  24dp                                        |
|                                              |
|  [Status Badge]                              |
|  pill shape, height 32dp                     |
|  icon + label                                |
|                                              |
|  16dp                                        |
|                                              |
|  [Info Rows]                                 |
|  Rezhim opovesheniya | Uvedomlenie          |
|  Sozdano | 5 iyunya 2026, 14:30             |
|  style: bodyMedium + labelMedium             |
|  row height: 48dp                            |
|  divider: 1dp outline                        |
|                                              |
|  Spacer(weight = 1f)                         |
|                                              |
+----------------------------------------------+
| [BottomAppBar] (only if SCHEDULED)           |
| height: 64dp                                 |
| [Cancel] (tonal, 48dp) + [Snooze] (filled, 56dp) |
+----------------------------------------------+
| Navigation bar                               |
+----------------------------------------------+
```

**Status Badge:**
- `shapePill`, height 32dp, horizontal padding 16dp
- Background: status color at 15% opacity
- Text: status color, `labelMedium`
- Icon: 16dp, same color, left of text, 8dp gap

### 1.4.6. Alarm Fired Screen (Full-Screen Intent)

```
+----------------------------------------------+
|                                              |
|                                              |
|           09:00                              |
|        displayXL (48sp)                      |
|        textPrimary                           |
|                                              |
|        Zavtra                                |
|        titleMedium, textSecondary            |
|                                              |
|                                              |
|     Pozvonit sosedu                          |
|     headlineSmall (24sp)                     |
|                                              |
|                                              |
|                                              |
|  +--------------------------------------+    |
|  | [VYPOLNENO]                          |    |
|  | height: 72dp                           |    |
|  | shapePill, accentPrimary               |    |
|  | text: onPrimary, titleMedium           |    |
|  +--------------------------------------+    |
|                                              |
|  +--------------------------------------+    |
|  | [OTLOZHIT]                             |    |
|  | height: 56dp                           |    |
|  | shapePill, surfaceElevated             |    |
|  | text: textPrimary, labelLarge         |    |
|  +--------------------------------------+    |
|                                              |
|  [OTMENIT]                                   |
|  textButton, textMuted                       |
|                                              |
|                                              |
+----------------------------------------------+
| Navigation bar (keep behind content)         |
+----------------------------------------------+
```

- Background: `backgroundPrimary` (#040608), pure OLED black
- Subtle breathing glow behind primary button: `drawBehind`, radial gradient `accentGlow` alpha 0.08, radius 200dp, pulse 3s loop
- No TopAppBar
- No status bar scrim (draw behind)

### 1.4.7. Settings Screen

```
+----------------------------------------------+
| [TopAppBar] 64dp                             |
| Title: "Nastroiki"                           |
+----------------------------------------------+
|                                              |
|  padding: 24dp horizontal, 16dp top        |
|                                              |
|  [Settings Card]                             |
|  "Uvedomleniya"                              |
|  card radius: shapeLarge (28dp)              |
|  card bg: surfaceElevated                    |
|  internal padding: 0dp (items have 16dp)    |
|                                              |
|  [Permission Card]                           |
|  radius: shapeMedium (18dp)                  |
|  bg: error container (error at 8%)           |
|  OR bg: success container (success at 8%)    |
|                                              |
|  16dp gap between cards                      |
|                                              |
|  [About Section]                             |
|  plain text rows                             |
|                                              |
|  Spacer(weight = 1f)                         |
+----------------------------------------------+
| Bottom Nav 80dp                              |
+----------------------------------------------+
| Navigation bar                               |
+----------------------------------------------+
```

**Settings Card internal:**
- Section title: `labelLarge`, `textMuted`, 16dp padding top/horizontal
- Each row: 56dp height, 16dp horizontal padding
- Toggle row: Text (Column, weight 1) + Switch (right)
- Row text title: `bodyLarge`
- Row text subtitle: `bodySmall`, `textMuted`
- Divider between rows: 1dp `outline`, inset 16dp left
- Expandable section (e.g., alarm sound on): `AnimatedVisibility` + `animateContentSize`

**Permission Card:**
- Icon: 24dp, left, 16dp padding
- Title: `bodyLarge`, `textPrimary`
- Subtitle: `bodySmall`, status color (red/amber/green)
- Action button: `TextButton`, right-aligned or full-width below
- Padding: 16dp all sides

---

# 2. MOTION & ANIMATION — EXACT CURVES

## 2.1. Easing Definitions

```kotlin
object NeoWaveEasing {
    val Standard = FastOutSlowInEasing
    val Enter = FastOutLinearInEasing
    val Exit = LinearOutSlowInEasing
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}
```

## 2.2. Duration Tokens

```kotlin
object NeoWaveDuration {
    const val Micro = 120
    const val Short = 180
    const val Standard = 220
    const val Screen = 300
    const val Sheet = 350
    const val Hero = 450
    const val Ambient = 3000
}
```

## 2.3. Spring Definitions

```kotlin
object NeoWaveSpring {
    val Default = spring<Float>(stiffness = 380f, dampingRatio = 0.9f)
    val Snappy = spring<Float>(stiffness = 500f, dampingRatio = 0.85f)
    val Bouncy = spring<Float>(stiffness = 300f, dampingRatio = 0.6f)
    val Gentle = spring<Float>(stiffness = 200f, dampingRatio = 0.95f)
    val MicPress = spring<Float>(stiffness = 400f, dampingRatio = 0.75f)
}
```

## 2.4. Animation Specs by Component

### NeoMicButton

**Idle -> Listening:**
```kotlin
val scale by animateFloatAsState(
    targetValue = if (isListening) 1.08f else 1.0f,
    animationSpec = NeoWaveSpring.MicPress
)

val infiniteTransition = rememberInfiniteTransition()
val glowScale by infiniteTransition.animateFloat(
    initialValue = 1f, targetValue = 1.45f,
    animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = LinearEasing)
    )
)
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f, targetValue = 0f,
    animationSpec = infiniteRepeatable(
        animation = tween(1200, easing = LinearEasing)
    )
)
```

**Waveform bars (simulated):**
```kotlin
val barHeights = List(5) { index ->
    infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600 + (index * 150), easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
}
```

**Idle -> Pressed (tap):**
```kotlin
animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = tween(NeoWaveDuration.Micro, easing = NeoWaveEasing.Emphasized)
)
```

### Card Tap
```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.98f else 1f,
    animationSpec = tween(NeoWaveDuration.Micro, easing = NeoWaveEasing.Emphasized)
)
```

### Screen Enter (Overlay)
```kotlin
AnimatedVisibility(
    visible = isVisible,
    enter = slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(NeoWaveDuration.Screen, easing = NeoWaveEasing.Enter)
    ) + fadeIn(
        animationSpec = tween(NeoWaveDuration.Screen, easing = NeoWaveEasing.Standard)
    ),
    exit = slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(NeoWaveDuration.Standard, easing = NeoWaveEasing.Exit)
    ) + fadeOut(
        animationSpec = tween(NeoWaveDuration.Short, easing = NeoWaveEasing.Standard)
    )
)
```

### Save Button Morph
```kotlin
// Step 1: shrink width + fade text
animateFloatAsState(
    targetValue = if (saveComplete) 0f else 1f,
    animationSpec = tween(150, easing = NeoWaveEasing.Emphasized)
)

// Step 2: icon scale in (checkmark)
animateFloatAsState(
    targetValue = if (saveComplete) 1f else 0f,
    animationSpec = tween(200, delayMillis = 100, easing = NeoWaveEasing.Bounce)
)
// Step 3: success haptic (see Haptics)
// Step 4: after 600ms delay, slide screen out
```

### Tab Indicator
```kotlin
val indicatorOffset by animateFloatAsState(
    targetValue = selectedTabIndex.toFloat(),
    animationSpec = NeoWaveSpring.Snappy
)
```

### Alarm Fired — Breathing Glow
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.05f, targetValue = 0.15f,
    animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = NeoWaveEasing.Standard),
        repeatMode = RepeatMode.Reverse
    )
)
```

---

# 3. HAPTICS SPECIFICATION

```kotlin
object NeoWaveHaptics {
    fun perform(context: Context, type: HapticType) {
        val view = (context as? Activity)?.window?.decorView?.rootView ?: return
        when (type) {
            HapticType.Light -> view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            HapticType.Medium -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            HapticType.Heavy -> view.performHapticFeedback(HapticFeedbackConstants.REJECT)
            HapticType.Success -> view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            HapticType.Toggle -> view.performHapticFeedback(HapticFeedbackConstants.TOGGLE_ON)
        }
    }
}

enum class HapticType {
    Light, Medium, Heavy, Success, Toggle
}
```

**Mapping:**

| Action | Haptic | Notes |
|--------|--------|-------|
| Card tap | Light | Any list card tap |
| Chip tap | Light | DateTimeChip, filter chips |
| Toggle switch | Toggle | Settings toggles |
| Start listening | Medium | Mic button press |
| Stop listening | Medium | Mic button release / timeout |
| Save reminder | Success | After Room insert + AlarmManager schedule |
| Mark done | Success | Checkbox tap |
| Snooze scheduled | Success | After reschedule |
| Delete / Cancel | Heavy | Swipe delete, cancel alarm |
| Error | Heavy | Short reject pattern |
| Alarm fired | Medium | When AlarmActivity opens |
| Alarm complete | Success | Press "Vypolneno" on alarm screen |

---

# 4. REMINDER CARD — COMPLETE STATE MATRIX

## 4.1. Visual Structure (all states)

```
+---------------------------------------------------------+
| | 09:00          Pozvonit sosedu               [ ]     |
| | Segodnya       cherez 2 chasa                        |
+---------------------------------------------------------+
```

- Left accent bar: 4dp width, full card height
- Time column: 72dp fixed width
- Content column: flexible weight 1f
- Delivery icon column: 40dp width, icon 24dp
- Card padding: 16dp horizontal, 14dp vertical
- Card shape: `shapeMedium` (18dp)
- Card min height: 72dp
- Gap between cards: 8dp

## 4.2. State Definitions

| State | Trigger | Left Bar | Card Background | Body Text | Time Text | Meta Text | Icon |
|-------|---------|----------|-----------------|-----------|-----------|-----------|------|
| Normal | >24h away, SCHEDULED | outlineStrong (3dp) | surface | textPrimary, bodyLarge | titleMedium, textPrimary | textMuted, labelMedium | delivery color at 70% |
| Urgent | <=24h, >1h, SCHEDULED | timeWarning (4dp) | surface (amber tint 4%) | textPrimary | titleMedium, timeWarning | timeWarning | timeWarning |
| Critical | <=1h, SCHEDULED | timeCritical (4dp) | surface (red tint 4%) | textPrimary | titleMedium, timeCritical | timeCritical | timeCritical |
| Overdue | fireAt < now, SCHEDULED | timeCritical (4dp), pulsing | surface (red tint 6%) | textPrimary | titleMedium, timeCritical | "Prosrocheno" | timeCritical |
| Completed | DISMISSED | success (3dp) | surface (green tint 3%) | strikethrough, alpha 0.7 | textMuted, alpha 0.7 | "Vypolneno" | success at 70% |
| Cancelled | CANCELLED | textDisabled (2dp) | backgroundSecondary | textMuted, alpha 0.6 | textMuted, alpha 0.6 | "Otmeneno" | textDisabled |
| Snoozed | SNOOZED | accentSoft (3dp) | surface | textPrimary | titleMedium, accentPrimary | "Otlozheno" | accentSoft |
| Fired | FIRED | timeWarning (3dp) | surface (amber tint 4%) | textPrimary | titleMedium, timeWarning | "Srabotalo" | timeWarning |

### Delivery Mode Colors (Normal state)

| Mode | Color | Icon |
|------|-------|------|
| NOTIFICATION | accentPrimary | Notifications |
| ALARM | timeWarning | Alarm |
| VIBRATE_ONLY | #A78BFA | Vibration |
| SILENT | textMuted | NotificationsOff |

### Overdue Pulsing
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val barAlpha by infiniteTransition.animateFloat(
    initialValue = 0.5f, targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = NeoWaveEasing.Standard),
        repeatMode = RepeatMode.Reverse
    )
)
```

---

# 5. SNACKBAR & ERROR HANDLING SPEC

## 5.1. Snackbar Container

```
+---------------------------------------------------------+
|  [Icon]  Error text here                    [Action]    |
+---------------------------------------------------------+
```

- Position: anchored to bottom above BottomNav (80dp + nav bar inset)
- Background: surfaceElevated
- Shape: shapeMedium (18dp)
- Horizontal margin: 16dp
- Bottom margin: 16dp above BottomNav
- Padding: 14dp horizontal, 12dp vertical
- Elevation: tonal level 3

## 5.2. Snackbar Content

**With icon:**
- Icon: 20dp, left, 8dp gap to text
- Icon color: status color (error/warning/success/info)
- Text: bodyMedium, textPrimary
- Action: labelLarge, accentPrimary, right-aligned
- Action touch target: 48dp min

## 5.3. Types & Colors

| Type | Icon | Background tint | Use case |
|------|------|-----------------|----------|
| Error | Error icon | subtle error (4%) | DB failure, schedule failure, permission denied |
| Warning | Warning icon | subtle warning (4%) | Low confidence parse, past time adjusted |
| Success | Check icon | subtle success (4%) | Saved, deleted, snoozed |
| Info | Info icon | subtle accent (4%) | Generic info, tips |

## 5.4. Behavior

```kotlin
// Duration
val snackbarDuration = when (type) {
    SnackbarType.Error -> SnackbarDuration.Long   // 10s
    SnackbarType.Warning -> SnackbarDuration.Short // 4s
    SnackbarType.Success -> SnackbarDuration.Short  // 4s
    SnackbarType.Info -> SnackbarDuration.Short     // 4s
}

// Animation enter
slideInVertically(
    initialOffsetY = { it },
    animationSpec = tween(250, easing = NeoWaveEasing.Emphasized)
)

// Animation exit
slideOutVertically(
    targetOffsetY = { it },
    animationSpec = tween(200, easing = NeoWaveEasing.Exit)
)
```

## 5.5. Dismiss Action
- Swipe down to dismiss
- OR tap action button
- OR auto-dismiss after duration

---

# 6. WAVEFORM SPECIFICATION (SIMULATED)

## 6.1. Technical Constraint

`SpeechRecognizer` on-device and `RecognizerIntent` system dialog do NOT provide audio amplitude buffers. Therefore, waveform visualization must be **simulated** based on `SpeechRecognizer.onRmsChanged(rmsdB)` or purely decorative.

## 6.2. Implementation

**Mode A: RMS-based (preferred, if using inline SpeechRecognizer)**
```kotlin
// Map RMS (-2..10 dB typical) to bar heights 0.2..1.0
val normalizedRms = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
// Smooth with exponential moving average
val smoothedRms = prevRms * 0.7f + normalizedRms * 0.3f
```

**Mode B: Purely decorative (if using system RecognizerIntent)**
```kotlin
val infiniteTransition = rememberInfiniteTransition()
val bars = List(5) { i ->
    infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.3f + Random.nextFloat() * 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 400 + i * 120,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        )
    )
}
```

## 6.3. Visual Spec

- 5 bars, width: 4dp each, gap: 3dp
- Height range: 12dp..40dp
- Color: accentPrimary
- Bar shape: shapeSmall (10dp radius)
- Container: 40dp x 40dp, centered below mic button
- Visible only in Listening state

## 6.4. Reduced Motion

When accessibility reduced-motion enabled:
```kotlin
// Static bars at medium height
val staticBars = List(5) { 0.5f }
```

---

# 7. SWIPE ACTIONS (M3-REALISTIC)

## 7.1. Scope

v4 proposed: Right -> Complete, Left -> Snooze, Long swipe -> Delete.
This is **not achievable** with `SwipeToDismissBox` (single action per direction, no long swipe).

## 7.2. Realistic Implementation

Use **M3 SwipeToDismissBox** with single action per direction. Choose:

### Option A: End-to-Start (Left swipe) = Cancel
- Background: error color, Delete icon
- Keep existing behavior (matches v4 actual implementation)
- Complete action moved to checkbox tap (already in code)

### Option B: Two-direction dismiss (custom, NOT SwipeToDismissBox)
If you need both Complete and Snooze via swipe, implement via `AnchoredDraggable`:

```kotlin
// Anchor points: -200dp (Complete), 0 (Rest), 200dp (Snooze)
val anchoredDraggableState = remember {
    AnchoredDraggableState(
        initialValue = 0f,
        positionalThreshold = { distance: Float -> distance * 0.5f },
        velocityThreshold = { with(density) { 125.dp.toPx() } },
        snapAnimationSpec = spring(stiffness = 300f, dampingRatio = 0.8f)
    )
}
```

**Recommended: Option A** (Cancel on swipe left, Complete via checkbox tap, Snooze via Detail screen). This matches current codebase capability and is less error-prone.

## 7.3. Swipe Background Design

```
+---------------------------------------------------------+
|                         [Delete icon]                   | <- red bg, revealed on swipe
| | 09:00          Pozvonit sosedu               [ ]     |
| | Segodnya       cherez 2 chasa                        |
+---------------------------------------------------------+
```

- Background color: error at 12% opacity
- Icon: Delete, 24dp, error color
- Icon position: centered vertically, end-aligned with 24dp padding
- Threshold: 0.4 * card width to trigger dismiss
- Resistance: elastic beyond threshold (spring with stiffness 200f)

---

# 8. LIGHT THEME TOKENS

| Token | Dark Value | Light Value |
|-------|-----------|-------------|
| backgroundPrimary | #040608 | #FFFFFF |
| backgroundSecondary | #0A0E14 | #F8F9FA |
| backgroundTertiary | #10151D | #F1F3F4 |
| surface | #121923 | #FFFFFF |
| surfaceElevated | #182231 | #F8F9FA |
| surfaceFloating | #1D293A | #F1F3F4 |
| surfaceHighlight | #243247 | #E8EAED |
| textPrimary | #F6F8FA | #202124 |
| textSecondary | #B0BAC8 | #5F6368 |
| textMuted | #7E8997 | #9AA0A6 |
| textDisabled | #5A6573 | #BDC1C6 |
| outline | #2B3442 | #DADCE0 |
| outlineStrong | #364153 | #9AA0A6 |
| accentPrimary | #2EC4B6 | #1A9E8F |
| accentSoft | #1B6E65 | #B2DFDB |
| accentContainer | #103B37 | #E0F2F1 |
| accentGlow | #53F2E1 | #80CBC4 |
| timeCritical | #FF5A5F | #D32F2F |
| timeWarning | #F8B84E | #F9A825 |
| timeSafe | #4CAF50 | #2E7D32 |
| error | #EF5350 | #D32F2F |
| warning | #FFB020 | #F9A825 |
| success | #4CAF50 | #2E7D32 |
| info | #42A5F5 | #1976D2 |

**Light Theme Implementation Note:**
- Use `MaterialTheme.colorScheme` as source of truth.
- Define `LightColorScheme` in `Theme.kt`.
- Do NOT implement light theme until project phase 4+.
- These tokens are provided for future-proofing and component testing.

---

# 9. WIDGET EXACT LAYOUTS

## 9.1. 2x1 Widget

```
+----------------------------------+
| 09:00   Pozvonit sosedu          |
| Segodnya  cherez 2 ch            |
+----------------------------------+
```

- Background: surfaceElevated (#182231)
- Padding: 12dp all sides
- Radius: shapeMedium (18dp)
- Time: 20sp, SemiBold, textPrimary
- Body: 14sp, Regular, textPrimary, single line, ellipsize end
- Meta: 12sp, textMuted, below body
- Touch: entire widget opens app

## 9.2. 2x2 Widget

```
+----------------------------------+
| 09:00                            |
| Pozvonit sosedu                  |
| Segodnya                         |
|        [Mic]                     |
+----------------------------------+
```

- Background: surfaceElevated
- Padding: 16dp
- Radius: shapeLarge (28dp)
- Time: 32sp, SemiBold
- Body: 16sp, Regular, max 2 lines
- Date: 14sp, textSecondary
- Mic button: 48dp, accentPrimary bg, bottom-right
- Touch: mic opens app voice input; rest opens app

## 9.3. 4x2 Widget (Hero)

```
+------------------------------------------+
| 09:00                [Mic 48dp]          |
| Pozvonit sosedu                          |
| Segodnya                                 |
| cherez 2 chasa                           |
+------------------------------------------+
```

- Background: backgroundSecondary with subtle accent glow
- Padding: 20dp
- Radius: shapeXL (36dp)
- Time: 40sp, SemiBold, tabular nums
- Body: 20sp, SemiBold
- Countdown: 14sp, accentPrimary
- Mic: 48dp, bottom-right
- Touch: mic -> voice; body -> detail

---

# 10. SCOPE CREEP AUDIT

The following items from v4 are **NEW FEATURES**, not redesign of existing screens. They require new backend/domain logic and must be tracked separately.

| v4 Item | Status | Why | Recommended Action |
|---------|--------|-----|-------------------|
| Manual Screen — Suggestions chips (Kupit produkty, Pozvonit, Oplatit) | **Out of Scope** | Requires ML/NLP on user history, no backend exists | Remove from redesign. Add as Phase 5 research item. |
| Detail Screen — History Timeline (Created, Edited, Snoozed, Completed) | **Out of Scope** | Requires new DB table `reminder_events` | Remove from redesign. Add as Phase 5 feature. |
| Search bar in List | **Out of Scope** | Requires `SearchBar` composable + DAO query. Already planned for Phase 5. | Do not implement in redesign. Keep as Phase 5. |
| Bulk actions (multi-select delete/complete) | **Out of Scope** | Requires selection state + batch operations. Phase 5+. | Remove from redesign. |
| SharedElementTransition (Mic -> Confirm morph) | **Experimental** | `SharedTransitionLayout` is experimental in Compose. Risky for production. | Defer to Phase 4. Use simple slide/fade for now. |
| Tablet / Foldable layouts | **Out of Scope** | Phase 6+. No devices to test. | Keep as future enhancement. |
| Wear OS companion | **Out of Scope** | Phase 7. Entirely separate module. | Keep as future enhancement. |
| Analytics | **Out of Scope** | Not in architecture. | Remove entirely. |

**What IS in scope for this redesign:**
- New color tokens, typography, shapes
- NeoMicButton redesign (simulated waveform, glow)
- Card redesign with urgency states and left accent bar
- Hero Card on Home
- Quick Chips on Confirm
- Listening Overlay visual redesign
- Alarm Fired screen layout
- Settings card-based layout
- Snackbar spec
- Motion/haptics refinements
- Widget visual refresh

---

# 11. PHASE MAPPING: Redesign vs Project Phases

Map redesign phases to existing `docs/FEATURE_PLAN.md` phases.

| Redesign Phase | Project Phase | Work Items | Effort |
|----------------|---------------|------------|--------|
| Redesign Phase 1 — Tokens & Cards | **Phase 3 (current)** | Color.kt, Type.kt, Theme.kt update; ReminderCard redesign; List grouping by date; TopAppBar scroll behavior | 2–3 days |
| Redesign Phase 2 — Dashboard | **Phase 3+** | Hero Card; NeoMicButton with glow; Empty state illustration; Upcoming Preview section | 2–3 days |
| Redesign Phase 3 — Voice Experience | **Phase 4** | Listening Overlay; Simulated waveform; Save button morph; Haptics integration | 3–4 days |
| Redesign Phase 4 — Alarm UI | **Phase 4** | Alarm Fired Activity layout; Breathing glow; Button sizing; Wake-up haptics | 1–2 days |
| Redesign Phase 5 — Polish | **Phase 4+** | Settings card layout; Permission cards; Snackbar styling; Tab indicator animation; Bottom Nav indicator | 2 days |
| Redesign Phase 6 — Widgets | **Phase 7** | Widget refresh with new tokens; Mic button in widget | 1–2 days |
| (Deferred) Light Theme | **Phase 4+** | LightColorScheme in Theme.kt; component testing | 1 day |

**Total redesign effort: ~11–16 days** spread across project phases 3–4.

**Critical path:**
1. Implement tokens first (Colors, Type, Shapes, Springs, Easings) — 1 day
2. Redesign ReminderCard + ListScreen — 2 days
3. Redesign Home (Hero + MicButton) — 2 days
4. Redesign ConfirmScreen (Quick Chips + Confidence) — 2 days
5. Redesign Settings + Snackbar — 1 day
6. Alarm Fired screen — 1 day
7. Polish (haptics, edge cases, a11y audit) — 2–3 days

---

# 12. IMPLEMENTATION CHECKLIST FOR DEVELOPERS

## Before starting
- [ ] Copy all token values into `ui/theme/` files (`Color.kt`, `Type.kt`, `Dimens.kt`, `Theme.kt`, `Shapes.kt`)
- [ ] Define `NeoWaveEasing`, `NeoWaveDuration`, `NeoWaveSpring` objects in `ui/theme/` or `util/`
- [ ] Define `NeoWaveHaptics` utility
- [ ] Update `VoiceMindTheme` to use new dark color scheme

## Screen by screen
- [ ] **ReminderCard** — implement all 8 states with left accent bar, colors, strikethrough, pulsing
- [ ] **ReminderListScreen** — add sticky date headers, grouping logic, new card integration
- [ ] **HomeScreen** — add Hero Card, NeoMicButton glow, Upcoming Preview (max 3 items)
- [ ] **ConfirmScreen** — add Confidence Indicator, Quick Chips row, Delivery Mode Grid 2x2
- [ ] **ManualScreen** — no suggestions (per Scope Creep Audit), keep clean form
- [ ] **DetailScreen** — add Status Badge, Info Rows layout; NO Timeline (out of scope)
- [ ] **AlarmFiredScreen** — OLED black, giant time, breathing glow behind primary button
- [ ] **SettingsScreen** — card-based sections, Permission Cards with status colors
- [ ] **Snackbar** — custom container styling anchored above BottomNav

## Global
- [ ] All screens use `WindowInsets.safeDrawing`
- [ ] All tap targets >= 48dp
- [ ] All animations respect reduced-motion
- [ ] All icons have contentDescription
- [ ] All text passes WCAG AA contrast (test with contrast checker)
- [ ] Test at 200% font scale

---

> Appendix complete. Combine with `VoiceMind_Comprehensive_Redesign_v4.md` for full production design system.
