## Context

VoiceMind MVP uses a stock Material 3 dark theme (CLEAR BELL) with basic components. While functional, the UI lacks visual depth, voice-first identity, and modern premium feel. The redesign (NeoWave Voice) upgrades the design system without changing functional behavior.

Current state:
- `Color.kt` uses legacy CLEAR BELL tokens (Midnight, Slate, SlateVariant)
- `Type.kt` has standard Material 3 scale, no special time/countdown styles
- `Shapes.kt` uses stock Material 3 radii (4/8/12/16/24)
- `MicButton` has basic pulse animation, no waveform or glow
- `ReminderListScreen` shows flat cards without urgency differentiation
- `ConfirmReminderScreen` shows a large yellow warning block for low confidence
- `SettingsScreen` uses plain section grouping without visual hierarchy
- No haptic feedback utility

Constraints:
- Single `VoiceMindViewModel`, no additional ViewModels
- Jetpack Compose + Material 3 only
- Dark theme default, light theme deferred to phase 4+
- No Lottie, no custom icon fonts
- No audio buffer access (waveform must be simulated)

## Goals / Non-Goals

**Goals:**
- Establish a cohesive, production-ready design token system (colors, typography, spacing, shapes, motion)
- Redesign `MicButton` with radial glow, simulated waveform, and animated state transitions
- Add `HeroCard` to Home showing next reminder with accent glow and countdown
- Redesign `ReminderCard` with left accent bar, 8 visual urgency states, and delivery mode color coding
- Group reminder list by date with sticky headers
- Replace yellow warning block in Confirm with a subtle `ConfidenceIndicator`
- Add quick date/time chips (Today, Tomorrow, In 1 hour, etc.) to Confirm
- Redesign `DeliveryModePicker` as a 2×2 grid of selectable cards
- Redesign `SettingsScreen` with card-based sections and status-colored permission cards
- Design full-screen `AlarmFiredScreen` with OLED-black background and breathing glow
- Implement haptic feedback mapping for all user actions
- Add custom Snackbar styling anchored above BottomNav
- Provide reduced-motion fallbacks for all animations

**Non-Goals:**
- No new database schema changes
- No new permissions
- No changes to alarm scheduling logic (ReminderScheduler stays untouched)
- No changes to parser behavior
- No new features (search, bulk actions, suggestions, timeline) — this is pure redesign
- No shared element transitions (experimental API, deferred)
- No tablet/foldable layouts (phase 6+)
- No light theme implementation (tokens provided but not wired)

## Decisions

### 1. Simulated waveform instead of real audio visualization
**Why:** Android `SpeechRecognizer` (both on-device and system `RecognizerIntent`) does not expose audio amplitude buffers. `onRmsChanged()` gives a weak RMS value, but a convincing waveform requires either real audio access (not available) or simulated animation.
**Alternative considered:** Use real audio via `AudioRecord` directly. **Rejected:** adds `RECORD_AUDIO` complexity, battery drain, and is unnecessary for a visual cue.
**Implementation:** Decorative waveform using `rememberInfiniteTransition()` with staggered bar heights. Falls back to static bars when reduced-motion is enabled.

### 2. Tonal elevation instead of shadows
**Why:** In dark themes, real drop shadows are nearly invisible. Material 3 recommends surface tonal elevation (slight color shift) for depth.
**Alternative considered:** Custom shadow layers with `graphicsLayer`. **Rejected:** Performance cost, inconsistent across devices, harder to maintain.
**Implementation:** Surface colors shift subtly with accent overlay percentages (2–8%) instead of shadows.

### 3. Left accent bar on ReminderCard instead of full-card color
**Why:** Full-card color tinting (e.g., red background for overdue) reduces readability and looks alarming. A thin left bar provides state information without overwhelming the content.
**Alternative considered:** Colored background tint + border. **Rejected:** Too heavy visually, conflicts with dark theme calmness.
**Implementation:** 4dp width vertical bar on the left edge of each card, color matched to urgency/delivery mode.

### 4. Spring animations for UI, tween for ambient loops
**Why:** Springs feel organic and interruptible. Tween loops (glow pulse, waveform) are predictable and cheap.
**Alternative considered:** All tween. **Rejected:** Tween feels robotic for interactive elements. All spring. **Rejected:** Springs don't loop well for ambient effects.
**Implementation:** `spring()` for tap, drag, screen transitions. `tween()` + `rememberInfiniteTransition()` for glow, waveform, overdue pulse.

### 5. Single SwipeToDismissBox direction (EndToStart = Cancel)
**Why:** M3 `SwipeToDismissBox` supports only one action per direction. Implementing two-direction swipe with `AnchoredDraggable` is a custom component that adds complexity and risk.
**Alternative considered:** Custom `AnchoredDraggable` with Complete (right) + Snooze (left). **Rejected:** Significant custom code, potential gesture conflicts with list scroll, out of scope for a redesign.
**Implementation:** Keep existing EndToStart swipe for Cancel. Complete action via checkbox tap. Snooze via Detail screen.

### 6. No SharedElementTransition
**Why:** `SharedTransitionLayout` is still experimental in Compose and requires `androidx.compose.animation:animation` experimental opt-in.
**Alternative considered:** Use it anyway with `@OptIn`. **Rejected:** Risk of API changes breaking build, not worth it for a visual flourish.
**Implementation:** Standard `slideInVertically` + `fadeIn` for overlay screens.

## Risks / Trade-offs

| Risk | Mitigation |
|------|-----------|
| **OLED black (#040608) causes smearing on some LCD panels** | Background falls back to `#0A0E14` on devices without OLED. Token allows override. |
| **Simulated waveform feels fake** | Only shown during listening; clearly decorative (not claiming to be real audio). Reduced-motion users see static bars anyway. |
| **Too many color tokens increases maintenance** | Tokens organized by semantic category (background, surface, accent, status, text, outline). All centralized in `Color.kt`. |
| **Spring animations may feel "too bouncy" on low-end devices** | Spring stiffness tuned conservatively (380f default). Tested on mid-range devices. |
| **Card-based Settings may confuse users expecting plain lists** | Cards used only for grouping; within cards, layout remains familiar (toggle rows, radio rows). |
| **Left accent bar color coding may not be colorblind-accessible** | Always paired with text label and icon. Colors chosen for distinguishability in deuteranopia simulations. |

## Migration Plan

This is a pure redesign with no data migration:
1. Update `ui/theme/*` tokens first (non-breaking, existing screens still compile)
2. Update components one by one (MicButton, ReminderCard, EmptyState, etc.)
3. Update screens one by one (Home, List, Confirm, Manual, Detail, Settings)
4. Update `MainActivity` for Snackbar anchoring and BottomNav styling
5. Add utility files (haptics, motion tokens)
6. Test on physical device for haptics, reduced-motion, and font scaling
7. Rollback: revert single commit (all changes isolated to `ui/` + utility)

## Open Questions

- Should the `AlarmFiredScreen` be implemented as a separate `Activity` (full-screen intent) or a composable overlay? **Decision:** Separate `Activity` for `fullScreenIntent` support (phase 4 requirement).
- Should the widget visual refresh happen in this change or separately? **Decision:** Included — only token/color updates, no new widget functionality.
