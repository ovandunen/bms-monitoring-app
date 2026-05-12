package com.fleet.ecocar.map

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fleet.ecocar.BuildConfig
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.style.BaseStyle

private const val MAPTILER_DARK_BASE = "https://api.maptiler.com/maps/streets-v2-dark/style.json"

@Composable
actual fun EcoMapContent(modifier: Modifier) {
    val styleUri = remember(BuildConfig.MAPTILER_KEY) {
        val key = BuildConfig.MAPTILER_KEY
        if (key.isBlank()) MAPTILER_DARK_BASE else "$MAPTILER_DARK_BASE?key=$key"
    }
    MaplibreMap(
        modifier = modifier,
        baseStyle = BaseStyle.Uri(styleUri),
    )
}
