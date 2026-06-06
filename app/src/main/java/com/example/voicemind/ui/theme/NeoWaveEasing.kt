package com.example.voicemind.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

object NeoWaveEasing {
    /** Standard UI transitions (Material recommended) cubic(0.4, 0.0, 0.2, 1) */
    val Standard = FastOutSlowInEasing

    /** Enter / appear cubic(0.0, 0.0, 0.2, 1) */
    val Enter = FastOutLinearInEasing

    /** Exit / dismiss cubic(0.4, 0.0, 1.0, 1) */
    val Exit = LinearOutSlowInEasing

    /** Emphasized (primary actions, mic, hero) */
    val Emphasized = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /** Decelerate (spring-like settle) */
    val Decelerate = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Bounce (success, checkmark) */
    val Bounce = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)
}
