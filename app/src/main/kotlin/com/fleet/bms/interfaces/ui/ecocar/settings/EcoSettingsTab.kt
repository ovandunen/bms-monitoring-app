package com.fleet.bms.interfaces.ui.ecocar.settings

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

@Composable
fun EcoSettingsTab(
    modifier: Modifier = Modifier,
    viewModel: EcoSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val loadError by viewModel.loadFailed.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "BMS & Cloud",
            style = MaterialTheme.typography.headlineSmall,
            color = EcoCarColors.OnDark,
        )
        Text(
            text = "Pack-ID, Fahrzeug-ID und MQTT-Broker. Nach Speichern wird die App neu geladen, damit Verbindungen die Werte übernehmen.",
            style = MaterialTheme.typography.bodyMedium,
            color = EcoCarColors.OnDarkSecondary,
        )
        loadError?.let { err ->
            Text(
                text = err,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        OutlinedTextField(
            value = settings.batteryPackId,
            onValueChange = { viewModel.update { s -> s.copy(batteryPackId = it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Battery Pack ID") },
            colors = ecoFieldColors(),
        )
        OutlinedTextField(
            value = settings.vehicleId,
            onValueChange = { viewModel.update { s -> s.copy(vehicleId = it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Vehicle ID") },
            colors = ecoFieldColors(),
        )
        OutlinedTextField(
            value = settings.mqttBroker,
            onValueChange = { viewModel.update { s -> s.copy(mqttBroker = it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("MQTT Broker (tcp://… oder ssl://…)") },
            colors = ecoFieldColors(),
        )
        OutlinedTextField(
            value = settings.mqttUsername,
            onValueChange = { viewModel.update { s -> s.copy(mqttUsername = it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("MQTT Benutzername") },
            colors = ecoFieldColors(),
        )
        OutlinedTextField(
            value = settings.mqttPassword,
            onValueChange = { viewModel.update { s -> s.copy(mqttPassword = it) } },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("MQTT Passwort") },
            colors = ecoFieldColors(),
        )
        Button(
            onClick = {
                viewModel.save {
                    activity?.recreate()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoCarColors.GoldenYellow,
                contentColor = EcoCarColors.NearBlack,
            ),
        ) {
            Text("Speichern & neu laden")
        }
    }
}

@Composable
private fun ecoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = EcoCarColors.OnDark,
    unfocusedTextColor = EcoCarColors.OnDark,
    focusedBorderColor = EcoCarColors.GoldenYellow,
    unfocusedBorderColor = EcoCarColors.Divider,
    focusedLabelColor = EcoCarColors.GoldenYellow,
    unfocusedLabelColor = EcoCarColors.OnDarkSecondary,
    cursorColor = EcoCarColors.GoldenYellow,
)
