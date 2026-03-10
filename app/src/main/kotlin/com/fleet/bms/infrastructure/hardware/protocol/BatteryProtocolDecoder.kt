package com.fleet.bms.infrastructure.hardware.protocol

import com.fleet.bms.domain.model.*
import com.fleet.bms.domain.service.PackStatus
import com.fleet.bms.domain.service.ParsedCanData
import timber.log.Timber

/**
 * Adapter: Battery Protocol Decoder
 * 
 * Decodes ENNOID BMS CAN-Bus protocol.
 * 
 * Message IDs:
 * - 0x100: Pack status (SOC, voltage, current, power)
 * - 0x101: Pack current (high resolution)
 * - 0x102: Temperatures
 * - 0x103: Cell voltage statistics
 * - 0x104: Warnings/errors
 * - 0x110-0x11E: Cell voltages (8 cells per message, 15 messages for 114 cells)
 * - 0x180: GPS latitude
 * - 0x181: GPS longitude
 */
class BatteryProtocolDecoder : CanProtocolParser {
    
    override fun parse(frame: CanFrame): ParsedCanData? {
        return try {
            when (frame.id.value) {
                0x100 -> parsePackStatus(frame.data)
                0x101 -> parsePackCurrent(frame.data)
                0x102 -> parseTemperatures(frame.data)
                0x103 -> parseCellStats(frame.data)
                0x104 -> parseWarnings(frame.data)
                in 0x110..0x11E -> parseCellVoltages(frame)
                0x180 -> parseGpsLatitude(frame.data)
                0x181 -> parseGpsLongitude(frame.data)
                else -> {
                    // Unknown message ID
                    null
                }
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode CAN frame: ${frame.id.value}")
            null
        }
    }
    
    /**
     * 0x100: Pack Status
     * Byte 0-1: SOC (uint16, scale 0.01, 0-100%)
     * Byte 2-3: Voltage (uint16, scale 0.1V)
     * Byte 4-5: Current (int16, scale 0.1A, negative = discharge)
     * Byte 6-7: Power (int16, scale 0.1W)
     */
    private fun parsePackStatus(data: ByteArray): ParsedCanData.PackStatus {
        val soc = decodeUInt16(data, 0) * 0.01
        val voltage = decodeUInt16(data, 2) * 0.1
        val current = decodeInt16(data, 4) * 0.1
        val power = decodeInt16(data, 6) * 0.1
        
        return ParsedCanData.PackStatus(
            PackStatus(
                soc = StateOfCharge(soc),
                voltage = Voltage(voltage),
                current = Current(current),
                power = Power(power)
            )
        )
    }
    
    /**
     * 0x101: Pack Current (high resolution)
     * Byte 0-3: Current (int32, scale 0.001A)
     */
    private fun parsePackCurrent(data: ByteArray): ParsedCanData.PackStatus? {
        // Optional: Override current from 0x100 with higher resolution
        // For now, we rely on 0x100
        return null
    }
    
    /**
     * 0x102: Temperatures
     * Byte 0-1: Min temp (int16, scale 0.1°C)
     * Byte 2-3: Max temp (int16, scale 0.1°C)
     * Byte 4-5: Avg temp (int16, scale 0.1°C)
     */
    private fun parseTemperatures(data: ByteArray): ParsedCanData.Temperatures {
        val min = decodeInt16(data, 0) * 0.1
        val max = decodeInt16(data, 2) * 0.1
        val avg = decodeInt16(data, 4) * 0.1
        
        return ParsedCanData.Temperatures(
            TemperatureReading(min = min, max = max, avg = avg)
        )
    }
    
    /**
     * 0x103: Cell Voltage Statistics
     * Byte 0-1: Min cell voltage (uint16, scale 0.001V, offset 2.5V)
     * Byte 2-3: Max cell voltage (uint16, scale 0.001V, offset 2.5V)
     * Byte 4-5: Delta (uint16, scale 0.001V)
     * Byte 6-7: Avg cell voltage (uint16, scale 0.001V, offset 2.5V)
     */
    private fun parseCellStats(data: ByteArray): ParsedCanData.CellStats {
        val min = 2.5 + (decodeUInt16(data, 0) * 0.001)
        val max = 2.5 + (decodeUInt16(data, 2) * 0.001)
        val delta = decodeUInt16(data, 4) * 0.001
        val avg = 2.5 + (decodeUInt16(data, 6) * 0.001)
        
        return ParsedCanData.CellStats(
            com.fleet.bms.domain.service.CellVoltageStats(
                min = min,
                max = max,
                delta = delta,
                average = avg
            )
        )
    }
    
    /**
     * 0x104: Warnings/Errors
     * Byte 0: Warning flags (bitfield)
     */
    private fun parseWarnings(data: ByteArray): ParsedCanData.Warnings {
        val flags = data[0].toInt()
        
        return ParsedCanData.Warnings(
            BatteryWarnings(
                hasLowVoltage = (flags and 0x01) != 0,
                hasHighVoltage = (flags and 0x02) != 0,
                hasLowTemperature = (flags and 0x04) != 0,
                hasHighTemperature = (flags and 0x08) != 0,
                hasCellImbalance = (flags and 0x10) != 0,
                hasLowSoc = (flags and 0x20) != 0
            )
        )
    }
    
    /**
     * 0x110-0x11E: Cell Voltages
     * 15 messages, 8 cells each = 120 cells (we use 114)
     * Each byte: uint8, scale 0.02V, offset 2.5V
     * Cell voltage = 2.5 + (byte * 0.02)
     */
    private fun parseCellVoltages(frame: CanFrame): ParsedCanData.CellVoltages {
        val messageIndex = frame.id.value - 0x110
        val startCell = messageIndex * 8
        
        val voltages = frame.data.map { byte ->
            2.5 + (byte.toUByte().toInt() * 0.02)
        }
        
        return ParsedCanData.CellVoltages(
            voltages = voltages,
            startIndex = startCell
        )
    }
    
    /**
     * 0x180: GPS Latitude
     * Byte 0-3: Latitude (int32, scale 1e-7 degrees)
     */
    private fun parseGpsLatitude(data: ByteArray): ParsedCanData? {
        // Store temporarily, combine with longitude in 0x181
        return null
    }
    
    /**
     * 0x181: GPS Longitude
     * Byte 0-3: Longitude (int32, scale 1e-7 degrees)
     */
    private fun parseGpsLongitude(data: ByteArray): ParsedCanData? {
        // Would need to combine with latitude
        // For now, skip GPS from CAN-Bus (use Android location instead)
        return null
    }
    
    // Helper functions for decoding
    
    private fun decodeUInt16(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or 
               (data[offset + 1].toInt() and 0xFF)
    }
    
    private fun decodeInt16(data: ByteArray, offset: Int): Int {
        val unsigned = decodeUInt16(data, offset)
        return if (unsigned >= 32768) unsigned - 65536 else unsigned
    }
    
    private fun decodeUInt32(data: ByteArray, offset: Int): Long {
        return ((data[offset].toLong() and 0xFF) shl 24) or
               ((data[offset + 1].toLong() and 0xFF) shl 16) or
               ((data[offset + 2].toLong() and 0xFF) shl 8) or
               (data[offset + 3].toLong() and 0xFF)
    }
    
    private fun decodeInt32(data: ByteArray, offset: Int): Long {
        val unsigned = decodeUInt32(data, offset)
        return if (unsigned >= 2147483648L) unsigned - 4294967296L else unsigned
    }
}
