---
description: Самопроверка кода перед коммитом
---

Перед коммитом или PR проверь следующее:

1. **Один ViewModel** — нет ли второго ViewModel на экране. Должен быть только `VoiceMindViewModel`.
2. **Alarm scheduling** — все вызовы `AlarmManager` только через `ReminderScheduler.schedule()` / `cancel()` / `rescheduleAll()`.
3. **Confirm перед schedule** — нет ли прямого сохранения напоминания без `ConfirmReminderScreen`.
4. **Parser** — если менял `ReminderParser`, новый паттерн покрыт тестом (TDD).
5. **Запуск тестов парсера**:
// turbo
   ```powershell
   Start-Process -FilePath "gradlew.bat" -ArgumentList ":app:testDebugUnitTest", "--tests", "com.example.voicemind.data.parse.ReminderParserTest", "--no-daemon" -WorkingDirectory "." -Wait -NoNewWindow
   ```
6. **DB migration** — нет ли `fallbackToDestructiveMigration()` в release. Новое поле → migration класс + bump версии.
7. **UI theme** — нет ли inline hex-цветов в `@Composable`. Только через `MaterialTheme.colorScheme` / `VoiceMindTheme.colors`.
8. **Логи** — не печатаются ли `body` и `rawPhrase` в release-логах.
9. **Ошибки** — DB/IO ошибки обёрнуты в `safeDb` и показаны через `Snackbar`.
10. **Сортировка** — предстоящие `fireAt ASC`, история `fireAt DESC`.
