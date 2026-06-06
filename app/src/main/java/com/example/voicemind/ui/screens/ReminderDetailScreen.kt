package com.example.voicemind.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.data.ReminderStatus
import com.example.voicemind.ui.theme.HapticType
import com.example.voicemind.ui.theme.NeoWaveHaptics
import com.example.voicemind.ui.theme.ShapePill
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.SurfaceElevated
import com.example.voicemind.ui.theme.Teal
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TextPrimaryDark
import com.example.voicemind.ui.theme.TimeCritical
import com.example.voicemind.ui.theme.TimeSafe
import com.example.voicemind.ui.theme.TimeWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: (minutes: Int) -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showSnoozeSheet by remember { mutableStateOf(false) }

    val isScheduled = reminder.status == ReminderStatus.PENDING.name

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.confirm_back),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.detail_edit),
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.detail_delete),
                            tint = TimeCritical,
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (isScheduled) {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md),
                        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    ) {
                        FilledTonalButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = ShapePill,
                        ) {
                            Text(stringResource(R.string.detail_cancel))
                        }
                        Button(
                            onClick = onComplete,
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = ShapePill,
                        ) {
                            Text(stringResource(R.string.detail_done))
                        }
                        Button(
                            onClick = { showSnoozeSheet = true },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = ShapePill,
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null)
                            Spacer(modifier = Modifier.size(Spacing.xs))
                            Text(stringResource(R.string.detail_snooze))
                        }
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Status badge
            StatusBadge(status = reminder.status)

            Spacer(modifier = Modifier.height(Spacing.xs))

            // Info rows
            DetailRow(
                label = stringResource(R.string.detail_status_label),
                value = FormatUtils.statusLabel(reminder.status),
            )
            DetailRow(
                label = stringResource(R.string.confirm_when_label),
                value = FormatUtils.formatFireAt(reminder.fireAt),
            )
            DetailRow(
                label = stringResource(R.string.confirm_body_label),
                value = reminder.body,
            )
            reminder.rawPhrase?.takeIf { it != reminder.body }?.let { phrase ->
                DetailRow(
                    label = stringResource(R.string.confirm_phrase_label),
                    value = phrase,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.md))
            FilledTonalButton(
                onClick = onDuplicate,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = ShapePill,
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null)
                Spacer(modifier = Modifier.size(Spacing.xs))
                Text(stringResource(R.string.detail_duplicate))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_confirm_title)) },
            text = { Text(stringResource(R.string.detail_delete_confirm_text)) },
            confirmButton = {
                val context = androidx.compose.ui.platform.LocalContext.current
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        NeoWaveHaptics.perform(context, HapticType.Heavy)
                        onDelete()
                    },
                ) {
                    Text(
                        stringResource(R.string.detail_delete),
                        color = TimeCritical,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.confirm_cancel))
                }
            },
        )
    }

    if (showSnoozeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSnoozeSheet = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    text = stringResource(R.string.detail_snooze),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                listOf(10, 30, 60).forEach { minutes ->
                    TextButton(
                        onClick = {
                            showSnoozeSheet = false
                            onSnooze(minutes)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        val label = when (minutes) {
                            10 -> stringResource(R.string.detail_snooze_10)
                            30 -> stringResource(R.string.detail_snooze_30)
                            60 -> stringResource(R.string.detail_snooze_1h)
                            else -> "+$minutes мин"
                        }
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val info = when (status) {
        ReminderStatus.PENDING.name -> Triple(TimeSafe.copy(alpha = 0.15f), TimeSafe, "Ожидает")
        ReminderStatus.TRIGGERED.name -> Triple(TimeWarning.copy(alpha = 0.15f), TimeWarning, "Сработало")
        ReminderStatus.DONE.name -> Triple(Teal.copy(alpha = 0.15f), Teal, "Выполнено")
        ReminderStatus.CANCELLED.name -> Triple(TextMuted.copy(alpha = 0.15f), TextMuted, "Отменено")
        else -> Triple(SurfaceElevated, TextPrimaryDark, status)
    }
    val (bgColor, textColor, label) = info

    Box(
        modifier = Modifier
            .background(bgColor, shape = ShapePill)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = textColor,
        )
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimaryDark,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TextMuted.copy(alpha = 0.15f)),
        )
    }
}
