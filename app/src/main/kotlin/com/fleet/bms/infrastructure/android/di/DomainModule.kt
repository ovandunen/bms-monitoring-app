package com.fleet.bms.infrastructure.android.di

import android.content.Context
import com.fleet.bms.domain.model.BatteryPackId
import com.fleet.bms.domain.model.VehicleId
import com.fleet.bms.domain.service.AlertEvaluator
import com.fleet.bms.domain.service.TelemetryAggregator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module: Domain Layer Dependencies
 * 
 * Provides domain services and configuration.
 * Domain layer has NO Android dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DomainModule {
    
    @Provides
    @Singleton
    fun provideBatteryPackId(
        @ApplicationContext context: Context
    ): BatteryPackId {
        // TODO: Load from SharedPreferences or DataStore
        // For now, use a default ID
        val prefs = context.getSharedPreferences("bms_config", Context.MODE_PRIVATE)
        val batteryId = prefs.getString("battery_pack_id", "550e8400-e29b-41d4-a716-446655440000")
        return BatteryPackId(batteryId!!)
    }
    
    @Provides
    @Singleton
    fun provideVehicleId(
        @ApplicationContext context: Context
    ): VehicleId {
        // TODO: Load from configuration
        val prefs = context.getSharedPreferences("bms_config", Context.MODE_PRIVATE)
        val vehicleId = prefs.getString("vehicle_id", "vehicle_001")
        return VehicleId(vehicleId!!)
    }
    
    @Provides
    @Singleton
    fun provideTelemetryAggregator(
        batteryPackId: BatteryPackId,
        vehicleId: VehicleId
    ): TelemetryAggregator {
        return TelemetryAggregator(
            batteryPackId = batteryPackId,
            vehicleId = vehicleId
        )
    }
    
    @Provides
    @Singleton
    fun provideAlertEvaluator(): AlertEvaluator {
        return AlertEvaluator()
    }
}
