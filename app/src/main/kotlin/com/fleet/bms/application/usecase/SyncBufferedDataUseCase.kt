package com.fleet.bms.application.usecase

import com.fleet.bms.domain.repository.TelemetryPublisherPort
import com.fleet.bms.domain.repository.TelemetryStoragePort
import timber.log.Timber

/**
 * Use Case: Sync Buffered Data
 * 
 * Syncs locally buffered telemetry to cloud
 * when connection is restored.
 */
class SyncBufferedDataUseCase(
    private val publisherPort: TelemetryPublisherPort,
    private val storagePort: TelemetryStoragePort
) {
    
    suspend fun execute(): Result<SyncResult> {
        if (!publisherPort.isConnected()) {
            Timber.w("Cannot sync: not connected to cloud")
            return Result.failure(Exception("Publisher not connected"))
        }
        
        val buffered = storagePort.getBuffered()
        if (buffered.isEmpty()) {
            Timber.d("No buffered data to sync")
            return Result.success(SyncResult(synced = 0, failed = 0, total = 0))
        }
        
        Timber.i("Starting sync of ${buffered.size} buffered telemetry records")
        
        var syncedCount = 0
        var failedCount = 0
        
        buffered.forEach { telemetry ->
            publisherPort.publish(telemetry)
                .onSuccess {
                    storagePort.removeBuffered(telemetry)
                        .onSuccess {
                            syncedCount++
                            Timber.d("Synced buffered telemetry: ${telemetry.messageId.value}")
                        }
                        .onFailure { error ->
                            failedCount++
                            Timber.w(error, "Failed to remove synced telemetry from buffer")
                        }
                }
                .onFailure { error ->
                    failedCount++
                    Timber.w(error, "Failed to sync telemetry: ${telemetry.messageId.value}")
                    
                    // Stop syncing on first failure to avoid wasting attempts
                    if (failedCount >= 3) {
                        Timber.w("Too many sync failures, stopping")
                        return@forEach
                    }
                }
        }
        
        val result = SyncResult(
            synced = syncedCount,
            failed = failedCount,
            total = buffered.size
        )
        
        Timber.i("Sync complete: $result")
        return Result.success(result)
    }
}

/**
 * Sync Result
 */
data class SyncResult(
    val synced: Int,
    val failed: Int,
    val total: Int
) {
    val remaining: Int get() = total - synced - failed
}
