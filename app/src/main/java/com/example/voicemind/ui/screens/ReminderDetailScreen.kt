package com.example.voicemind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.Reminder
import com.example.voicemind.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDetailScreen(
    reminder: Reminder,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
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
            val mode = runCatching { DeliveryMode.valueOf(reminder.deliveryMode) }.getOrNull()
            if (mode != null) {
                DetailRow(
                    label = stringResource(R.string.confirm_delivery_label),
                    value = deliveryModeLabel(mode),
                )
            }
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.detail_close))
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Text(
        text = value,
        style = MaterialTheme.typography.bodyLarge,
    )
}

@Composable
private fun deliveryModeLabel(mode: DeliveryMode): String = when (mode) {
    DeliveryMode.NOTIFICATION -> stringResource(R.string.delivery_notification)
    DeliveryMode.ALARM -> stringResource(R.string.delivery_alarm)
    DeliveryMode.VIBRATE_ONLY -> stringResource(R.string.delivery_vibrate)
    DeliveryMode.SILENT -> stringResource(R.string.delivery_silent)
}
