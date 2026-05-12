package com.fleet.ecocar.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Genau sechs Nav-Bereiche laut Wireframe (Seite 3).
 */
enum class MainDestination(
    val label: String,
    val icon: ImageVector,
) {
    Music("Music", Icons.Filled.MusicNote),
    Map("Map", Icons.Filled.Map),
    Battery("Battery", Icons.Filled.BatteryChargingFull),
    Charts("Charts", Icons.AutoMirrored.Filled.ShowChart),
    Browser("Browser", Icons.Filled.Language),
    Settings("Settings", Icons.Filled.Settings),
}
