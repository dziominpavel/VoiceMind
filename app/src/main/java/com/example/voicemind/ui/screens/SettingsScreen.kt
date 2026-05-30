package com.example.voicemind.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.example.voicemind.BuildConfig
import com.example.voicemind.R
import com.example.voicemind.data.DismissBehavior
import com.example.voicemind.ui.components.WarningCard
import com.example.voicemind.ui.theme.Spacing
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
    dismissBehavior: DismissBehavior,
    onConfirmBeforeScheduleChange: (Boolean) -> Unit,
    onUseAlarmSoundChange: (Boolean) -> Unit,
    onUsePushNotificationChange: (Boolean) -> Unit,
    onUseVibrationChange: (Boolean) -> Unit,
    onSelectRingtone: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onDismissBehaviorChange: (DismissBehavior) -> Unit,
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

        // Поведение
        SettingsSection(title = stringResource(R.string.settings_defaults_title)) {
            Text(
                text = stringResource(R.string.settings_dismiss_behavior_title),
                style = MaterialTheme.typography.bodyMedium,
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

        // О разработчике
        SettingsSection(title = stringResource(R.string.settings_developer_title)) {
            Text(
                text = stringResource(R.string.settings_developer_name),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = stringResource(R.string.settings_developer_link),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("GitHub", "https://github.com/dziominpavel")
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                },
            )
        }

        // Вопрос–ответ
        ExpandableSettingsSection(
            title = stringResource(R.string.settings_faq_title),
            initiallyExpanded = false,
        ) {
            FaqItem(
                question = stringResource(R.string.faq_phrases_question),
                answer = stringResource(R.string.faq_phrases_answer),
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

@Composable
private fun ExpandableSettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                Spacer(modifier = Modifier.height(Spacing.xs))
                content()
            }
        }
    }
}

@Composable
private fun RingtonePickerRow(
    title: String,
    uri: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val defaultLabel = stringResource(R.string.settings_alarm_ringtone_default)
    val subtitle by produceState(initialValue = defaultLabel, uri) {
        value = if (uri == null) {
            defaultLabel
        } else {
            withContext(Dispatchers.IO) {
                try {
                    val parsedUri = Uri.parse(uri)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        RingtoneManager.getRingtone(context, parsedUri)?.getTitle(context)
                    } else {
                        null
                    }
                } catch (_: Exception) {
                    null
                } ?: defaultLabel
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
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
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RadioOptionRow(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun FaqItem(
    question: String,
    answer: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = question,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = answer,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
