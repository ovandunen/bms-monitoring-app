package com.fleet.bms.domain.repository

import com.fleet.bms.domain.model.BatteryTelemetry

/**
 * Port: Telemetry Publisher Interface
 * 
 * Defines the contract for publishing telemetry to external systems.
 * Implementations provide adapters for MQTT, HTTP, etc.
 */
interface TelemetryPublisherPort {
    
    /**
     * Connect to the publishing endpoint (e.g., MQTT broker)
     */
    suspend fun connect(): Result<Unit>
    
    /**
     * Publish telemetry
     */
    suspend fun publish(telemetry: BatteryTelemetry): Result<Unit>
    
    /**
     * Disconnect from the publishing endpoint
     */
    suspend fun disconnect()
    
    /**
     * Check if connected and ready to publish
     */
    fun isConnected(): Boolean
    
    /**
     * Get connection state
     */
    fun getConnectionState(): ConnectionState
}

/**
 * Connection State
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}
