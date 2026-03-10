package com.fleet.bms.sniffer.domain.model

/**
 * Raw CAN Frame for sniffing (SocketCAN format).
 * Uses Int id and Long timestamp for low-level logging.
 */
data class SnifferCanFrame(
    val id: Int,
    val data: ByteArray,
    val timestamp: Long,
    val isExtended: Boolean = false
) {
    init {
        require(data.size in 0..8) {
            "CAN frame must have 0-8 data bytes, got ${data.size}"
        }
    }

    fun toHexString(): String =
        data.joinToString("") { byte ->
            byte.toUByte().toString(16).uppercase().padStart(2, '0')
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SnifferCanFrame
        if (id != other.id) return false
        if (!data.contentEquals(other.data)) return false
        if (timestamp != other.timestamp) return false
        if (isExtended != other.isExtended) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + data.contentHashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + isExtended.hashCode()
        return result
    }
}
