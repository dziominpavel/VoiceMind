package com.example.voicemind.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.voicemind.R
import com.example.voicemind.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/** Быстрый preset для напоминания */
enum class TimePreset(val labelRes: Int) {
    PLUS_15_MIN(R.string.preset_plus_15_min),
    PLUS_1_HOUR(R.string.preset_plus_1_hour),
    TOMORROW_9(R.string.preset_tomorrow_9),
    PLUS_1_WEEK(R.string.preset_plus_1_week),
}

/** Вычисляет epoch millis для выбранного preset относительно now */
fun TimePreset.toEpochMillis(zone: ZoneId = ZoneId.systemDefault()): Long {
    val now = Instant.now().atZone(zone)
    val dt = when (this) {
        TimePreset.PLUS_15_MIN -> now.plusMinutes(15)
        TimePreset.PLUS_1_HOUR -> now.plusHours(1)
        TimePreset.TOMORROW_9 -> now.plusDays(1).with(LocalTime.of(9, 0))
        TimePreset.PLUS_1_WEEK -> now.plusWeeks(1)
    }
    return dt.toInstant().toEpochMilli()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PresetChips(
    selected: TimePreset?,
    onSelected: (TimePreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        verticalArrangement = Arrangement.spacedBy(Spacing.xs),
    ) {
        TimePreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelected(preset) },
                label = { Text(stringResource(preset.labelRes)) },
            )
        }
    }
}
