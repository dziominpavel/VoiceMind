package com.example.voicemind

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.voicemind.ui.navigation.AppDestination

enum class AppDestinations(
    override val label: String,
    override val icon: ImageVector,
) : AppDestination {
    HOME("Главная", Icons.Default.Home),
    LIST("Список", Icons.AutoMirrored.Filled.List),
    SETTINGS("Настройки", Icons.Default.Settings),
}
