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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.viewmodel.ReliabilityIssue

@Composable
fun ReliabilityOnboardingScreen(
    reliabilityIssues: List<ReliabilityIssue>,
    onRequestNotificationPermission: () -> Unit,
    onRequestExactAlarm: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onCreateTestReminder: () -> Unit,
    onDismiss: () -> Unit,
    onComplete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.reliability_onboarding_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.reliability_onboarding_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (reliabilityIssues.isEmpty()) {
                AllGoodCard(onCreateTestReminder = onCreateTestReminder)
            } else {
                reliabilityIssues.forEach { issue ->
                    when (issue) {
                        ReliabilityIssue.NOTIFICATIONS_MISSING -> IssueCard(
                            icon = Icons.Default.Notifications,
                            title = stringResource(R.string.reliability_issue_notifications_title),
                            description = stringResource(R.string.reliability_issue_notifications_desc),
                            actionLabel = stringResource(R.string.reliability_action_grant),
                            onAction = onRequestNotificationPermission,
                        )

                        ReliabilityIssue.EXACT_ALARM_MISSING -> IssueCard(
                            icon = Icons.Default.Schedule,
                            title = stringResource(R.string.reliability_issue_exact_alarm_title),
                            description = stringResource(R.string.reliability_issue_exact_alarm_desc),
                            actionLabel = stringResource(R.string.reliability_action_grant),
                            onAction = onRequestExactAlarm,
                        )

                        ReliabilityIssue.BATTERY_OPTIMIZATION_NOT_IGNORED -> IssueCard(
                            icon = Icons.Default.BatteryAlert,
                            title = stringResource(R.string.reliability_issue_battery_title),
                            description = stringResource(R.string.reliability_issue_battery_desc),
                            actionLabel = stringResource(R.string.reliability_action_grant),
                            onAction = onRequestBatteryOptimization,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.reliability_action_skip))
                }
                Button(onClick = onComplete) {
                    Text(stringResource(R.string.reliability_action_done))
                }
            }
        }
    }
}

@Composable
private fun IssueCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun AllGoodCard(
    onCreateTestReminder: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.reliability_all_good_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Text(
                text = stringResource(R.string.reliability_all_good_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Button(onClick = onCreateTestReminder) {
                Text(stringResource(R.string.reliability_action_test_reminder))
            }
        }
    }
}
