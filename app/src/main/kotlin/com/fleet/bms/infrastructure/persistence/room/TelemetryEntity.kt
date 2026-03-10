package com.fleet.bms.infrastructure.persistence.room

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fleet.bms.domain.model.*

/**
 * Room Entity: Telemetry
 * 
 * Represents buffered telemetry awaiting sync to cloud.
 */
@Entity(tableName = "telemetry_buffer")
data class TelemetryEntity(
    @PrimaryKey
    val messageId: String,
    val batteryPackId: String,
    val vehicleId: String,
    val timestamp: Long,  // Unix timestamp milliseconds
    val stateOfCharge: Double,
    val voltage: Double,
    val current: Double,
    val power: Double,
    val temperatureMin: Double,
    val temperatureMax: Double,
    val temperatureAvg: Double,
    val cellVoltageMin: Double,
    val cellVoltageMax: Double,
    val cellVoltageDelta: Double,
    val cellVoltagesJson: String,  // JSON array of 114 voltages
    val latitude: Double?,
    val longitude: Double?,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromDomain(telemetry: BatteryTelemetry): TelemetryEntity {
            return TelemetryEntity(
                messageId = telemetry.messageId.value,
                batteryPackId = telemetry.batteryPackId.value,
                vehicleId = telemetry.vehicleId.value,
                timestamp = telemetry.timestamp.toEpochMilli(),
                stateOfCharge = telemetry.stateOfCharge.value,
                voltage = telemetry.voltage.value,
                current = telemetry.current.value,
                power = telemetry.power.value,
                temperatureMin = telemetry.temperatures.min,
                temperatureMax = telemetry.temperatures.max,
                temperatureAvg = telemetry.temperatures.avg,
                cellVoltageMin = telemetry.cellVoltages.min(),
                cellVoltageMax = telemetry.cellVoltages.max(),
                cellVoltageDelta = telemetry.cellVoltages.delta(),
                cellVoltagesJson = telemetry.cellVoltages.voltages.joinToString(","),
                latitude = telemetry.location?.latitude,
                longitude = telemetry.location?.longitude
            )
        }
    }
    
    fun toDomain(): BatteryTelemetry {
        val cellVoltages = cellVoltagesJson.split(",").map { it.toDouble() }
        
        return BatteryTelemetry(
            messageId = MessageId(messageId),
            batteryPackId = BatteryPackId(batteryPackId),
            vehicleId = VehicleId(vehicleId),
            timestamp = java.time.Instant.ofEpochMilli(timestamp),
            stateOfCharge = StateOfCharge(stateOfCharge),
            voltage = Voltage(voltage),
            current = Current(current),
            power = Power(power),
            temperatures = TemperatureReading(
                min = temperatureMin,
                max = temperatureMax,
                avg = temperatureAvg
            ),
            cellVoltages = CellVoltages(cellVoltages),
            location = if (latitude != null && longitude != null) {
                GpsLocation(latitude, longitude)
            } else null
        )
    }
}
