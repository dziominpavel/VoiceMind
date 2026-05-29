---
description: Реализация фичи по плану из FEATURE_PLAN.md
---

# Реализация фичи VoiceMind

## Подготовка
1. Прочитать `docs/FEATURE_PLAN.md` — найти текущую фазу и задачу.
2. Прочитать `docs/ARCHITECTURE.md` — понять, куда вставлять код.
3. Прочитать `docs/DESIGN_SYSTEM.md` — если затрагивает UI.

## Проектирование
4. Определить: нужна ли новая сущность Room / поле / DataStore ключ?
5. Если Room-изменение — спроектировать миграцию (version + `Migration`).
6. Если alarm/scheduler — проверить `ReminderScheduler` единственная точка.

## Реализация
7. Написать domain/data слой (repository, dao, model).
8. Написать ViewModel-логику (VoiceMindViewModel, StateFlow).
9. Написать Compose UI (Preview + VoiceMindTheme).
10. Подключить навигацию (overlay-стиль, как GymProgress).

## Проверка
11. Unit-тесты для парсера / логики (если применимо).
12. Проверить `safeDb` обёртки.
13. Проверить сортировку: предстоящие `ASC`, история `DESC`.
14. Убедиться, что alarm не ставится без confirm.

## Завершение
15. Обновить `docs/FEATURE_PLAN.md` — отметить фазу.
16. Краткий summary пользователю: что сделано, что проверить.
