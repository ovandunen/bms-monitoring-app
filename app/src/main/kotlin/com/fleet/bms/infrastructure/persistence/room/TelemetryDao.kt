package com.fleet.bms.infrastructure.persistence.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room DAO: Telemetry
 * 
 * Database access for telemetry buffer.
 */
@Dao
interface TelemetryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(telemetry: TelemetryEntity)
    
    @Query("SELECT * FROM telemetry_buffer WHERE isSynced = 0 ORDER BY timestamp ASC")
    suspend fun getAllUnsynced(): List<TelemetryEntity>
    
    @Query("SELECT COUNT(*) FROM telemetry_buffer WHERE isSynced = 0")
    suspend fun getUnsyncedCount(): Int
    
    @Query("DELETE FROM telemetry_buffer WHERE messageId = :messageId")
    suspend fun delete(messageId: String)
    
    @Query("DELETE FROM telemetry_buffer")
    suspend fun deleteAll()
    
    @Query("SELECT MIN(timestamp) FROM telemetry_buffer WHERE isSynced = 0")
    suspend fun getOldestTimestamp(): Long?
    
    @Query("DELETE FROM telemetry_buffer WHERE isSynced = 1 AND createdAt < :olderThan")
    suspend fun deleteSyncedOlderThan(olderThan: Long)
}
