package com.example.voicemind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    primaryContainer = TealContainer,
    onPrimaryContainer = Teal,
    background = Midnight,
    onBackground = TextPrimaryDark,
    surface = Slate,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateVariant,
    onSurfaceVariant = TextSecondaryDark,
    outline = OutlineDark,
    error = ErrorCoral,
    onError = Color.Black,
)

@Composable
fun VoiceMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
