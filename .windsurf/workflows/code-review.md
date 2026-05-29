---
description: Самопроверка кода перед коммитом
---

# Code Review Checklist (VoiceMind)

## Архитектура
- [ ] Изменения затрагивают только нужные слои (data / domain / ui).
- [ ] Нет дублирования `ReminderScheduler` — alarm только через него.
- [ ] ViewModel один — `VoiceMindViewModel` или наследник.

## Безопасность
- [ ] Alarm не ставится без confirm (preview → save).
- [ ] `canScheduleExactAlarms()` проверена (Android 12+).
- [ ] `safeDb` обёртка на DB-операциях.

## Качество
- [ ] Нет `LiveData` — только `StateFlow`.
- [ ] UI-тексты в `strings.xml` (русские).
- [ ] Код на английском.
- [ ] Логи не содержат `body` / `rawPhrase`.

## Room
- [ ] Если entity изменён — есть миграция (не destructive).
- [ ] Версия БД увеличена.

## Тесты
- [ ] Парсер — unit-тест на новые кейсы.
- [ ] Compose Preview есть для новых экранов.

## Дизайн
- [ ] Используется `VoiceMindTheme`, не цвета GymProgress.
