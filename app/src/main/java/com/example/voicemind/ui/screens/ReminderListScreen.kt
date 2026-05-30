package com.example.voicemind.ui.screens

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.ui.components.EmptyState
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.viewmodel.ReminderListTab

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
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    val itemModifier = Modifier.animateItemPlacement(
                        animationSpec = spring(
                            stiffness = Spring.StiffnessMediumLow,
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
    val relative = FormatUtils.formatRelativeFireAt(reminder.fireAt)
    val isOverdue = reminder.fireAt < System.currentTimeMillis() &&
        reminder.status == ReminderStatus.PENDING.name

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = if (isOverdue) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            )
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = false,
                onCheckedChange = { if (it) onComplete() },
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onClick),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = FormatUtils.formatFireAt(reminder.fireAt),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isOverdue) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                        Spacer(modifier = Modifier.height(Spacing.xxs))
                        Text(
                            text = relative,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = reminder.body,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
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
    val isDone = reminder.status != ReminderStatus.PENDING.name

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = FormatUtils.formatFireAt(reminder.fireAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.xxs))
            Text(
                text = reminder.body,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                color = if (isDone) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}
