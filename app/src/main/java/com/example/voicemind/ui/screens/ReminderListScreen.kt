package com.example.voicemind.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.ui.components.EmptyState
import com.example.voicemind.ui.theme.BackgroundSecondary
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.DeliveryVibrate
import com.example.voicemind.ui.theme.NeoWaveDuration
import com.example.voicemind.ui.theme.NeoWaveEasing
import com.example.voicemind.ui.theme.OutlineStrong
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.SurfaceElevated
import com.example.voicemind.ui.theme.Teal
import com.example.voicemind.ui.theme.TextDisabled
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TextPrimaryDark
import com.example.voicemind.ui.theme.TimeCritical
import com.example.voicemind.ui.theme.TimeDisplay
import com.example.voicemind.ui.theme.TimeSafe
import com.example.voicemind.ui.theme.TimeWarning
import com.example.voicemind.viewmodel.ReminderListTab
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReminderListScreen(
    selectedTab: ReminderListTab,
    onTabSelected: (ReminderListTab) -> Unit,
    upcomingReminders: List<Reminder>,
    historyReminders: List<Reminder>,
    onUpcomingClick: (Long) -> Unit,
    onHistoryClick: (Reminder) -> Unit,
    onCancel: (Long) -> Unit,
    onComplete: (Long) -> Unit,
    onVoiceClick: () -> Unit,
    onMicPermissionDenied: () -> Unit,
    onManualCreateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reminders = when (selectedTab) {
        ReminderListTab.Upcoming -> upcomingReminders
        ReminderListTab.History -> historyReminders
    }

    val grouped = if (selectedTab == ReminderListTab.Upcoming) {
        groupByDate(reminders)
    } else {
        mapOf("" to reminders) // History: no grouping
    }

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab.ordinal) {
            Tab(
                selected = selectedTab == ReminderListTab.Upcoming,
                onClick = { onTabSelected(ReminderListTab.Upcoming) },
                text = { Text(stringResource(R.string.list_tab_upcoming)) },
            )
            Tab(
                selected = selectedTab == ReminderListTab.History,
                onClick = { onTabSelected(ReminderListTab.History) },
                text = { Text(stringResource(R.string.list_tab_history)) },
            )
        }

        if (reminders.isEmpty()) {
            EmptyState(
                icon = if (selectedTab == ReminderListTab.Upcoming) Icons.Default.Schedule else Icons.Default.History,
                title = if (selectedTab == ReminderListTab.Upcoming) {
                    stringResource(R.string.list_empty)
                } else {
                    stringResource(R.string.list_history_empty)
                },
                subtitle = stringResource(R.string.list_empty_subtitle),
                modifier = Modifier.weight(1f),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                grouped.forEach { (header, items) ->
                    if (selectedTab == ReminderListTab.Upcoming && header.isNotEmpty()) {
                        stickyHeader(key = "header_$header") {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextMuted,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = Spacing.md, bottom = Spacing.xs)
                                    .background(MaterialTheme.colorScheme.background),
                            )
                        }
                    }

                    items(items, key = { it.id }) { reminder ->
                        val itemModifier = Modifier.animateItemPlacement(
                            animationSpec = spring(
                                stiffness = 300f,
                                dampingRatio = 0.85f,
                            ),
                        )
                        when (selectedTab) {
                            ReminderListTab.Upcoming -> SwipeableUpcomingCard(
                                reminder = reminder,
                                onClick = { onUpcomingClick(reminder.id) },
                                onCancel = { onCancel(reminder.id) },
                                onComplete = { onComplete(reminder.id) },
                                modifier = itemModifier,
                            )
                            ReminderListTab.History -> HistoryReminderCard(
                                reminder = reminder,
                                onClick = { onHistoryClick(reminder) },
                                modifier = itemModifier,
                            )
                        }
                    }
                }
            }
        }

        BottomAppBar {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledIconButton(
                    onClick = onVoiceClick,
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.home_mic_start),
                        modifier = Modifier.size(32.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = onManualCreateClick,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = stringResource(R.string.home_manual_create),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

private fun groupByDate(reminders: List<Reminder>): Map<String, List<Reminder>> {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val today = now.atZone(zone).toLocalDate()
    val tomorrow = today.plusDays(1)
    val weekEnd = today.plusDays(7)

    return reminders.groupBy { reminder ->
        val date = Instant.ofEpochMilli(reminder.fireAt).atZone(zone).toLocalDate()
        when {
            date.isEqual(today) -> "\u0421\u0435\u0433\u043e\u0434\u043d\u044f"
            date.isEqual(tomorrow) -> "\u0417\u0430\u0432\u0442\u0440\u0430"
            date.isBefore(weekEnd) -> "\u041d\u0430 \u043d\u0435\u0434\u0435\u043b\u0435"
            else -> "\u041f\u043e\u0437\u0436\u0435"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableUpcomingCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onCancel()
                true
            } else {
                false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        modifier = modifier,
        backgroundContent = {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Spacing.md),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.list_cancel),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        },
    ) {
        UpcomingReminderCard(
            reminder = reminder,
            onClick = onClick,
            onComplete = onComplete,
        )
    }
}

@Composable
private fun UpcomingReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = System.currentTimeMillis()
    val isOverdue = reminder.fireAt < now && reminder.status == ReminderStatus.PENDING.name
    val hoursUntil = ChronoUnit.HOURS.between(
        Instant.ofEpochMilli(now),
        Instant.ofEpochMilli(reminder.fireAt),
    )
    val isCritical = hoursUntil in 0..1 && !isOverdue
    val isUrgent = hoursUntil in 2..24 && !isOverdue

    val cardState = when {
        isOverdue -> CardState.OVERDUE
        isCritical -> CardState.CRITICAL
        isUrgent -> CardState.URGENT
        else -> CardState.NORMAL
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(NeoWaveDuration.Micro, easing = NeoWaveEasing.Emphasized),
    )

    val accentColor = cardState.accentColor()
    val bgTint = cardState.backgroundTint()
    val barWidth = cardState.barWidth.dp

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (bgTint != Color.Unspecified) {
                bgTint.copy(alpha = cardState.tintAlpha)
            } else {
                SurfaceElevated
            },
        ),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = ComponentSize.cardMinHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .fillMaxWidth()
                    .background(accentColor)
                    .then(
                        if (cardState == CardState.OVERDUE) {
                            val infiniteTransition = rememberInfiniteTransition(label = "overdue_pulse")
                            val alpha by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse,
                                ),
                            )
                            Modifier.background(accentColor.copy(alpha = alpha))
                        } else Modifier
                    ),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.md, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Time column (72dp)
                Column(modifier = Modifier.width(72.dp)) {
                    Text(
                        text = FormatUtils.formatTime(reminder.fireAt),
                        style = TimeDisplay.copy(
                            fontSize = MaterialTheme.typography.titleMedium.fontSize,
                        ),
                        color = when (cardState) {
                            CardState.CRITICAL, CardState.OVERDUE -> TimeCritical
                            CardState.URGENT -> TimeWarning
                            else -> TextPrimaryDark
                        },
                    )
                    Text(
                        text = FormatUtils.formatRelativeFireAt(reminder.fireAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = when (cardState) {
                            CardState.CRITICAL, CardState.OVERDUE -> TimeCritical
                            CardState.URGENT -> TimeWarning
                            else -> TextMuted
                        },
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.md))

                // Content column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = reminder.body,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = TextPrimaryDark,
                    )
                }

                Spacer(modifier = Modifier.width(Spacing.sm))

                // Delivery icon + checkbox
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val deliveryIcon = when (reminder.deliveryMode) {
                        DeliveryMode.NOTIFICATION.name -> Icons.Default.Notifications to Teal
                        DeliveryMode.ALARM.name -> Icons.Default.Alarm to TimeWarning
                        DeliveryMode.VIBRATE_ONLY.name -> Icons.Default.Vibration to DeliveryVibrate
                        else -> Icons.Default.NotificationsOff to TextMuted
                    }
                    Icon(
                        imageVector = deliveryIcon.first,
                        contentDescription = null,
                        tint = deliveryIcon.second,
                        modifier = Modifier.size(ComponentSize.iconMd),
                    )
                    Checkbox(
                        checked = false,
                        onCheckedChange = { if (it) onComplete() },
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDone = reminder.status == ReminderStatus.DONE.name
    val isCancelled = reminder.status == ReminderStatus.CANCELLED.name

    val cardState = when {
        isDone -> CardState.COMPLETED
        isCancelled -> CardState.CANCELLED
        else -> CardState.FIRED
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(NeoWaveDuration.Micro, easing = NeoWaveEasing.Emphasized),
    )

    val accentColor = cardState.accentColor()
    val bgColor = when (cardState) {
        CardState.COMPLETED -> TimeSafe.copy(alpha = 0.03f)
        CardState.CANCELLED -> BackgroundSecondary
        else -> SurfaceElevated
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.md, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(cardState.barWidth.dp)
                    .height(ComponentSize.cardMinHeight - 8.dp)
                    .background(accentColor, shape = CircleShape),
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            // Time column
            Column(modifier = Modifier.width(72.dp)) {
                Text(
                    text = FormatUtils.formatTime(reminder.fireAt),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isDone || isCancelled) TextMuted else TextPrimaryDark,
                )
                Text(
                    text = FormatUtils.formatRelativeFireAt(reminder.fireAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = reminder.body,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isDone || isCancelled) TextMuted else TextPrimaryDark,
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                )
            }
        }
    }
}

private enum class CardState(val barWidth: Int, val tintAlpha: Float) {
    NORMAL(3, 0f),
    URGENT(4, 0.04f),
    CRITICAL(4, 0.04f),
    OVERDUE(4, 0.06f),
    COMPLETED(3, 0.03f),
    CANCELLED(2, 0f),
    SNOOZED(3, 0f),
    FIRED(3, 0.04f),
}

@Composable
private fun CardState.accentColor(): Color = when (this) {
    CardState.NORMAL -> OutlineStrong
    CardState.URGENT -> TimeWarning
    CardState.CRITICAL -> TimeCritical
    CardState.OVERDUE -> TimeCritical
    CardState.COMPLETED -> TimeSafe
    CardState.CANCELLED -> TextDisabled
    CardState.SNOOZED -> Teal
    CardState.FIRED -> TimeWarning
}

@Composable
private fun CardState.backgroundTint(): Color = when (this) {
    CardState.URGENT -> TimeWarning
    CardState.CRITICAL -> TimeCritical
    CardState.OVERDUE -> TimeCritical
    CardState.COMPLETED -> TimeSafe
    CardState.FIRED -> TimeWarning
    else -> Color.Unspecified
}


