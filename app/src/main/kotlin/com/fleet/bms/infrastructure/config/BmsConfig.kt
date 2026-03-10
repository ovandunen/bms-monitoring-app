package com.fleet.bms.infrastructure.config

/**
 * Root BMS configuration loaded from bms_config.yml
 */
data class BmsConfig(
    val canAdapter: String = "pcan-usb-fd",
    val bmsProtocol: String = "ennoid",
    val canBus: CanBusConfig = CanBusConfig(),
    val adapters: Map<String, AdapterConfig> = emptyMap(),
    val protocols: Map<String, ProtocolConfig> = emptyMap()
) {
    fun getAdapterConfig(adapterName: String): AdapterConfig? = adapters[adapterName]
    fun getProtocolConfig(protocolName: String): ProtocolConfig? = protocols[protocolName]
}

/**
 * CAN-Bus parameters
 */
data class CanBusConfig(
    val bitrate: Int = 500_000,
    val samplePoint: Double = 0.875,
    val listenOnly: Boolean = false
)

/**
 * Adapter-specific configuration
 */
data class AdapterConfig(
    val vendorId: Int = 0x0C72,
    val productId: Int = 0x000C,
    val usbBaudRate: Int = 115200
)

/**
 * BMS protocol-specific configuration
 */
data class ProtocolConfig(
    val cellCount: Int = 114,
    val messageIds: MessageIdConfig = MessageIdConfig()
)

/**
 * CAN message ID mapping for protocol
 */
data class MessageIdConfig(
    val packStatus: Int = 0x100,
    val packCurrent: Int = 0x101,
    val temperatures: Int = 0x102,
    val cellStats: Int = 0x103,
    val warnings: Int = 0x104,
    val cellVoltagesStart: Int = 0x110,
    val cellVoltagesEnd: Int = 0x11E,
    val gpsLatitude: Int = 0x180,
    val gpsLongitude: Int = 0x181
)
