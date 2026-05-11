package com.fleet.ecocar.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.ecocar.nav.MainDestination
import com.fleet.ecocar.theme.EcoCarColors

@Composable
fun PlaceholderScreen(destination: MainDestination) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = destination.label,
            style = MaterialTheme.typography.headlineMedium,
            color = EcoCarColors.OnDark,
        )
        Text(
            text = placeholderBlurb(destination),
            style = MaterialTheme.typography.bodyLarge,
            color = EcoCarColors.OnDarkSecondary,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}

private fun placeholderBlurb(d: MainDestination): String = when (d) {
    MainDestination.Music -> "Platzhalter: Wiedergabe, Quelle (UKW/USB/Bluetooth) — v1 ohne echte Medienpipeline."
    MainDestination.Map -> "Karte (Android: MapLibre Compose + MapTiler dark; Desktop: Platzhalter)."
    MainDestination.Battery -> "Platzhalter: BMS / Ladezustand / Zellübersicht — Anbindung folgt."
    MainDestination.Charts -> "Platzhalter: Verbrauch, Effizienz, Historie — Datenanbindung folgt."
    MainDestination.Browser -> "Platzhalter: eingebetteter Browser / Internet — v1 ohne WebView."
    MainDestination.Settings -> "System- und Fahrzeugeinstellungen (Tablet, Sounds, VCU, Battery). " +
        "Nutzen Sie die Schaltfläche unten, um den Low-Battery-Dialog zu testen."
}
