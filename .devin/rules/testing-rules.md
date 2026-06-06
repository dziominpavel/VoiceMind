---
trigger: glob
description: Правила для unit, instrumented и manual тестов
globs: ["**/test/**", "**/androidTest/**", "**/*Test.kt"]
---

# Testing

## Unit-тесты
- **ReminderParser** — приоритет №1. Замороженный `Instant now` для детерминизма.
- **ReminderScheduler** — fake `AlarmManager` (robolectric опционально).
- Новый паттерн парсера → сначала **test-case**, потом реализация.

## Instrumented
- DAO: insert / query / `ORDER BY` проверки.
- Receivers: `BootReceiver` + reschedule после reboot.
- Migration: установка поверх предыдущей версии БД.

## Manual
- Reboot + reschedule.
- Snooze (10 мин).
- Exact alarm permission denied (Android 12+).
- Doze / battery optimization.

## Стиль
- Именование: `methodName_condition_expected()`.
- Парсер: 55+ кейсов, edge (полночь, полдень, «через полчаса»).
- Не использовать `Thread.sleep()` в unit-тестах.

## Запуск

### Unit-тесты (Android)
Проект VoiceMind — Android Gradle. Unit-тесты лежат в `:app`, task — `testDebugUnitTest`.

**Windows (PowerShell / Cascade):**
```powershell
Start-Process -FilePath "C:\projects\VoiceMind\gradlew.bat" -ArgumentList ":app:testDebugUnitTest", "--no-daemon" -WorkingDirectory "C:\projects\VoiceMind" -Wait -NoNewWindow
# Фильтр по классу:
Start-Process -FilePath "C:\projects\VoiceMind\gradlew.bat" -ArgumentList ":app:testDebugUnitTest", "--tests", "com.example.voicemind.data.parse.ReminderParserTest", "--no-daemon" -WorkingDirectory "C:\projects\VoiceMind" -Wait -NoNewWindow
```

**macOS / Linux:**
```bash
./gradlew :app:testDebugUnitTest --no-daemon
./gradlew :app:testDebugUnitTest --tests "com.example.voicemind.data.parse.ReminderParserTest" --no-daemon
```

**Важно:**
- Использовать `Start-Process` с `-WorkingDirectory` — PowerShell некорректно интерпретирует двоеточие в `:app:testDebugUnitTest` при прямом вызове.
- `--no-daemon` — в окружении Cascade daemon падает по I/O таймауту.

### Instrumented
- `./gradlew connectedAndroidTest` — эмулятор/девайс.
