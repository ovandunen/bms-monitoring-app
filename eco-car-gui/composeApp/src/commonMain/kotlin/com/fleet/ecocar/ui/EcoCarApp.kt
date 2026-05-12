package com.fleet.ecocar.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import com.fleet.ecocar.nav.MainDestination
import com.fleet.ecocar.ui.bottom.BottomTelemetry
import com.fleet.ecocar.ui.theme.EcoCarTheme
import com.fleet.ecocar.ui.top.rememberLiveMusicTopBarState

@Composable
fun EcoCarApp() {
    EcoCarTheme {
        var sidebarExpanded by remember { mutableStateOf(true) }
        var bottomExpanded by remember { mutableStateOf(true) }
        var selected by remember { mutableStateOf(MainDestination.Battery) }
        var showLowBattery by remember { mutableStateOf(false) }
        var showSnifferDemo by remember { mutableStateOf(false) }

        val music = rememberLiveMusicTopBarState()
        val telemetry = remember { BottomTelemetry() }

        Box(Modifier.fillMaxSize()) {
            AppScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown && ev.key == Key.F9) {
                            showLowBattery = true
                            true
                        } else {
                            false
                        }
                    },
            sidebarExpanded = sidebarExpanded,
            onSidebarToggle = { sidebarExpanded = !sidebarExpanded },
            bottomExpanded = bottomExpanded,
            onBottomToggle = { bottomExpanded = !bottomExpanded },
            selected = selected,
            onSelectDestination = { selected = it },
            music = music,
            telemetry = telemetry,
            showLowBattery = showLowBattery,
            onDismissLowBattery = { showLowBattery = false },
            onNavigateToCharging = { /* v1: Hook für Navigation */ },
            onTechnicalIssues = { /* v1: Hook für Diagnose */ },
            onSimulateLowBattery = { showLowBattery = true },
            onBottomSettings = { selected = MainDestination.Settings },
            onBottomInfo = { /* v1: Info-Panel */ },
            onOpenSniffer = { showSnifferDemo = true },
            )
            if (showSnifferDemo) {
                AlertDialog(
                    onDismissRequest = { showSnifferDemo = false },
                    title = { Text("CAN-Sniffer (Demo)") },
                    text = {
                        Text(
                            "In der BMS-Haupt-App öffnet diese Aktion den CAN-Sniffer. " +
                                "Dieses EcoCar-GUI-Projekt bleibt eigenständig ohne Bus-Anbindung.",
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { showSnifferDemo = false }) {
                            Text("OK")
                        }
                    },
                )
            }
        }
    }
}
