---
description: Захватить adb logcat и дать Cascade прочитать логи самостоятельно
---

Снятие runtime-логов с устройства:

1. Убедись, что adb доступен в PATH или по пути `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe`.
2. Подключи устройство / запусти эмулятор.
3. Запусти скрипт захвата:
// turbo
   ```powershell
   .\scripts\capture-logcat.ps1 -DurationSec 15 -Lines 500
   ```
   - Параметры: `-DurationSec N` (ждать N сек), `-Lines M` (дамп последних M строк), `-ClearBuffer`.
4. Прочитай результат из `logs/voicemind-logcat.txt`.
5. Проанализируй логи: фильтр по `com.example.voicemind`, `ReminderScheduler`, `BootReceiver`, `ReminderAlarmReceiver`, `ReminderNotifier`.
6. Если нашёл crash / ANR — проверь соответствие guardrails (alarm в `ReminderScheduler`, BootReceiver reschedule, exact alarm permission).
