package com.example.voicemind.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.voicemind.R
import kotlinx.coroutines.launch

@Composable
fun SwipeToRevealBox(
    modifier: Modifier = Modifier,
    maxReveal: Dp = 80.dp,
    threshold: Float = 0.5f,
    isRevealed: Boolean = false,
    onRevealedChange: (Boolean) -> Unit = {},
    onClick: () -> Unit,
    onAction: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val maxRevealPx = with(density) { maxReveal.toPx() }
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val animSpec = tween<Float>(300)

    // Sync with external state (radio-behavior or programmatic collapse)
    LaunchedEffect(isRevealed) {
        val target = if (isRevealed) -maxRevealPx else 0f
        if (kotlin.math.abs(offsetX.value - target) > 0.5f) {
            offsetX.animateTo(target, animationSpec = animSpec)
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {
        // Background reveal panel
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.errorContainer)
                .clickable { onAction() },
            contentAlignment = Alignment.CenterEnd,
        ) {
            Box(
                modifier = Modifier
                    .width(maxReveal)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.list_cancel),
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        // Foreground content (draggable card)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.toInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            val shouldReveal = offsetX.value < -maxRevealPx * threshold
                            scope.launch {
                                offsetX.animateTo(
                                    if (shouldReveal) -maxRevealPx else 0f,
                                    animationSpec = animSpec,
                                )
                            }
                            onRevealedChange(shouldReveal)
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val newOffset = (offsetX.value + dragAmount)
                                .coerceIn(-maxRevealPx, 0f)
                            scope.launch { offsetX.snapTo(newOffset) }
                        },
                    )
                }
                .pointerInput(offsetX.value) {
                    detectTapGestures(
                        onTap = {
                            if (offsetX.value < -1f) {
                                scope.launch {
                                    offsetX.animateTo(0f, animationSpec = animSpec)
                                    onRevealedChange(false)
                                }
                            } else {
                                onClick()
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
