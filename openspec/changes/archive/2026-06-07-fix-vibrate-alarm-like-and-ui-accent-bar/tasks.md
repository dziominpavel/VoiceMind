## 1. Fix VIBRATE to behave like ALARM without sound

- [x] 1.1 Update `ReminderAlarmReceiver.kt`: merge ALARM and VIBRATE wake lock branches, use `FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE` for both, call `AlarmSoundPlayer.playVibrationOnly()` for VIBRATE
- [x] 1.2 Update `ReminderNotifier.kt`: add `fullScreenIntent`, `CATEGORY_ALARM`, `PRIORITY_MAX`, `setSilent(true)` for VIBRATE mode (same as ALARM but without ringtone)
- [x] 1.3 Update `openspec/specs/notification-delivery/spec.md` with final VIBRATE behavior

## 2. Fix accent bar UI overlap in ReminderListScreen

- [x] 2.1 In `ReminderListScreen.kt` accent bar `Box`, replace `fillMaxWidth()` with `fillMaxHeight()`
- [x] 2.2 Verify accent bar renders as 4dp wide vertical strip without overlapping content
- [x] 2.3 Update `openspec/specs/ui-screens/spec.md` with accent bar layout requirement

## 3. Verification

- [x] 3.1 Build debug APK successfully
- [x] 3.2 Test VIBRATE reminder triggers AlarmActivity with vibration and no sound
- [x] 3.3 Test ALARM reminder still works with sound + vibration + AlarmActivity
- [x] 3.4 Verify ReminderListScreen accent bar renders correctly for all card states
