package com.example.voicemind.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import com.example.voicemind.ui.theme.AccentGlow
import com.example.voicemind.ui.theme.ComponentSize
import com.example.voicemind.ui.theme.HapticType
import com.example.voicemind.ui.theme.NeoWaveDuration
import com.example.voicemind.ui.theme.NeoWaveEasing
import com.example.voicemind.ui.theme.NeoWaveHaptics

@Composable
fun MicButton(
    isListening: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isListening -> 1.08f
            isPressed -> 0.92f
            else -> 1f
        },
        animationSpec = tween(
            durationMillis = if (isPressed) NeoWaveDuration.Micro else NeoWaveDuration.Standard,
            easing = NeoWaveEasing.Emphasized,
        ),
        label = "mic_scale",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(ComponentSize.micButtonRing)
                .semantics {
                    contentDescription = if (isListening) {
                        context.getString(R.string.home_mic_stop)
                    } else {
                        context.getString(R.string.home_mic_start)
                    }
                },
        ) {
            // Radial glow ring (always present, stronger when listening)
            GlowRing(isListening = isListening)

            // Main button
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(ComponentSize.micButton)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            NeoWaveHaptics.perform(
                                context,
                                if (isListening) HapticType.Medium else HapticType.Medium,
                            )
                            onClick()
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(ComponentSize.micIcon),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }

        // Simulated waveform (visible only when listening)
        if (isListening) {
            Spacer(modifier = Modifier.height(8.dp))
            SimulatedWaveform()
        }
    }
}

@Composable
private fun GlowRing(isListening: Boolean) {
    val glowAlpha = if (isListening) 0.35f else 0.12f

    val infiniteTransition = rememberInfiniteTransition(label = "glow_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = glowAlpha,
        targetValue = if (isListening) 0.05f else glowAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.45f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_scale",
    )

    Box(
        modifier = Modifier
            .size(ComponentSize.micButtonRing)
            .scale(pulseScale)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentGlow.copy(alpha = pulseAlpha),
                            Color.Transparent,
                        ),
                        radius = size.minDimension * 0.65f,
                    ),
                    radius = size.minDimension * 0.5f,
                )
            },
    )
}

@Composable
private fun SimulatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")

    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 0.3f + (index % 3 + 1) * 0.23f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 400 + index * 150,
                        easing = LinearEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "bar_$index",
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height((12 + height * 28).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape,
                    ),
            )
        }
    }
}
