package com.fleet.ecocar.ui.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.ecocar.browser.EcoBrowserContent
import com.fleet.ecocar.music.EcoMusicContent
import com.fleet.ecocar.map.EcoMapContent
import com.fleet.ecocar.nav.MainDestination
import com.fleet.ecocar.ui.battery.BatterySubNav
import com.fleet.ecocar.ui.charts.ChartsSubNav
import com.fleet.ecocar.theme.EcoCarColors

@Composable
fun MainContentArea(
    destination: MainDestination,
    onSimulateLowBattery: () -> Unit,
    onOpenSniffer: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            when (destination) {
                MainDestination.Music ->
                    EcoMusicContent(Modifier.fillMaxSize())
                MainDestination.Battery ->
                    BatterySubNav(
                        onOpenSniffer = onOpenSniffer,
                        modifier = Modifier.fillMaxSize(),
                    )
                MainDestination.Map ->
                    EcoMapContent(Modifier.fillMaxSize())
                MainDestination.Browser ->
                    EcoBrowserContent(Modifier.fillMaxSize())
                MainDestination.Charts ->
                    ChartsSubNav(Modifier.fillMaxSize())
                else ->
                    PlaceholderScreen(destination = destination)
            }
        }
        if (destination == MainDestination.Settings) {
            Button(
                onClick = onSimulateLowBattery,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EcoCarColors.GoldenYellow,
                    contentColor = EcoCarColors.NearBlack,
                ),
            ) {
                Text(
                    text = "Low-Battery-Dialog testen",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
