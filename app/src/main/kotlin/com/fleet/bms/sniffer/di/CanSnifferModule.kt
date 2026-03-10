package com.fleet.bms.sniffer.di

import android.content.Context
import com.fleet.bms.sniffer.domain.usecase.CanSniffingUseCase
import com.fleet.bms.sniffer.domain.usecase.DetectCanCandidatesUseCase
import com.fleet.bms.sniffer.infrastructure.CanIdRegistry
import com.fleet.bms.sniffer.infrastructure.CanLogExporter
import com.fleet.bms.sniffer.infrastructure.CanSocketReader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object CanSnifferModule {

    @Provides
    @Singleton
    fun provideCanSocketReader(): CanSocketReader =
        CanSocketReader(interfaceName = "can0")

    @Provides
    @Singleton
    fun provideCanIdRegistry(): CanIdRegistry =
        CanIdRegistry(activeThresholdMs = 5000)

    @Provides
    @Singleton
    fun provideCanLogExporter(
        @ApplicationContext context: Context
    ): CanLogExporter {
        val basePath = (context.getExternalFilesDir(null)?.absolutePath ?: "/sdcard") + "/can_logs"
        return CanLogExporter(basePath = basePath)
    }

    @Provides
    @Singleton
    @CanSnifferScope
    fun provideCanSnifferScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    fun provideCanSniffingUseCase(
        socketReader: CanSocketReader,
        registry: CanIdRegistry,
        exporter: CanLogExporter,
        @CanSnifferScope scope: CoroutineScope
    ): CanSniffingUseCase =
        CanSniffingUseCase(socketReader, registry, exporter, scope)

    @Provides
    @Singleton
    fun provideDetectCanCandidatesUseCase(registry: CanIdRegistry): DetectCanCandidatesUseCase =
        DetectCanCandidatesUseCase(registry)
}
