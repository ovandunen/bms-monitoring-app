package com.fleet.ecocar.ui.battery

import kotlin.math.sin
import kotlin.random.Random

internal enum class DemoAlertSeverity {
    INFO,
    WARNING,
    CRITICAL,
}

internal data class DemoBatteryAlert(
    val severity: DemoAlertSeverity,
    val message: String,
)

internal data class DemoBatterySnapshot(
    val socPercent: Float,
    val packVoltageV: Float,
    val packCurrentA: Float,
    val cellVolts: List<Float>,
) {
    val powerKw: Float
        get() = (packVoltageV * packCurrentA) / 1000f

    fun evolve(): DemoBatterySnapshot {
        val r = Random.Default
        val dSoc = (r.nextFloat() - 0.5f) * 0.15f
        val dV = (r.nextFloat() - 0.5f) * 0.4f
        val dI = (r.nextFloat() - 0.5f) * 2.5f
        val newSoc = (socPercent + dSoc).coerceIn(12f, 98f)
        val newI = (packCurrentA + dI).coerceIn(-180f, 220f)
        val newCells = cellVolts.map { v ->
            val cellNoise = (r.nextFloat() - 0.5f) * 0.004f
            val drift = (newSoc - socPercent) * 0.00025f + dV * 0.01f
            (v + cellNoise + drift).coerceIn(2.95f, 4.25f)
        }
        val sumV = newCells.sum().coerceIn(300f, 450f)
        return copy(
            socPercent = newSoc,
            packVoltageV = sumV,
            packCurrentA = newI,
            cellVolts = newCells,
        )
    }

    companion object {
        fun initial(): DemoBatterySnapshot {
            val rnd = Random(42)
            val base = 3.55f
            val cells = List(CELL_COUNT) { i ->
                (base + (rnd.nextFloat() - 0.5f) * 0.08f + sinWave(i)).coerceIn(3.2f, 3.75f)
            }
            val sum = cells.sum()
            return DemoBatterySnapshot(
                socPercent = 74f,
                packVoltageV = sum,
                packCurrentA = -12f,
                cellVolts = cells,
            )
        }

        private fun sinWave(i: Int): Float = (0.02f * sin(i / 4.0)).toFloat()
    }
}

internal const val CELL_COUNT = 96

internal fun demoAlerts(): List<DemoBatteryAlert> = listOf(
    DemoBatteryAlert(DemoAlertSeverity.INFO, "Zell-Balance aktiv — keine Eingriffe nötig."),
    DemoBatteryAlert(DemoAlertSeverity.WARNING, "Kühlkreislauf: Temperaturgradient leicht erhöht (Demo)."),
    DemoBatteryAlert(DemoAlertSeverity.CRITICAL, "Isolationswiderstand: Prüfung empfohlen (Demo-Hinweis)."),
)
