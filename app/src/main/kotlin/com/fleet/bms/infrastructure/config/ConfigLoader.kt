package com.fleet.bms.infrastructure.config

import android.content.Context
import org.yaml.snakeyaml.Yaml
import timber.log.Timber
import java.io.InputStreamReader

/**
 * Loads BMS configuration from assets/bms_config.yml
 */
object ConfigLoader {

    private const val CONFIG_FILE = "bms_config.yml"

    fun load(context: Context): BmsConfig {
        return try {
            context.assets.open(CONFIG_FILE).use { inputStream ->
                val content = InputStreamReader(inputStream).readText()
                parseYaml(content)
            }
        } catch (e: Exception) {
            Timber.w(e, "Failed to load config from $CONFIG_FILE, using defaults")
            BmsConfig()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseYaml(content: String): BmsConfig {
        val yaml = Yaml()
        val map = yaml.load<Map<String, Any>>(content) ?: return BmsConfig()

        val bms = map["bms"] as? Map<String, Any> ?: return BmsConfig()

        val canAdapter = (bms["can_adapter"] as? String) ?: "pcan-usb-fd"
        val bmsProtocol = (bms["bms_protocol"] as? String) ?: "ennoid"

        val canBusMap = bms["can_bus"] as? Map<String, Any>
        val canBus = CanBusConfig(
            bitrate = (canBusMap?.get("bitrate") as? Number)?.toInt() ?: 500_000,
            samplePoint = (canBusMap?.get("sample_point") as? Number)?.toDouble() ?: 0.875,
            listenOnly = (canBusMap?.get("listen_only") as? Boolean) ?: false
        )

        val adaptersMap = bms["adapters"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val adapters = adaptersMap.mapValues { (_, config) ->
            AdapterConfig(
                vendorId = parseHex(config["vendor_id"]) ?: 0x0C72,
                productId = parseHex(config["product_id"]) ?: 0x000C,
                usbBaudRate = (config["usb_baud_rate"] as? Number)?.toInt() ?: 115200
            )
        }.ifEmpty {
            mapOf(
                "pcan-usb-fd" to AdapterConfig(0x0C72, 0x000C, 115200),
                "canable" to AdapterConfig(0x1D50, 0x606F, 921600)
            )
        }

        val protocolsMap = bms["protocols"] as? Map<String, Map<String, Any>> ?: emptyMap()
        val protocols = protocolsMap.mapValues { (_, config) ->
            val msgIds = config["message_ids"] as? Map<String, Any>
            ProtocolConfig(
                cellCount = (config["cell_count"] as? Number)?.toInt() ?: 114,
                messageIds = MessageIdConfig(
                    packStatus = parseHex(msgIds?.get("pack_status")) ?: 0x100,
                    packCurrent = parseHex(msgIds?.get("pack_current")) ?: 0x101,
                    temperatures = parseHex(msgIds?.get("temperatures")) ?: 0x102,
                    cellStats = parseHex(msgIds?.get("cell_stats")) ?: 0x103,
                    warnings = parseHex(msgIds?.get("warnings")) ?: 0x104,
                    cellVoltagesStart = parseHex(msgIds?.get("cell_voltages_start")) ?: 0x110,
                    cellVoltagesEnd = parseHex(msgIds?.get("cell_voltages_end")) ?: 0x11E,
                    gpsLatitude = parseHex(msgIds?.get("gps_latitude")) ?: 0x180,
                    gpsLongitude = parseHex(msgIds?.get("gps_longitude")) ?: 0x181
                )
            )
        }.ifEmpty {
            mapOf("ennoid" to ProtocolConfig())
        }

        return BmsConfig(
            canAdapter = canAdapter,
            bmsProtocol = bmsProtocol,
            canBus = canBus,
            adapters = adapters,
            protocols = protocols
        )
    }

    private fun parseHex(value: Any?): Int? {
        return when (value) {
            is Number -> value.toInt()
            is String -> value.removePrefix("0x").toIntOrNull(16)
            else -> null
        }
    }
}
