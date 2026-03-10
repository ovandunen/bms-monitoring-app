package com.fleet.bms.infrastructure.persistence.room

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Room Database: Telemetry
 * 
 * Local storage for offline buffering of telemetry data.
 */
@Database(
    entities = [TelemetryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TelemetryDatabase : RoomDatabase() {
    abstract fun telemetryDao(): TelemetryDao
}
