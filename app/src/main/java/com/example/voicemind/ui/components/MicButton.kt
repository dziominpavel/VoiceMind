package com.example.voicemind.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.example.voicemind.R
import com.example.voicemind.ui.theme.ComponentSize

@Composable
fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(ComponentSize.micButtonRing),
        contentAlignment = Alignment.Center,
    ) {
        if (isListening) {
            val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
            val scale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.35f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        1.0f at 0
                        1.25f at 600
                        1.35f at 1200
                    },
                ),
                label = "scale",
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.6f at 0
                        0.3f at 600
                        0f at 1200
                    },
                ),
                label = "alpha",
            )
            Box(
                modifier = Modifier
                    .size(ComponentSize.micButtonRing)
                    .scale(scale)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                        shape = CircleShape,
                    ),
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(ComponentSize.micButton)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                ),
        ) {
            Icon(
                imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isListening) {
                    stringResource(R.string.home_mic_stop)
                } else {
                    stringResource(R.string.home_mic_start)
                },
                modifier = Modifier.size(ComponentSize.micIcon),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
