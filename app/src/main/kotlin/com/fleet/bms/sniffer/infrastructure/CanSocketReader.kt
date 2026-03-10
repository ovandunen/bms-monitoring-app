package com.fleet.bms.sniffer.infrastructure

import com.fleet.bms.sniffer.domain.model.SnifferCanFrame
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import timber.log.Timber

/**
 * Reads CAN frames from SocketCAN interface (can0).
 * Uses raw /dev/can0 or candump for LineageOS compatibility.
 * 
 * Strategy:
 * 1. Try candump (can-utils) - works on most LineageOS
 * 2. Try reading from /sys/class/net/can0 for interface check
 */
class CanSocketReader(
    private val interfaceName: String = "can0",
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 2000
) {

    companion object {
        private const val CAN_EFF_FLAG = 0x80000000
    }

    private var process: Process? = null

    /**
     * Read CAN frames as Flow.
     * Reconnects on failure with retry logic.
     */
    fun readFrames(): Flow<SnifferCanFrame> = callbackFlow {
        var retries = 0

        while (retries <= maxRetries) {
            try {
                val frameFlow = tryCandump()
                frameFlow.collect { frame ->
                    trySend(frame)
                }
            } catch (e: Exception) {
                retries++
                Timber.e(e, "CAN read failed (attempt $retries/$maxRetries)")
                if (retries <= maxRetries) {
                    delay(retryDelayMs)
                } else {
                    close(e)
                }
            }
        }

        awaitClose {
            process?.destroy()
            process = null
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Use candump (can-utils) - standard on LineageOS with SocketCAN.
     * Format: "can0 123 [4] 11 22 33 44" or "can0 123 11 22 33 44"
     */
    private fun tryCandump(): Flow<SnifferCanFrame> = callbackFlow {
        Timber.i("Starting candump on $interfaceName")
        process = ProcessBuilder("candump", "-n", "-e", interfaceName)
            .redirectErrorStream(true)
            .start()

        process!!.inputStream.bufferedReader().useLines { lines ->
            lines.forEach { line ->
                parseCandumpLine(line)?.let { trySend(it) }
            }
        }
        awaitClose {
            process?.destroy()
            process = null
        }
    }

    /**
     * Parse candump output: "can0 123 [4] 11 22 33 44"
     */
    private fun parseCandumpLine(line: String): SnifferCanFrame? {
        return try {
            val parts = line.trim().split("\\s+".toRegex())
            if (parts.size < 2) return null
            val idStr = parts[1]
            val id = idStr.toIntOrNull(16) ?: return null
            val dataBytes = when {
                parts.getOrNull(2)?.startsWith("[") == true -> {
                    val len = parts[2].removeSurrounding("[", "]").toIntOrNull() ?: 0
                    parts.drop(3).take(len).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
                }
                else -> parts.drop(2).mapNotNull { it.toIntOrNull(16)?.toByte() }.toByteArray()
            }
            SnifferCanFrame(
                id = id and 0x1FFFFFFF,
                data = dataBytes,
                timestamp = System.currentTimeMillis(),
                isExtended = id and CAN_EFF_FLAG != 0
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse candump line: $line")
            null
        }
    }

    fun close() {
        process?.destroy()
        process = null
    }
}
