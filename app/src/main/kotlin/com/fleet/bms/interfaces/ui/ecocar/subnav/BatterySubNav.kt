package com.fleet.bms.interfaces.ui.ecocar.subnav

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.service.AlertSeverity
import com.fleet.bms.domain.service.BatteryAlert
import com.fleet.bms.interfaces.ui.dashboard.DashboardScreen
import com.fleet.bms.interfaces.ui.dashboard.DashboardViewModel
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

private val batterySubLabels = listOf("Übersicht", "Zellen", "Alarme")

@Composable
fun BatterySubNav(
    dashboardViewModel: DashboardViewModel,
    telemetry: BatteryTelemetry?,
    alerts: List<BatteryAlert>,
    onOpenSniffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        EcoSubChipsBar(
            labels = batterySubLabels,
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        when (tab) {
            0 -> Box(Modifier.weight(1f).fillMaxWidth()) {
                DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToSniffer = onOpenSniffer,
                    autoStartMonitoring = false,
                )
            }
            1 -> BatteryCellsGrid(
                voltages = telemetry?.cellVoltages?.voltages,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            2 -> BatteryAlertsList(
                alerts = alerts,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BatteryCellsGrid(
    voltages: List<Double>?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(12.dp)) {
        Text(
            text = if (voltages != null) "Zellspannungen (Live)" else "Zellspannungen — warte auf Telemetrie",
            style = MaterialTheme.typography.titleMedium,
            color = EcoCarColors.OnDark,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (voltages == null) {
            Text(
                text = "Sobald der CAN-Bus vollständige Zellwerte liefert, erscheinen sie hier.",
                style = MaterialTheme.typography.bodyMedium,
                color = EcoCarColors.OnDarkSecondary,
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 56.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(voltages.size) { index ->
                    val v = voltages[index]
                    Card(
                        colors = CardDefaults.cardColors(containerColor = EcoCarColors.SurfaceElevated),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Column(
                            Modifier.padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "#${index + 1}",
                                style = MaterialTheme.typography.labelSmall,
                                color = EcoCarColors.OnDarkSecondary,
                            )
                            Text(
                                "%.2f V".format(v),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = EcoCarColors.GoldenYellow,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryAlertsList(
    alerts: List<BatteryAlert>,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text(
                text = "Alarme & Hinweise",
                style = MaterialTheme.typography.titleMedium,
                color = EcoCarColors.OnDark,
            )
        }
        if (alerts.isEmpty()) {
            item {
                Text(
                    text = "Keine aktiven Alarme aus den derzeitigen Telemetrie-Daten.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoCarColors.OnDarkSecondary,
                )
            }
        } else {
            items(alerts.size) { i ->
                val alert = alerts[i]
                Card(
                    colors = CardDefaults.cardColors(containerColor = EcoCarColors.SurfaceElevated),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            severityLabel(alert.severity),
                            color = EcoCarColors.GoldenYellow,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            alert.message,
                            color = EcoCarColors.OnDark,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun severityLabel(severity: AlertSeverity): String = when (severity) {
    AlertSeverity.INFO -> "Info"
    AlertSeverity.WARNING -> "Warnung"
    AlertSeverity.CRITICAL -> "Kritisch"
}
