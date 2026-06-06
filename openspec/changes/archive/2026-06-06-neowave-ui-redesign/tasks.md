## 1. Design Tokens & Foundation

- [x] 1.1 Update `Color.kt` with NeoWave Voice tokens (BackgroundPrimary, SurfaceElevated, AccentGlow, TimeCritical, TimeWarning, TimeSafe, TextMuted, TextDisabled, OutlineStrong, InfoBlue, DeliveryVibrate)
- [x] 1.2 Update `Type.kt`: add `displayXL`, refine scale sizes, add `TimeDisplay`, `CountdownLabel`, `ReminderBody`, `Metadata` special styles
- [x] 1.3 Update `Dimens.kt`: add spacing 40dp, update `ComponentSize` (micButton 112dp, micButtonRing 128dp, micIcon 40dp)
- [x] 1.4 Update `Shapes.kt`: new radii (10dp, 18dp, 28dp, 36dp, 999dp) mapped to shapeSmall/medium/large/XL/pill
- [x] 1.5 Update `Theme.kt` DarkColorScheme with new tokens (BackgroundPrimary, SurfaceElevated, TextMuted, TextDisabled, OutlineStrong)
- [x] 1.6 Create `NeoWaveEasing.kt` utility: Standard, Enter, Exit, Emphasized, Decelerate, Bounce
- [x] 1.7 Create `NeoWaveSpring.kt` utility: Default, Snappy, Bouncy, Gentle, MicPress
- [x] 1.8 Create `NeoWaveDuration.kt` utility: Micro, Short, Standard, Screen, Sheet, Hero, Ambient
- [x] 1.9 Create `NeoWaveHaptics.kt` utility: perform() mapping actions to haptic types

## 2. NeoMicButton Redesign

- [x] 2.1 Redesign MicButton: 112dp idle, 124dp listening, radial glow ring with `drawBehind`
- [x] 2.2 Add simulated waveform: 5 bars, Canvas, `rememberInfiniteTransition()`, staggered heights
- [x] 2.3 Add listening state glow pulse animation (scale 1→1.45, alpha 0.5→0, 1200ms loop)
- [x] 2.4 Add press scale animation (0.92, tween 120ms Emphasized)
- [x] 2.5 Add haptic Medium on start/stop listening
- [x] 2.6 Add reduced-motion fallback: static bars, no glow pulse

## 3. ReminderCard & List Redesign

- [x] 3.1 Redesign ReminderCard: left accent bar 4dp, 3-column layout (time 72dp, content flexible, icon 40dp)
- [x] 3.2 Implement 8 card states: Normal, Urgent, Critical, Overdue (pulsing bar), Completed (strikethrough), Cancelled, Snoozed, Fired
- [x] 3.3 Add delivery mode color coding (NOTIFICATION=teal, ALARM=amber, VIBRATE=purple, SILENT=gray)
- [x] 3.4 Add card tap scale animation (0.98, tween 150ms)
- [x] 3.5 Add `animateItemPlacement` spring for list reorder
- [x] 3.6 Implement sticky date headers in ReminderListScreen (Today, Tomorrow, This Week, Later)
- [x] 3.7 Group upcoming reminders by date with sticky headers
- [x] 3.8 Redesign EmptyState for List: waveform icon, voice prompt

## 4. Home Screen Redesign

- [x] 4.1 Create HeroCard component (220dp height, shapeXL 36dp, accent glow `drawBehind`)
- [x] 4.2 HeroCard layout: displayXL time, bodyLarge text, CountdownLabel, delivery icon
- [x] 4.3 Add Upcoming Preview section below Hero (max 3 items, 64dp each, divider)
- [x] 4.4 Home empty state: waveform illustration, voice prompt, example phrase
- [x] 4.5 Add "Vse napominaniya ->" link at bottom of preview
- [x] 4.6 Integrate NeoMicButton with glow into Home layout
- [x] 4.7 Add haptic Medium on mic press

## 5. Confirm Screen Redesign

- [x] 5.1 Create ConfidenceIndicator component (dot + label, 4 color levels: success/accent/warning/error)
- [x] 5.2 Replace yellow WarningCard with ConfidenceIndicator for low-confidence cases
- [x] 5.3 Keep WarningCard for parser warnings only (TIME_AMBIGUOUS, etc.)
- [x] 5.4 Create QuickDateTimeChip component (40dp height, shapeSmall 10dp, selectable states)
- [x] 5.5 Add Quick Chips row: Segodnya, Zavtra, Cherez chas, Vecherom, Vybrat
- [x] 5.6 Implement chip selection logic: tap updates fireAtMillis immediately
- [x] 5.7 Redesign DeliveryModePicker as 2×2 grid of cards (80dp height each)
- [x] 5.8 Grid card: icon 32dp, title labelLarge, subtitle labelMedium, selected/unselected states
- [x] 5.9 Save button morph animation: text fade → checkmark scale-in (bounce) → screen slide out
- [x] 5.10 Add haptic Success on save

## 6. Manual Screen Polish

- [x] 6.1 Clean up ManualReminderScreen: remove suggestions block (out of scope per design)
- [x] 6.2 Ensure default time "Zavtra, 09:00" pre-filled
- [x] 6.3 Apply new token styling to form fields

## 7. Detail Screen Polish

- [x] 7.1 Add StatusBadge component (pill 32dp, status color at 15% opacity, icon + label)
- [x] 7.2 Redesign info rows: 48dp height, label + value, 1dp outline divider
- [x] 7.3 Ensure BottomAppBar actions (Cancel, Snooze) use new button sizes and styles
- [x] 7.4 Add haptic Heavy on delete confirmation

## 8. Alarm Fired Screen

- [ ] 8.1 Create AlarmActivity with full-screen intent configuration
- [ ] 8.2 Layout: OLED black BackgroundPrimary, displayXL time, headlineSmall body
- [ ] 8.3 Primary button "Vypolneno": 72dp height, pill, accentPrimary
- [ ] 8.4 Secondary button "Otlozhit": 56dp height, pill, surfaceElevated
- [ ] 8.5 Tertiary "Otmenit": textButton, textMuted
- [ ] 8.6 Breathing glow behind primary button (alpha 0.05→0.15, 3000ms loop)
- [ ] 8.7 Add haptic Medium on screen open, Success on complete

## 9. Settings Redesign

- [x] 9.1 Redesign SettingsScreen: card-based sections (shapeLarge 28dp, surfaceElevated)
- [x] 9.2 Section title: labelLarge, textMuted, 16dp padding
- [x] 9.3 Row: 56dp height, title bodyLarge, subtitle bodySmall textMuted, Switch right
- [x] 9.4 Create PermissionCard component: status-colored bg (error/success at 8%), icon, title, subtitle, action button
- [x] 9.5 Exact alarm permission: red card if denied, green if granted
- [x] 9.6 POST_NOTIFICATIONS permission: same pattern
- [x] 9.7 Delivery mode selector: same 2×2 grid component as in Confirm
- [x] 9.8 Ringtone row: inline play button + name
- [x] 9.9 Expandable alarm volume section: AnimatedVisibility + animateContentSize

## 10. Snackbar & Global Polish

- [x] 10.1 Create custom Snackbar container: surfaceElevated, shapeMedium 18dp, above BottomNav
- [x] 10.2 Snackbar types: Error (10s), Warning (4s), Success (4s), Info (4s)
- [x] 10.3 Snackbar content: 20dp status icon, bodyMedium text, labelLarge action
- [x] 10.4 Snackbar enter: slide from bottom (tween 250ms Emphasized)
- [x] 10.5 Snackbar exit: slide down (tween 200ms Exit)
- [x] 10.6 Update BottomNav styling: height 80dp, backgroundSecondary tonal elevation 1, active indicator pill
- [x] 10.7 Update TopAppBar: transparent bg, scroll-driven tonal transition
- [x] 10.8 Ensure all screens use WindowInsets.safeDrawing
- [x] 10.9 Verify 48dp+ touch targets everywhere

## 11. Widget Visual Refresh

- [x] 11.1 Update widget colors to NeoWave Voice tokens (textPrimary, textSecondary, accentPrimary)
- [x] 11.2 Update widget shape radius to shapeMedium (18dp)
- [x] 11.3 Verify widget click targets unchanged

## 12. Accessibility & Testing

- [ ] 12.1 Test at 200% font scale: no layout breaks
- [ ] 12.2 Enable TalkBack: verify all icons have contentDescription, headings marked
- [ ] 12.3 Enable reduced-motion: verify waveform static, glow disabled, scale→fade
- [ ] 12.4 Verify WCAG AA contrast for all text (4.5:1 normal, 3:1 large)
- [ ] 12.5 Test haptics on physical device
- [x] 12.6 Run `./gradlew assembleDebug` — zero errors
- [x] 12.7 Run existing tests (ReminderParserTest) — all pass (112 tests, BUILD SUCCESSFUL)
