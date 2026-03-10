package com.fleet.bms.infrastructure.android.di

import android.content.Context
import com.fleet.bms.domain.repository.CanBusConfig
import com.fleet.bms.infrastructure.config.BmsConfig
import com.fleet.bms.infrastructure.config.ConfigLoader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt Module: Configuration
 *
 * Loads BMS config from assets/bms_config.yml
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideBmsConfig(
        @ApplicationContext context: Context
    ): BmsConfig {
        return ConfigLoader.load(context)
    }

    @Provides
    @Singleton
    fun provideCanBusConfig(bmsConfig: BmsConfig): CanBusConfig {
        return CanBusConfig(
            bitrate = bmsConfig.canBus.bitrate,
            samplePoint = bmsConfig.canBus.samplePoint.toFloat(),
            listenOnly = bmsConfig.canBus.listenOnly
        )
    }
}
