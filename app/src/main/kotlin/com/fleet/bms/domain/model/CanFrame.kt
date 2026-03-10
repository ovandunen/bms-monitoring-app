package com.fleet.bms.domain.model

import java.time.Instant

/**
 * Value Object: CAN Frame
 * 
 * Represents a raw CAN-Bus frame with ID and data.
 * Standard CAN (11-bit ID) or Extended CAN (29-bit ID)
 */
data class CanFrame(
    val id: CanId,
    val data: ByteArray,
    val timestamp: Instant,
    val isExtended: Boolean = false
) {
    init {
        require(data.size in 0..8) { 
            "CAN frame must have 0-8 data bytes, got ${data.size}" 
        }
    }
    
    fun getDataLength() = data.size
    
    fun toHexString(): String {
        return buildString {
            append("ID: 0x${id.value.toString(16).uppercase().padStart(if (isExtended) 8 else 3, '0')}")
            append(" [${data.size}] ")
            append(data.joinToString(" ") { byte -> 
                byte.toUByte().toString(16).uppercase().padStart(2, '0')
            })
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanFrame
        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (isExtended != other.isExtended) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + isExtended.hashCode()
        return result
    }
    
    companion object {
        fun create(id: Int, vararg bytes: Byte, isExtended: Boolean = false): CanFrame {
            return CanFrame(
                id = CanId(id),
                data = bytes,
                timestamp = Instant.now(),
                isExtended = isExtended
            )
        }
    }
}
