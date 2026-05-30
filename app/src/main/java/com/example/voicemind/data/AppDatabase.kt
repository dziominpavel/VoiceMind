package com.example.voicemind.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Reminder::class],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun reminderDao(): ReminderDao

    companion object {
        private const val DB_NAME = "voice_mind_db"

        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE reminders_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        clientId TEXT NOT NULL,
                        fireAt INTEGER NOT NULL,
                        body TEXT NOT NULL,
                        rawPhrase TEXT,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        snoozeCount INTEGER NOT NULL DEFAULT 0,
                        alarmRequestCode INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO reminders_new (id, clientId, fireAt, body, rawPhrase, status, createdAt, snoozeCount, alarmRequestCode)
                    SELECT id, clientId, fireAt, body, rawPhrase, status, createdAt, snoozeCount, alarmRequestCode FROM reminders
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE reminders")
                db.execSQL("ALTER TABLE reminders_new RENAME TO reminders")
                db.execSQL("CREATE INDEX index_reminders_fireAt ON reminders(fireAt)")
                db.execSQL("CREATE INDEX index_reminders_status ON reminders(status)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE reminders ADD COLUMN deliveryMode TEXT NOT NULL DEFAULT 'NOTIFICATION'",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    UPDATE reminders SET status = CASE status
                        WHEN 'SCHEDULED' THEN 'PENDING'
                        WHEN 'SNOOZED' THEN 'PENDING'
                        WHEN 'FIRED' THEN 'TRIGGERED'
                        WHEN 'DISMISSED' THEN 'DONE'
                        WHEN 'COMPLETED' THEN 'DONE'
                        ELSE status
                    END
                    """.trimIndent(),
                )
            }
        }

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME,
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
    }
}
