package com.example.voicemind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.voicemind.BuildConfig
import com.example.voicemind.R
import com.example.voicemind.ui.components.WarningCard
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.util.ReminderPermissions

@Composable
fun SettingsScreen(
    confirmBeforeSchedule: Boolean,
    useAlarmSound: Boolean,
    usePushNotification: Boolean,
    useVibration: Boolean,
    onConfirmBeforeScheduleChange: (Boolean) -> Unit,
    onUseAlarmSoundChange: (Boolean) -> Unit,
    onUsePushNotificationChange: (Boolean) -> Unit,
    onUseVibrationChange: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        // Уведомления
        SettingsSection(title = stringResource(R.string.settings_notifications_title)) {
            NotificationToggle(
                title = stringResource(R.string.settings_use_alarm_sound),
                subtitle = stringResource(R.string.settings_use_alarm_sound_hint),
                checked = useAlarmSound,
                onCheckedChange = onUseAlarmSoundChange,
            )
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

        // Поведение
        SettingsSection(title = stringResource(R.string.settings_defaults_title)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_confirm_before_schedule),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(R.string.settings_confirm_before_schedule_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = confirmBeforeSchedule,
                    onCheckedChange = onConfirmBeforeScheduleChange,
                )
            }
        }

        // Разрешения
        SettingsSection(title = stringResource(R.string.settings_permissions_title)) {
            val exactOk = ReminderPermissions.canScheduleExactAlarms(context)
            if (!exactOk) {
                WarningCard(
                    messages = listOf(stringResource(R.string.settings_exact_alarm_denied)),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                OutlinedButton(
                    onClick = {
                        context.startActivity(ReminderPermissions.exactAlarmSettingsIntent(context))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.settings_open_exact_alarm))
                }
                Spacer(modifier = Modifier.height(Spacing.sm))
            } else {
                Text(
                    text = stringResource(R.string.settings_exact_alarm_granted),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (ReminderPermissions.needsPostNotificationsPermission()) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                val granted = ReminderPermissions.hasPostNotifications(context)
                if (!granted) {
                    WarningCard(
                        messages = listOf(stringResource(R.string.settings_notifications_denied)),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Button(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.settings_request_notifications))
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_notifications_granted),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // О приложении
        SettingsSection(title = stringResource(R.string.settings_about_title)) {
            Text(
                text = stringResource(R.string.settings_test_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NotificationToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        content()
    }
}
