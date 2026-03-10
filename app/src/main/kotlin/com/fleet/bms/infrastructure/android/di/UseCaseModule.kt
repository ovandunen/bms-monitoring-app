package com.fleet.bms.infrastructure.android.di

import com.fleet.bms.application.usecase.*
import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import com.fleet.bms.domain.repository.TelemetryStoragePort
import com.fleet.bms.domain.service.AlertEvaluator
import com.fleet.bms.domain.service.TelemetryAggregator
import com.fleet.bms.infrastructure.hardware.protocol.CanProtocolParser
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt Module: Use Case Dependencies
 * 
 * Provides use cases with ViewModelScoped lifecycle.
 * Use cases coordinate domain logic.
 */
@Module
@InstallIn(ViewModelComponent::class)
object UseCaseModule {
    
    @Provides
    @ViewModelScoped
    fun provideCollectTelemetryUseCase(
        canBusPort: CanBusPort,
        protocolParser: CanProtocolParser,
        telemetryAggregator: TelemetryAggregator,
        alertEvaluator: AlertEvaluator
    ): CollectTelemetryUseCase {
        return CollectTelemetryUseCase(
            canBusPort = canBusPort,
            protocolParser = protocolParser,
            telemetryAggregator = telemetryAggregator,
            alertEvaluator = alertEvaluator
        )
    }
    
    @Provides
    @ViewModelScoped
    fun providePublishTelemetryUseCase(
        publisherPort: TelemetryPublisherPort,
        storagePort: TelemetryStoragePort
    ): PublishTelemetryUseCase {
        return PublishTelemetryUseCase(
            publisherPort = publisherPort,
            storagePort = storagePort
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideSyncBufferedDataUseCase(
        publisherPort: TelemetryPublisherPort,
        storagePort: TelemetryStoragePort
    ): SyncBufferedDataUseCase {
        return SyncBufferedDataUseCase(
            publisherPort = publisherPort,
            storagePort = storagePort
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideStartMonitoringUseCase(
        canBusPort: CanBusPort,
        publisherPort: TelemetryPublisherPort,
        canBusConfig: CanBusConfig
    ): StartMonitoringUseCase {
        return StartMonitoringUseCase(
            canBusPort = canBusPort,
            publisherPort = publisherPort,
            canBusConfig = canBusConfig
        )
    }
    
    @Provides
    @ViewModelScoped
    fun provideStopMonitoringUseCase(
        canBusPort: CanBusPort,
        publisherPort: TelemetryPublisherPort
    ): StopMonitoringUseCase {
        return StopMonitoringUseCase(
            canBusPort = canBusPort,
            publisherPort = publisherPort
        )
    }
}
