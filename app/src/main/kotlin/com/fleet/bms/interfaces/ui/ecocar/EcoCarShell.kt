package com.fleet.bms.interfaces.ui.ecocar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleet.bms.BuildConfig
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.ConnectionState
import com.fleet.bms.domain.service.BatteryAlert
import com.fleet.bms.interfaces.ui.dashboard.DashboardUiState
import com.fleet.bms.interfaces.ui.dashboard.DashboardViewModel
import com.fleet.bms.interfaces.ui.ecocar.bottom.EcoBottomBar
import com.fleet.bms.interfaces.ui.ecocar.bottom.EcoBottomTelemetry
import com.fleet.bms.interfaces.ui.ecocar.content.EcoTabContent
import com.fleet.bms.interfaces.ui.ecocar.dialog.LowBatteryDialog
import com.fleet.bms.interfaces.ui.ecocar.nav.EcoMainDestination
import com.fleet.bms.interfaces.ui.ecocar.side.EcoSideNav
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors
import com.fleet.bms.interfaces.ui.ecocar.top.EcoTopBar
import com.fleet.bms.interfaces.ui.ecocar.top.EcoTopBarMusicState
import com.fleet.bms.sniffer.presentation.CanSnifferScreen
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private const val NOMINAL_FULL_RANGE_KM_AT_100 = 220
private val lowBatterySocRecovery = 25.0

/**
 * EcoCar 4-Zonen-Shell für Pixel-Tablet: TopBar, SideNav, Hauptinhalt, BottomBar.
 * Gemeinsamer [DashboardViewModel]: Telemetrie startet beim Öffnen der Shell.
 */
@Composable
fun EcoCarShell(
    modifier: Modifier = Modifier,
    /** Nur gesetzt im Debug-Build: Navigation zum klassischen BMS-UI. */
    onDevNavigateToLegacyDashboard: (() -> Unit)? = null,
    onDevNavigateToLegacySniffer: (() -> Unit)? = null,
    dashboardVm: DashboardViewModel = hiltViewModel(),
) {
    LaunchedEffect(Unit) {
        dashboardVm.startMonitoring()
    }

    var sidebarExpanded by rememberSaveable { mutableStateOf(true) }
    var bottomExpanded by rememberSaveable { mutableStateOf(true) }
    var selectedName by rememberSaveable { mutableStateOf(EcoMainDestination.Battery.name) }
    val selected = EcoMainDestination.valueOf(selectedName)

    var showLowBattery by rememberSaveable { mutableStateOf(false) }
    var lowBatteryMutedUntilRecovery by rememberSaveable { mutableStateOf(false) }
    var showSniffer by rememberSaveable { mutableStateOf(false) }
    var showInfo by rememberSaveable { mutableStateOf(false) }

    val uiState by dashboardVm.uiState.collectAsState()
    val socHistory by dashboardVm.socHistory.collectAsState()
    val powerKwHistory by dashboardVm.powerKwHistory.collectAsState()
    val connectionState by dashboardVm.connectionState.collectAsState()

    var cachedTelemetry by remember { mutableStateOf<BatteryTelemetry?>(null) }
    var cachedAlerts by remember { mutableStateOf<List<BatteryAlert>>(emptyList()) }
    LaunchedEffect(uiState) {
        val s = uiState
        if (s is DashboardUiState.Success) {
            cachedTelemetry = s.telemetry
            cachedAlerts = s.alerts

            val soc = s.telemetry.stateOfCharge
            if (soc.value >= lowBatterySocRecovery) {
                lowBatteryMutedUntilRecovery = false
            }
            if (soc.isLow() && !lowBatteryMutedUntilRecovery) {
                showLowBattery = true
            }
        }
    }

    var music by remember { mutableStateOf(EcoTopBarMusicState()) }
    LaunchedEffect(Unit) {
        val fmt = DateTimeFormatter.ofPattern("HH:mm")
        while (true) {
            music = music.copy(clock = LocalTime.now().format(fmt))
            delay(30_000L)
        }
    }

    val bottomTelemetry = remember(cachedTelemetry) {
        telemetryToEcoBottomBar(cachedTelemetry)
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showSniffer) {
            CanSnifferScreen(onNavigateBack = { showSniffer = false })
        } else {
            Row(modifier = Modifier.fillMaxSize()) {
                EcoSideNav(
                    expanded = sidebarExpanded,
                    onToggleExpand = { sidebarExpanded = !sidebarExpanded },
                    selected = selected,
                    onSelect = { selectedName = it.name },
                )
                Column(modifier = Modifier.weight(1f).fillMaxSize()) {
                    EcoTopBar(music = music)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(EcoCarColors.NearBlack),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                EcoTabContent(
                                    destination = selected,
                                    dashboardViewModel = dashboardVm,
                                    batteryTelemetry = cachedTelemetry,
                                    batteryAlerts = cachedAlerts,
                                    socHistory = socHistory,
                                    powerKwHistory = powerKwHistory,
                                    onOpenSniffer = { showSniffer = true },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            if (selected == EcoMainDestination.Settings && BuildConfig.DEBUG) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Button(
                                        onClick = { showLowBattery = true },
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
                                    if (onDevNavigateToLegacyDashboard != null &&
                                        onDevNavigateToLegacySniffer != null
                                    ) {
                                        HorizontalDivider(color = EcoCarColors.Divider)
                                        Text(
                                            text = "Entwickler",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = EcoCarColors.OnDarkSecondary,
                                        )
                                        OutlinedButton(
                                            onClick = onDevNavigateToLegacyDashboard,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                "Legacy BMS-Dashboard",
                                                color = EcoCarColors.GoldenYellow,
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = onDevNavigateToLegacySniffer,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                "Legacy CAN-Sniffer",
                                                color = EcoCarColors.GoldenYellow,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    EcoBottomBar(
                        expanded = bottomExpanded,
                        onToggleExpand = { bottomExpanded = !bottomExpanded },
                        telemetry = bottomTelemetry,
                        onSettingsClick = { selectedName = EcoMainDestination.Settings.name },
                        onInfoClick = { showInfo = true },
                    )
                }
            }
        }
        if (showLowBattery) {
            LowBatteryDialog(
                onDismiss = {
                    showLowBattery = false
                    if (cachedTelemetry?.stateOfCharge?.isLow() == true) {
                        lowBatteryMutedUntilRecovery = true
                    }
                },
                onNavigateToCharging = {
                    showLowBattery = false
                    if (cachedTelemetry?.stateOfCharge?.isLow() == true) {
                        lowBatteryMutedUntilRecovery = true
                    }
                    selectedName = EcoMainDestination.Map.name
                },
                onTechnicalIssues = {
                    showLowBattery = false
                    if (cachedTelemetry?.stateOfCharge?.isLow() == true) {
                        lowBatteryMutedUntilRecovery = true
                    }
                    showSniffer = true
                },
            )
        }
        if (showInfo) {
            EcoTelemetryInfoDialog(
                uiState = uiState,
                connectionState = connectionState,
                onDismiss = { showInfo = false },
            )
        }
    }
}

@Composable
private fun EcoTelemetryInfoDialog(
    uiState: DashboardUiState,
    connectionState: ConnectionState,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EcoCarColors.SurfaceElevated,
        titleContentColor = EcoCarColors.OnDark,
        textContentColor = EcoCarColors.OnDarkSecondary,
        title = { Text("Telemetrie-Status") },
        text = {
            Column {
                Text("MQTT: ${connectionLabel(connectionState)}")
                Text(shellStateLine(uiState), modifier = Modifier.padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = EcoCarColors.GoldenYellow)
            }
        },
    )
}

private fun shellStateLine(state: DashboardUiState): String = when (state) {
    is DashboardUiState.Loading -> "BMS-Dashboard: Verbindungsaufbau …"
    is DashboardUiState.Collecting ->
        "BMS aggregiert Daten (${state.percentage.roundToInt()} %)."
    is DashboardUiState.Success ->
        "Letztes SOC: ${state.telemetry.stateOfCharge.value.roundToInt()} % • " +
            "Paket-Spannung: ${"%.1f".format(state.telemetry.voltage.value)} V"
    is DashboardUiState.Error -> state.message
}

private fun connectionLabel(state: ConnectionState): String = when (state) {
    ConnectionState.DISCONNECTED -> "getrennt"
    ConnectionState.CONNECTING -> "verbindet …"
    ConnectionState.CONNECTED -> "verbunden"
    ConnectionState.RECONNECTING -> "verbindet erneut …"
    ConnectionState.ERROR -> "Fehler"
}

private fun telemetryToEcoBottomBar(telemetry: BatteryTelemetry?): EcoBottomTelemetry {
    if (telemetry == null) return EcoBottomTelemetry()
    val soc = telemetry.stateOfCharge.value.toInt().coerceIn(0, 100)
    val rangeKm = ((soc / 100.0) * NOMINAL_FULL_RANGE_KM_AT_100).roundToInt()
    return EcoBottomTelemetry(
        socPercent = soc,
        tripDistanceKm = null,
        rangeKm = rangeKm,
        co2SavingTons = null,
    )
}
