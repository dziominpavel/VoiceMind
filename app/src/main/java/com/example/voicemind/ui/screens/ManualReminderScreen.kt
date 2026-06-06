package com.example.voicemind.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import com.example.voicemind.ui.components.DateTimeField
import com.example.voicemind.ui.components.WarningCard
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.ShapePill
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TimeDisplay
import com.example.voicemind.viewmodel.ManualReminderDraft
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualReminderScreen(
    draft: ManualReminderDraft,
    onBack: () -> Unit,
    onSave: (body: String, fireAtMillis: Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = remember { ZoneId.systemDefault() }
    var body by remember(draft) { mutableStateOf(draft.body) }
    var fireAtMillis by remember(draft) { mutableStateOf(draft.fireAtMillis) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleRes = when {
        draft.editingReminderId != null -> R.string.manual_edit_title
        draft.fromVoiceParseFailure -> R.string.manual_voice_fallback_title
        else -> R.string.manual_create_title
    }

    val fireAtLabel = fireAtMillis?.let { FormatUtils.formatFireAt(it, zone) }
        ?: stringResource(R.string.confirm_time_not_set)

    val warningMessages = if (draft.fromVoiceParseFailure) {
        listOf(stringResource(R.string.manual_voice_parse_failed))
    } else {
        emptyList()
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes)) },
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
                    onClick = { onSave(body.trim(), fireAtMillis) },
                    modifier = Modifier.fillMaxWidth().height(ComponentSize.saveButtonHeight),
                    shape = ShapePill,
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
                .padding(horizontal = Spacing.lg, vertical = Spacing.md)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            // Time display
            Text(
                text = fireAtLabel,
                style = TimeDisplay,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Warnings
            if (warningMessages.isNotEmpty()) {
                WarningCard(messages = warningMessages)
            }

            // Raw phrase (if from voice)
            draft.rawPhrase?.let { phrase ->
                Text(
                    text = stringResource(R.string.confirm_phrase_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            DateTimeField(
                label = fireAtLabel,
                onDateClick = { showDatePicker = true },
                onTimeClick = { showTimePicker = true },
            )

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(R.string.confirm_body_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
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
                showTimePicker = false
            },
        )
    }
}
