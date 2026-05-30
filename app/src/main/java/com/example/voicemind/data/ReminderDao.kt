package com.example.voicemind.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Reminder?

    @Query(
        """
        SELECT * FROM reminders
        WHERE status = 'PENDING'
        ORDER BY fireAt ASC, id ASC
        """,
    )
    fun observeUpcomingScheduled(): Flow<List<Reminder>>

    @Query(
        """
        SELECT * FROM reminders
        WHERE status = 'PENDING'
        ORDER BY fireAt ASC, id ASC
        """,
    )
    suspend fun getAllScheduled(): List<Reminder>

    @Query(
        """
        SELECT * FROM reminders
        WHERE status IN ('TRIGGERED', 'DONE', 'CANCELLED')
        ORDER BY fireAt DESC, id DESC
        """,
    )
    fun observeHistory(): Flow<List<Reminder>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query(
        """
        UPDATE reminders SET status = :status
        WHERE id = :id
        """,
    )
    suspend fun updateStatus(id: Long, status: String)

    @Query(
        """
        UPDATE reminders SET status = 'DONE', fireAt = :fireAt
        WHERE id = :id
        """,
    )
    suspend fun complete(id: Long, fireAt: Long)

    @Query(
        """
        UPDATE reminders SET status = 'PENDING', fireAt = :fireAt, snoozeCount = snoozeCount + 1
        WHERE id = :id
        """,
    )
    suspend fun snooze(id: Long, fireAt: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query(
        """
        SELECT * FROM reminders
        WHERE status IN ('TRIGGERED', 'DONE', 'CANCELLED')
        ORDER BY fireAt DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun getRecentHistory(limit: Int): List<Reminder>

    @Query(
        """
        SELECT * FROM reminders
        WHERE status = 'PENDING'
        ORDER BY fireAt ASC, id ASC
        LIMIT :limit
        """,
    )
    suspend fun getWidgetUpcoming(limit: Int): List<Reminder>

    @Query(
        """
        SELECT * FROM reminders
        WHERE status IN ('DONE', 'TRIGGERED')
          AND fireAt > :cutoff
        ORDER BY fireAt DESC, id DESC
        LIMIT :limit
        """,
    )
    suspend fun getWidgetRecentDone(cutoff: Long, limit: Int): List<Reminder>

    @Query(
        """
        SELECT * FROM reminders
        WHERE status = 'PENDING'
          AND fireAt <= :now
        ORDER BY fireAt ASC, id ASC
        """,
    )
    suspend fun getOverduePending(now: Long): List<Reminder>
}
