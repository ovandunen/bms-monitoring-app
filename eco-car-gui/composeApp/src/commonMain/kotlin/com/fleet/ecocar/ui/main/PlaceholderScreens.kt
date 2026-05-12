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
    MainDestination.Music -> "Musik: USB/Lokal + Radio (Media3) auf Android."
    MainDestination.Map -> "Karte (Android: MapLibre Compose + MapTiler dark; Desktop: Platzhalter)."
    MainDestination.Battery -> "Battery-Tab nutzt BatterySubNav (Übersicht / Zellen / Alarme, Demo-Daten); dieser Text nur bei direktem Aufruf."
    MainDestination.Charts -> "Charts-Tab: ChartsSubNav (Temperatur, Staubdichte, Luftfeuchtigkeit — Demo); dieser Text nur bei direktem Aufruf."
    MainDestination.Browser -> "Browser: GeckoView (Android). Desktop: nicht verfügbar."
    MainDestination.Settings -> "System- und Fahrzeugeinstellungen (Tablet, Sounds, VCU, Battery). " +
        "Nutzen Sie die Schaltfläche unten, um den Low-Battery-Dialog zu testen."
}
