package com.example.voicemind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_mind_settings")

class SettingsRepository(private val context: Context) {

    private val confirmBeforeScheduleKey = booleanPreferencesKey("confirm_before_schedule")
    private val useAlarmSoundKey = booleanPreferencesKey("use_alarm_sound")
    private val usePushNotificationKey = booleanPreferencesKey("use_push_notification")
    private val useVibrationKey = booleanPreferencesKey("use_vibration")
    private val alarmRingtoneUriKey = stringPreferencesKey("alarm_ringtone_uri")
    private val dismissBehaviorKey = stringPreferencesKey("dismiss_behavior")

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

    val alarmRingtoneUri: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[alarmRingtoneUriKey]
    }

    val dismissBehavior: Flow<DismissBehavior> = context.settingsDataStore.data.map { prefs ->
        prefs[dismissBehaviorKey]?.let {
            runCatching { DismissBehavior.valueOf(it) }.getOrNull()
        } ?: DismissBehavior.MARK_DONE
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

    suspend fun setAlarmRingtoneUri(uri: String?) {
        context.settingsDataStore.edit { prefs ->
            if (uri != null) {
                prefs[alarmRingtoneUriKey] = uri
            } else {
                prefs.remove(alarmRingtoneUriKey)
            }
        }
    }

    suspend fun setDismissBehavior(behavior: DismissBehavior) {
        context.settingsDataStore.edit { prefs ->
            prefs[dismissBehaviorKey] = behavior.name
        }
    }

    suspend fun getDefaultDeliveryMode(): DeliveryMode {
        return when {
            useAlarmSound.first() -> DeliveryMode.ALARM
            usePushNotification.first() -> DeliveryMode.NOTIFICATION
            useVibration.first() -> DeliveryMode.VIBRATE_ONLY
            else -> DeliveryMode.SILENT
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
