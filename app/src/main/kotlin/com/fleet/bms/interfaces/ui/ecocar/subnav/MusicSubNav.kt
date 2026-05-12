package com.fleet.bms.interfaces.ui.ecocar.subnav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

private val musicSubLabels = listOf("UKW / FM", "USB", "Bluetooth")

@Composable
fun MusicSubNav(modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        EcoSubChipsBar(
            labels = musicSubLabels,
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (tab) {
                0 -> {
                    Icon(Icons.Outlined.Radio, null, tint = EcoCarColors.GoldenYellow, modifier = Modifier.padding(16.dp))
                    Text("UKW / FM", style = MaterialTheme.typography.headlineSmall, color = EcoCarColors.OnDark)
                    Text(
                        "Senderliste und RDS — Anbindung an Radio-HAL / ExoPlayer folgt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoCarColors.OnDarkSecondary,
                    )
                }
                1 -> {
                    Icon(Icons.Filled.Usb, null, tint = EcoCarColors.GoldenYellow, modifier = Modifier.padding(16.dp))
                    Text("USB", style = MaterialTheme.typography.headlineSmall, color = EcoCarColors.OnDark)
                    Text(
                        "MTP / Massenspeicher-Wiedergabe — Dateiauswahl folgt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoCarColors.OnDarkSecondary,
                    )
                }
                else -> {
                    Icon(Icons.Filled.Bluetooth, null, tint = EcoCarColors.GoldenYellow, modifier = Modifier.padding(16.dp))
                    Text("Bluetooth", style = MaterialTheme.typography.headlineSmall, color = EcoCarColors.OnDark)
                    Text(
                        "Kopplung und A2DP — System-Bluetooth-Integration folgt.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EcoCarColors.OnDarkSecondary,
                    )
                }
            }
        }
    }
}
