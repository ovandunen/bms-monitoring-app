package com.fleet.bms.infrastructure.persistence.room

import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.TelemetryStoragePort
import timber.log.Timber

/**
 * Adapter: Local Telemetry Repository
 * 
 * Implements TelemetryStoragePort using Room database.
 */
class LocalTelemetryRepository(
    private val dao: TelemetryDao
) : TelemetryStoragePort {
    
    override suspend fun store(telemetry: BatteryTelemetry): Result<Unit> {
        return try {
            val entity = TelemetryEntity.fromDomain(telemetry)
            dao.insert(entity)
            Timber.d("Stored telemetry: ${telemetry.messageId.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to store telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun getBuffered(): List<BatteryTelemetry> {
        return try {
            val entities = dao.getAllUnsynced()
            entities.map { it.toDomain() }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get buffered telemetry")
            emptyList()
        }
    }
    
    override suspend fun removeBuffered(telemetry: BatteryTelemetry): Result<Unit> {
        return try {
            dao.delete(telemetry.messageId.value)
            Timber.d("Removed buffered telemetry: ${telemetry.messageId.value}")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove buffered telemetry")
            Result.failure(e)
        }
    }
    
    override suspend fun clearAll() {
        try {
            dao.deleteAll()
            Timber.i("Cleared all buffered telemetry")
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear buffered telemetry")
        }
    }
    
    override suspend fun getBufferSize(): Int {
        return try {
            dao.getUnsyncedCount()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get buffer size")
            0
        }
    }
    
    override suspend fun getOldestBufferedTimestamp(): Long? {
        return try {
            dao.getOldestTimestamp()
        } catch (e: Exception) {
            Timber.e(e, "Failed to get oldest timestamp")
            null
        }
    }
    
    /**
     * Clean up old synced records (e.g., older than 7 days)
     */
    suspend fun cleanupOldSynced(daysOld: Int = 7) {
        try {
            val cutoff = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
            dao.deleteSyncedOlderThan(cutoff)
            Timber.d("Cleaned up synced telemetry older than $daysOld days")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old synced telemetry")
        }
    }
}
