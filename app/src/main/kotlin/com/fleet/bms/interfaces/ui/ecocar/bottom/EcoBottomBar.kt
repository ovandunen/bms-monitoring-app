package com.fleet.bms.interfaces.ui.ecocar.bottom

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

data class EcoBottomTelemetry(
    /** SOC only field always filled when CAN data is flowing; UI shows 0 before first packet. */
    val socPercent: Int = 0,
    /** No trip sensor in-app; null shows as "—". */
    val tripDistanceKm: Int? = null,
    /** Naive SOC-based estimate until VCU integration. */
    val rangeKm: Int? = null,
    val co2SavingTons: Double? = null,
)

@Composable
fun EcoBottomBar(
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    telemetry: EcoBottomTelemetry,
    onSettingsClick: () -> Unit,
    onInfoClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = EcoCarColors.SurfaceElevated,
    ) {
        Column {
            HorizontalDivider(color = EcoCarColors.Divider, thickness = 1.dp)
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TelemetryChip("Ladestand", "${telemetry.socPercent} %")
                    TelemetryChip("Distanz (Trip)", formatKm(telemetry.tripDistanceKm))
                    TelemetryChip("Reichweite", formatKm(telemetry.rangeKm))
                    TelemetryChip("CO₂-Ersparnis", formatTons(telemetry.co2SavingTons))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onSettingsClick) {
                        Icon(
                            Icons.Filled.Settings,
                            contentDescription = null,
                            tint = EcoCarColors.GoldenYellow,
                            modifier = Modifier.padding(end = 6.dp),
                        )
                        Text("Settings", color = EcoCarColors.OnDark)
                    }
                    TextButton(onClick = onInfoClick) {
                        Text("Info", color = EcoCarColors.OnDark)
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = EcoCarColors.GoldenYellow,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${telemetry.socPercent} %",
                        style = MaterialTheme.typography.titleMedium,
                        color = EcoCarColors.GoldenYellow,
                    )
                    Text(
                        text = "${formatKm(telemetry.tripDistanceKm)} · ${formatKm(telemetry.rangeKm)} · ${formatTons(telemetry.co2SavingTons)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = EcoCarColors.OnDarkSecondary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                        contentDescription = if (expanded) "Untere Leiste einklappen" else "Untere Leiste aufklappen",
                        tint = EcoCarColors.GoldenYellow,
                    )
                }
            }
        }
    }
}

@Composable
private fun formatKm(km: Int?): String =
    km?.let { "$it km" } ?: "—"

private fun formatTons(t: Double?): String =
    t?.let { String.format("%.1f t", it) } ?: "—"

private fun TelemetryChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = EcoCarColors.OnDarkSecondary)
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = EcoCarColors.OnDark)
    }
}
