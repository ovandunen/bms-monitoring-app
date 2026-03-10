package com.fleet.bms.application.usecase

import com.fleet.bms.domain.model.BatteryTelemetry
import com.fleet.bms.domain.repository.CanBusPort
import com.fleet.bms.domain.service.AlertEvaluator
import com.fleet.bms.domain.service.AggregationResult
import com.fleet.bms.domain.service.BatteryAlert
import com.fleet.bms.domain.service.TelemetryAggregator
import com.fleet.bms.infrastructure.hardware.protocol.CanProtocolParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import timber.log.Timber

/**
 * Use Case: Collect Telemetry from CAN-Bus
 * 
 * Orchestrates:
 * 1. Reading CAN frames
 * 2. Parsing protocol
 * 3. Aggregating into telemetry
 * 4. Evaluating alerts
 */
class CollectTelemetryUseCase(
    private val canBusPort: CanBusPort,
    private val protocolParser: CanProtocolParser,
    private val telemetryAggregator: TelemetryAggregator,
    private val alertEvaluator: AlertEvaluator
) {
    
    suspend fun execute(): Flow<TelemetryResult> = flow {
        canBusPort.readFrames()
            .mapNotNull { frame -> 
                try {
                    protocolParser.parse(frame)
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse CAN frame: ${frame.id}")
                    null
                }
            }
            .collect { parsedData ->
                when (val result = telemetryAggregator.aggregate(parsedData)) {
                    is AggregationResult.Complete -> {
                        val telemetry = result.telemetry
                        val alerts = alertEvaluator.evaluate(telemetry)
                        
                        Timber.d("Telemetry complete: SOC=${telemetry.stateOfCharge.value}%, " +
                                "Voltage=${telemetry.voltage.value}V, " +
                                "Alerts=${alerts.size}")
                        
                        emit(TelemetryResult.Success(telemetry, alerts))
                        telemetryAggregator.reset()
                    }
                    is AggregationResult.Incomplete -> {
                        emit(TelemetryResult.Partial(result.percentage))
                    }
                }
            }
    }
}

/**
 * Telemetry Result
 */
sealed class TelemetryResult {
    data class Success(
        val telemetry: BatteryTelemetry,
        val alerts: List<BatteryAlert>
    ) : TelemetryResult()
    
    data class Partial(val percentage: Float) : TelemetryResult()
    
    data class Error(val error: Throwable) : TelemetryResult()
}
