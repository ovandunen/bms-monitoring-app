package com.fleet.bms.domain.repository

import com.fleet.bms.domain.model.CanFrame
import kotlinx.coroutines.flow.Flow

/**
 * Port: CAN-Bus Interface
 * 
 * Defines the contract for CAN-Bus communication.
 * Implementations provide hardware-specific adapters (e.g., PCAN-USB).
 */
interface CanBusPort {
    
    /**
     * Connect to CAN-Bus adapter
     */
    suspend fun connect(): Result<Unit>
    
    /**
     * Configure CAN-Bus parameters
     */
    suspend fun configure(config: CanBusConfig): Result<Unit>
    
    /**
     * Read CAN frames as a stream
     */
    fun readFrames(): Flow<CanFrame>
    
    /**
     * Disconnect from CAN-Bus adapter
     */
    suspend fun disconnect()
    
    /**
     * Check if connected
     */
    fun isConnected(): Boolean
}

/**
 * CAN-Bus Configuration
 */
data class CanBusConfig(
    val bitrate: Int = 500_000,  // 500 kbps (standard for ENNOID BMS)
    val samplePoint: Float = 0.875f,  // 87.5% sample point
    val listenOnly: Boolean = false,
    val loopback: Boolean = false
) {
    companion object {
        val DEFAULT_500K = CanBusConfig(bitrate = 500_000)
        val DEFAULT_250K = CanBusConfig(bitrate = 250_000)
        val DEFAULT_1M = CanBusConfig(bitrate = 1_000_000)
    }
}
