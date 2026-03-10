package com.fleet.bms.application.usecase

import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import timber.log.Timber

/**
 * Use Case: Start Monitoring
 *
 * Initializes CAN-Bus and MQTT connections
 * to begin battery monitoring.
 * CAN-Bus parameters come from bms_config.yml.
 */
class StartMonitoringUseCase(
    private val canBusPort: CanBusPort,
    private val publisherPort: TelemetryPublisherPort,
    private val canBusConfig: CanBusConfig
) {

    suspend fun execute(): Result<Unit> {
        Timber.i("Starting battery monitoring")

        // Connect to CAN-Bus
        canBusPort.connect()
            .onFailure { error ->
                Timber.e(error, "Failed to connect to CAN-Bus")
                return Result.failure(error)
            }

        // Configure CAN-Bus (from bms_config.yml)
        canBusPort.configure(canBusConfig)
            .onFailure { error ->
                Timber.e(error, "Failed to configure CAN-Bus")
                canBusPort.disconnect()
                return Result.failure(error)
            }
        
        // Connect to MQTT broker (non-blocking)
        publisherPort.connect()
            .onFailure { error ->
                Timber.w(error, "Failed to connect to MQTT (will buffer data)")
                // Continue anyway - we can buffer data locally
            }
        
        Timber.i("Battery monitoring started successfully")
        return Result.success(Unit)
    }
}
