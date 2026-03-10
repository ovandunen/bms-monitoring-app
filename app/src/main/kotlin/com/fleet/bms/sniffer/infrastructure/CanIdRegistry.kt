package com.fleet.bms.sniffer.infrastructure

import com.fleet.bms.sniffer.domain.model.CanIdEntry
import com.fleet.bms.sniffer.domain.model.SnifferCanFrame
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Maintains registry of all seen CAN-IDs with statistics.
 * Thread-safe for concurrent frame processing.
 */
class CanIdRegistry(
    private val activeThresholdMs: Long = 5000
) {

    private val entries = ConcurrentHashMap<Int, CanIdEntry>()
    private val lastActivityById = ConcurrentHashMap<Int, Long>()
    private val frameCounter = AtomicLong(0)

    fun getFrameCount(): Long = frameCounter.get()

    fun getEntries(): Map<Int, CanIdEntry> = entries.toMap()

    fun getEntriesSortedByChangeFrequency(): List<CanIdEntry> =
        entries.values.sortedByDescending { it.valueChangeCount }

    fun getActiveIds(): Set<Int> {
        val now = System.currentTimeMillis()
        return lastActivityById.entries
            .filter { (_, lastSeen) -> now - lastSeen < activeThresholdMs }
            .map { it.key }
            .toSet()
    }

    fun recordFrame(frame: SnifferCanFrame, currentSessionLabel: String?) {
        frameCounter.incrementAndGet()

        val entry = entries.getOrPut(frame.id) {
            CanIdEntry(
                id = frame.id,
                firstSeen = frame.timestamp,
                lastSeen = frame.timestamp,
                frameCount = 0,
                lastValue = ByteArray(0),
                valueChangeCount = 0,
                minPerByte = ByteArray(8) { 0xFF.toByte() },
                maxPerByte = ByteArray(8) { 0 }
            )
        }

        synchronized(entry) {
            entry.lastSeen = frame.timestamp
            entry.frameCount++

            val valueChanged = !frame.data.contentEquals(entry.lastValue)
            if (valueChanged) {
                entry.valueChangeCount++
                entry.lastValue = frame.data.copyOf()

                for (i in frame.data.indices) {
                    if (frame.data[i] < entry.minPerByte[i]) entry.minPerByte[i] = frame.data[i]
                    if (frame.data[i] > entry.maxPerByte[i]) entry.maxPerByte[i] = frame.data[i]
                }

                currentSessionLabel?.let { entry.activeDuringSessions.add(it) }
                lastActivityById[frame.id] = frame.timestamp
            }
        }
    }

    fun clear() {
        entries.clear()
        lastActivityById.clear()
        frameCounter.set(0)
        Timber.d("CanIdRegistry cleared")
    }

    fun getEntry(id: Int): CanIdEntry? = entries[id]
}
