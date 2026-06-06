package com.example.voicemind.ui.theme

import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring

object NeoWaveSpring {
    /** General UI elements */
    val Default: SpringSpec<Float> = spring(
        stiffness = 380f,
        dampingRatio = 0.9f
    )

    /** Snappy (chips, tabs, switches) */
    val Snappy: SpringSpec<Float> = spring(
        stiffness = 500f,
        dampingRatio = 0.85f
    )

    /** Bouncy (success, check, mic release) */
    val Bouncy: SpringSpec<Float> = spring(
        stiffness = 300f,
        dampingRatio = 0.6f
    )

    /** Gentle (large cards, bottom sheet settle) */
    val Gentle: SpringSpec<Float> = spring(
        stiffness = 200f,
        dampingRatio = 0.95f
    )

    /** Mic button press */
    val MicPress: SpringSpec<Float> = spring(
        stiffness = 400f,
        dampingRatio = 0.75f
    )
}
