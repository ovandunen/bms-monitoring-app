package com.fleet.bms.application.usecase

import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import timber.log.Timber

/**
 * Use Case: Stop Monitoring
 * 
 * Cleanly disconnects from CAN-Bus and MQTT.
 */
class StopMonitoringUseCase(
    private val canBusPort: CanBusPort,
    private val publisherPort: TelemetryPublisherPort
) {
    
    suspend fun execute(): Result<Unit> {
        Timber.i("Stopping battery monitoring")
        
        try {
            // Disconnect from CAN-Bus
            canBusPort.disconnect()
            Timber.d("Disconnected from CAN-Bus")
            
            // Disconnect from MQTT
            publisherPort.disconnect()
            Timber.d("Disconnected from MQTT")
            
            Timber.i("Battery monitoring stopped successfully")
            return Result.success(Unit)
            
        } catch (e: Exception) {
            Timber.e(e, "Error stopping monitoring")
            return Result.failure(e)
        }
    }
}
