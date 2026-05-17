package com.example.voicemind.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.viewmodel.ReminderListTab

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderListScreen(
    selectedTab: ReminderListTab,
    onTabSelected: (ReminderListTab) -> Unit,
    upcomingReminders: List<Reminder>,
    historyReminders: List<Reminder>,
    onUpcomingClick: (Long) -> Unit,
    onHistoryClick: (Reminder) -> Unit,
    onCancel: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reminders = when (selectedTab) {
        ReminderListTab.Upcoming -> upcomingReminders
        ReminderListTab.History -> historyReminders
    }
    val emptyText = when (selectedTab) {
        ReminderListTab.Upcoming -> stringResource(R.string.list_empty)
        ReminderListTab.History -> stringResource(R.string.list_history_empty)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = emptyText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.md),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                items(reminders, key = { it.id }) { reminder ->
                    when (selectedTab) {
                        ReminderListTab.Upcoming -> SwipeableUpcomingCard(
                            reminder = reminder,
                            onClick = { onUpcomingClick(reminder.id) },
                            onCancel = { onCancel(reminder.id) },
                        )
                        ReminderListTab.History -> HistoryReminderCard(
                            reminder = reminder,
                            onClick = { onHistoryClick(reminder) },
                        )
                    }
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
            onCancel = onCancel,
        )
    }
}

@Composable
private fun UpcomingReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
    onCancel: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = FormatUtils.formatFireAt(reminder.fireAt),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = reminder.body,
                style = MaterialTheme.typography.bodyLarge,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.xs),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.list_cancel))
                }
            }
        }
    }
}

@Composable
private fun HistoryReminderCard(
    reminder: Reminder,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(Spacing.md)) {
            Text(
                text = FormatUtils.formatFireAt(reminder.fireAt),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = reminder.body,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = FormatUtils.statusLabel(reminder.status),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Spacing.xs),
            )
        }
    }
}
