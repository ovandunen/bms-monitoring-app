package com.fleet.bms.domain.model

import java.util.UUID

/**
 * Value Object: Battery Pack ID
 * Strongly typed identifier to prevent mixing IDs
 */
@JvmInline
value class BatteryPackId(val value: String) {
    init {
        require(value.isNotBlank()) { "Battery Pack ID cannot be blank" }
    }
    
    companion object {
        fun generate() = BatteryPackId(UUID.randomUUID().toString())
        fun from(uuid: UUID) = BatteryPackId(uuid.toString())
    }
}

/**
 * Value Object: Vehicle ID
 */
@JvmInline
value class VehicleId(val value: String) {
    init {
        require(value.isNotBlank()) { "Vehicle ID cannot be blank" }
    }
}

/**
 * Value Object: Message ID (for idempotency)
 */
@JvmInline
value class MessageId(val value: String) {
    companion object {
        fun generate() = MessageId(UUID.randomUUID().toString())
    }
}

/**
 * Value Object: CAN ID (11-bit or 29-bit)
 */
@JvmInline
value class CanId(val value: Int) {
    init {
        require(value in 0..0x1FFFFFFF) { "CAN ID out of range: $value" }
    }
    
    fun isExtended() = value > 0x7FF
    fun isStandard() = !isExtended()
}
