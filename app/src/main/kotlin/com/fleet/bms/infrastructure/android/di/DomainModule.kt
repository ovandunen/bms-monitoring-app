package com.fleet.bms.infrastructure.android.di

import com.fleet.bms.domain.model.BatteryPackId
import com.fleet.bms.domain.model.VehicleId
import com.fleet.bms.domain.service.AlertEvaluator
import com.fleet.bms.domain.service.TelemetryAggregator
import com.fleet.bms.infrastructure.android.preferences.BmsSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
    fun provideBatteryPackId(repository: BmsSettingsRepository): BatteryPackId {
        val id = runBlocking(Dispatchers.IO) {
            repository.load().batteryPackId
        }
        return BatteryPackId(id)
    }
    
    @Provides
    @Singleton
    fun provideVehicleId(repository: BmsSettingsRepository): VehicleId {
        val id = runBlocking(Dispatchers.IO) {
            repository.load().vehicleId
        }
        return VehicleId(id)
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
