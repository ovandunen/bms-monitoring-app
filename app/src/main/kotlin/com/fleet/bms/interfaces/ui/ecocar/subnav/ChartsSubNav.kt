package com.fleet.bms.interfaces.ui.ecocar.subnav

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.fleet.bms.interfaces.ui.ecocar.theme.EcoCarColors

private val chartSubLabels = listOf("Leistung", "SOC")

@Composable
fun ChartsSubNav(
    socHistory: List<Float>,
    powerKwHistory: List<Float>,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        EcoSubChipsBar(
            labels = chartSubLabels,
            selectedIndex = tab,
            onSelect = { tab = it },
        )
        HorizontalDivider(color = EcoCarColors.Divider)
        when (tab) {
            0 -> PowerChart(powerKwHistory, Modifier.weight(1f).fillMaxWidth())
            1 -> SocChart(socHistory, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun PowerChart(kwSamples: List<Float>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Leistung (kW)",
            style = MaterialTheme.typography.titleMedium,
            color = EcoCarColors.OnDark,
        )
        Text(
            text = "Aus Pack-Spannung × Strom, gleitend während dieser Sitzung. Kein Fahrzeug-Trip-Verbrauch.",
            style = MaterialTheme.typography.bodySmall,
            color = EcoCarColors.OnDarkSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        val values = kwSamples.takeIf { it.size >= 2 }
            ?: listOf(0f, 0f)
        LineChartCanvas(values = values, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun SocChart(socSamples: List<Float>, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Ladezustand (%) — Sitzung",
            style = MaterialTheme.typography.titleMedium,
            color = EcoCarColors.OnDark,
        )
        Text(
            text = "Letzte SOC-Stichproben aus dem BMS-Stream dieser Sitzung.",
            style = MaterialTheme.typography.bodySmall,
            color = EcoCarColors.OnDarkSecondary,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        val values = socSamples.takeIf { it.size >= 2 }
            ?: listOf(0f, 0f)
        LineChartCanvas(values = values, modifier = Modifier.weight(1f).fillMaxWidth())
    }
}

@Composable
private fun LineChartCanvas(
    values: List<Float>,
    modifier: Modifier = Modifier,
) {
    val lineColor = EcoCarColors.GoldenYellow
    val gridColor = EcoCarColors.Divider
    Canvas(modifier = modifier.padding(8.dp)) {
        val pad = 40f
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val minV = values.minOrNull() ?: 0f
        val maxV = values.maxOrNull() ?: 1f
        val span = (maxV - minV).coerceAtLeast(0.01f)

        // grid
        for (i in 0..4) {
            val y = pad + h * i / 4f
            drawLine(gridColor, Offset(pad, y), Offset(pad + w, y), strokeWidth = 1f)
        }
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = pad + w * i / (values.size - 1).coerceAtLeast(1)
            val y = pad + h * (1f - (v - minV) / span)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f))
    }
}
