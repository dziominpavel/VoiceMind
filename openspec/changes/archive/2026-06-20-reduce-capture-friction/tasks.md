## 1. Quick Settings tile

- [x] 1.1 Создать `TileService` (`onClick` → запуск слушания через стандартный маршрут).
- [x] 1.2 Зарегистрировать сервис в `AndroidManifest.xml` с нужным permission/intent-filter.
- [x] 1.3 Иконка и label плитки.

## 2. App shortcut

- [x] 2.1 Создать `res/xml/shortcuts.xml` со статическим shortcut «Новое напоминание (голос)».
- [x] 2.2 Подключить `meta-data` shortcut к `MainActivity`.
- [x] 2.3 Обработать deep-link/intent shortcut в `MainActivity` (тот же маршрут, что виджет).

## 3. Общий маршрут захвата

- [x] 3.1 Убедиться, что tile и shortcut переиспользуют `WidgetActions.ACTION_START_VOICE`-маршрут (без дублирования логики).

## 4. Быстрое сохранение + undo (по согласованию)

- [x] 4.1 Реализовать «быстрое сохранение» при high-confidence/выключенном подтверждении с Snackbar «Запланировано · Отменить».
- [x] 4.2 Сохранить маршрутизацию неоднозначных результатов в подтверждение/ручной ввод.

## 5. Проверка

- [x] 5.1 `openspec validate --all` — без ошибок.
- [x] 5.2 Сборка `:app:assembleDebug` и `:app:testDebugUnitTest` — успешно.
- [ ] 5.3 Ручной чек на API 24/25+: tile и shortcut запускают захват; undo отменяет быстрое сохранение.
