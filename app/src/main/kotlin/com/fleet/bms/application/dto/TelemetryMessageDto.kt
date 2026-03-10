package com.fleet.bms.application.dto

import com.fleet.bms.domain.model.BatteryTelemetry
import kotlinx.serialization.Serializable

/**
 * DTO: Telemetry Message
 * 
 * CRITICAL: This format MUST match the backend's TelemetryMessageDto exactly!
 * 
 * Backend expects:
 * - messageId (String)
 * - batteryPackId (String)
 * - vehicleId (String)
 * - timestamp (ISO-8601 String)
 * - stateOfCharge (Double 0-100)
 * - voltage (Double)
 * - current (Double, negative = discharging)
 * - temperatureMin, temperatureMax, temperatureAvg (Double)
 * - cellVoltageMin, cellVoltageMax, cellVoltageDelta (Double)
 * - cellVoltages (List<Double>, 114 elements)
 */
@Serializable
data class TelemetryMessageDto(
    val messageId: String,
    val batteryPackId: String,
    val vehicleId: String,
    val timestamp: String,  // ISO-8601 format: "2026-01-30T12:34:56.789Z"
    val stateOfCharge: Double,
    val voltage: Double,
    val current: Double,
    val temperatureMin: Double,
    val temperatureMax: Double,
    val temperatureAvg: Double,
    val cellVoltageMin: Double,
    val cellVoltageMax: Double,
    val cellVoltageDelta: Double,
    val cellVoltages: List<Double>,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    companion object {
        /**
         * Convert domain model to DTO
         */
        fun fromDomain(telemetry: BatteryTelemetry): TelemetryMessageDto {
            return TelemetryMessageDto(
                messageId = telemetry.messageId.value,
                batteryPackId = telemetry.batteryPackId.value,
                vehicleId = telemetry.vehicleId.value,
                timestamp = telemetry.timestamp.toString(),
                stateOfCharge = telemetry.stateOfCharge.value,
                voltage = telemetry.voltage.value,
                current = telemetry.current.value,
                temperatureMin = telemetry.temperatures.min,
                temperatureMax = telemetry.temperatures.max,
                temperatureAvg = telemetry.temperatures.avg,
                cellVoltageMin = telemetry.cellVoltages.min(),
                cellVoltageMax = telemetry.cellVoltages.max(),
                cellVoltageDelta = telemetry.cellVoltages.delta(),
                cellVoltages = telemetry.cellVoltages.voltages,
                latitude = telemetry.location?.latitude,
                longitude = telemetry.location?.longitude
            )
        }
        
        /**
         * Convert DTO to domain model
         */
        fun toDomain(dto: TelemetryMessageDto): BatteryTelemetry {
            return BatteryTelemetry(
                messageId = com.fleet.bms.domain.model.MessageId(dto.messageId),
                batteryPackId = com.fleet.bms.domain.model.BatteryPackId(dto.batteryPackId),
                vehicleId = com.fleet.bms.domain.model.VehicleId(dto.vehicleId),
                timestamp = java.time.Instant.parse(dto.timestamp),
                stateOfCharge = com.fleet.bms.domain.model.StateOfCharge(dto.stateOfCharge),
                voltage = com.fleet.bms.domain.model.Voltage(dto.voltage),
                current = com.fleet.bms.domain.model.Current(dto.current),
                power = com.fleet.bms.domain.model.Power(dto.voltage * dto.current),
                temperatures = com.fleet.bms.domain.model.TemperatureReading(
                    min = dto.temperatureMin,
                    max = dto.temperatureMax,
                    avg = dto.temperatureAvg
                ),
                cellVoltages = com.fleet.bms.domain.model.CellVoltages(dto.cellVoltages),
                location = if (dto.latitude != null && dto.longitude != null) {
                    com.fleet.bms.domain.model.GpsLocation(
                        latitude = dto.latitude,
                        longitude = dto.longitude
                    )
                } else null
            )
        }
    }
}
