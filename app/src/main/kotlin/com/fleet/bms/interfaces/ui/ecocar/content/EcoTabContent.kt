package com.fleet.bms.interfaces.ui.ecocar.content

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.service.BatteryAlert
import com.fleet.bms.interfaces.ui.dashboard.DashboardViewModel
import com.fleet.bms.interfaces.ui.ecocar.nav.EcoMainDestination
import com.fleet.bms.interfaces.ui.ecocar.settings.EcoSettingsTab
import com.fleet.bms.interfaces.ui.ecocar.subnav.BatterySubNav
import com.fleet.bms.interfaces.ui.ecocar.subnav.BrowserSubNav
import com.fleet.bms.interfaces.ui.ecocar.subnav.ChartsSubNav
import com.fleet.bms.interfaces.ui.ecocar.subnav.MapSubNav
import com.fleet.bms.interfaces.ui.ecocar.subnav.MusicSubNav

@Composable
fun EcoTabContent(
    destination: EcoMainDestination,
    dashboardViewModel: DashboardViewModel,
    batteryTelemetry: BatteryTelemetry?,
    batteryAlerts: List<BatteryAlert>,
    socHistory: List<Float>,
    powerKwHistory: List<Float>,
    onOpenSniffer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (destination) {
        EcoMainDestination.Battery ->
            BatterySubNav(
                dashboardViewModel = dashboardViewModel,
                telemetry = batteryTelemetry,
                alerts = batteryAlerts,
                onOpenSniffer = onOpenSniffer,
                modifier = modifier.fillMaxSize(),
            )
        EcoMainDestination.Charts ->
            ChartsSubNav(
                socHistory = socHistory,
                powerKwHistory = powerKwHistory,
                modifier = modifier.fillMaxSize(),
            )
        EcoMainDestination.Music ->
            MusicSubNav(modifier = modifier.fillMaxSize())
        EcoMainDestination.Browser ->
            BrowserSubNav(modifier = modifier.fillMaxSize())
        EcoMainDestination.Map ->
            MapSubNav(modifier = modifier.fillMaxSize())
        EcoMainDestination.Settings ->
            EcoSettingsTab(modifier = modifier.fillMaxSize())
    }
}
