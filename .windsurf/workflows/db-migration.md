---
description: Добавление поля или таблицы в Room с миграцией
---

Добавление поля/таблицы в Room (без destructive migration):

1. Изменить `@Entity` — добавить поле с `@ColumnInfo(defaultValue = "...")`.
2. Увеличить `version` в `@Database(entities = [...], version = N)`.
3. Создать `Migration_X_to_Y`:
   ```kotlin
   val MIGRATION_X_Y = object : Migration(X, Y) {
       override fun migrate(db: SupportSQLiteDatabase) {
           db.execSQL("ALTER TABLE reminders ADD COLUMN new_field INTEGER NOT NULL DEFAULT 0")
       }
   }
   ```
4. Добавить migration в `Room.databaseBuilder(...).addMigrations(MIGRATION_X_Y)`.
5. Обновить `SettingsRepository` / `ViewModel`, если поле влияет на UI.
6. **Запрещено** `fallbackToDestructiveMigration()` в release-сборке.
// turbo
7. Собрать debug (`:app:assembleDebug`) и проверить, что старая БД открывается без crash.
8. (Опционально) Добавить instrumented-тест: создать БД версии X, применить миграцию, проверить чтение версии Y.
