package com.example.voicemind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.parse.ParseWarning
import com.example.voicemind.ui.components.DateTimeField
import com.example.voicemind.ui.components.PresetChips
import com.example.voicemind.ui.components.TimePreset
import com.example.voicemind.ui.components.WarningCard
import com.example.voicemind.ui.components.toEpochMillis
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
    onSave: (body: String, fireAtMillis: Long?) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    var body by remember(pending) { mutableStateOf(pending.body) }
    var fireAtMillis by remember(pending) { mutableStateOf(pending.fireAtMillis) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val fireAtLabel = fireAtMillis?.let { FormatUtils.formatFireAt(it, zone) }
        ?: stringResource(R.string.confirm_time_not_set)

    val warningMessages = buildList {
        if (pending.confidence < 0.7f) {
            add(stringResource(R.string.confirm_low_confidence_hint))
        }
        addAll(pending.warnings.mapNotNull { warningText(it) })
    }

    fun emitSave() {
        onSave(body, fireAtMillis)
    }

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
        bottomBar = {
            BottomAppBar {
                Button(
                    onClick = {
                        emitSave()
                        onConfirm()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.confirm_save))
                }
            }
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
            WarningCard(messages = warningMessages)

            Text(
                text = stringResource(R.string.confirm_phrase_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = pending.rawPhrase,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            DateTimeField(
                label = fireAtLabel,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
            )

            PresetChips(
                selected = null,
                onSelected = {
                    fireAtMillis = it.toEpochMillis(zone)
                    emitSave()
                },
            )

            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    emitSave()
                },
                label = { Text(stringResource(R.string.confirm_body_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
            )
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
                emitSave()
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
                emitSave()
                showTimePicker = false
            },
        )
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
