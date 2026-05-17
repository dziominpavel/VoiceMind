package com.example.voicemind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_mind_settings")

class SettingsRepository(private val context: Context) {

    private val defaultDeliveryModeKey = stringPreferencesKey("default_delivery_mode")
    private val confirmBeforeScheduleKey = booleanPreferencesKey("confirm_before_schedule")

    val defaultDeliveryMode: Flow<DeliveryMode> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[defaultDeliveryModeKey] ?: DeliveryMode.NOTIFICATION.name
        DeliveryMode.entries.find { it.name == name } ?: DeliveryMode.NOTIFICATION
    }

    val confirmBeforeSchedule: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[confirmBeforeScheduleKey] ?: true
    }

    suspend fun setDefaultDeliveryMode(mode: DeliveryMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[defaultDeliveryModeKey] = mode.name
        }
    }

    suspend fun setConfirmBeforeSchedule(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[confirmBeforeScheduleKey] = enabled
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
