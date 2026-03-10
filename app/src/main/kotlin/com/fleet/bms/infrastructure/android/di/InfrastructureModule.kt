package com.fleet.bms.infrastructure.android.di

import android.content.Context
import androidx.room.Room
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.domain.repository.TelemetryPublisherPort
import com.fleet.bms.domain.repository.TelemetryStoragePort
import com.fleet.bms.infrastructure.config.BmsConfig
import com.fleet.bms.infrastructure.hardware.adapter.BmsProtocolFactory
import com.fleet.bms.infrastructure.hardware.adapter.CanBusAdapterFactory
import com.fleet.bms.infrastructure.hardware.protocol.CanProtocolParser
import com.fleet.bms.infrastructure.messaging.mqtt.MqttConfig
import com.fleet.bms.infrastructure.messaging.mqtt.MqttTelemetryPublisher
import com.fleet.bms.infrastructure.persistence.room.LocalTelemetryRepository
import com.fleet.bms.infrastructure.persistence.room.TelemetryDao
import com.fleet.bms.infrastructure.persistence.room.TelemetryDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID
import javax.inject.Singleton

/**
 * Hilt Module: Infrastructure Layer Dependencies
 *
 * Provides adapters that implement domain ports.
 * Adapter and protocol selection is driven by bms_config.yml.
 */
@Module
@InstallIn(SingletonComponent::class)
object InfrastructureModule {

    @Provides
    @Singleton
    fun provideCanBusPort(
        @ApplicationContext context: Context,
        bmsConfig: BmsConfig
    ): CanBusPort {
        return CanBusAdapterFactory.create(context, bmsConfig)
    }

    @Provides
    @Singleton
    fun provideCanProtocolParser(bmsConfig: BmsConfig): CanProtocolParser {
        return BmsProtocolFactory.create(bmsConfig)
    }
    
    @Provides
    @Singleton
    fun provideMqttConfig(
        @ApplicationContext context: Context
    ): MqttConfig {
        // TODO: Load from SharedPreferences or DataStore
        val prefs = context.getSharedPreferences("bms_config", Context.MODE_PRIVATE)
        
        return MqttConfig(
            brokerUrl = prefs.getString("mqtt_broker", "tcp://mqtt.fleet.cloud:1883")
                ?: "tcp://localhost:1883",
            clientId = "android_${UUID.randomUUID()}",
            username = prefs.getString("mqtt_username", "backend") ?: "backend",
            password = prefs.getString("mqtt_password", "backend123") ?: "backend123"
        )
    }
    
    @Provides
    @Singleton
    fun provideTelemetryPublisher(
        config: MqttConfig
    ): TelemetryPublisherPort {
        return MqttTelemetryPublisher(config)
    }
    
    @Provides
    @Singleton
    fun provideTelemetryDatabase(
        @ApplicationContext context: Context
    ): TelemetryDatabase {
        return Room.databaseBuilder(
            context,
            TelemetryDatabase::class.java,
            "telemetry-db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    fun provideTelemetryDao(database: TelemetryDatabase): TelemetryDao {
        return database.telemetryDao()
    }
    
    @Provides
    @Singleton
    fun provideTelemetryStoragePort(
        dao: TelemetryDao
    ): TelemetryStoragePort {
        return LocalTelemetryRepository(dao)
    }
}
