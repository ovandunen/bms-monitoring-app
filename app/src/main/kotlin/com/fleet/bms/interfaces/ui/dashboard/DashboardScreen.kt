package com.fleet.bms.interfaces.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.service.BatteryAlert

/**
 * Dashboard Screen
 *
 * Main screen showing battery telemetry and alerts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToSniffer: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    // Auto-start monitoring when screen appears
    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CarRapide BMS Monitor") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    if (onNavigateToSniffer != null) {
                        IconButton(onClick = onNavigateToSniffer) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = "CAN Sniffer"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    LoadingView()
                }
                is DashboardUiState.Collecting -> {
                    CollectingView(state.percentage)
                }
                is DashboardUiState.Success -> {
                    TelemetryView(
                        telemetry = state.telemetry,
                        alerts = state.alerts
                    )
                }
                is DashboardUiState.Error -> {
                    ErrorView(state.message)
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Connecting to CAN-Bus...")
        }
    }
}

@Composable
private fun CollectingView(percentage: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Collecting telemetry: ${percentage.toInt()}%")
        }
    }
}

@Composable
private fun TelemetryView(
    telemetry: BatteryTelemetry,
    alerts: List<BatteryAlert>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Alerts (if any)
        if (alerts.isNotEmpty()) {
            AlertsCard(alerts)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Main battery status
        BatteryStatusCard(telemetry)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Cell voltages grid
        CellVoltagesCard(telemetry)
    }
}

@Composable
private fun AlertsCard(alerts: List<BatteryAlert>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alerts",
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alerts (${alerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            alerts.forEach { alert ->
                Text(
                    text = "• ${alert.message}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun BatteryStatusCard(telemetry: BatteryTelemetry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Battery Status",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("SOC", "${telemetry.stateOfCharge.value.toInt()}%")
                MetricItem("Voltage", "${"%.1f".format(telemetry.voltage.value)}V")
                MetricItem("Current", "${"%.1f".format(telemetry.current.value)}A")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Min Temp", "${"%.1f".format(telemetry.temperatures.min)}°C")
                MetricItem("Avg Temp", "${"%.1f".format(telemetry.temperatures.avg)}°C")
                MetricItem("Max Temp", "${"%.1f".format(telemetry.temperatures.max)}°C")
            }
        }
    }
}

@Composable
private fun CellVoltagesCard(telemetry: BatteryTelemetry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Cell Voltages (114 cells)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Min: ${"%.3f".format(telemetry.cellVoltages.min())}V")
                Text("Max: ${"%.3f".format(telemetry.cellVoltages.max())}V")
                Text("Δ: ${"%.3f".format(telemetry.cellVoltages.delta())}V")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Show first 20 cells in a grid as example
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier.height(200.dp),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(telemetry.cellVoltages.voltages.take(20).withIndex().toList()) { (index, voltage) ->
                    CellVoltageChip(index + 1, voltage)
                }
            }
            
            if (telemetry.cellVoltages.voltages.size > 20) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "... and ${telemetry.cellVoltages.voltages.size - 20} more cells",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CellVoltageChip(cellNumber: Int, voltage: Double) {
    Surface(
        modifier = Modifier.size(60.dp, 40.dp),
        color = getCellColor(voltage),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "#$cellNumber",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "%.2fV".format(voltage),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun getCellColor(voltage: Double): Color {
    return when {
        voltage < 2.8 -> Color(0xFFD32F2F)  // Critical low
        voltage < 3.0 -> Color(0xFFFF9800)  // Low
        voltage > 4.1 -> Color(0xFFFF5722)  // High
        else -> Color(0xFF4CAF50)           // Normal
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ErrorView(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Error",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
