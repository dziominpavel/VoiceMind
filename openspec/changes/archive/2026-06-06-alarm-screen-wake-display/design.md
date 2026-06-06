## Context

Текущая реализация ALARM в `ReminderAlarmReceiver` и `ReminderNotifier` не пробуждает экран при выключенном устройстве. Пользователь видит только уведомление в шторке после разблокировки. Стандартное приложение «Будильник» на Android включает экран и показывает UI поверх блокировки.

Ключевые ограничения:
- Android 10+ (API 29+): `USE_FULL_SCREEN_INTENT` permission для `NotificationCompat.Builder.setFullScreenIntent()`.
- Android 12+ (API 31+): `SCHEDULE_EXACT_ALARM` уже есть.
- WakeLock (`PowerManager.WakeLock`) — deprecated `FLAG_SHOW_WHEN_LOCKED` / `FLAG_DISMISS_KEYGUARD` на API 27+, но `AlarmManager` + `fullScreenIntent` — рекомендуемый путь.
- Окно активности над keyguard: `WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED` + `FLAG_KEEP_SCREEN_ON` + `FLAG_TURN_SCREEN_ON`.
- На API 27+ `FLAG_TURN_SCREEN_ON` deprecated; альтернатива — `setTurnScreenOn(true)` / `setShowWhenLocked(true)`.

## Goals / Non-Goals

**Goals:**
- При срабатывании ALARM экран включается и отображается `AlarmActivity` с текстом напоминания.
- `AlarmActivity` показывается поверх экрана блокировки (без необходимости разблокировки).
- Действия на `AlarmActivity` («Готово», «Отложить», «Отменить») работают аналогично actions в уведомлении.
- Звук будильника останавливается при любом действии или закрытии UI.
- Работает на API 26+ (minSdk).

**Non-Goals:**
- Не реализуем собственный экран блокировки (lock screen replacement).
- Не добавляем выбор мелодии из `AlarmActivity` (только из SettingsScreen).
- Не меняем поведение для NOTIFICATION / VIBRATE_ONLY / SILENT.

## Decisions

### 1. AlarmManager + fullScreenIntent вместо standalone WakeLock-Intent
- **Выбор**: `ReminderNotifier` добавляет `setFullScreenIntent(pendingIntent)` к notification ALARM. Система сама запускает `AlarmActivity` поверх блокировки.
- **Почему**: Это рекомендуемый Android-путь. Не нужен отдельный WakeLock-Receiver. Работает надёжнее на разных OEM (Samsung, Xiaomi, Huawei).
- **Альтернатива**: Прямой `startActivity()` из `ReminderAlarmReceiver` + `WakeLock`. Отвергнуто: на некоторых OEM активность не всплывает при заблокированном экране без full-screen intent.

### 2. Отдельная `AlarmActivity` вместо переиспользования `MainActivity`
- **Выбор**: Создаём новую `AlarmActivity` с `Theme.VoiceMind.Alarm` (fullscreen, no action bar, dark background, keep screen on).
- **Почему**: `MainActivity` имеет Scaffold/NavigationSuiteScaffold — сложно показать чистый full-screen UI. `AlarmActivity` — декларативный Compose-экран без навигации.
- **Альтернатива**: Динамический route в `MainActivity`. Отвергнуто: требует refactoring навигации; `MainActivity` может быть в background с другим backstack.

### 3. Обработка действий через `VoiceMindViewModel`, а не отдельный Receiver
- **Выбор**: `AlarmActivity` использует `setContent { VoiceMindTheme { AlarmScreen(...) } }` и обращается к `VoiceMindViewModel` (или dedicated `AlarmViewModel`).
- **Почему**: Единообразие с остальным приложением, reuse `safeDb`, `ReminderScheduler`.
- **Альтернатива**: Повторение логики `ReminderActionReceiver` внутри `AlarmActivity`. Отвергнуто: дублирование кода.

### 4. Остановка звука в `AlarmActivity.onDestroy` / `onPause`
- **Выбор**: `AlarmActivity` вызывает `AlarmSoundPlayer.stop()` при любом действии и в `onPause`/`onDestroy`.
- **Почему**: Защита от утечки звука, если пользователь закроет активность свайпом или кнопкой питания.

## Risks / Trade-offs

- **[Risk]** На некоторых OEM (Xiaomi, OPPO) full-screen intent может блокироваться «экраном блокировки сторонних приложений». → **Mitigation**: Добавить инструкцию в SettingsScreen о разрешении «Показывать поверх экрана блокировки» (фаза 4, отдельно).
- **[Risk]** `setFullScreenIntent` требует `USE_FULL_SCREEN_INTENT` на API 34+. → **Mitigation**: Permission в manifest; runtime request не требуется (это normal permission на API 33, dangerous на API 34+). На API 34+ нужен runtime request.
- **[Risk]** Пользователь может случайно нажать кнопку питания и пропустить будильник. → **Mitigation**: Notification остаётся в шторке как fallback; звук продолжается до явного действия (если не нажатие питания — тогда onPause остановит).
- **[Trade-off]** `AlarmActivity` запускается в отдельном task. Если пользователь свернёт её, она останется в Recent Apps. → **Mitigation**: `android:excludeFromRecents="true"`, `android:taskAffinity=""`, `android:launchMode="singleTask"`.

## Migration Plan

Нет destructive migration. Новые permissions — normal или runtime (USE_FULL_SCREEN_INTENT API 34+). Приложение продолжит работать на старых версиях: без full-screen intent fallback на текущее high-priority notification.

## Open Questions

- Нужен ли отдельный `AlarmViewModel` или использовать существующий `VoiceMindViewModel` через `ViewModelProvider`? → Решение: `VoiceMindViewModel` (единственный ViewModel по правилам проекта), но `AlarmActivity` может использовать `viewModel()` Compose-функцию.
