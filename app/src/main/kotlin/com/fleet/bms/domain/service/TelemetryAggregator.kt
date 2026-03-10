package com.fleet.bms.domain.service

import com.fleet.bms.domain.model.*
import java.time.Instant

/**
 * Domain Service: Telemetry Aggregator
 * 
 * Aggregates individual CAN frames into a complete BatteryTelemetry.
 * Handles 114 cells split across multiple CAN messages.
 */
class TelemetryAggregator(
    private val batteryPackId: BatteryPackId,
    private val vehicleId: VehicleId
) {
    private val cellVoltages = MutableList(114) { 0.0 }
    private var packStatus: PackStatus? = null
    private var temperatures: TemperatureReading? = null
    private var cellStats: CellVoltageStats? = null
    private var warnings: BatteryWarnings? = null
    private var location: GpsLocation? = null
    private var lastUpdateTime: Instant = Instant.now()
    
    fun aggregate(parsedData: ParsedCanData): AggregationResult {
        lastUpdateTime = Instant.now()
        
        when (parsedData) {
            is ParsedCanData.PackStatus -> packStatus = parsedData.status
            is ParsedCanData.Temperatures -> temperatures = parsedData.reading
            is ParsedCanData.CellVoltages -> updateCellVoltages(parsedData)
            is ParsedCanData.CellStats -> cellStats = parsedData.stats
            is ParsedCanData.Warnings -> warnings = parsedData.warnings
            is ParsedCanData.Location -> location = parsedData.location
        }
        
        return if (isComplete()) {
            AggregationResult.Complete(buildTelemetry())
        } else {
            AggregationResult.Incomplete(completionPercentage())
        }
    }
    
    private fun updateCellVoltages(data: ParsedCanData.CellVoltages) {
        data.voltages.forEachIndexed { index, voltage ->
            val cellIndex = data.startIndex + index
            if (cellIndex < cellVoltages.size) {
                cellVoltages[cellIndex] = voltage
            }
        }
    }
    
    private fun isComplete(): Boolean {
        return packStatus != null && 
               temperatures != null && 
               cellVoltages.all { it > 0.0 }
    }
    
    private fun completionPercentage(): Float {
        var completed = 0
        var total = 0
        
        // Pack status
        total++
        if (packStatus != null) completed++
        
        // Temperatures
        total++
        if (temperatures != null) completed++
        
        // Cell voltages (all 114)
        total += 114
        completed += cellVoltages.count { it > 0.0 }
        
        return (completed.toFloat() / total.toFloat()) * 100f
    }
    
    private fun buildTelemetry(): BatteryTelemetry {
        val status = packStatus!!
        
        return BatteryTelemetry(
            messageId = MessageId.generate(),
            batteryPackId = batteryPackId,
            vehicleId = vehicleId,
            timestamp = Instant.now(),
            stateOfCharge = status.soc,
            voltage = status.voltage,
            current = status.current,
            power = status.power,
            temperatures = temperatures!!,
            cellVoltages = CellVoltages(cellVoltages.toList()),
            warnings = warnings,
            location = location
        )
    }
    
    fun reset() {
        cellVoltages.fill(0.0)
        packStatus = null
        temperatures = null
        cellStats = null
        warnings = null
        // Keep location (doesn't reset every cycle)
    }
    
    fun getLastUpdateTime(): Instant = lastUpdateTime
}

/**
 * Aggregation Result
 */
sealed class AggregationResult {
    data class Complete(val telemetry: BatteryTelemetry) : AggregationResult()
    data class Incomplete(val percentage: Float) : AggregationResult()
}

/**
 * Parsed CAN Data (from protocol decoder)
 */
sealed class ParsedCanData {
    data class PackStatus(val status: com.fleet.bms.domain.service.PackStatus) : ParsedCanData()
    data class Temperatures(val reading: TemperatureReading) : ParsedCanData()
    data class CellVoltages(val voltages: List<Double>, val startIndex: Int) : ParsedCanData()
    data class CellStats(val stats: CellVoltageStats) : ParsedCanData()
    data class Warnings(val warnings: BatteryWarnings) : ParsedCanData()
    data class Location(val location: GpsLocation) : ParsedCanData()
}

/**
 * Pack Status (from CAN message 0x100)
 */
data class PackStatus(
    val soc: StateOfCharge,
    val voltage: Voltage,
    val current: Current,
    val power: Power
)

/**
 * Cell Voltage Statistics (from CAN message 0x103)
 */
data class CellVoltageStats(
    val min: Double,
    val max: Double,
    val delta: Double,
    val average: Double
)
