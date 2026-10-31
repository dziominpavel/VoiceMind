package com.example.voicemind.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "voice_mind_settings")

class SettingsRepository(private val context: Context) {

    private val confirmBeforeScheduleKey = booleanPreferencesKey("confirm_before_schedule")
    private val defaultDeliveryModeKey = stringPreferencesKey("default_delivery_mode")
    private val useVibrationKey = booleanPreferencesKey("use_vibration")
    private val alarmRingtoneUriKey = stringPreferencesKey("alarm_ringtone_uri")
    private val alarmVolumeKey = intPreferencesKey("alarm_volume")
    private val dismissBehaviorKey = stringPreferencesKey("dismiss_behavior")
    private val quietHoursEnabledKey = booleanPreferencesKey("quiet_hours_enabled")
    private val quietHoursStartMinuteKey = intPreferencesKey("quiet_hours_start_minute")
    private val quietHoursEndMinuteKey = intPreferencesKey("quiet_hours_end_minute")
    private val quietHoursModeKey = stringPreferencesKey("quiet_hours_mode")
    private val deliveryModeSyncedV6Key = booleanPreferencesKey("delivery_mode_synced_v6")
    private val alarmVolumeMigratedV2Key = booleanPreferencesKey("alarm_volume_migrated_v2")

    // Legacy keys for one-time migration
    private val useAlarmSoundKey = booleanPreferencesKey("use_alarm_sound")
    private val usePushNotificationKey = booleanPreferencesKey("use_push_notification")

    val confirmBeforeSchedule: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[confirmBeforeScheduleKey] ?: true
    }

    val defaultDeliveryMode: Flow<DeliveryMode> = context.settingsDataStore.data.map { prefs ->
        prefs[defaultDeliveryModeKey]?.let {
            runCatching { DeliveryMode.valueOf(it) }.getOrNull()
        } ?: DeliveryMode.ALARM
    }

    val useVibration: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[useVibrationKey] ?: true
    }

    val alarmRingtoneUri: Flow<String?> = context.settingsDataStore.data.map { prefs ->
        prefs[alarmRingtoneUriKey]
    }

    val alarmVolume: Flow<Int> = context.settingsDataStore.data.map { prefs ->
        prefs[alarmVolumeKey] ?: 100
    }

    val dismissBehavior: Flow<DismissBehavior> = context.settingsDataStore.data.map { prefs ->
        prefs[dismissBehaviorKey]?.let {
            runCatching { DismissBehavior.valueOf(it) }.getOrNull()
        } ?: DismissBehavior.MARK_DONE
    }

    val quietHours: Flow<QuietHoursSettings> = context.settingsDataStore.data.map { prefs ->
        QuietHoursSettings(
            enabled = prefs[quietHoursEnabledKey] ?: false,
            startMinute = QuietHoursSettings.normalizeMinute(
                prefs[quietHoursStartMinuteKey] ?: QuietHoursSettings.DEFAULT_START_MINUTE,
            ),
            endMinute = QuietHoursSettings.normalizeMinute(
                prefs[quietHoursEndMinuteKey] ?: QuietHoursSettings.DEFAULT_END_MINUTE,
            ),
            mode = prefs[quietHoursModeKey]?.let {
                runCatching { QuietHoursMode.valueOf(it) }.getOrNull()
            } ?: QuietHoursMode.SILENT,
        )
    }

    suspend fun getQuietHours(): QuietHoursSettings = quietHours.first()

    suspend fun setConfirmBeforeSchedule(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[confirmBeforeScheduleKey] = enabled
        }
    }

    suspend fun setDefaultDeliveryMode(mode: DeliveryMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[defaultDeliveryModeKey] = mode.name
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

    suspend fun setAlarmVolume(volume: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[alarmVolumeKey] = volume.coerceIn(0, 100)
        }
    }

    suspend fun setDismissBehavior(behavior: DismissBehavior) {
        context.settingsDataStore.edit { prefs ->
            prefs[dismissBehaviorKey] = behavior.name
        }
    }

    suspend fun setQuietHoursEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[quietHoursEnabledKey] = enabled
        }
    }

    suspend fun setQuietHoursStartMinute(minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[quietHoursStartMinuteKey] = QuietHoursSettings.normalizeMinute(minute)
        }
    }

    suspend fun setQuietHoursEndMinute(minute: Int) {
        context.settingsDataStore.edit { prefs ->
            prefs[quietHoursEndMinuteKey] = QuietHoursSettings.normalizeMinute(minute)
        }
    }

    suspend fun setQuietHoursMode(mode: QuietHoursMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[quietHoursModeKey] = mode.name
        }
    }

    /**
     * Reads the default delivery mode, performing a one-time migration from legacy keys.
     * Legacy keys (use_alarm_sound, use_push_notification) are removed after migration.
     */
    suspend fun getDefaultDeliveryMode(): DeliveryMode {
        val prefs = context.settingsDataStore.data.first()

        // Check if already migrated
        prefs[defaultDeliveryModeKey]?.let {
            return runCatching { DeliveryMode.valueOf(it) }.getOrNull() ?: DeliveryMode.ALARM
        }

        // One-time migration from legacy keys
        val hasAlarmSound = prefs[useAlarmSoundKey] ?: false
        val hasPushNotification = prefs[usePushNotificationKey] ?: true
        val hasVibration = prefs[useVibrationKey] ?: true

        val migratedMode = when {
            hasAlarmSound -> DeliveryMode.ALARM
            hasPushNotification -> DeliveryMode.NOTIFICATION
            hasVibration -> DeliveryMode.VIBRATE
            else -> DeliveryMode.SILENT
        }

        // Save new key and clean up legacy keys
        context.settingsDataStore.edit { editor ->
            editor[defaultDeliveryModeKey] = migratedMode.name
            editor.remove(useAlarmSoundKey)
            editor.remove(usePushNotificationKey)
        }

        return migratedMode
    }

    suspend fun isDeliveryModeSyncedV6(): Boolean {
        val prefs = context.settingsDataStore.data.first()
        return prefs[deliveryModeSyncedV6Key] == true
    }

    suspend fun markDeliveryModeSyncedV6() {
        context.settingsDataStore.edit { prefs ->
            prefs[deliveryModeSyncedV6Key] = true
        }
    }

    /**
     * One-time migration of the alarm volume from the legacy 0..15 slider scale to a 0..100 percent.
     * Idempotent: runs only once (guarded by [alarmVolumeMigratedV2Key]) and only rescales values
     * that look like the old scale (<= 15). Values already in percent (> 15) or absent (default 100)
     * are left untouched.
     */
    suspend fun migrateAlarmVolumeIfNeeded() {
        val prefs = context.settingsDataStore.data.first()
        if (prefs[alarmVolumeMigratedV2Key] == true) {
            return
        }
        val stored = prefs[alarmVolumeKey]
        context.settingsDataStore.edit { editor ->
            if (stored != null && stored <= 15) {
                editor[alarmVolumeKey] = Math.round(stored / 15f * 100f).coerceIn(0, 100)
            }
            editor[alarmVolumeMigratedV2Key] = true
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
