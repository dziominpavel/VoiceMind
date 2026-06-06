package com.example.voicemind.ui.screens

import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.voicemind.BuildConfig
import com.example.voicemind.R
import com.example.voicemind.data.DismissBehavior
import com.example.voicemind.ui.theme.ErrorCoral
import com.example.voicemind.ui.theme.HapticType
import com.example.voicemind.ui.theme.NeoWaveHaptics
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.SuccessGreen
import com.example.voicemind.ui.theme.SurfaceElevated
import com.example.voicemind.ui.theme.Teal
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TextPrimaryDark
import com.example.voicemind.util.ReminderPermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    confirmBeforeSchedule: Boolean,
    useAlarmSound: Boolean,
    usePushNotification: Boolean,
    useVibration: Boolean,
    alarmRingtoneUri: String?,
    alarmVolume: Int,
    dismissBehavior: DismissBehavior,
    onConfirmBeforeScheduleChange: (Boolean) -> Unit,
    onUseAlarmSoundChange: (Boolean) -> Unit,
    onUsePushNotificationChange: (Boolean) -> Unit,
    onUseVibrationChange: (Boolean) -> Unit,
    onSelectRingtone: () -> Unit,
    onAlarmVolumeChange: (Int) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onRequestFullScreenIntentPermission: () -> Unit,
    onDismissBehaviorChange: (DismissBehavior) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Notifications Card
        SettingsCard(title = stringResource(R.string.settings_notifications_title)) {
            NotificationToggle(
                title = stringResource(R.string.settings_use_alarm_sound),
                subtitle = stringResource(R.string.settings_use_alarm_sound_hint),
                checked = useAlarmSound,
                onCheckedChange = onUseAlarmSoundChange,
            )
            AnimatedVisibility(
                visible = useAlarmSound,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    RingtonePickerRow(
                        title = stringResource(R.string.settings_alarm_ringtone),
                        uri = alarmRingtoneUri,
                        onClick = onSelectRingtone,
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    AlarmVolumeSlider(
                        volume = alarmVolume,
                        onVolumeChange = onAlarmVolumeChange,
                    )
                }
            }
            Spacer(modifier = Modifier.height(Spacing.sm))
            NotificationToggle(
                title = stringResource(R.string.settings_use_push_notification),
                subtitle = stringResource(R.string.settings_use_push_notification_hint),
                checked = usePushNotification,
                onCheckedChange = onUsePushNotificationChange,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            NotificationToggle(
                title = stringResource(R.string.settings_use_vibration),
                subtitle = stringResource(R.string.settings_use_vibration_hint),
                checked = useVibration,
                onCheckedChange = onUseVibrationChange,
            )
        }

        // Behavior Card
        SettingsCard(title = stringResource(R.string.settings_defaults_title)) {
            Text(
                text = stringResource(R.string.settings_dismiss_behavior_title),
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            RadioOptionRow(
                title = stringResource(R.string.settings_dismiss_behavior_done),
                selected = dismissBehavior == DismissBehavior.MARK_DONE,
                onClick = { onDismissBehaviorChange(DismissBehavior.MARK_DONE) },
            )
            RadioOptionRow(
                title = stringResource(R.string.settings_dismiss_behavior_snooze),
                selected = dismissBehavior == DismissBehavior.SNOOZE_15,
                onClick = { onDismissBehaviorChange(DismissBehavior.SNOOZE_15) },
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_confirm_before_schedule),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Text(
                        text = stringResource(R.string.settings_confirm_before_schedule_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = confirmBeforeSchedule,
                    onCheckedChange = {
                        NeoWaveHaptics.perform(context, HapticType.Toggle)
                        onConfirmBeforeScheduleChange(it)
                    },
                )
            }
        }

        // Permissions Card
        SettingsCard(title = stringResource(R.string.settings_permissions_title)) {
            val exactOk = ReminderPermissions.canScheduleExactAlarms(context)
            PermissionCard(
                title = "Точные будильники",
                subtitle = if (exactOk) "Разрешение получено" else "Требуется разрешение",
                isGranted = exactOk,
                onAction = {
                    context.startActivity(ReminderPermissions.exactAlarmSettingsIntent(context))
                },
            )

            val notificationsOk = ReminderPermissions.hasPostNotifications(context)
            PermissionCard(
                title = "Уведомления",
                subtitle = if (notificationsOk) "Разрешены" else "Не разрешены",
                isGranted = notificationsOk,
                onAction = onRequestNotificationPermission,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val fullScreenOk = ReminderPermissions.hasUseFullScreenIntent(context)
                PermissionCard(
                    title = "Полноэкранные уведомления",
                    subtitle = if (fullScreenOk) "Разрешены" else "Требуется разрешение для пробуждения экрана",
                    isGranted = fullScreenOk,
                    onAction = onRequestFullScreenIntentPermission,
                )
            }
        }

        // About
        SettingsCard(title = stringResource(R.string.settings_about_title)) {
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.settings_developer_label) + " " + stringResource(R.string.settings_developer_name),
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
            )
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
                modifier = Modifier.padding(bottom = Spacing.sm),
            )
            content()
        }
    }
}

@Composable
private fun PermissionCard(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onAction: () -> Unit,
) {
    val bgColor = if (isGranted) SuccessGreen.copy(alpha = 0.08f) else ErrorCoral.copy(alpha = 0.08f)
    val icon = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Error
    val iconColor = if (isGranted) SuccessGreen else ErrorCoral

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs)
            .clickable { if (!isGranted) onAction() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = bgColor),
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(24.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimaryDark,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = iconColor,
                )
            }
            if (!isGranted) {
                TextButton(onClick = onAction) {
                    Text("Разрешить", color = Teal)
                }
            }
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun RadioOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun RingtonePickerRow(
    title: String,
    uri: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val ringtoneName by produceState(initialValue = stringResource(R.string.settings_alarm_ringtone_default), uri) {
        value = withContext(Dispatchers.IO) {
            try {
                val parsedUri = uri?.let { Uri.parse(it) }
                if (parsedUri != null) {
                    RingtoneManager.getRingtone(context, parsedUri)?.getTitle(context)
                        ?: context.getString(R.string.settings_alarm_ringtone_default)
                } else {
                    context.getString(R.string.settings_alarm_ringtone_default)
                }
            } catch (_: Exception) {
                context.getString(R.string.settings_alarm_ringtone_default)
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = ringtoneName, style = MaterialTheme.typography.bodyMedium, color = TextMuted)
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = TextMuted,
            )
        }
    }
}

@Composable
private fun AlarmVolumeSlider(
    volume: Int,
    onVolumeChange: (Int) -> Unit,
) {
    Column {
        Text(
            text = stringResource(R.string.settings_alarm_volume),
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            text = stringResource(R.string.settings_alarm_volume_hint),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
        )
        Slider(
            value = volume.toFloat(),
            onValueChange = { onVolumeChange(it.toInt()) },
            valueRange = 0f..15f,
            steps = 14,
        )
    }
}
