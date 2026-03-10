package com.fleet.bms.infrastructure.hardware.protocol

import com.fleet.bms.domain.model.*
import com.fleet.bms.domain.service.PackStatus
import com.fleet.bms.domain.service.ParsedCanData
import com.fleet.bms.infrastructure.config.ProtocolConfig
import timber.log.Timber

/**
 * Adapter: ENNOID BMS Protocol Decoder
 *
 * Decodes ENNOID BMS CAN-Bus protocol.
 * Message IDs are configurable via ProtocolConfig (from bms_config.yml).
 */
class EnnoidBmsProtocolDecoder(
    private val config: ProtocolConfig = ProtocolConfig()
) : CanProtocolParser {

    private val ids = config.messageIds

    override fun parse(frame: CanFrame): ParsedCanData? {
        return try {
            when (frame.id.value) {
                ids.packStatus -> parsePackStatus(frame.data)
                ids.packCurrent -> parsePackCurrent(frame.data)
                ids.temperatures -> parseTemperatures(frame.data)
                ids.cellStats -> parseCellStats(frame.data)
                ids.warnings -> parseWarnings(frame.data)
                in ids.cellVoltagesStart..ids.cellVoltagesEnd -> parseCellVoltages(frame)
                ids.gpsLatitude -> parseGpsLatitude(frame.data)
                ids.gpsLongitude -> parseGpsLongitude(frame.data)
                else -> null
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to decode CAN frame: ${frame.id.value}")
            null
        }
    }

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

    private fun parsePackCurrent(data: ByteArray): ParsedCanData.PackStatus? = null

    private fun parseTemperatures(data: ByteArray): ParsedCanData.Temperatures {
        val min = decodeInt16(data, 0) * 0.1
        val max = decodeInt16(data, 2) * 0.1
        val avg = decodeInt16(data, 4) * 0.1
        return ParsedCanData.Temperatures(TemperatureReading(min = min, max = max, avg = avg))
    }

    private fun parseCellStats(data: ByteArray): ParsedCanData.CellStats {
        val min = 2.5 + (decodeUInt16(data, 0) * 0.001)
        val max = 2.5 + (decodeUInt16(data, 2) * 0.001)
        val delta = decodeUInt16(data, 4) * 0.001
        val avg = 2.5 + (decodeUInt16(data, 6) * 0.001)
        return ParsedCanData.CellStats(
            com.fleet.bms.domain.service.CellVoltageStats(min = min, max = max, delta = delta, average = avg)
        )
    }

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

    private fun parseCellVoltages(frame: CanFrame): ParsedCanData.CellVoltages {
        val messageIndex = frame.id.value - ids.cellVoltagesStart
        val startCell = messageIndex * 8
        val voltages = frame.data.map { byte -> 2.5 + (byte.toUByte().toInt() * 0.02) }
        return ParsedCanData.CellVoltages(voltages = voltages, startIndex = startCell)
    }

    private fun parseGpsLatitude(data: ByteArray): ParsedCanData? = null
    private fun parseGpsLongitude(data: ByteArray): ParsedCanData? = null

    private fun decodeUInt16(data: ByteArray, offset: Int): Int =
        ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)

    private fun decodeInt16(data: ByteArray, offset: Int): Int {
        val unsigned = decodeUInt16(data, offset)
        return if (unsigned >= 32768) unsigned - 65536 else unsigned
    }
}
