package com.example.voicemind.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.ui.components.DeliveryModePicker
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.util.ReminderPermissions

@Composable
fun SettingsScreen(
    defaultDeliveryMode: DeliveryMode,
    confirmBeforeSchedule: Boolean,
    onDefaultDeliveryModeChange: (DeliveryMode) -> Unit,
    onConfirmBeforeScheduleChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.md),
    ) {
        Text(
            text = stringResource(R.string.settings_defaults_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))

        DeliveryModePicker(
            selected = defaultDeliveryMode,
            onSelected = onDefaultDeliveryModeChange,
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.settings_confirm_before_schedule),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = confirmBeforeSchedule,
                onCheckedChange = onConfirmBeforeScheduleChange,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = stringResource(R.string.settings_permissions_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))

        if (ReminderPermissions.needsPostNotificationsPermission()) {
            val granted = ReminderPermissions.hasPostNotifications(context)
            Text(
                text = if (granted) {
                    stringResource(R.string.settings_notifications_granted)
                } else {
                    stringResource(R.string.settings_notifications_denied)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xs))

        val exactOk = ReminderPermissions.canScheduleExactAlarms(context)
        Text(
            text = if (exactOk) {
                stringResource(R.string.settings_exact_alarm_granted)
            } else {
                stringResource(R.string.settings_exact_alarm_denied)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!exactOk) {
            Spacer(modifier = Modifier.height(Spacing.xs))
            OutlinedButton(
                onClick = {
                    context.startActivity(ReminderPermissions.exactAlarmSettingsIntent(context))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_open_exact_alarm))
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.settings_test_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
