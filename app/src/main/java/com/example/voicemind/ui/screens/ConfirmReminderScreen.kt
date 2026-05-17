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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.parse.ParseWarning
import com.example.voicemind.ui.components.DeliveryModePicker
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.viewmodel.PendingReminderConfirm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfirmReminderScreen(
    pending: PendingReminderConfirm,
    onBack: () -> Unit,
    onSave: (body: String, fireAtMillis: Long?, deliveryMode: DeliveryMode) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    var body by remember(pending) { mutableStateOf(pending.body) }
    var deliveryMode by remember(pending) { mutableStateOf(pending.deliveryMode) }
    var fireAtMillis by remember(pending) { mutableStateOf(pending.fireAtMillis) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val fireAtLabel = fireAtMillis?.let { FormatUtils.formatFireAt(it, zone) }
        ?: stringResource(R.string.confirm_time_not_set)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.confirm_voice_title)) },
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
            if (pending.confidence < 0.7f || pending.warnings.isNotEmpty()) {
                ParseWarningsBlock(pending.warnings)
            }

            Text(
                text = stringResource(R.string.confirm_phrase_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pending.rawPhrase,
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = stringResource(R.string.confirm_when_label),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = fireAtLabel,
                style = MaterialTheme.typography.titleMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                OutlinedButton(onClick = { showDatePicker = true }) {
                    Text(stringResource(R.string.confirm_pick_date))
                }
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text(stringResource(R.string.confirm_pick_time))
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    onSave(it, fireAtMillis, deliveryMode)
                },
                label = { Text(stringResource(R.string.confirm_body_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )

            DeliveryModePicker(
                selected = deliveryMode,
                onSelected = {
                    deliveryMode = it
                    onSave(body, fireAtMillis, it)
                },
            )

            Spacer(modifier = Modifier.height(Spacing.xs))

            Button(
                onClick = {
                    onSave(body, fireAtMillis, deliveryMode)
                    onConfirm()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.confirm_save))
            }
        }
    }

    val initialDate = fireAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
    } ?: LocalDate.now().plusDays(1)

    val initialTime = fireAtMillis?.let {
        Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
    } ?: LocalTime.of(9, 0)

    if (showDatePicker) {
        ReminderDatePickerDialog(
            initialDate = initialDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                val time = fireAtMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalTime()
                } ?: LocalTime.of(9, 0)
                fireAtMillis = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
                onSave(body, fireAtMillis, deliveryMode)
                showDatePicker = false
            },
        )
    }

    if (showTimePicker) {
        ReminderTimePickerDialog(
            initialTime = initialTime,
            onDismiss = { showTimePicker = false },
            onConfirm = { time ->
                val date = fireAtMillis?.let {
                    Instant.ofEpochMilli(it).atZone(zone).toLocalDate()
                } ?: LocalDate.now()
                fireAtMillis = LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
                onSave(body, fireAtMillis, deliveryMode)
                showTimePicker = false
            },
        )
    }
}

@Composable
private fun ParseWarningsBlock(warnings: List<ParseWarning>) {
    val messages = warnings.mapNotNull { warningText(it) }
    if (messages.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = Spacing.xs),
    ) {
        messages.forEach { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.tertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun warningText(warning: ParseWarning): String? = when (warning) {
    ParseWarning.TIME_AMBIGUOUS -> stringResource(R.string.warning_time_ambiguous)
    ParseWarning.DATE_MISSING_YEAR -> stringResource(R.string.warning_date_year)
    ParseWarning.NO_TIME_FOUND -> stringResource(R.string.warning_no_time)
    ParseWarning.BODY_EMPTY -> stringResource(R.string.warning_body_empty)
    ParseWarning.PAST_TIME_ADJUSTED -> stringResource(R.string.warning_past_adjusted)
}
