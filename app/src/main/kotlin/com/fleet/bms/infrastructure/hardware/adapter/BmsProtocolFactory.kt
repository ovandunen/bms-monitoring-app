package com.fleet.bms.infrastructure.hardware.adapter

import com.fleet.bms.infrastructure.config.BmsConfig
import com.fleet.bms.infrastructure.hardware.protocol.CanProtocolParser
import com.fleet.bms.infrastructure.hardware.protocol.EnnoidBmsProtocolDecoder
import timber.log.Timber

/**
 * Factory: BMS Protocol Parser
 *
 * Creates the appropriate CanProtocolParser implementation based on YAML config.
 *
 * Supported protocols:
 * - ennoid: ENNOID BMS CAN protocol
 */
object BmsProtocolFactory {

    fun create(config: BmsConfig): CanProtocolParser {
        val protocolName = config.bmsProtocol
        val protocolConfig = config.getProtocolConfig(protocolName)

        return when (protocolName) {
            "ennoid" -> {
                Timber.i("Creating ENNOID BMS protocol decoder (config: $protocolConfig)")
                EnnoidBmsProtocolDecoder(protocolConfig ?: com.fleet.bms.infrastructure.config.ProtocolConfig())
            }
            else -> {
                Timber.w("Unknown protocol '$protocolName', defaulting to ennoid")
                EnnoidBmsProtocolDecoder(protocolConfig ?: com.fleet.bms.infrastructure.config.ProtocolConfig())
            }
        }
    }
}
