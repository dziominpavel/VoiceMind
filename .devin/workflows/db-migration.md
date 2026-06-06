---
description: Добавление поля или таблицы в Room с миграцией
---

# Миграция Room (VoiceMind)

## Правило
**Никаких destructive миграций в release.** Только additive.

## Шаги
1. Определить: новая таблица, новое поле, или индекс?
2. Обновить entity-класс ( `@Entity` / `@ColumnInfo` ).
3. Увеличить `version` в `@Database`.
4. Написать `Migration(N, N+1)` — `addColumn` / `createTable`.
5. Передать миграцию в `Room.databaseBuilder.addMigrations(...)`.
6. Проверить `fallbackToDestructiveMigration()` — **не должно быть включено** в release.
7. Написать instrumented test или хотя бы проверить установку поверх предыдущей версии.

## Проверка
- `Room.databaseBuilder(...)` без destructive fallback.
- Миграция покрывает `N → N+1`.
- DAO-запросы не сломаны (особенно `ORDER BY` поля).
