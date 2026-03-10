package com.fleet.bms.domain.service

import com.fleet.bms.domain.model.*

/**
 * Domain Service: Alert Evaluator
 * 
 * Evaluates battery telemetry against safety thresholds
 * and generates appropriate alerts.
 */
class AlertEvaluator {
    
    fun evaluate(telemetry: BatteryTelemetry): List<BatteryAlert> {
        val alerts = mutableListOf<BatteryAlert>()
        
        // Voltage checks
        evaluateVoltage(telemetry, alerts)
        
        // Temperature checks
        evaluateTemperature(telemetry, alerts)
        
        // Cell imbalance checks
        evaluateCellBalance(telemetry, alerts)
        
        // SOC checks
        evaluateSoc(telemetry, alerts)
        
        // Current checks
        evaluateCurrent(telemetry, alerts)
        
        return alerts
    }
    
    private fun evaluateVoltage(telemetry: BatteryTelemetry, alerts: MutableList<BatteryAlert>) {
        val voltage = telemetry.voltage.value
        
        when {
            voltage < 285.0 -> alerts.add(
                BatteryAlert.CriticalVoltageLow(
                    voltage = voltage,
                    threshold = 285.0,
                    severity = AlertSeverity.CRITICAL
                )
            )
            voltage < 300.0 -> alerts.add(
                BatteryAlert.VoltageLow(
                    voltage = voltage,
                    threshold = 300.0,
                    severity = AlertSeverity.WARNING
                )
            )
            voltage > 478.8 -> alerts.add(
                BatteryAlert.VoltageHigh(
                    voltage = voltage,
                    threshold = 478.8,
                    severity = AlertSeverity.CRITICAL
                )
            )
            voltage > 470.0 -> alerts.add(
                BatteryAlert.VoltageHigh(
                    voltage = voltage,
                    threshold = 470.0,
                    severity = AlertSeverity.WARNING
                )
            )
        }
    }
    
    private fun evaluateTemperature(telemetry: BatteryTelemetry, alerts: MutableList<BatteryAlert>) {
        val temps = telemetry.temperatures
        
        when {
            temps.max > 60.0 -> alerts.add(
                BatteryAlert.HighTemperature(
                    temperature = temps.max,
                    threshold = 60.0,
                    severity = AlertSeverity.CRITICAL
                )
            )
            temps.max > 50.0 -> alerts.add(
                BatteryAlert.HighTemperature(
                    temperature = temps.max,
                    threshold = 50.0,
                    severity = AlertSeverity.WARNING
                )
            )
            temps.min < -20.0 -> alerts.add(
                BatteryAlert.LowTemperature(
                    temperature = temps.min,
                    threshold = -20.0,
                    severity = AlertSeverity.CRITICAL
                )
            )
            temps.min < 0.0 -> alerts.add(
                BatteryAlert.LowTemperature(
                    temperature = temps.min,
                    threshold = 0.0,
                    severity = AlertSeverity.WARNING
                )
            )
        }
    }
    
    private fun evaluateCellBalance(telemetry: BatteryTelemetry, alerts: MutableList<BatteryAlert>) {
        val cellVoltages = telemetry.cellVoltages
        val delta = cellVoltages.delta()
        
        when {
            delta > 0.20 -> alerts.add(
                BatteryAlert.CellImbalance(
                    delta = delta,
                    threshold = 0.20,
                    severity = AlertSeverity.CRITICAL,
                    imbalancedCells = cellVoltages.getImbalancedCells(0.15)
                )
            )
            delta > 0.10 -> alerts.add(
                BatteryAlert.CellImbalance(
                    delta = delta,
                    threshold = 0.10,
                    severity = AlertSeverity.WARNING,
                    imbalancedCells = cellVoltages.getImbalancedCells(0.10)
                )
            )
        }
        
        // Individual cell checks
        val lowCells = cellVoltages.getLowVoltageCells(2.8)
        if (lowCells.isNotEmpty()) {
            alerts.add(
                BatteryAlert.LowCellVoltage(
                    cells = lowCells,
                    threshold = 2.8,
                    severity = AlertSeverity.CRITICAL
                )
            )
        }
        
        val highCells = cellVoltages.getHighVoltageCells(4.15)
        if (highCells.isNotEmpty()) {
            alerts.add(
                BatteryAlert.HighCellVoltage(
                    cells = highCells,
                    threshold = 4.15,
                    severity = AlertSeverity.WARNING
                )
            )
        }
    }
    
    private fun evaluateSoc(telemetry: BatteryTelemetry, alerts: MutableList<BatteryAlert>) {
        val soc = telemetry.stateOfCharge.value
        
        when {
            soc < 5.0 -> alerts.add(
                BatteryAlert.CriticalLowSoc(
                    soc = soc,
                    threshold = 5.0,
                    severity = AlertSeverity.CRITICAL
                )
            )
            soc < 15.0 -> alerts.add(
                BatteryAlert.LowSoc(
                    soc = soc,
                    threshold = 15.0,
                    severity = AlertSeverity.WARNING
                )
            )
        }
    }
    
    private fun evaluateCurrent(telemetry: BatteryTelemetry, alerts: MutableList<BatteryAlert>) {
        val current = kotlin.math.abs(telemetry.current.value)
        
        if (current > 200.0) {
            alerts.add(
                BatteryAlert.HighCurrent(
                    current = current,
                    threshold = 200.0,
                    severity = AlertSeverity.WARNING
                )
            )
        }
    }
}

/**
 * Battery Alert
 */
sealed class BatteryAlert {
    abstract val severity: AlertSeverity
    abstract val message: String
    
    data class CriticalVoltageLow(
        val voltage: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Critical: Pack voltage ${voltage}V below ${threshold}V"
    }
    
    data class VoltageLow(
        val voltage: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Warning: Pack voltage ${voltage}V below ${threshold}V"
    }
    
    data class VoltageHigh(
        val voltage: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Warning: Pack voltage ${voltage}V above ${threshold}V"
    }
    
    data class HighTemperature(
        val temperature: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "High temperature: ${temperature}°C (max: ${threshold}°C)"
    }
    
    data class LowTemperature(
        val temperature: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Low temperature: ${temperature}°C (min: ${threshold}°C)"
    }
    
    data class CellImbalance(
        val delta: Double,
        val threshold: Double,
        override val severity: AlertSeverity,
        val imbalancedCells: List<Pair<Int, Double>>
    ) : BatteryAlert() {
        override val message = "Cell imbalance: ${delta}V delta (max: ${threshold}V)"
    }
    
    data class LowCellVoltage(
        val cells: List<Pair<Int, Double>>,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Low cell voltage: ${cells.size} cells below ${threshold}V"
    }
    
    data class HighCellVoltage(
        val cells: List<Pair<Int, Double>>,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "High cell voltage: ${cells.size} cells above ${threshold}V"
    }
    
    data class CriticalLowSoc(
        val soc: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Critical: Battery ${soc}% (below ${threshold}%)"
    }
    
    data class LowSoc(
        val soc: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "Low battery: ${soc}% remaining"
    }
    
    data class HighCurrent(
        val current: Double,
        val threshold: Double,
        override val severity: AlertSeverity
    ) : BatteryAlert() {
        override val message = "High current: ${current}A (max: ${threshold}A)"
    }
}

/**
 * Alert Severity
 */
enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}
