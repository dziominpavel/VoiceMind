---
description: Захватить adb logcat и дать Cascade прочитать логи самостоятельно
---
# /debug-logcat — отладка через logcat

Контекст: у пользователя проблема в runtime (UI, alarm, парсер, уведомление и т.д.).
Cascade не имеет прямого доступа к эмулятору/телефону, поэтому logcat пишется в файл.

## Шаги

1. Проверить, что `logs/voicemind-logcat.txt` уже существует и свежий.
   - Если да → сразу читать и анализировать.
   - Если нет / устарел → предложить пользователю запустить скрипт (см. шаг 2).

2. Предложить пользователю запустить в PowerShell (из корня проекта):
   ```powershell
   .\scripts\capture-logcat.ps1 -DurationSec 15
   ```
   Или для быстрого дампа:
   ```powershell
   .\scripts\capture-logcat.ps1 -Lines 500
   ```

3. После завершения скрипта прочитать файл:
   ```
   logs/voicemind-logcat.txt
   ```

4. Проанализировать: искать `FATAL EXCEPTION`, `AndroidRuntime`, `Reminder`, `Alarm`, `Parser`, `VoiceMind`.

5. Выдать диагноз или запросить дополнительные действия (например, воспроизвести баг ещё раз с `-ClearBuffer`).
