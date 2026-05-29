package com.example.voicemind.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "reminders",
    indices = [
        Index(value = ["fireAt"]),
        Index(value = ["status"]),
    ],
)
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: String,
    val fireAt: Long,
    val body: String,
    val rawPhrase: String?,
    val status: String,
    val createdAt: Long,
    val snoozeCount: Int = 0,
    val alarmRequestCode: Int = 0,
)
