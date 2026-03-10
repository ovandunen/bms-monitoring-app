package com.fleet.bms.domain.repository

import com.fleet.bms.domain.model.BatteryTelemetry

/**
 * Port: Telemetry Storage Interface
 * 
 * Defines the contract for local persistence of telemetry.
 * Used for offline buffering when cloud connection is unavailable.
 */
interface TelemetryStoragePort {
    
    /**
     * Store telemetry locally
     */
    suspend fun store(telemetry: BatteryTelemetry): Result<Unit>
    
    /**
     * Get all buffered telemetry (not yet synced)
     */
    suspend fun getBuffered(): List<BatteryTelemetry>
    
    /**
     * Remove a buffered telemetry after successful sync
     */
    suspend fun removeBuffered(telemetry: BatteryTelemetry): Result<Unit>
    
    /**
     * Clear all buffered data
     */
    suspend fun clearAll()
    
    /**
     * Get count of buffered records
     */
    suspend fun getBufferSize(): Int
    
    /**
     * Get oldest buffered timestamp
     */
    suspend fun getOldestBufferedTimestamp(): Long?
}
