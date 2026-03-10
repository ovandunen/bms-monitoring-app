package com.fleet.bms.sniffer.domain.model

/**
 * Registry entry for a single CAN-ID observed during sniffing.
 */
data class CanIdEntry(
    val id: Int,
    val firstSeen: Long,
    var lastSeen: Long,
    var frameCount: Long,
    var lastValue: ByteArray,
    var valueChangeCount: Int,
    val minPerByte: ByteArray,
    val maxPerByte: ByteArray,
    val activeDuringSessions: MutableSet<String> = mutableSetOf()
) {
    val idHex: String get() = "0x${id.toString(16).uppercase().padStart(3, '0')}"

    fun lastValueHex(): String =
        lastValue.joinToString("") { b -> b.toUByte().toString(16).uppercase().padStart(2, '0') }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanIdEntry
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id
}
