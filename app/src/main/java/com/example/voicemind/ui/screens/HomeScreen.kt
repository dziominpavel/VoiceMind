package com.example.voicemind.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.ui.components.EmptyState
import com.example.voicemind.ui.components.MicButton
import com.example.voicemind.ui.theme.AccentGlow
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.DeliveryVibrate
import com.example.voicemind.ui.theme.HapticType
import com.example.voicemind.ui.theme.NeoWaveHaptics
import com.example.voicemind.ui.theme.ShapePill
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.SurfaceElevated
import com.example.voicemind.ui.theme.Teal
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TextPrimaryDark
import com.example.voicemind.ui.theme.TimeDisplay
import com.example.voicemind.ui.theme.TimeWarning
import com.example.voicemind.viewmodel.ReliabilityIssue

@Composable
fun HomeScreen(
    nextReminder: Reminder?,
    upcomingReminders: List<Reminder>,
    currentDeliveryMode: DeliveryMode,
    onMicClick: () -> Unit,
    onManualCreateClick: () -> Unit,
    onNextReminderClick: () -> Unit,
    onViewAllClick: () -> Unit,
    onUpcomingClick: (Long) -> Unit,
    reliabilityIssues: List<ReliabilityIssue>,
    onOpenReliabilityOnboarding: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Hero Card — next reminder or empty state
        if (nextReminder != null) {
            HeroCard(
                reminder = nextReminder,
                currentDeliveryMode = currentDeliveryMode,
                onClick = onNextReminderClick,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            EmptyState(
                icon = Icons.Default.Notifications,
                title = stringResource(R.string.home_empty_title),
                subtitle = stringResource(R.string.home_empty_subtitle),
                actionLabel = stringResource(R.string.home_empty_action),
                onAction = onManualCreateClick,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Mic Button
        MicButton(
            isListening = false,
            onClick = {
                NeoWaveHaptics.perform(context, HapticType.Medium)
                onMicClick()
            },
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quick text input button
        OutlinedButton(
            onClick = onManualCreateClick,
            modifier = Modifier.height(48.dp),
            shape = ShapePill,
        ) {
            Icon(
                imageVector = Icons.Default.Keyboard,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.size(Spacing.xs))
            Text(stringResource(R.string.home_manual_create))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Reliability Banner
        if (reliabilityIssues.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = com.example.voicemind.ui.theme.ErrorCoral.copy(alpha = 0.12f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.reliability_banner_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = com.example.voicemind.ui.theme.ErrorCoral,
                    )
                    Text(
                        text = stringResource(R.string.reliability_banner_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimaryDark,
                    )
                    TextButton(onClick = onOpenReliabilityOnboarding) {
                        Text(
                            stringResource(R.string.reliability_banner_action),
                            color = Teal,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Upcoming Preview
        if (upcomingReminders.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_upcoming_section),
                    style = MaterialTheme.typography.labelLarge,
                    color = TextMuted,
                )
                Text(
                    text = stringResource(R.string.home_view_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = Teal,
                    modifier = Modifier.clickable { onViewAllClick() },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            upcomingReminders.take(3).forEach { reminder ->
                UpcomingPreviewItem(
                    reminder = reminder,
                    currentDeliveryMode = currentDeliveryMode,
                    onClick = { onUpcomingClick(reminder.id) },
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeroCard(
    reminder: Reminder,
    currentDeliveryMode: DeliveryMode,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .height(ComponentSize.heroCardHeight)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = SurfaceElevated,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.lg)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AccentGlow.copy(alpha = 0.12f),
                                Color.Transparent,
                            ),
                            radius = size.minDimension * 0.7f,
                        ),
                        radius = 140.dp.toPx(),
                        center = center.copy(x = size.width * 0.75f, y = size.height * 0.75f),
                    )
                },
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // Top: Time
                Text(
                    text = FormatUtils.formatTime(reminder.fireAt),
                    style = TimeDisplay,
                    color = TextPrimaryDark,
                )

                // Middle: Body + Countdown
                Column {
                    Text(
                        text = reminder.body,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimaryDark,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = FormatUtils.formatRelativeFireAt(reminder.fireAt),
                        style = MaterialTheme.typography.labelLarge,
                        color = Teal,
                    )
                }

                // Bottom: Delivery icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    val deliveryIcon = when (currentDeliveryMode) {
                        DeliveryMode.NOTIFICATION -> Icons.Default.Notifications to Teal
                        DeliveryMode.ALARM -> Icons.Default.Alarm to TimeWarning
                        DeliveryMode.VIBRATE -> Icons.Default.Vibration to DeliveryVibrate
                        DeliveryMode.SILENT -> Icons.Default.NotificationsOff to TextMuted
                    }
                    Icon(
                        imageVector = deliveryIcon.first,
                        contentDescription = null,
                        tint = deliveryIcon.second,
                        modifier = Modifier.size(ComponentSize.iconMd),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingPreviewItem(
    reminder: Reminder,
    currentDeliveryMode: DeliveryMode,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.width(64.dp)) {
            Text(
                text = FormatUtils.formatTime(reminder.fireAt),
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimaryDark,
            )
            Text(
                text = FormatUtils.formatShortDate(reminder.fireAt),
                style = MaterialTheme.typography.labelSmall,
                color = TextMuted,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Text(
            text = reminder.body,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = TextPrimaryDark,
            modifier = Modifier.weight(1f),
        )

        val deliveryIcon = when (currentDeliveryMode) {
            DeliveryMode.NOTIFICATION -> Icons.Default.Notifications to Teal
            DeliveryMode.ALARM -> Icons.Default.Alarm to TimeWarning
            DeliveryMode.VIBRATE -> Icons.Default.Vibration to DeliveryVibrate
            DeliveryMode.SILENT -> Icons.Default.NotificationsOff to TextMuted
        }
        Icon(
            imageVector = deliveryIcon.first,
            contentDescription = null,
            tint = deliveryIcon.second,
            modifier = Modifier.size(ComponentSize.iconMd),
        )
    }
}
