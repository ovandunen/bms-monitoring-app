package com.fleet.ecocar.ui.battery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fleet.ecocar.theme.EcoCarColors
import com.fleet.ecocar.ui.subnav.EcoSubChipsBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val batterySubLabels = listOf("Übersicht", "Zellen", "Alarme")

@Composable
fun BatterySubNav(
    onOpenSniffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }
    var snapshot by remember { mutableStateOf(DemoBatterySnapshot.initial()) }
    val alerts = remember { demoAlerts() }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(900L)
            snapshot = snapshot.evolve()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        EcoSubChipsBar(
            labels = batterySubLabels,
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        when (tab) {
            0 -> BatteryOverviewTab(
                snapshot = snapshot,
                onOpenSniffer = onOpenSniffer,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            1 -> BatteryCellsGrid(
                cellVolts = snapshot.cellVolts,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
            else -> BatteryAlertsList(
                alerts = alerts,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BatteryOverviewTab(
    snapshot: DemoBatterySnapshot,
    onOpenSniffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Hochvolt-Batterie",
            style = MaterialTheme.typography.titleLarge,
            color = EcoCarColors.OnDark,
        )
        Text(
            text = "Demo-Telemetrie für Layout — Anbindung CAN/BMS folgt.",
            style = MaterialTheme.typography.bodySmall,
            color = EcoCarColors.OnDarkSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                title = "SOC",
                value = "%.1f".format(snapshot.socPercent),
                unit = "%",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Pack-Spannung",
                value = "%.1f".format(snapshot.packVoltageV),
                unit = "V",
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MetricCard(
                title = "Pack-Strom",
                value = "%.1f".format(snapshot.packCurrentA),
                unit = "A",
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                title = "Leistung",
                value = "%.2f".format(snapshot.powerKw),
                unit = "kW",
                modifier = Modifier.weight(1f),
            )
        }
        Button(
            onClick = onOpenSniffer,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = EcoCarColors.GoldenYellow,
                contentColor = EcoCarColors.NearBlack,
            ),
        ) {
            Text("CAN-Sniffer öffnen (Demo)")
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = EcoCarColors.SurfaceElevated),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = EcoCarColors.OnDarkSecondary,
            )
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = EcoCarColors.GoldenYellow,
                )
                Text(
                    text = " $unit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoCarColors.OnDark,
                )
            }
        }
    }
}

@Composable
private fun BatteryCellsGrid(
    cellVolts: List<Float>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(12.dp)) {
        Text(
            text = "Zellspannungen (Demo, $CELL_COUNT Zellen)",
            style = MaterialTheme.typography.titleMedium,
            color = EcoCarColors.OnDark,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        Text(
            text = "Simulierte Einzelzellwerte — Live-Daten erscheinen hier nach CAN-Anbindung.",
            style = MaterialTheme.typography.bodySmall,
            color = EcoCarColors.OnDarkSecondary,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 56.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            itemsIndexed(cellVolts) { index, v ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = EcoCarColors.SurfaceElevated),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = "#${index + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = EcoCarColors.OnDarkSecondary,
                        )
                        Text(
                            text = "%.2f V".format(v),
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

@Composable
private fun BatteryAlertsList(
    alerts: List<DemoBatteryAlert>,
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
                    text = "Keine aktiven Alarme.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EcoCarColors.OnDarkSecondary,
                )
            }
        } else {
            items(alerts, key = { it.message }) { alert ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = EcoCarColors.SurfaceElevated),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            text = severityLabel(alert.severity),
                            color = EcoCarColors.GoldenYellow,
                            style = MaterialTheme.typography.labelMedium,
                        )
                        Text(
                            text = alert.message,
                            color = EcoCarColors.OnDark,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

private fun severityLabel(severity: DemoAlertSeverity): String = when (severity) {
    DemoAlertSeverity.INFO -> "Info"
    DemoAlertSeverity.WARNING -> "Warnung"
    DemoAlertSeverity.CRITICAL -> "Kritisch"
}
