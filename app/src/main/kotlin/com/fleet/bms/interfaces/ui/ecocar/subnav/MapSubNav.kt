package com.fleet.bms.interfaces.ui.ecocar.subnav

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

private val mapSubLabels = listOf("Karte", "Routen")

/** OSM-Embed um Dakar (Demo, kein Google-API-Key). */
private const val OsmEmbedDakar =
    "https://www.openstreetmap.org/export/embed.html?bbox=-17.55,14.65,-17.38,14.80&layer=mapnik"

@Composable
fun MapSubNav(modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        EcoSubChipsBar(
            labels = mapSubLabels,
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        when (tab) {
            0 -> {
                EcoWebView(
                    url = OsmEmbedDakar,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                ) {
                    Text(
                        text = "Routen & Ziele",
                        style = MaterialTheme.typography.titleLarge,
                        color = EcoCarColors.OnDark,
                    )
                    Text(
                        text = "Zielwahl, nächste Ladestation — später mit Navigation / Google Maps SDK.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoCarColors.OnDarkSecondary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}
