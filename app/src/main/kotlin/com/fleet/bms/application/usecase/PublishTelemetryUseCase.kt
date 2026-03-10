package com.fleet.bms.application.usecase

import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import com.fleet.bms.domain.repository.TelemetryStoragePort
import timber.log.Timber

/**
 * Use Case: Publish Telemetry
 * 
 * Publishes telemetry to cloud if connected,
 * otherwise buffers locally for later sync.
 */
class PublishTelemetryUseCase(
    private val publisherPort: TelemetryPublisherPort,
    private val storagePort: TelemetryStoragePort
) {
    
    suspend fun execute(telemetry: BatteryTelemetry): Result<PublishResult> {
        return if (publisherPort.isConnected()) {
            // Publish immediately
            publisherPort.publish(telemetry)
                .fold(
                    onSuccess = { 
                        Timber.d("Published telemetry: ${telemetry.messageId.value}")
                        Result.success(PublishResult.Published)
                    },
                    onFailure = { error ->
                        // Failed to publish, buffer it
                        Timber.w(error, "Failed to publish, buffering telemetry")
                        bufferTelemetry(telemetry)
                    }
                )
        } else {
            // Not connected, buffer immediately
            Timber.d("Not connected, buffering telemetry")
            bufferTelemetry(telemetry)
        }
    }
    
    private suspend fun bufferTelemetry(telemetry: BatteryTelemetry): Result<PublishResult> {
        return storagePort.store(telemetry)
            .fold(
                onSuccess = {
                    val bufferSize = storagePort.getBufferSize()
                    Timber.d("Buffered telemetry (buffer size: $bufferSize)")
                    Result.success(PublishResult.Buffered(bufferSize))
                },
                onFailure = { error ->
                    Timber.e(error, "Failed to buffer telemetry")
                    Result.failure(error)
                }
            )
    }
}

/**
 * Publish Result
 */
sealed class PublishResult {
    object Published : PublishResult()
    data class Buffered(val bufferSize: Int) : PublishResult()
}
