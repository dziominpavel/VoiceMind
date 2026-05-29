package com.example.voicemind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_mind_settings")

class SettingsRepository(private val context: Context) {

    private val confirmBeforeScheduleKey = booleanPreferencesKey("confirm_before_schedule")
    private val useAlarmSoundKey = booleanPreferencesKey("use_alarm_sound")
    private val usePushNotificationKey = booleanPreferencesKey("use_push_notification")
    private val useVibrationKey = booleanPreferencesKey("use_vibration")

    val confirmBeforeSchedule: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[confirmBeforeScheduleKey] ?: true
    }

    val useAlarmSound: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[useAlarmSoundKey] ?: false
    }

    val usePushNotification: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[usePushNotificationKey] ?: true
    }

    val useVibration: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[useVibrationKey] ?: true
    }

    suspend fun setConfirmBeforeSchedule(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[confirmBeforeScheduleKey] = enabled
        }
    }

    suspend fun setUseAlarmSound(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[useAlarmSoundKey] = enabled
        }
    }

    suspend fun setUsePushNotification(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[usePushNotificationKey] = enabled
        }
    }

    suspend fun setUseVibration(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[useVibrationKey] = enabled
        }
    }

    companion object {
        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
