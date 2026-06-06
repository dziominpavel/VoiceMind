## Why

The VoiceMind MVP (phases 0–3) delivers core functionality, but the UI feels like a stock Material 3 recolor. Cards are visually flat, the microphone button lacks presence, the Confirm screen requires excessive scrolling, and there is no visual differentiation between urgent and normal reminders. The interface does not communicate the product's voice-first identity. This redesign upgrades the visual language to a modern, premium utility aesthetic (NeoWave Voice) while keeping all existing behavior intact.

## What Changes

- **New design tokens**: Expanded color palette (OLED-black backgrounds, tonal elevation surfaces, time-urgency colors), refined typography scale, new shape radii, exact motion curves (springs, easings, durations).
- **NeoMicButton redesign**: Simulated waveform visualization, radial glow ring, animated press states, haptic feedback on start/stop.
- **Home Dashboard**: Hero card for the next reminder (220dp, accent glow), upcoming preview list (max 3 items), empty state with voice prompt.
- **ReminderCard overhaul**: Left accent bar (4dp), 8 visual states (normal, urgent, critical, overdue, completed, cancelled, snoozed, fired), delivery mode color coding, overdue pulsing animation.
- **List grouping**: Group reminders by date with sticky headers (Today, Tomorrow, This Week, Later).
- **ConfirmScreen improvements**: Confidence indicator (dot + label, replaces yellow warning block), quick date/time chips (Today, Tomorrow, In 1 hour, Evening, Custom), Delivery Mode selector as a 2×2 grid of cards.
- **ManualScreen**: Clean form without suggestions (scope audit: suggestions require ML backend), default time pre-filled.
- **Settings redesign**: Card-based sections, permission cards with status colors (granted/required), visual ringtone preview.
- **Alarm Fired screen**: OLED-black full-screen layout, 64sp time display, 72dp primary action button, breathing glow animation.
- **Snackbar**: Custom container anchored above BottomNav, 4 types (Error/Warning/Success/Info), swipe-to-dismiss.
- **Haptics**: Utility object mapping every user action to a haptic type (light/medium/heavy/success/toggle).
- **Accessibility**: Reduced-motion fallbacks, 48dp+ touch targets, TalkBack content descriptions, WCAG AA contrast.

## Capabilities

### New Capabilities
- None. This is a visual redesign of existing capabilities.

### Modified Capabilities
- `ui-screens`: Visual tokens, layout specifications, component states, motion behavior, and accessibility requirements for all screens are being updated. The functional requirements (navigation flow, data model, alarm scheduling) remain unchanged.
- `widget`: Visual styling updated to match NeoWave Voice tokens (colors, typography, shapes). Functional behavior unchanged.

## Impact

- `app/src/main/java/com/example/voicemind/ui/theme/*` — all token files updated.
- `app/src/main/java/com/example/voicemind/ui/components/*` — new and updated reusable components.
- `app/src/main/java/com/example/voicemind/ui/screens/*` — all screens restyled.
- `app/src/main/java/com/example/voicemind/MainActivity.kt` — BottomNav styling, Snackbar anchoring.
- New utility files: `NeoWaveHaptics.kt`, motion token objects.
- `docs/design/*` — design documentation preserved as reference.
- **No database schema changes.**
- **No API changes.**
- **No new permissions.**
