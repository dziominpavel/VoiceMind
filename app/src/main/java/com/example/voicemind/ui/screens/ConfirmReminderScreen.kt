package com.example.voicemind.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.data.FormatUtils
import com.example.voicemind.data.parse.ParseWarning
import com.example.voicemind.ui.components.WarningCard
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.DeliveryVibrate
import com.example.voicemind.ui.theme.HapticType
import com.example.voicemind.ui.theme.NeoWaveHaptics
import com.example.voicemind.ui.theme.ShapePill
import com.example.voicemind.ui.theme.Spacing
import com.example.voicemind.ui.theme.SurfaceElevated
import com.example.voicemind.ui.theme.Teal
import com.example.voicemind.ui.theme.TealContainer
import com.example.voicemind.ui.theme.TextMuted
import com.example.voicemind.ui.theme.TextPrimaryDark
import com.example.voicemind.ui.theme.TimeCritical
import com.example.voicemind.ui.theme.TimeDisplay
import com.example.voicemind.ui.theme.TimeWarning
import com.example.voicemind.viewmodel.PendingReminderConfirm
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ConfirmReminderScreen(
    pending: PendingReminderConfirm,
    defaultDeliveryMode: DeliveryMode = DeliveryMode.NOTIFICATION,
    onBack: () -> Unit,
    onSave: (body: String, fireAtMillis: Long?) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val zone = remember { ZoneId.systemDefault() }
    var body by remember(pending) { mutableStateOf(pending.body) }
    var fireAtMillis by remember(pending) { mutableStateOf(pending.fireAtMillis) }
    var selectedDeliveryMode by remember(defaultDeliveryMode) { mutableStateOf(defaultDeliveryMode) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val fireAtLabel = fireAtMillis?.let { FormatUtils.formatFireAt(it, zone) }
        ?: stringResource(R.string.confirm_time_not_set)

    val warningMessages = buildList {
        addAll(pending.warnings.mapNotNull { warningText(it) })
    }

    fun emitSave() {
        onSave(body, fireAtMillis)
    }

    Scaffold(
        modifier = modifier.fillMaxSize().imePadding(),
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
                            NeoWaveHaptics.perform(context, HapticType.Success)
                        onConfirm()
                    },
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
            // Confidence Indicator
            ConfidenceIndicator(confidence = pending.confidence)

            // Recognized phrase
            if (pending.rawPhrase.isNotBlank()) {
                Text(
                    text = stringResource(R.string.confirm_phrase_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMuted,
                )
                Text(
                    text = pending.rawPhrase,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted,
                )
            }

            // Time Display
            Text(
                text = fireAtLabel,
                style = TimeDisplay,
                color = TextPrimaryDark,
            )

            // Quick Chips
            QuickDateTimeChipsRow(
                currentFireAt = fireAtMillis,
                onChipSelected = { newMillis ->
                    fireAtMillis = newMillis
                    emitSave()
                },
                onCustomClick = { showDatePicker = true },
            )

            // Body field
            OutlinedTextField(
                value = body,
                onValueChange = {
                    body = it
                    emitSave()
                },
                label = { Text(stringResource(R.string.confirm_body_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                shape = MaterialTheme.shapes.medium,
            )

            // Warnings (only parser warnings, not confidence)
            if (warningMessages.isNotEmpty()) {
                WarningCard(messages = warningMessages)
            }

            // Delivery Mode Grid
            Text(
                text = "Режим оповещения",
                style = MaterialTheme.typography.labelLarge,
                color = TextMuted,
            )
            DeliveryModeGrid(
                selectedMode = selectedDeliveryMode,
                onModeSelected = { selectedDeliveryMode = it },
            )

            Spacer(modifier = Modifier.weight(1f))
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
private fun ConfidenceIndicator(confidence: Float) {
    val (dotColor, label) = when {
        confidence >= 0.9f -> Teal to "Высокая уверенность"
        confidence >= 0.7f -> Teal to "Хорошая уверенность"
        confidence >= 0.4f -> TimeWarning to "Средняя уверенность — проверьте"
        else -> TimeCritical to "Низкая уверенность — проверьте"
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, shape = CircleShape),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = dotColor,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickDateTimeChipsRow(
    currentFireAt: Long?,
    onChipSelected: (Long) -> Unit,
    onCustomClick: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val now = LocalDateTime.now(zone)

    val chips = listOf(
        "Сегодня" to now.withHour(9).withMinute(0),
        "Завтра" to now.plusDays(1).withHour(9).withMinute(0),
        "Через час" to now.plusHours(1),
        "Вечером" to now.withHour(18).withMinute(0),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        chips.forEach { (label, dateTime) ->
            val millis = dateTime.atZone(zone).toInstant().toEpochMilli()
            val isSelected = currentFireAt != null && kotlin.math.abs(currentFireAt - millis) < 60_000

            Chip(
                label = label,
                selected = isSelected,
                onClick = { onChipSelected(millis) },
            )
        }
        Chip(
            label = "Выбрать",
            selected = false,
            onClick = onCustomClick,
        )
    }
}

@Composable
private fun Chip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .height(ComponentSize.chipMinHeight)
            .clickable(onClick = onClick)
            .background(
                color = if (selected) TealContainer else SurfaceElevated,
                shape = MaterialTheme.shapes.small,
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) Teal else TextPrimaryDark,
        )
    }
}

@Composable
internal fun DeliveryModeGrid(
    selectedMode: DeliveryMode,
    onModeSelected: (DeliveryMode) -> Unit,
) {
    val modes = listOf(
        DeliveryMode.NOTIFICATION to Triple("Уведомление", "Стандартный пуш", Icons.Default.Notifications),
        DeliveryMode.ALARM to Triple("Будильник", "Громкий звук", Icons.Default.Alarm),
        DeliveryMode.VIBRATE to Triple("Вибрация", "Только вибро", Icons.Default.Vibration),
        DeliveryMode.SILENT to Triple("Тихий", "Без звука", Icons.Default.NotificationsOff),
    )

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        modes.chunked(2).forEach { rowModes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                rowModes.forEach { (mode, info) ->
                    val (title, subtitle, icon) = info
                    val isSelected = mode == selectedMode

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(ComponentSize.deliveryCardHeight)
                            .clickable { onModeSelected(mode) },
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) TealContainer else SurfaceElevated,
                        ),
                        border = if (isSelected) {
                            androidx.compose.foundation.BorderStroke(2.dp, Teal)
                        } else {
                            androidx.compose.foundation.BorderStroke(1.dp, TextMuted.copy(alpha = 0.3f))
                        },
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(32.dp),
                                tint = when (mode) {
                                    DeliveryMode.ALARM -> TimeWarning
                                    DeliveryMode.VIBRATE -> DeliveryVibrate
                                    else -> if (isSelected) Teal else TextMuted
                                },
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isSelected) Teal else TextPrimaryDark,
                            )
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = TextMuted,
                            )
                        }
                    }
                }
            }
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

