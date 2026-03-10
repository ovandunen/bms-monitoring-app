package com.fleet.bms.domain.model

import java.time.Instant

/**
 * Domain Entity: Battery Telemetry
 * 
 * Complete telemetry snapshot aggregated from CAN-Bus frames.
 * This is the core domain model that represents battery state.
 */
data class BatteryTelemetry(
    val messageId: MessageId,
    val batteryPackId: BatteryPackId,
    val vehicleId: VehicleId,
    val timestamp: Instant,
    val stateOfCharge: StateOfCharge,
    val voltage: Voltage,
    val current: Current,
    val power: Power,
    val temperatures: TemperatureReading,
    val cellVoltages: CellVoltages,
    val warnings: BatteryWarnings? = null,
    val location: GpsLocation? = null
) {
    init {
        require(stateOfCharge.value in 0.0..100.0) {
            "SOC must be between 0-100%"
        }
    }
    
    fun hasCriticalIssues(): Boolean {
        return warnings?.hasAnyWarning() == true || 
               !voltage.isSafe() ||
               !temperatures.isSafe() ||
               !cellVoltages.isBalanced()
    }
    
    fun getHealthStatus(): HealthStatus {
        return when {
            !voltage.isSafe() -> HealthStatus.CRITICAL
            !temperatures.isSafe() -> HealthStatus.CRITICAL
            !cellVoltages.isBalanced() -> HealthStatus.WARNING
            stateOfCharge.isCritical() -> HealthStatus.CRITICAL
            stateOfCharge.isLow() -> HealthStatus.WARNING
            warnings?.hasAnyWarning() == true -> HealthStatus.WARNING
            else -> HealthStatus.HEALTHY
        }
    }
    
    fun getDisplaySummary(): String {
        return buildString {
            appendLine("Battery Pack: ${batteryPackId.value}")
            appendLine("Vehicle: ${vehicleId.value}")
            appendLine("SOC: ${stateOfCharge.value}%")
            appendLine("Voltage: ${voltage.value}V")
            appendLine("Current: ${current.value}A")
            appendLine("Power: ${power.value}W")
            appendLine("Temp: ${temperatures.min}°C - ${temperatures.max}°C")
            appendLine("Cell Delta: ${cellVoltages.delta()}V")
            appendLine("Status: ${getHealthStatus()}")
        }
    }
    
    enum class HealthStatus {
        HEALTHY,
        WARNING,
        CRITICAL
    }
    
    companion object {
        fun createEmpty(
            batteryPackId: BatteryPackId,
            vehicleId: VehicleId
        ): BatteryTelemetry {
            return BatteryTelemetry(
                messageId = MessageId.generate(),
                batteryPackId = batteryPackId,
                vehicleId = vehicleId,
                timestamp = Instant.now(),
                stateOfCharge = StateOfCharge(0.0),
                voltage = Voltage(0.0),
                current = Current(0.0),
                power = Power(0.0),
                temperatures = TemperatureReading(0.0, 0.0, 0.0),
                cellVoltages = CellVoltages(List(CellVoltages.CELL_COUNT) { 0.0 })
            )
        }
    }
}
