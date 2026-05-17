package com.example.voicemind.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.example.voicemind.R
import com.example.voicemind.data.DeliveryMode
import com.example.voicemind.ui.theme.Spacing

@Composable
fun DeliveryModePicker(
    selected: DeliveryMode,
    onSelected: (DeliveryMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.confirm_delivery_label),
            style = MaterialTheme.typography.labelLarge,
        )
        DeliveryMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = mode == selected,
                        onClick = { onSelected(mode) },
                        role = Role.RadioButton,
                    ),
            ) {
                RadioButton(
                    selected = mode == selected,
                    onClick = null,
                )
                Text(
                    text = deliveryModeLabel(mode),
                    modifier = Modifier.padding(start = Spacing.xs),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun deliveryModeLabel(mode: DeliveryMode): String = when (mode) {
    DeliveryMode.NOTIFICATION -> stringResource(R.string.delivery_notification)
    DeliveryMode.ALARM -> stringResource(R.string.delivery_alarm)
    DeliveryMode.VIBRATE_ONLY -> stringResource(R.string.delivery_vibrate)
    DeliveryMode.SILENT -> stringResource(R.string.delivery_silent)
}
