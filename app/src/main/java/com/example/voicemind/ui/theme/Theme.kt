package com.example.voicemind.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

internal val DarkColorScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color.Black,
    primaryContainer = TealContainer,
    onPrimaryContainer = Teal,
    secondary = AccentSoft,
    onSecondary = Color.White,
    secondaryContainer = TealContainer,
    onSecondaryContainer = Teal,
    background = BackgroundPrimary,
    onBackground = TextPrimaryDark,
    surface = Slate,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceTint = Teal,
    outline = OutlineDark,
    outlineVariant = OutlineStrong,
    error = ErrorCoral,
    onError = Color.Black,
    errorContainer = Color(0xFF3D1F1F),
    onErrorContainer = ErrorCoral,
)

@Composable
fun VoiceMindTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = VoiceMindTypography,
        shapes = VoiceMindShapes,
        content = content,
    )
}
