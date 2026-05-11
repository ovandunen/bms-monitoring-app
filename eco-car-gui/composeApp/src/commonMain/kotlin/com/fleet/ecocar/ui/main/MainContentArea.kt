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
import com.fleet.ecocar.map.EcoMapContent
import com.fleet.ecocar.nav.MainDestination
import com.fleet.ecocar.theme.EcoCarColors

@Composable
fun MainContentArea(
    destination: MainDestination,
    onSimulateLowBattery: () -> Unit,
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
                MainDestination.Map ->
                    EcoMapContent(Modifier.fillMaxSize())
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
